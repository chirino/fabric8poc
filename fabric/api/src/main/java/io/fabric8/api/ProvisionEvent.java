/*
 * #%L
 * Fabric8 :: API
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
package io.fabric8.api;

import org.jboss.gravia.utils.IllegalArgumentAssertion;



/**
 * A provisioning event
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
@SuppressWarnings("serial")
public class ProvisionEvent extends FabricEvent<Container, ProvisionEvent.EventType> {

    public enum EventType {
        PROVISIONING, PROVISIONED, ERROR
    }

    private final Profile profile;

    public ProvisionEvent(Container source, EventType type, Profile profile) {
        this(source, type, profile, null);
    }

    public ProvisionEvent(Container source, EventType type, Profile profile, Throwable error) {
        super(source, type, error);
        IllegalArgumentAssertion.assertNotNull(profile, "profile");
        this.profile = profile;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public String toString() {
        return "ProvisionEvent[source=" + getSource().getIdentity() + ",profile=" + profile.getIdentity() + ",type=" + getType() + "]";
    }
}
