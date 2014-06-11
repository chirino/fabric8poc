/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.core.groups.internal;

import io.fabric8.api.group.Group;
import io.fabric8.api.group.GroupFactory;
import io.fabric8.api.group.NodeState;
import io.fabric8.spi.permit.PermitKey;
import io.fabric8.spi.scr.AbstractComponent;
import io.fabric8.spi.scr.AbstractProtectedComponent;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.*;
import org.jboss.gravia.provision.ProvisionException;

import java.util.Map;

/**
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(GroupFactory.class)
public final class ZooKeeperGroupFactory extends AbstractComponent implements GroupFactory {

    static final PermitKey<GroupFactory> PERMIT = new PermitKey<GroupFactory>(GroupFactory.class);

    @Reference
    CuratorFramework curator;

    @Override
    public <T extends NodeState> Group<T> createGroup(String path, Class<T> clazz) {
        return new ZooKeeperGroup<T>(curator, path, clazz);
    }

    @Override
    public <T extends NodeState> Group<T> createMultiGroup(String path, Class<T> clazz) {
        return new ZooKeeperMultiGroup<T>(curator, path, clazz);
    }

    protected void bindCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    protected void unbindCurator(CuratorFramework curator) {
        this.curator = null;
    }

}
