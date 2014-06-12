/*
 * #%L
 * Fabric8 :: Testsuite :: Smoke :: Embedded
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
package io.fabric8.test.smoke.embedded;

import io.fabric8.test.embedded.support.EmbeddedTestSupport;
import io.fabric8.test.embedded.support.ServiceConsumer;
import io.fabric8.test.embedded.support.Tracer;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Hashtable;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;


/**
 */
public class ComponentTest {


    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }


    @Test
    public void test() throws Exception {
        ConfigurationAdmin ca = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        ca.getConfiguration("service-provider", null).update(new Hashtable<String, Object>(){{
            put("version", "1");
        }});
        ca.getConfiguration("service-consumer", null).update(new Hashtable<String, Object>() {{
            put("version", "1");
        }});

        ServiceConsumer consumer = ServiceLocator.awaitService(ServiceConsumer.class, 10, SECONDS);
        assertEquals("1", consumer.providerVersion());
//        assertEquals("1", consumer.consumerVersion());

        // Don't care about events up to now..
        Tracer.traces.clear();
        assertEquals(0, Tracer.count("io.fabric8.test.embedded.support.ServiceConsumerImpl:activate"));

        System.out.println("Updating a configuration.");
        ca.getConfiguration("service-provider", null).update(new Hashtable<String, Object>() {{
            put("version", "2");
        }});
        Thread.sleep(500);

        consumer = ServiceLocator.awaitService(ServiceConsumer.class, 10, SECONDS);
        assertEquals("2", consumer.providerVersion());

        assertEquals("Wrong number of activations: "+Tracer.traces.toString(), 1, Tracer.count("io.fabric8.test.embedded.support.ServiceConsumerImpl:activate"));

    }

}
