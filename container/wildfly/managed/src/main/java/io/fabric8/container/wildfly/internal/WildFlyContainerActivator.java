/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Managed
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

package io.fabric8.container.wildfly.internal;

import io.fabric8.container.wildfly.WildFlyProcessHandler;
import io.fabric8.spi.process.ProcessHandler;

import org.jboss.gravia.runtime.ModuleActivator;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.ServiceRegistration;

/**
 * The WildFly container activator
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Apr-2014
 */
public final class WildFlyContainerActivator implements ModuleActivator {

    private ServiceRegistration<?> registration;

    @Override
    public void start(ModuleContext context) throws Exception {
        registration = context.registerService(ProcessHandler.class, new WildFlyProcessHandler(), null);
    }

    @Override
    public void stop(ModuleContext context) throws Exception {
        registration.unregister();
    }
}
