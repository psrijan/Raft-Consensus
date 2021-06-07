package com.srijan.pandey;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.srijan.pandey.raft.ClientNode;
import com.srijan.pandey.raft.Node;
import com.srijan.pandey.raft.messages.*;
import com.srijan.pandey.raft.state.NodeType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import akka.testkit.javadsl.TestKit;
public class BasicTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void teardown() {
        TestKit.shutdownActorSystem(system, Duration.create(100, TimeUnit.MILLISECONDS), false);
        system = null;
    }

//    @Test
//    public void testIt() {
//        new TestKit(system) {
//            {
//                final Props props = Props.create(Node.class);
//                final ActorRef actorRef = system.actorOf(props, "node-1");
//                final TestKit probe = new TestKit(system);
//
//                within(Duration.create(1000, TimeUnit.MILLISECONDS), Duration.create(2000, TimeUnit.MILLISECONDS), () -> {
//                    StartMessage startMessage = new StartMessage(Arrays.asList(probe.getRef()), Arrays.asList("probe"));
//                    actorRef.tell(startMessage, getRef());
//                    expectMsg(Duration.create(1000, TimeUnit.MILLISECONDS), RequestVote.class);
//                    return null;
//                });
//            }
//        };
//
//    }

    @Test
    public void testIt1() {
        /*
         * Wrap the whole test procedure within a testkit constructor
         * if you want to receive actor replies or use Within(), etc.
         */
        new TestKit(system) {
            {
                final Props props = Props.create(ClientNode.class);
                final ActorRef subject = system.actorOf(props);

                // can also use JavaTestKit “from the outside”
                final TestKit probe = new TestKit(system);
                // “inject” the probe by passing it to the test subject
                // like a real resource would be passed in production
                // await the correct response
//                StartMessage startMessage = new StartMessage(Arrays.asList(probe.getRef()), Arrays.asList("probe"));
//                subject.tell(startMessage, getRef());
//
//                ClientRequest clientRequest = new ClientRequest("REF-1", OperationType.ADD, null);
//                subject.tell(clientRequest, getRef());
//                expectMsg(java.time.Duration.ofSeconds(1), clientRequest);

//                // the run() method needs to finish within 3 seconds
//                within(
//                        java.time.Duration.ofSeconds(3),
//                        () -> {
//                            subject.tell("hello", getRef());
//
//                            // This is a demo: would normally use expectMsgEquals().
//                            // Wait time is bounded by 3-second deadline above.
//                            awaitCond(probe::msgAvailable);
//
//                            // response must have been enqueued to us before probe
//                            expectMsg(java.time.Duration.ZERO, "world");
//                            // check that the probe we injected earlier got the msg
//                            probe.expectMsg(java.time.Duration.ZERO, "hello");
//                            Assert.assertEquals(getRef(), probe.getLastSender());
//
//                            // Will wait for the rest of the 3 seconds
//                            expectNoMessage();
//                            return null;
//                        });
            }
        };
    }
}
