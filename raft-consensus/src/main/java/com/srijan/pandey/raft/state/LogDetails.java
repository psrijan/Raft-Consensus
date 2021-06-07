package com.srijan.pandey.raft.state;

import com.srijan.pandey.raft.messages.OperationType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The log must have information about term
 * and the command that it performed
 *
 */
@ToString
@Getter
@Setter
public class LogDetails {
    private String refId;
    private long term;
    private OperationType command;
}
