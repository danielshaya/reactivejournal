package org.reactivejournal.impl;

/**
 * A container class to encapsulate all the configurations settings that can be passed
 * into the play() method of {@link ReactivePlayer}
 */
public class PlayOptions {
    private String filter = "";
    private ReplayRate replayRate = ReplayRate.ACTUAL_TIME;
    private PauseStrategy pauseStrategy = PauseStrategy.YIELD;
    private Object using = null;
    private boolean playFromNow = false;
    private long playFromTime = Long.MIN_VALUE;
    private long playUntilTime = Long.MAX_VALUE;
    private long playFromSeqNo = Long.MIN_VALUE;
    private long playUntilSeqNo = Long.MAX_VALUE;
    private boolean completeAtEndOfFile = false;
    private boolean sameThread = false;

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
     * When polling for events the pause strategy is set by {@link PauseStrategy}.
     * This will default to PauseStrategy.YIELD which calls Thread.yield()
     * between each unsuccessful attempt to call for the next event. This is
     * suitable for most applications.<p>
     * In low latency senitive applications however you might want to set the
     * {@link PauseStrategy} to PauseStrategy.SPIN which will poll continuously
     * taking up a full CPU. This will reduce latency to get an event into the
     * data stream but you need to make sure you have the available compute
     * power.
     *
     * @param pauseStrategy The PauseStrategy to be applied
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions pauseStrategy(PauseStrategy pauseStrategy) {
        this.pauseStrategy = pauseStrategy;
        return this;
    }

    Object using() {
        return using;
    }

    /**
     * Where keeping GC to a minimum is a requirement the program should not
     * create a new object for every data event. Instead we can just repopulate
     * the same object each time.  There is an assumption that the getters/setters
     * are all available. With this method you are able to pass in a protoype object
     * which will be repopulated with the data for each event.
     * <p>
     * It is important to remember that if you want to keep a reference to the object
     * that comes off the stream you will need to clone the object.
     *
     * @param using The prototype object to be used to populate data from the stream
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions using(Object using) {
        this.using = using;
        return this;
    }

    boolean playFromNow() {
        return playFromNow;
    }

    /**
     * This setting is used when you only want to listen to events put on the stream
     * from now and you are not interested in replaying from the start of the recording.
     * This might be true, for example, if you have a remote connection to a hot event
     * source.
     *
     * @param playFromNow Defaulted to false
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions playFromNow(boolean playFromNow) {
        this.playFromNow = playFromNow;
        return this;
    }

    long playFromTime() {
        return playFromTime;
    }

    /**
     * Play back the recording from this time,  Useful if you only want to play a subset of the recording.
     * @param playFromTime time in millis from 1970
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions playFromTime(long playFromTime) {
        this.playFromTime = playFromTime;
        return this;
    }

    long playUntilTime() {
        return playUntilTime;
    }

    /**
     * Play back until this time. Useful if you only want to play a subset of the recording.
     * @param playUntilTime time in millis from 1970
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions playUntilTime(long playUntilTime) {
        this.playUntilTime = playUntilTime;
        return this;
    }


    long playFromSeqNo() {
        return playFromSeqNo;
    }

    /**
     * Play back the recording from this seqNo,  Useful if you only want to play a subset of the recording.
     * @param playFromSeqNo sequence number to start from
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions playFromSeqNo(long playFromSeqNo) {
        this.playFromSeqNo = playFromSeqNo;
        return this;
    }

    long playUntilSeqNo() {
        return playUntilSeqNo;
    }

    /**
     * Play back until this sequence number. Useful if you only want to play a subset of the recording.
     * @param playUntilSeqNo play until this sequence number
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions playUntilSeqNo(long playUntilSeqNo) {
        this.playUntilSeqNo = playUntilSeqNo;
        return this;
    }

    boolean completeAtEndOfFile() {
        return completeAtEndOfFile;
    }

    /**
     * Used when you want to stop as soon as there are no more items. For example
     * in a unit test. This is only useful where there was no completion item.
     *
     * @param waitForMoreItems Defaulted to true
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions completeAtEndOfFile(boolean waitForMoreItems) {
        this.completeAtEndOfFile = waitForMoreItems;
        return this;
    }

    boolean sameThread() {
        return sameThread;
    }

    /**
     * By default the subscriber will be called back on a different thread.
     *
     * @param sameThread set the callback on the same thread
     * @return PlayOptions for use in the Builder pattern
     */
    public PlayOptions sameThread(boolean sameThread) {
        this.sameThread = sameThread;
        return this;
    }

    void validate() throws IllegalArgumentException {
        if(playFromTime !=  Long.MIN_VALUE && playFromNow){
            throw new IllegalArgumentException("Illegal combination: PlayFromTime and PlayFromNow" +
                    "not allowed to be set at the same time");
        }

        if(playFromTime !=  Long.MIN_VALUE && playFromSeqNo != Long.MIN_VALUE){
            throw new IllegalArgumentException("Illegal combination: PlayFromTime and PlayFromSeqNo" +
                    "not allowed to be set at the same time");
        }

        if(playFromSeqNo !=  Long.MIN_VALUE && playFromNow){
            throw new IllegalArgumentException("Illegal combination: PlayFromSeqNo and PlayFromNow" +
                    "not allowed to be set at the same time");
        }

        if(playUntilTime !=  Long.MAX_VALUE && playUntilSeqNo != Long.MAX_VALUE){
            throw new IllegalArgumentException("Illegal combination: PlayUntiTime and PlayUntilSeqNo" +
                    "not allowed to be set at the same time");
        }

        if(playFromNow){
            playFromTime = System.currentTimeMillis();
        }
    }

    public enum ReplayRate {ACTUAL_TIME, FAST}

    public enum PauseStrategy {SPIN, YIELD}
}
