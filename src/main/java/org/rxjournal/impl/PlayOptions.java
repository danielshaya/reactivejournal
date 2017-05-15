package org.rxjournal.impl;

/**
 * Created by daniel on 26/04/17.
 */
public class PlayOptions {
    public enum Replay {REAL_TIME, FAST}
    private String filter = "";
    private Replay replayStrategy = Replay.REAL_TIME;
    private boolean endOfStreamToken = true;
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

    public Replay replayStrategy() {
        return replayStrategy;
    }

    public PlayOptions replayStrategy(Replay replayStrategy) {
        this.replayStrategy = replayStrategy;
        return this;
    }

    public boolean endOfStreamToken() {
        return endOfStreamToken;
    }

    public PlayOptions endOfStreamToken(boolean endOfStreamToken) {
        this.endOfStreamToken = endOfStreamToken;
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
