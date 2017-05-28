package org.rxjournal.impl;

/**
 * Created by daniel on 26/04/17.
 */
public class PlayOptions {
    private String filter = "";
    private ReplayRate replayRate = ReplayRate.ACTUAL_TIME;
    private PauseStrategy pauseStrategy = PauseStrategy.YIELD;
    private Object using = null;
    private boolean playFromNow = false;
    private long playFrom = Long.MAX_VALUE;
    private long playUntil = Long.MIN_VALUE;
    private boolean waitForMoreItems = true;

    String filter() {
        return filter;
    }

    /**
     * Allows the user to play back the events in the journal which weretagged with this filter.
     * Typical examples would be recording input events
     * to a process with 'input' and results of the processor with 'output'.
     * @param filter The events in the journal saved with the tag
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions filter(String filter) {
        this.filter = filter;
        return this;
    }

    ReplayRate replayRate() {
        return replayRate;
    }

    /**
     * When play() is called, RxPlayer can either play the events back in ACTUAL_TIME
     * respecting the gaps between the originally recoded events, or FAST, playing
     * the evnts back as fast as it can. Typically for testing you would use ACTUAL_TIME
     * whereas for remote and back pressure usages you would use FAST.
     *
     * @param replayRateStrategy strategy determining the replay rate.
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions replayRate(ReplayRate replayRateStrategy) {
        this.replayRate = replayRateStrategy;
        return this;
    }

    PauseStrategy pauseStrategy() {
        return pauseStrategy;
    }

    /**
     *
     * @param pauseStrategy
     * @return
     */
    public PlayOptions pauseStrategy(PauseStrategy pauseStrategy) {
        this.pauseStrategy = pauseStrategy;
        return this;
    }

    Object using() {
        return using;
    }

    public PlayOptions using(Object using) {
        this.using = using;
        return this;
    }

    boolean playFromNow() {
        return playFromNow;
    }

    /**
     *
     * @param playFromNow
     * @return
     */
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

    public enum ReplayRate {ACTUAL_TIME, FAST}

    public enum PauseStrategy {SPIN, YIELD}
}
