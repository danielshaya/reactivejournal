package org.rxjournal.impl;

/**
 * Created by daniel on 26/04/17.
 */
public class PlayOptions {
    public enum ReplayRate {ACTUAL_TIME, FAST}
    public enum PauseStrategy {SPIN, YIELD}
    private String filter = "";
    private ReplayRate replayRate = ReplayRate.ACTUAL_TIME;
    private PauseStrategy pauseStrategy = PauseStrategy.YIELD;
    private Object using = null;
    private boolean playFromNow = false;
    private long playFrom = Long.MAX_VALUE;
    private long playUntil = Long.MIN_VALUE;
    private boolean waitForMoreItems = true;

    public String filter() {
        return filter;
    }

    public PlayOptions filter(String filter) {
        this.filter = filter;
        return this;
    }

    public ReplayRate replayRate() {
        return replayRate;
    }

    public PlayOptions replayRate(ReplayRate replayRateStrategy) {
        this.replayRate = replayRateStrategy;
        return this;
    }

    public PauseStrategy pauseStrategy() {
        return pauseStrategy;
    }

    public PlayOptions pauseStrategy(PauseStrategy pauseStrategy) {
        this.pauseStrategy = pauseStrategy;
        return this;
    }

    public Object using() {
        return using;
    }

    public PlayOptions using(Object using) {
        this.using = using;
        return this;
    }

    public boolean playFromNow() {
        return playFromNow;
    }

    public PlayOptions playFromNow(boolean playFromNow) {
        this.playFromNow = playFromNow;
        return this;
    }
    public long playFrom() {
        return playFrom;
    }

    public PlayOptions playFrom(long playFrom) {
        this.playFrom = playFrom;
        return this;
    }

    public long playUntil() {
        return playUntil;
    }

    public PlayOptions playUntil(long playUntil) {
        this.playUntil = playUntil;
        return this;
    }

    public boolean completeAtEndOfFile() {
        return waitForMoreItems;
    }

    public PlayOptions completeAtEndOfFile(boolean waitForMoreItems) {
        this.waitForMoreItems = waitForMoreItems;
        return this;
    }
}
