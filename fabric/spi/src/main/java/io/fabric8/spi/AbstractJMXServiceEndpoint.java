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
package io.fabric8.spi;

import io.fabric8.api.AttributeKey;
import io.fabric8.api.ContainerAttributes;
import io.fabric8.api.JMXServiceEndpoint;
import io.fabric8.api.ServiceEndpointIdentity;
import io.fabric8.spi.utils.ManagementUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

/**
 * An abstract JMX service endpoint
 *
 * @author thomas.diesler@jboss.com
 * @since 06-Jun-2014
 */
public class AbstractJMXServiceEndpoint extends AbstractServiceEndpoint<JMXServiceEndpoint> implements JMXServiceEndpoint {

    public AbstractJMXServiceEndpoint(ServiceEndpointIdentity<JMXServiceEndpoint> identity, Map<AttributeKey<?>, Object> attributes) {
        super(identity, attributes);
    }

    public AbstractJMXServiceEndpoint(ServiceEndpointIdentity<JMXServiceEndpoint> identity, String jmxServerUrl) {
        super(identity, getJmxServerAttributes(jmxServerUrl));
    }

    private static Map<AttributeKey<?>, Object> getJmxServerAttributes(String jmxServerUrl) {
        if (jmxServerUrl != null) {
            return Collections.<AttributeKey<?>, Object> singletonMap(ContainerAttributes.ATTRIBUTE_KEY_JMX_SERVER_URL, jmxServerUrl);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public JMXConnector getJMXConnector(String jmxUsername, String jmxPassword, long timeout, TimeUnit unit) {
        return ManagementUtils.getJMXConnector(this, jmxUsername, jmxPassword, timeout, unit);
    }

    @Override
    public <T> T getMBeanProxy(MBeanServerConnection server, ObjectName oname, Class<T> type) throws IOException {
        return ManagementUtils.getMBeanProxy(server, oname, type);
    }
}
