package com.github.steventn96.dstudy;


import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

import java.util.*;

public class PomoTimer {
    private static class PomoTask {
        public boolean isWork;
        public int time;
        PomoTask(boolean w, int t) {
            isWork = w;
            time = t;
        }

        @Override
        public String toString() {
            return time + " second " + ((isWork) ? "work " : "rest ") + "cycle in progress";
        }
    }

    private static final Map<Character, PomoTask> cycles = new HashMap<>();
    /* we'll do the following as defaults
     * short work (25): S
     * short break (5): B
     * long work (50): L
     * long break (10): R
     */
    static {
        cycles.put('S', new PomoTask(true, 25));
        cycles.put('L', new PomoTask(true, 50));
        cycles.put('B', new PomoTask(false, 5));
        cycles.put('R', new PomoTask(false, 10));
    }

    // overall state
    private boolean hasStarted; // do we need this?
    private boolean expired; // and do we need this?

    // scheduling
    private Timer internalTimer;
    private Queue<PomoTask> taskQueue;

    // current execution state
    private PomoTask currentCycle;
    private TimerTask currentTask; // seems a bit redundant to have two tasks; perhaps pomo task can wrap timer

    /* Pomo timer should also take in a reference to the Audio Player to do playing and pausing of music */
    private Mono<MessageChannel> chatchannel;

    PomoTimer(String cycle, Mono<MessageChannel> channel) {
        hasStarted = false;
        expired = false;
        internalTimer = new Timer();
        currentCycle = null;
        taskQueue = new LinkedList<>();
        chatchannel = channel;
        if (!initializePomo(cycle)) // this should never happen if we have proper input parsing on the user side
            throw new IllegalArgumentException("bad pomo format");
    }

    private void sendMessage(String m) {
        chatchannel.block().createMessage(m).block();
    }

    private boolean initializePomo(String cycle) {
        for (char c: cycle.toCharArray()) {
            if (!cycles.containsKey(c))
                return false;
            taskQueue.add(cycles.get(c));
        }
        return true;
    }

    // start the overall cycle (can only be called once?)
    public boolean startPomo() {
        if (hasStarted)
            return false;
        startNextPomo();
        hasStarted = true;
        return true;
    }

    private void startNextPomo() {
        currentCycle = taskQueue.poll();
        sendMessage(currentCycle.toString());
        TimerTask pomotask = new TimerTask() {
            @Override
            public void run() {
                sendMessage("Finished Task");
                if (!taskQueue.isEmpty())
                    startNextPomo();
                else
                    sendMessage("Finished Pomo");
            }
        };
        currentTask = pomotask;
        internalTimer.schedule(pomotask, currentCycle.time * 1000);
    }

    // end session -- all references hopefully will be maintained properly
    public boolean endPomo() {
        if (expired)
            return true;
        internalTimer.cancel();
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
