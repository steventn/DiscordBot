package com.github.steventn96.dstudy;


import java.util.*;

public class PomoTimer {
    private class PomoTask {
        public boolean isWork;
        public int time;
        PomoTask(boolean w, int t) {
            isWork = w;
            time = t;
        }

        @Override
        public String toString() {
            return time + " minute " + ((isWork) ? "work " : "rest ") + "cycle in progress";
        }
    }
    // would represent an instance of a timer
    private boolean hasStarted;
    private boolean expired;
    private Timer internalTimer;
    private Queue<PomoTask> taskQueue;
    private PomoTask currentCycle;
    private static final Map<Character, PomoTask> cycles = new HashMap<>();

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

    /* Pomo timer should also take in a reference to the Audio Player to do playing and pausing of music */
    PomoTimer(String cycle) {
        hasStarted = false;
        expired = false;
        internalTimer = new Timer();
        currentCycle = null;
        taskQueue = new LinkedList<>();
        cycles.put('S', new PomoTask(true, 25));
        cycles.put('L', new PomoTask(true, 50));
        cycles.put('B', new PomoTask(false, 5));
        cycles.put('R', new PomoTask(false, 10));
        if (!initializePomo(cycle)) // this should never happen if we have proper input parsing on the user side
            throw new IllegalArgumentException("bad pomo format");
    }

    private void startNextPomo() {
        currentCycle = taskQueue.poll();
        System.out.println(currentCycle);
        TimerTask pomotask = new TimerTask() {
            @Override
            public void run() {
                System.out.println("Finished Task");
                if (!taskQueue.isEmpty())
                    startNextPomo();
                else
                    System.out.println("Finished Pomo");
            }
        };
        internalTimer.schedule(pomotask, currentCycle.time * 1000);
    }

    // start the overall cycle (can only be called once?)
    public boolean startPomo() {
        if (hasStarted)
            return false;
        startNextPomo();
        hasStarted = true;
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

    @Override
    public String toString() {
        if (!hasStarted)
            return "PomoTimer has not been started";
        if (expired)
            return "PomoTimer ended and expired, please discard reference";
        return currentCycle.toString();
    }
}
