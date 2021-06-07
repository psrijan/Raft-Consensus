package com.srijan.pandey.raft.messages;

public enum OperationType {
    INCONSISTENT_CHECK_TICKET_AVAILABILITY(1),
    CONSISTENT_CHECK_TICKET_AVAILABILITY(2),
    PURCHASE(3);


    private int operationIndex;
    private int n;
    private OperationType(int operationIndex) {
        this.operationIndex = operationIndex;
    }

    public int getOperationIndex() {
        return operationIndex;
    }

    public void setTicket(int n) {
        this.n = n;
    }


}
