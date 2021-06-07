package com.srijan.pandey.raft.test;

public class TimeTest {
    public static void main(String[] argss) {
        long prevTime = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) {
            long time = System.currentTimeMillis();
            System.out.printf("Current Time: %d Difference To Previous %d \n",  time , time - prevTime);
        }
    }
}
