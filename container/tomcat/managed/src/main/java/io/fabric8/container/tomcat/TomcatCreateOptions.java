/*
 * #%L
 * Fabric8 :: Container :: Karaf :: Managed
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
package io.fabric8.container.tomcat;

import io.fabric8.api.ContainerIdentity;
import io.fabric8.api.process.ManagedCreateOptions;
import io.fabric8.spi.MutableCreateOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.gravia.resource.Version;
import org.jboss.gravia.utils.IllegalArgumentAssertion;


public final class TomcatCreateOptions extends TomcatProcessOptions implements ManagedCreateOptions, MutableCreateOptions {

    private List<String> profiles = new ArrayList<>();
    private Version version = Version.emptyVersion;

    @Override
    public ContainerIdentity getIdentity() {
        return ContainerIdentity.create(getIdentityPrefix());
    }

    @Override
    public Version getProfileVersion() {
        return version;
    }

    @Override
    public List<String> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    @Override
    public void setIdentity(ContainerIdentity identity) {
        assertMutable();
        IllegalArgumentAssertion.assertNotNull(identity, "identity");
        setIdentityPrefix(identity.getSymbolicName());
    }

    @Override
    public void setVersion(Version version) {
        assertMutable();
        IllegalArgumentAssertion.assertNotNull(version, "version");
        this.version = version;
    }

    @Override
    public void setProfiles(List<String> profiles) {
        assertMutable();
        IllegalArgumentAssertion.assertNotNull(profiles, "profiles");
        this.profiles = new ArrayList<>(profiles);
    }
}
