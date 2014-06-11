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

import io.fabric8.api.group.*;
import io.fabric8.core.zookeeper.FabricZooKeeperServer;
import io.fabric8.core.zookeeper.ZooKeeperServer;
import io.fabric8.test.embedded.support.EmbeddedTestSupport;
import org.apache.curator.framework.CuratorFramework;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Test the {@link io.fabric8.api.ProfileVersion}.
 *
 * @author thomas.diesler@jboss.com
 * @since 05-Mar-2014
 */
public class GroupTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        EmbeddedTestSupport.beforeClass();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        EmbeddedTestSupport.afterClass();
    }

    private GroupListener listener = new GroupListener<NodeState>() {
        @Override
        public void groupEvent(Group<NodeState> group, GroupListener.GroupEvent event) {
            boolean connected = group.isConnected();
            boolean master = group.isMaster();
            if (connected) {
                Collection<NodeState> members = group.members().values();
                System.err.println("GroupEvent: " + event + " (connected=" + connected + ", master=" + master + ", members=" + members + ")");
            } else {
                System.err.println("GroupEvent: " + event + " (connected=" + connected + ", master=false)");
            }
        }
    };


    @Test
    public void testJoinAfterConnect() throws Exception {

        CuratorFramework curator = ServiceLocator.awaitService(CuratorFramework.class, 10, SECONDS);
        ZooKeeperServer server = ServiceLocator.awaitService(ZooKeeperServer.class, 2, SECONDS);
        server.pause();
        blockUntilDisconnected(curator);

        GroupFactory groupFactory = GroupFactoryLocator.getGroupFactory();
        final Group<NodeState> group =  groupFactory.createGroup("/singletons/test" + uid(), NodeState.class);

        group.add(listener);
        group.start();

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());

        GroupCondition groupCondition = new GroupCondition();
        group.add(groupCondition);

        server.resume();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(groupCondition.waitForConnected(5, SECONDS));
        assertFalse(group.isMaster());

        group.update(new NodeState("foo"));
        assertTrue(groupCondition.waitForMaster(5, SECONDS));


        group.close();
    }

    private void blockUntilDisconnected(CuratorFramework curator) throws InterruptedException {
        while( curator.getZookeeperClient().isConnected() ) {
            Thread.sleep(100);
        }
    }


    static private long lastGuid;
    synchronized static private long uid() {
        if( lastGuid == 0 ) {
            lastGuid = System.currentTimeMillis();
        }
        return lastGuid++;
    }

    @Test
    public void testJoinBeforeConnect() throws Exception {

        CuratorFramework curator = ServiceLocator.awaitService(CuratorFramework.class, 10, SECONDS);
        ZooKeeperServer server = ServiceLocator.awaitService(ZooKeeperServer.class, 2, SECONDS);
        server.pause();
        blockUntilDisconnected(curator);

        GroupFactory groupFactory = GroupFactoryLocator.getGroupFactory();
        final Group<NodeState> group =  groupFactory.createGroup("/singletons/test" + uid(), NodeState.class);

        group.add(listener);
        group.start();

        GroupCondition groupCondition = new GroupCondition();
        group.add(groupCondition);

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());
        group.update(new NodeState("foo"));

        server.resume();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(groupCondition.waitForConnected(5, SECONDS));
        assertTrue(groupCondition.waitForMaster(5, SECONDS));


        group.close();
    }

    @Test
    public void testRejoinAfterDisconnect() throws Exception {

        CuratorFramework curator = ServiceLocator.awaitService(CuratorFramework.class, 10, SECONDS);
        ZooKeeperServer server = ServiceLocator.awaitService(ZooKeeperServer.class, 2, SECONDS);

        GroupFactory groupFactory = GroupFactoryLocator.getGroupFactory();
        final Group<NodeState> group =  groupFactory.createGroup("/singletons/test" + uid(), NodeState.class);

        group.add(listener);
        group.update(new NodeState("foo"));
        group.start();

        GroupCondition groupCondition = new GroupCondition();
        group.add(groupCondition);

        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        assertTrue(groupCondition.waitForConnected(5, SECONDS));
        assertTrue(groupCondition.waitForMaster(5, SECONDS));

        server.pause();
        blockUntilDisconnected(curator);

        groupCondition.waitForDisconnected(5, SECONDS);
        group.remove(groupCondition);

        assertFalse(group.isConnected());
        assertFalse(group.isMaster());

        groupCondition = new GroupCondition();
        group.add(groupCondition);

        server.resume();
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();

        assertTrue(groupCondition.waitForConnected(5, SECONDS));
        assertTrue(groupCondition.waitForMaster(5, SECONDS));


        group.close();
    }

    //Tests that if close() is executed right after start(), there are no left over entries.
    //(see  https://github.com/jboss-fuse/fuse/issues/133)
    @Test
    public void testGroupClose() throws Exception {

        String groupNode = "/singletons/test" + uid();
        CuratorFramework curator = ServiceLocator.awaitService(CuratorFramework.class, 2, SECONDS);
        curator.create().creatingParentsIfNeeded().forPath(groupNode);

        for (int i = 0; i < 10; i++) {
            GroupFactory groupFactory = GroupFactoryLocator.getGroupFactory();
            Group<NodeState> group =  groupFactory.createGroup(groupNode, NodeState.class);
            group.add(listener);
            group.update(new NodeState("foo"));
            group.start();
            group.close();
            List<String> entries = curator.getChildren().forPath(groupNode);
            assertTrue(entries.isEmpty());
        }
    }

    private class GroupCondition implements GroupListener<NodeState> {
        private CountDownLatch connected = new CountDownLatch(1);
        private CountDownLatch master = new CountDownLatch(1);
        private CountDownLatch disconnected = new CountDownLatch(1);

        @Override
        public void groupEvent(Group<NodeState> group, GroupEvent event) {
            switch (event) {
                case CONNECTED:
                case CHANGED:
                    connected.countDown();
                    if (group.isMaster()) {
                        master.countDown();
                    }
                    break;
                case DISCONNECTED:
                    disconnected.countDown();
            }
        }

        public boolean waitForConnected(long time, TimeUnit timeUnit) throws InterruptedException {
            return connected.await(time, timeUnit);
        }

        public boolean waitForDisconnected(long time, TimeUnit timeUnit) throws InterruptedException {
            return disconnected.await(time, timeUnit);
        }

        public boolean waitForMaster(long time, TimeUnit timeUnit) throws InterruptedException {
            return master.await(time, timeUnit);
        }
    }

}
