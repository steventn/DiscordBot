package com.github.steventn96.dstudy;


import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

import java.util.*; // maybe make this less broad to reduce footprint (although we already have a bajillion things)

public class PomoTimer {
    private static class PomoTask {
        public boolean isWork;
        public long time;
        private Date execTime;

        PomoTask(boolean w, int t) {
            isWork = w;
            time = t;
        }

        PomoTask(PomoTask copy) {
            isWork = copy.isWork;
            time = copy.time;
        }
        public void setExecTime() {
            execTime = new Date(System.currentTimeMillis() + time);
        }

        public Date getExecTime() {
            return execTime;
        }

        @Override
        public String toString() {
            return time / 1000 + " second " + ((isWork) ? "work " : "rest ") + "cycle in progress";
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
        cycles.put('s', new PomoTask(true, 25 * 1000));
        cycles.put('l', new PomoTask(true, 50 * 1000));
        cycles.put('b', new PomoTask(false, 5 * 1000));
        cycles.put('r', new PomoTask(false, 10 * 1000));
    }

    // overall state
    private boolean hasStarted; // do we need this?
    private boolean expired; // and do we need this?

    // scheduling
    private final Timer internalTimer;
    private final Queue<PomoTask> taskQueue;

    // current execution state
    private PomoTask currentCycle;
    private TimerTask currentTask; // seems a bit redundant to have two tasks; perhaps pomo task can wrap timer
    private boolean isPaused;

    /* Pomo timer should also take in a reference to the Audio Player to do playing and pausing of music */
    private final Mono<MessageChannel> chatChannel;
    private final AudioPlayer player;

    PomoTimer(String cycle, Mono<MessageChannel> channel, AudioPlayer player) {
        hasStarted = false;
        expired = false;
        internalTimer = new Timer();
        currentCycle = null;
        isPaused = false;
        taskQueue = new LinkedList<>();
        chatChannel = channel;
        if (!initializePomo(cycle)) // this should never happen if we have proper input parsing on the user side
            throw new IllegalArgumentException("bad pomo format");
        this.player = player;
    }

    private boolean initializePomo(String cycle) {
        for (char c: cycle.toLowerCase().toCharArray()) {
            if (!cycles.containsKey(c))
                return false;
            taskQueue.add(new PomoTask(cycles.get(c)));
        }
        return true;
    }

    private void startNextPomo() {
        if (!isPaused)
            currentCycle = taskQueue.poll();
        currentCycle.setExecTime();

        sendMessage(currentCycle.toString());
        if (player.getPlayingTrack() != null || player.isPaused()) {
            player.setPaused(!currentCycle.isWork);
        }
        else
            sendMessage("NOTE: No music playing or queued!");
        TimerTask pomoTask = new TimerTask() {
            @Override
            public void run() {
                sendMessage("Finished Task");
                if (!taskQueue.isEmpty())
                    startNextPomo();
                else {
                    expired = true;
                    sendMessage("Finished Pomo");
                }
            }
        };
        currentTask = pomoTask;
        internalTimer.schedule(pomoTask, currentCycle.getExecTime());
    }

    // start the overall cycle
    public boolean startPomo() {
        if (hasStarted)
            return false;
        startNextPomo();
        hasStarted = true;
        return true;
    }

    // end session -- all references hopefully will be maintained properly
    public boolean endPomo() {
        if (expired)
            return true;
        internalTimer.cancel();
        expired = true;
        sendMessage("pomo ended... feel free to start a new one");
        return true;
    }

    public boolean hasEnded() {
        return expired;
    }

    // pause the current timer regardless of study or rest session
    public boolean pause() {
        if (isPaused || expired)
            return false;
        isPaused = true;
        currentCycle.time = currentCycle.getExecTime().getTime() - System.currentTimeMillis();
        currentTask.cancel();
        sendMessage("cycle paused with " + currentCycle.time / 1000 + "s remaining, !presume to resume");
        return true;
    }

    // resume the current timer
    public boolean resume() {
        if (expired || !isPaused)
            return false;
        startNextPomo();
        isPaused = false;
        return true;
    }

    private void sendMessage(String m) {
        try {
            chatChannel.block().createMessage(m).block();
        }
        catch (NullPointerException e) {
            System.out.println("problem sending message"); // use a logger here
        }
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
