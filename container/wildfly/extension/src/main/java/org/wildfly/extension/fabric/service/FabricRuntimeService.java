/*
 * #%L
 * Fabric8 :: Container :: WildFly :: Extension
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

package org.wildfly.extension.fabric.service;


import java.util.Properties;

import org.wildfly.extension.gravia.service.RuntimeService;

/**
 * Service responsible for creating and managing the life-cycle of the gravia subsystem.
 *
 * @since 19-Apr-2013
 */
public final class FabricRuntimeService extends RuntimeService {

    @Override
    protected Properties getRuntimeProperties() {
        Properties properties = super.getRuntimeProperties();
        // [TODO] Derive port from wildfly config
        // https://issues.jboss.org/browse/FABRIC-762
        properties.setProperty("org.osgi.service.http.port", "8080");
        return properties;
    }
}
