/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.container.tomcat;

import io.fabric8.api.Constants;
import io.fabric8.spi.AbstractManagedContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.remote.JMXConnector;

import org.jboss.gravia.runtime.RuntimeType;


/**
 * The managed root container
 *
 * @since 26-Feb-2014
 */
public final class TomcatManagedContainer extends AbstractManagedContainer<TomcatCreateOptions> {

    TomcatManagedContainer(TomcatCreateOptions options) {
        super(options);
    }

    @Override
    protected void doConfigure() throws Exception {
        String jmxServerURL = "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + activeJMXPort() + "/jmxrmi";
        putAttribute(Constants.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerURL);
    }

    @Override
    protected void doStart() throws Exception {

        File catalinaHome = getContainerHome();
        if (!catalinaHome.isDirectory())
            throw new IllegalStateException("Not a valid Tomcat home dir: " + catalinaHome);

        // Construct a command to execute
        List<String> cmd = new ArrayList<String>();
        cmd.add("java");

        cmd.add("-Dcom.sun.management.jmxremote.port=" + activeJMXPort());
        cmd.add("-Dcom.sun.management.jmxremote.ssl=false");
        cmd.add("-Dcom.sun.management.jmxremote.authenticate=false");

        String javaArgs = getCreateOptions().getJavaVmArguments();
        cmd.addAll(Arrays.asList(javaArgs.split("\\s+")));

        String absolutePath = catalinaHome.getAbsolutePath();
        String CLASS_PATH = absolutePath + "/bin/bootstrap.jar" + File.pathSeparator;
        CLASS_PATH += absolutePath + "/bin/tomcat-juli.jar";

        cmd.add("-classpath");
        cmd.add(CLASS_PATH);
        cmd.add("-Djava.endorsed.dirs=" + absolutePath + "/endorsed");
        cmd.add("-Dcatalina.base=" + absolutePath);
        cmd.add("-Dcatalina.home=" + absolutePath);
        cmd.add("-Djava.io.tmpdir=" + absolutePath + "/temp");
        cmd.add("org.apache.catalina.startup.Bootstrap");
        cmd.add("start");

        // execute command
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(new File(catalinaHome, "bin"));
        startProcess(processBuilder);
    }

    @Override
    public JMXConnector getJMXConnector(Map<String, Object> env, long timeout, TimeUnit unit) {
        JMXConnector connector;
        if (RuntimeType.getRuntimeType() == RuntimeType.WILDFLY) {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(null);
                connector = super.getJMXConnector(env, timeout, unit);
            } finally {
                Thread.currentThread().setContextClassLoader(contextLoader);
            }
        } else {
            connector = super.getJMXConnector(env, timeout, unit);
        }
        return connector;
    }

    private int activeJMXPort() {
        int jmxPort = getCreateOptions().getJmxPort();
        if (jmxPort == TomcatCreateOptions.DEFAULT_JMX_PORT) {
            jmxPort = freePortValue(jmxPort);
        }
        return jmxPort;
    }
}