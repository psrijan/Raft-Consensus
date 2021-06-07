package com.srijan.pandey.raft;

import akka.actor.*;
import com.srijan.pandey.raft.dto.ClientRequestNodeDTO;
import com.srijan.pandey.raft.messages.*;

import com.srijan.pandey.raft.misc.Constants;
import com.srijan.pandey.raft.state.ClientState;
import com.srijan.pandey.raft.utils.ClientUtil;
import scala.concurrent.duration.Duration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class ClientNode extends AbstractActor {
    private ClientState state;
    int curOperation = 0;
    int refIndex = 0;

    public ClientNode() {
        state = new ClientState();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(ClientResponse.class , this::processClientResponse)
                .match(ClientStartMessage.class, this::processStartMessage)
                .match(Terminated.class, this:: processTerminatedMessage)
                .match(ClientPrimaryUnavailableTimeout.class, this::processClientPrimaryUnavailableTimeout).build();
    }

    /**
     * Tries to search for a primary node when the timeout to check if primary is available has expired.
     * And sees if the primary node has come back up. If this is the case it removes the primary node name
     * from terminated list.
     * @param clientPrimaryUnavailableTimeout
     */
    public void processClientPrimaryUnavailableTimeout(ClientPrimaryUnavailableTimeout clientPrimaryUnavailableTimeout ) {
        ActorSystem actorSystem = state.getClientStartMessage().getActorSystem();
        String primaryNodeName = state.getClientStartMessage().getPrimaryNodeName();
        String actorPath = "" + primaryNodeName;
        ActorRef actorRef = actorSystem.actorSelection(actorPath).anchor();
        if (!actorRef.isTerminated()) {
            List<String> namesList = state.getClientStartMessage().getNames();
            IntStream.range(0, namesList.size()).forEach(i -> {
                if (namesList.get(i).equalsIgnoreCase(primaryNodeName)) {
                    state.getClientStartMessage().getNodes().add(i, actorRef); // Replaces the existing value of actorRef.
                }
            });
            state.getTerminatedNodes().removeIf(s -> s.equalsIgnoreCase(primaryNodeName)); // removes the primary node name from terminated list
        } else {
            // Since we didn't find the primary to be up, reset the timer for a wait period.
            ClientUtil.clientsPrimaryNodeUnavailableTimeout(this, state);
        }
    }

    /**
     * Registers when a raft node terminates or is not working properly
     * @param terminated
     */
    public void processTerminatedMessage(Terminated terminated) {
        getContext().system().log().debug("Received Terminated message: {} ", terminated.getActor().path().name());
        System.out.println("Received Terminated Message");
        ActorRef actorRef = terminated.getActor();
        String name = actorRef.path().name();
        state.getTerminatedNodes().add(name);
    }

    /**
     * Client Sends it's request in 5 second period to the server with an initial delay of around 2 seconds.
     * Please wait around 7 seconds before seeing client requests to be seen in the Raft Cluster
     * @param startMessage
     */
    public void processStartMessage(ClientStartMessage startMessage) {
        System.out.println("CLIENT: Client Received Start Message");
        this.state.setClientStartMessage(startMessage);
        ClientUtil.createDeathWatch(startMessage, this);
        getContext().getSystem().scheduler().scheduleAtFixedRate(Duration.create(Constants.TimingIntervals.CLIENT_DELAY, TimeUnit.MILLISECONDS), Duration.create(Constants.TimingIntervals.CLIENT_SCHEDULE, TimeUnit.MILLISECONDS),
                ()-> {
                    // for purpose of simplicity client will repeat the commands initially decided by the user.
                    List<OperationType> commands = startMessage.getCommands();
                    OperationType curCommand = commands.get(curOperation++ % commands.size());

                    // Send a client request
                    ClientRequest clientRequest = new ClientRequest(startMessage.getClientName() + "-REFID-" + refIndex, curCommand, this.getSelf());
                    System.out.println("CLIENT REQUEST: " + clientRequest.toString());
                    ClientRequestNodeDTO clientRequestNodeDTO = ClientUtil.getClusterNode(this.state.getClientStartMessage(), this.state.getTerminatedNodes());

                    // If a Primary Node is unavailable on Start then set a primary node timeout to search for it
                    if (!clientRequestNodeDTO.isPrimary())
                        ClientUtil.clientsPrimaryNodeUnavailableTimeout(this, state);
                    clientRequestNodeDTO.getActorRef().tell(clientRequest, this.getSelf()); // client sends it to the first node it sees in the List
                    refIndex++;
                }, getContext().getSystem().getDispatcher());
    }

    public void processClientResponse(ClientResponse clientResponse) {
        System.out.println("CLIENT RESPONSE for " + state.getClientStartMessage().getNames() + clientResponse.toString());
        int res = clientResponse.getTicketNo();
        System.out.println("CLIENT " + state.getClientStartMessage().getClientName() + " RECEIVED: " + res + " OPERATION RESPONSE: " + clientResponse.getMessage());
    }
}
