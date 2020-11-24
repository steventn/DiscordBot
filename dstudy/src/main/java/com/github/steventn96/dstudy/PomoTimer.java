package com.github.steventn96.dstudy;


import javafx.util.Pair;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Timer;

public class PomoTimer {
    // would represent an instance of a timer
    private boolean hasStarted;
    private boolean expired;
    private Timer internalTimer;
    private Queue<Pair<Integer, Integer>> taskQueue;
    private static final Map<Character, Pair<Integer, Integer>> cycles = new HashMap<>();

    /* we'll do the following
     * short work (25): S
     * short break (5): B
     * long work (50): L
     * long break (10): R
     */
    private boolean initializePomo(String cycle) {
        for (char c: cycle.toCharArray()) {
            if (!cycles.containsKey(c))
                return false;
            taskQueue.add(cycles.get(c));
        }
        return true;
    }

    PomoTimer(String cycle) {
        hasStarted = false;
        expired = false;
        internalTimer = new Timer();
        cycles.put('S', new Pair<>(1, 25));
        cycles.put('L', new Pair<>(1, 50));
        cycles.put('B', new Pair<>(0, 5));
        cycles.put('R', new Pair<>(0, 10));
        if (!initializePomo(cycle)) // this should never happen if we have proper input parsing on the user side
            throw new IllegalArgumentException("bad pomo format");
    }

    // start the overall cycle (can only be called once?)
    public boolean startPomo() {
        if (hasStarted)
            return false;
        //start
        return true;
    }

    // end session -- all references hopefully will be maintained properly
    public boolean endPomo() {
        expired = true;
        return true;
    }

    public boolean hasEnded() {
        return expired;
    }

    // pause the current timer regardless of study or rest session
    public boolean pause() {
        return true;
    }

    // resume the current timer
    public boolean resume() {
        return true;
    }

    private String get_cycle() {
        return "Some Pomo Cycle";
    }

    @Override
    public String toString() {
        String toret = "";
        while (!taskQueue.isEmpty()) {
            Pair<Integer, Integer> next = taskQueue.poll();
            toret += next.first + ", " + next.second + "/n";
        }
        return toret;
    }
}
