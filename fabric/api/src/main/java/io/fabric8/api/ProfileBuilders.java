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

import org.jboss.gravia.resource.Version;

/**
 * A profile builder factory
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileBuilders {

    ProfileVersionBuilder profileVersionBuilder();

    ProfileVersionBuilder profileVersionBuilder(Version version);

    ProfileVersionBuilder profileVersionBuilderFrom(Version version);

    ProfileVersionBuilder profileVersionBuilderFrom(LinkedProfileVersion linkedVersion);

    ProfileBuilder profileBuilder();

    ProfileBuilder profileBuilder(String identity);

    ProfileBuilder profileBuilderFrom(Version version, String identity);

    ProfileBuilder profileBuilderFrom(LinkedProfile linkedProfile);

    ConfigurationItemBuilder configurationItemBuilder();

    ConfigurationItemBuilder configurationItemBuilder(String identity);
}
