/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package io.fabric8.spi.process;

import static io.fabric8.spi.Agent.NOTIFICATION_TYPE_AGENT_REGISTRATION;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.process.ProcessOptions;
import io.fabric8.spi.Agent;
import io.fabric8.spi.AgentRegistration;
import io.fabric8.spi.process.ManagedProcess.State;
import io.fabric8.spi.utils.HostUtils;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jboss.gravia.repository.DefaultMavenDelegateRepository;
import org.jboss.gravia.repository.MavenDelegateRepository;
import org.jboss.gravia.resource.MavenCoordinates;
import org.jboss.gravia.resource.Resource;
import org.jboss.gravia.resource.ResourceContent;
import org.jboss.gravia.runtime.LifecycleException;
import org.jboss.gravia.runtime.spi.PropertiesProvider;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * The managed root container
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Feb-2014
 */
public abstract class AbstractProcessHandler implements ProcessHandler {

    private final MavenDelegateRepository mavenRepository;
    private MutableManagedProcess managedProcess;
    private Process process;

    protected AbstractProcessHandler(PropertiesProvider propsProvider) {
        IllegalArgumentAssertion.assertNotNull(propsProvider, "propsProvider");
        this.mavenRepository = new DefaultMavenDelegateRepository(propsProvider);
    }

    @Override
    public final ManagedProcess create(AgentRegistration agentReg, ProcessOptions options, ProcessIdentity identity) {

        File targetDir = options.getTargetPath().toAbsolutePath().toFile();
        IllegalStateAssertion.assertTrue(targetDir.isDirectory() || targetDir.mkdirs(), "Cannot create target dir: " + targetDir);

        File homeDir = null;
        for (MavenCoordinates artefact : options.getMavenCoordinates()) {
            Resource resource = mavenRepository.findMavenResource(artefact);
            IllegalStateAssertion.assertNotNull(resource, "Cannot find maven resource: " + artefact);

            ResourceContent content = resource.adapt(ResourceContent.class);
            IllegalStateAssertion.assertNotNull(content, "Cannot obtain resource content for: " + artefact);

            try {
                ArchiveInputStream ais;
                if ("tar.gz".equals(artefact.getType())) {
                    InputStream inputStream = content.getContent();
                    ais = new TarArchiveInputStream(new GZIPInputStream(inputStream));
                } else {
                    InputStream inputStream = content.getContent();
                    ais = new ArchiveStreamFactory().createArchiveInputStream(artefact.getType(), inputStream);
                }
                ArchiveEntry entry = null;
                boolean needContainerHome = homeDir == null;
                while ((entry = ais.getNextEntry()) != null) {
                    File targetFile;
                    if (needContainerHome) {
                        targetFile = new File(targetDir, entry.getName());
                    } else {
                        targetFile = new File(homeDir, entry.getName());
                    }
                    if (!entry.isDirectory()) {
                        File parentDir = targetFile.getParentFile();
                        IllegalStateAssertion.assertTrue(parentDir.exists() || parentDir.mkdirs(), "Cannot create target directory: " + parentDir);

                        FileOutputStream fos = new FileOutputStream(targetFile);
                        copyStream(ais, fos);
                        fos.close();

                        if (needContainerHome && homeDir == null) {
                            File currentDir = parentDir;
                            while (!currentDir.getParentFile().equals(targetDir)) {
                                currentDir = currentDir.getParentFile();
                            }
                            homeDir = currentDir;
                        }
                    }
                }
                ais.close();
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot extract artefact: " + artefact, ex);
            }
        }

        managedProcess = new DefaultManagedProcess(identity, options, homeDir.toPath(), State.CREATED);
        managedProcess.addAttribute(ManagedProcess.ATTRIBUTE_KEY_AGENT_REGISTRATION, agentReg);
        managedProcess.addAttribute(ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_SERVER_URL, agentReg.getJmxServerUrl());
        managedProcess.addAttribute(ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_USERNAME, agentReg.getJmxUsername());
        managedProcess.addAttribute(ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_PASSWORD, agentReg.getJmxPassword());
        try {
            doConfigure(managedProcess);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot configure container", ex);
        }
        return new ImmutableManagedProcess(managedProcess);
    }

