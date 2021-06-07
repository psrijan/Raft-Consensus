package com.srijan.pandey.raft.state;

import akka.actor.Cancellable;
import com.srijan.pandey.raft.messages.ClientStartMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

/**
 * Class that holds state information for raft client
 * this is used by the util classes to makes changes
 * to the client state
 */
@Getter
@Setter
public class ClientState {
    private ClientStartMessage clientStartMessage;
    private Set<String> terminatedNodes = new HashSet<>();
    private Cancellable primaryNodeTimeout;

}
