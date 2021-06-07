package com.srijan.pandey.raft.misc;

public class Constants {
    public interface TimingIntervals {
        Integer SCHEDULER_DELAY = 1; // milliseconds
        Integer LEADER_HEARTBEAT_MIN = 100; // milliseconds
        Integer LEADER_HEARTBEAT_MAX = 1490; // milliseconds
        Integer ELECTION_TIMEOUT_MIN = 1500; // milliseconds
        Integer ELECTION_TIMEOUT_MAX = 3000; // milliseconds
        Integer CLIENT_SCHEDULE = 10000;
        Integer CLIENT_DELAY = 5000;
        Integer DELAYED_SENDS = 2000;
        Integer CLIENT_NODE_UNAVAILABLE_TIMEOUT = 100000;
    }

    public interface Misc {
        Boolean VOTE_GRANTED = true;
        String START = "START";
    }
}
