/*
 * #%L
 * Gravia :: Runtime :: Embedded
 * %%
 * Copyright (C) 2013 - 2014 JBoss by Red Hat
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
package io.fabric8.test.smoke;

import static io.fabric8.api.Constants.DEFAULT_PROFILE_VERSION;
import io.fabric8.api.ConfigurationProfileItemBuilder;
import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.Container.State;
import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.ContainerManager;
import io.fabric8.api.CreateOptions;
import io.fabric8.api.NullProfileItemBuilder;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileEvent;
import io.fabric8.api.ProfileEventListener;
import io.fabric8.api.ProfileItemBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProvisionEvent;
import io.fabric8.api.ProvisionEventListener;
import io.fabric8.api.ServiceLocator;
import io.fabric8.spi.DefaultContainerBuilder;

import java.util.Collections;
import java.util.Dictionary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test profile items functionality.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Mar-2014
 */
public abstract class ProfileItemsTests  {

    @Before
    public void preConditions() {
        TestConditions.assertPreConditions();
    }

    @After
    public void postConditions() {
        TestConditions.assertPostConditions();
    }

    @Test
    public void testConfigurationItem() throws Exception {

        Runtime runtime = RuntimeLocator.getRequiredRuntime();
        ModuleContext syscontext = runtime.getModuleContext();

        ContainerManager cntManager = ServiceLocator.getRequiredService(ContainerManager.class);
        ProfileManager prfManager = ServiceLocator.getRequiredService(ProfileManager.class);

        // Create container A
        DefaultContainerBuilder cntBuilder = new DefaultContainerBuilder();
        CreateOptions options = cntBuilder.addIdentity("cntA").getCreateOptions();
        Container cntA = cntManager.createContainer(options);

        // Verify parent identity
        ContainerIdentity idA = cntA.getIdentity();
        Assert.assertEquals("cntA", idA.getSymbolicName());
        Assert.assertEquals("default", cntA.getAttribute(Container.ATTKEY_CONFIG_TOKEN));

        // Start container A
        cntA = cntManager.startContainer(idA, null);
        Assert.assertSame(State.STARTED, cntA.getState());
        Assert.assertEquals(DEFAULT_PROFILE_VERSION, cntA.getProfileVersion());

        // Build an update profile
        ProfileBuilder profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("default");
        ConfigurationProfileItemBuilder configBuilder = profileBuilder.getItemBuilder(ConfigurationProfileItemBuilder.class);
        configBuilder.addIdentity("some.pid");
        configBuilder.setConfiguration(Collections.singletonMap("foo", (Object) "bar"));
        profileBuilder.addProfileItem(configBuilder.getProfileItem());
        Profile updateProfile = profileBuilder.getProfile();

        // Setup the profile listener
        final AtomicReference<CountDownLatch> latchA = new AtomicReference<CountDownLatch>(new CountDownLatch(1));
        ProfileEventListener profileListener = new ProfileEventListener() {
            @Override
            public void processEvent(ProfileEvent event) {
                String symbolicName = event.getSource().getIdentity().getSymbolicName();
                if (event.getType() == ProfileEvent.EventType.UPDATED && "default".equals(symbolicName)) {
                    latchA.get().countDown();
                }
            }
        };

        // Setup the provision listener
        final AtomicReference<CountDownLatch> latchB = new AtomicReference<CountDownLatch>(new CountDownLatch(2));
        ProvisionEventListener provisionListener = new ProvisionEventListener() {
            @Override
            public void processEvent(ProvisionEvent event) {
                String symbolicName = event.getProfile().getIdentity().getSymbolicName();
                if (event.getType() == ProvisionEvent.EventType.REMOVED && "default".equals(symbolicName)) {
                    latchB.get().countDown();
                }
                if (event.getType() == ProvisionEvent.EventType.PROVISIONED && "default".equals(symbolicName)) {
                    latchB.get().countDown();
                }
            }
        };
        ServiceRegistration<ProvisionEventListener> sregB = syscontext.registerService(ProvisionEventListener.class, provisionListener, null);

        // Verify that the configuration does not exist
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        Configuration config = configAdmin.getConfiguration("some.pid");
        Assert.assertNull("Configuration null", config.getProperties());

        // Update the default profile
        Profile defaultProfile = prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertEquals("One item", 2, defaultProfile.getProfileItems(null).size());

        // Verify the configuration
        config = configAdmin.getConfiguration("some.pid");
        Dictionary<String, Object> props = config.getProperties();
        Assert.assertEquals("bar", props.get("foo"));

        // Build an update profile
        profileBuilder = ProfileBuilder.Factory.create();
        profileBuilder.addIdentity("default");
        ProfileItemBuilder<?> itemBuilder = profileBuilder.getItemBuilder(NullProfileItemBuilder.class);
        profileBuilder.addProfileItem(itemBuilder.addIdentity("some.pid").getProfileItem());
        updateProfile = profileBuilder.getProfile();

        // Update the default profile
        latchA.set(new CountDownLatch(1));
        latchB.set(new CountDownLatch(2));
        defaultProfile = prfManager.updateProfile(Constants.DEFAULT_PROFILE_VERSION, updateProfile, profileListener);
        Assert.assertTrue("ProfileEvent received", latchA.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertTrue("ProvisionEvent received", latchB.get().await(200, TimeUnit.MILLISECONDS));
        Assert.assertEquals("One item", 1, defaultProfile.getProfileItems(null).size());

        sregB.unregister();

        cntA = cntManager.destroyContainer(idA);
        Assert.assertSame(State.DESTROYED, cntA.getState());
    }
}