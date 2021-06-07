package com.srijan.pandey.raft.stub;

import com.srijan.pandey.raft.messages.OperationType;
import java.util.ArrayList;
import java.util.List;

public class ClientOperationStubUtil {

    /**
     * Gives 5 unique operation type types based on clientNumber.
     * @param clientNumber
     * @return
     */
    public static List<OperationType> getClientBasedOperation(int clientNumber) {
        List<OperationType> listOne = new ArrayList<>();
        listOne.add(OperationType.PURCHASE);
        listOne.add(OperationType.PURCHASE);
        listOne.add(OperationType.PURCHASE);
        listOne.add(OperationType.PURCHASE);
        listOne.add(OperationType.PURCHASE);

        List<OperationType> listTwo = new ArrayList<>();
        listTwo.add(OperationType.PURCHASE);
        listTwo.add(OperationType.PURCHASE);
        listTwo.add(OperationType.PURCHASE);
        listTwo.add(OperationType.PURCHASE);
        listTwo.add(OperationType.PURCHASE);

        List<OperationType> listThree = new ArrayList<>();
        listThree.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.CONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.CONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.CONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.CONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listThree.add(OperationType.CONSISTENT_CHECK_TICKET_AVAILABILITY);

        List<OperationType> listFour = new ArrayList<>();
        listFour.add(OperationType.PURCHASE);
        listFour.add(OperationType.PURCHASE);
        listFour.add(OperationType.PURCHASE);
        listThree.add(OperationType.PURCHASE);
        listThree.add(OperationType.PURCHASE);

        List<OperationType> listFive = new ArrayList<>();
        listFive.add(OperationType.PURCHASE);
        listFive.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listFive.add(OperationType.INCONSISTENT_CHECK_TICKET_AVAILABILITY);
        listFive.add(OperationType.CONSISTENT_CHECK_TICKET_AVAILABILITY);
        listFive.add(OperationType.PURCHASE);

        List<List<OperationType>> res = new ArrayList<>();
        res.add(listOne);
        res.add(listTwo);
        res.add(listThree);
        res.add(listFour);
        res.add(listFive);

        return res.get(clientNumber);
    }

}
