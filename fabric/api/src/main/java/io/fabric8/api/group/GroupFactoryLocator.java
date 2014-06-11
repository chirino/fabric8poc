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
package io.fabric8.api.group;

import org.jboss.gravia.runtime.ServiceLocator;

/**
 * The group factory locator
 *
 * @author hchirino@redhat.com
 * @since 23-Apr-2014
 */
public final class GroupFactoryLocator {

    /**
     * Locate the group factory
     */
    public static GroupFactory getGroupFactory() {
        return ServiceLocator.getRequiredService(GroupFactory.class);
    }

    // Hide ctor
    private GroupFactoryLocator() {
    }
}