    @Override
    public final Future<ManagedProcess> start() {
        State state = managedProcess.getState();
        assertNotDestroyed(state);

        // Setup a call back notification to get the JMX connection of the started process
        final CountDownLatch latch = new CountDownLatch(1);
        String jmxAgentServiceURL = managedProcess.getAttribute(ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_SERVER_URL);
        String jmxAgentUsername = managedProcess.getAttribute(ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_USERNAME);
        String jmxAgentPassword = managedProcess.getAttribute(ContainerAttributes.ATTRIBUTE_KEY_AGENT_JMX_PASSWORD);
        JMXConnector connector = ManagementUtils.getJMXConnector(jmxAgentServiceURL, jmxAgentUsername, jmxAgentPassword, 200, TimeUnit.MILLISECONDS);
        try {
            final MBeanServerConnection server = connector.getMBeanServerConnection();
            server.addNotificationListener(Agent.OBJECT_NAME, new NotificationListener() {
                @Override
                public void handleNotification(Notification notification, Object handback) {
                    String eventType = notification.getType();
                    if (NOTIFICATION_TYPE_AGENT_REGISTRATION.equals(eventType)) {
                        AgentRegistration agentReg = (AgentRegistration) notification.getSource();
                        String agentName = agentReg.getIdentity().getName();
                        String procName = (String) handback;
                        if (agentName.equals(procName)) {
                            try {
                                server.removeNotificationListener(Agent.OBJECT_NAME, this);
                            } catch (Exception ex) {
                                // ignore
                            }
                            latch.countDown();
                        }
                    }
                }
            }, null, managedProcess.getIdentity().getName());
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            IOUtils.safeClose(connector);
        }

        try {
            if (state == State.CREATED || state == State.STOPPED) {
                doStart(managedProcess);
                IllegalStateAssertion.assertNotNull(process, "No process created");
                managedProcess.setState(State.STARTED);
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot start container", ex);
        }

        return new ProcessFuture(managedProcess); //, latch);
    }

    @Override
    public final Future<ManagedProcess> stop() {
        State state = managedProcess.getState();
        assertNotDestroyed(state);
        try {
            if (state == State.STARTED) {
                doStop(managedProcess);
                managedProcess.setState(State.STOPPED);
                destroyProcess();
            }
        } catch (Exception ex) {
            throw new LifecycleException("Cannot stop container", ex);
        }
        return new ProcessFuture(managedProcess);
    }

    @Override
    public final ManagedProcess destroy() {
        State state = managedProcess.getState();
        assertNotDestroyed(state);
        if (state == State.STARTED) {
            try {
                stop();
            } catch (Exception ex) {
                // ignore
            }
        }
        try {
            doDestroy(managedProcess);
        } catch (Exception ex) {
            throw new LifecycleException("Cannot destroy container", ex);
        } finally {
            managedProcess.setState(State.DESTROYED);
        }
        return new ImmutableManagedProcess(managedProcess);
    }

    private void assertNotDestroyed(State state) {
        IllegalStateAssertion.assertFalse(state == State.DESTROYED, "Cannot start container in state: " + state);
    }

    protected void doConfigure(MutableManagedProcess process) throws Exception {
    }

    protected void doStart(MutableManagedProcess process) throws Exception {
    }

    protected void doStop(MutableManagedProcess process) throws Exception {
    }

    protected void doDestroy(MutableManagedProcess process) throws Exception {
    }

    protected void startProcess(ProcessBuilder processBuilder, ProcessOptions options) throws IOException {
        process = processBuilder.start();
        new Thread(new ConsoleConsumer(process, options)).start();
    }

    protected void destroyProcess() throws Exception {
        if (process != null) {
            process.destroy();
            process.waitFor();
        }
    }

    protected final int nextAvailablePort(int portValue) {
        return nextAvailablePort(portValue, null);
    }

    protected int nextAvailablePort(int portValue, InetAddress bindAddr) {
        return HostUtils.nextAvailablePort(portValue, bindAddr);
    }

    // [TODO] call IOUtils.copyStream()
    private static long copyStream(InputStream input, OutputStream output) throws IOException {
        return copyStream(input, output, 8024);
    }

    private static long copyStream(InputStream input, OutputStream output, int buffersize) throws IOException {
        final byte[] buffer = new byte[buffersize];
        int n = 0;
        long count=0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Runnable that consumes the output of the process.
     * If nothing consumes the output the container may hang on some platforms
     */
    public static class ConsoleConsumer implements Runnable {

        private final Process process;
        private final ProcessOptions options;

        public ConsoleConsumer(Process process, ProcessOptions options) {
            this.process = process;
            this.options = options;
        }

        @Override
        public void run() {
            final InputStream stream = process.getInputStream();
            try {
                byte[] buf = new byte[32];
                int num;
                // Do not try reading a line cos it considers '\r' end of line
                while ((num = stream.read(buf)) != -1) {
                    if (options.isOutputToConsole())
                        System.out.write(buf, 0, num);
                }
            } catch (IOException e) {
            }
        }
    }
}
