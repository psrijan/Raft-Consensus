package com.srijan.pandey.raft.messages;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class ClientResponse {
    private int ticketNo;
    private String message;
    private boolean requestSuccess;
}
