package com.github.steventn96.dstudy;

public class PomoTimer {
    // would represent an instance of a timer
    PomoTimer() {

    }

    // start the overall cycle (can only be called once?)
    public boolean startPomo() {
        return true;
    }

    // end session -- this object should self destruct
    public boolean endPomo() {
        return true;
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
        return "Overall Cycle: a b a b a b\n" +
                "Current progress = 3min/20min";
    }
}
