package com.srijan.pandey.raft.test;
import java.lang.Math;
public class RandomTest {
    public static void main(String[] args ) {
        double random = Math.random();
        int val = getRandomNumber(1,100);
        System.out.println(val);
    }

    public static int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

}
