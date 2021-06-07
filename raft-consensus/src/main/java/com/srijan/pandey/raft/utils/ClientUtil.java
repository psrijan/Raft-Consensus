package com.srijan.pandey.raft.utils;

import akka.actor.*;
import com.srijan.pandey.raft.dto.ClientRequestNodeDTO;
import com.srijan.pandey.raft.messages.ClientStartMessage;
import com.srijan.pandey.raft.messages.ClientPrimaryUnavailableTimeout;
import com.srijan.pandey.raft.messages.StartMessage;
import com.srijan.pandey.raft.misc.Constants;
import com.srijan.pandey.raft.state.ClientState;
import scala.concurrent.duration.Duration;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that handles cllients basic operations
 */
public class ClientUtil {

    /**
     * This is a mechanism by which actors watch when other actors in the actor system are terminated.
     * This is a good way to identify if nodes in a sytem have failed. The client uses this mechanism
     * to identify if nodes in the TicketBooking systems have failed and call other nodes in the cluster
     */
    public static void createDeathWatch(StartMessage startMessage, AbstractActor actor) {
        for (ActorRef actorRef : startMessage.getNodes()) {
            actor.getContext().watch(actorRef);
        }
    }

    /**
     * @todo need to fix primary node recovery case
     * Select the primary node and if it is not available select the other nodes
     * in the server. Retry after sometime
     * @param clientStartMessage
     * @return
     */
    public static ClientRequestNodeDTO getClusterNode(ClientStartMessage clientStartMessage, Set<String> terminatedNodes) {
        ClientRequestNodeDTO clientRequestNodeDTO = new ClientRequestNodeDTO();

        // If primary node is still available then use the primary node
        if (!isNodeTerminated(clientStartMessage.getPrimaryNodeName(), terminatedNodes)) {
            clientRequestNodeDTO.setPrimary(true);
            clientRequestNodeDTO.setActorRef(clientStartMessage.getPrimaryNode());
            return clientRequestNodeDTO;
        }

        // Client Primary Node unavailable and tries to find a secondary Node
        for (int i = 0; i < clientStartMessage.getNames().size(); i++) {
            if(isNodeTerminated(clientStartMessage.getNames().get(i), terminatedNodes))
                continue;
            clientRequestNodeDTO.setPrimary(false);
            clientRequestNodeDTO.setActorRef(clientStartMessage.getNodes().get(i));
            return clientRequestNodeDTO;
        }
        return clientRequestNodeDTO;
    }

    private static boolean isNodeTerminated(String nodeName, Set<String> terminatedNodes) {
        return terminatedNodes.stream().anyMatch(s -> s.equalsIgnoreCase(nodeName));
    }


    /**
     * Sets up a timeout message to check if the primary node for the client is back up.
     * @param actor
     * @return
     */
    public static void clientsPrimaryNodeUnavailableTimeout(AbstractActor actor, ClientState clientState) {
        ClientPrimaryUnavailableTimeout nodeUnavailableTimeout = new ClientPrimaryUnavailableTimeout();
        Cancellable cancellable = actor.getContext().getSystem().scheduler().scheduleOnce(Duration.create(Constants.TimingIntervals.CLIENT_NODE_UNAVAILABLE_TIMEOUT,
                TimeUnit.MILLISECONDS), actor.getSelf(), nodeUnavailableTimeout, actor.getContext().getDispatcher(), actor.getSelf());
        clientState.setPrimaryNodeTimeout(cancellable);
    }
}
