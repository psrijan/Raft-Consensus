package com.srijan.pandey.raft.messages;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class BaseMessage {
    private long termNumber; // shared when sharing message
}
