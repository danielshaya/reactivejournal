# RxJournal

RxJournal augments the popular [RxJava](https://github.com/ReactiveX/RxJava) library by adding 
functionality to record and replay reactive streams. 

## Downloading the project

### Maven
RxJournal is a Maven project so you can clone the project and build in the usual way.

The intention is for this project to make its way in Maven Central (work in progress).

### Download the jar
Go to the releases section of the project. With each release there will be an uber jar that you 
can download with all the RXJournal classes and dependencies.

To test that it works try:

````
java -cp ./rxjournal-x.x.x.jar org.rxjournal.examples.helloworld.HelloWorld 
````

## Primary Motivations Behind RxJournal

### 1. Testing

Testing is a primary motivation for RxJournal. RxJournal allows developers to
black box test their code by recording all inputs and outputs in and out of their programs.

An obvious use case are unit tests where RxJournal recordings can be used to create
comprehensive tests (see [HelloWorldTest] for an example). This example makes use of
`RxValidator` which allows unit tests to compare their results against previously
recorded results in the journal.

Another powerful use case is to enable users to replay production data into test systems. 
By simply copying over the journal file from a production system and replaying all or part of the file
into a test system the exact conditions of the primary system will be reproduced.

### 2. Remote Connnections

RxJournal can be recorded on one JVM and can be replayed (in real-time if required) on one or more 
JVMs provided they have access to the journal file location.  

The remote connection can either read from the beginning of the recording or just start with live 
updates from the recorder. The remote connection (the 'listener') can optionally write back to the 
journal effecting a two way conversation or RPC. There can be multiple readers and writers to the
journal.

RxJournal uses Chronicle-Queue (a memory mapped file solution) serialisation meaning that
the process of moving data from one JVM to another is exceedingly efficient and can be achieved 
in single digit micro seconds. 

If you need to pass data between JVMs on the same machine this is not only the most efficient way 
to do so but you will also provide you with a full recording of the data that is 
transferred between the JVMs.

### 3. Slow consumers (handling back pressure)

If you have a fast producer that you can't slow down but your consumer can't keep up
there are a few options available to your system.

Most often you end up implementing strategies that hold buffers of data in memory allowing the
consumer to catch up. The problem with those sort of strategies are one, if your process
crashes you lose all the data in your buffer. Therefore if you need to consume the fast data in a 
transactional manner this will not be an option. Two, you may run out of memory if the 
buffers get really big. At the very least you will probably need to run your JVM with a large
memory setting that many be inefficient. For latency sensitive applications it will 
put pressure on the GC which will not be acceptable.


## Design Goals

- Recording to the journal is transactional i.e. no data will be lost if the 
program crashes
- Recording and playback is so fast that it won't slow down the host program.
- Recording and playback can be achieved without any gc overhead
- RxRecorder can be easily added (or even retro-fitted) into any RxJava project

# Quick Start
## Creating a Journal

An RxJournal is created as follows:

    RxJournal rxJournal = new RxJournal(String dir);

The directory is the location where the serialised file will be created 

## Recording a reactive stream
`RxRecorder` allows any RxJava `Observable`/`Flowable` to be journaled to disk using 
the `record` function:
    
    RxRecorder rxRecorder = rxJournal.createRxRecorder();
    rxRexcorder.record(Observable)

For notes on threading see FAQ below.

## Playing back a reactive stream

`RxPlayer` is used to playback the journal recording:

    RxPlayer rxPlayer = rxJournal.createRxPlayer();
    rxPlayer.play(new PlayOptions());
    
There are a number of options that can be configured using `PlayOptions`. These
include filtering the stream by time and stream. Playback speed can also be
controlled using this configuration.

## Viewing the contents of a journal

`RxJournal` is created and stored to disk using the low latency Chronicle-Queue library.
The data can be examined in plain ASCII using the writeToDisk function:

    rxJournal.writeToDisk(String fileName, boolean printToSdout)
    
## Putting it together with HelloWorld


Full code example code [HelloWorldApp].

```java
    package org.rxjournal.examples.helloworld;
    
    import io.reactivex.Flowable;
    import io.reactivex.Observable;
    import org.rxjournal.impl.PlayOptions;
    import org.rxjournal.impl.RxJournal;
    import org.rxjournal.impl.RxPlayer;
    import org.rxjournal.impl.RxRecorder;
    
    import java.io.IOException;
    
    /**
     * Simple Demo Program
     */
    public class HelloWorld {
        public static void main(String[] args) throws IOException {
            //Create the rxRecorder and delete any previous content by clearing the cache
            RxJournal rxJournal = new RxJournal("/tmp/Demo");
            rxJournal.clearCache();
    
            Flowable<String> helloWorldFlowable = Flowable.just("Hello World!!");
            //Pass the flowable into the rxRecorder which will subscribe to it and record all events.
            RxRecorder rxRecorder = rxJournal.createRxRecorder();
            rxRecorder.record(helloWorldFlowable);
    
            RxPlayer rxPlayer = rxJournal.createRxPlayer();
            Observable recordedObservable = rxPlayer.play(new PlayOptions());
    
            recordedObservable.subscribe(System.out::println);
            
            //Sometimes useful to see the recording written to a file
            rxJournal.writeToFile("/tmp/Demo/demo.txt",true);
        }
    }
```    
The results of running this program can be seen below:

````
[main] INFO org.rxjournal.impl.RxJournal - Deleting existing recording [/tmp/Demo]
Hello World!!
[main] INFO org.rxjournal.impl.RxJournal - Writing recording to dir [/tmp/Demo/demo.txt]
[main] INFO org.rxjournal.impl.RxJournal - VALID	1	2017-05-19T08:52:27.156		Hello World!!
[main] INFO org.rxjournal.impl.RxJournal - COMPLETE	2	2017-05-19T08:52:27.157		EndOfStream{}
[main] INFO org.rxjournal.impl.RxJournal - Writing to dir complete
````

## FAQ

### What types of data can be serialised by RxJournal

Items that can be serialised to RXJournal are those that can be serialised to 
Chronicle-Queue.

These are:
* AutoBoxed primitives
* Classes implementing [Serialisable]
* Classes implementing [Marshallable]

See [here](chronicle docs) for full documentation

### Flowable or Observable?

RxJava2 is divided into 2 types of streams `Flowable` which support back pressure
and `Observable` which do not support back pressure.

In terms of recording, `RxRecorder` supports both `Flowable` and `Observable`.  A subscription
is made to either and the data recorded is serialised into `RxJournal`.

On the other hand, `RxPlayer` returns an `Observable` because by definition there will be
no back pressure to worry about. 

The consumer of this Observable can process the events at 
their own speed backed up the guarantee that every item has been recorded into the journal.
If you want only the latest event (the events are replaceable) you can play the 
Observable into a Flowable that gives you the latest item. You will have a full
record of the complete stream of events whether they were dropped or not. You can even
record the processed event into the RxJournal again under a different filter. If you want a
record of the events that were actually processed.

### RxJournal on the critical path or as another subscriber 
 
There are 2 ways you might want to set up your RxJournal.

1. Record your Observable/Flowable input into RxJournal and then have your processor subscribe to
RxJournal for its stream of events. This effectively inserts RxJournal into the critical path of
your program. This will certainly be the setup if you are using RxJava to handle back pressure.
This is demonstrated in the example program [HelloWorldApp_JournalPlayThrough](link)

2. Have RxJournal as a second subscriber to your Observable input data. This has the benefit
of keeping all functions on the same thread. This might be the setup if you are using RxJournal
to record data for testing purposes. You might want to use the ConnectableObservable paradigm
for cold Observables as you probably don't want RxRecorder kicking off the connection until
all the other connections have been setup. 
This is demonstrated in the example program [HelloWorldApp_JounalAsObserver](link)

## Examples

There are few example applications in the code that are worth considering.

### HelloWorldApp_JournalPlayThrough

This demonstrates how to set up a simple 'play through' example.
 
We have an input `Flowable` with a stream of `Byte`s. These are recorded in the journal 
by `RxRecorder`.

We then subscribe to `RxJournal` with `RxPlayer` giving us an `Observable` of `Bytes`
which are processed by the `BytesToWordsProcessor`. The output of the processor is 
also recorded into `RxJournal` so we have a full record of all our input and outputs to
the program.

Note that we use `recordAsync` rather than `record` because otherwise we would 
block the main thread until all the event stream had completed recording and only
then would we proceed to process the items. Although in this trivial example
it's hard to see the effect this has I encourage you to play with the `INTERVAL_MS`
setting to see what happens as you increase the delay to something noticeable.
Then try and change `recordAsync` to `async` and you will see the effect of
the threading.

We then display the results of the program to stdout as well as writing to a file.

This recording will be valuable when it comes to writing a unit test for 
`BytesToWordsProcessor` which we'll see in another example.

### HelloWorldApp_JournalAsObserver

This is very similar to the last example except that we processes everything 
on the same thread. We can do this because rather than the `BytesToWordsProcessor`
subscribing to `RxJournal` it subscribes directly to the `Observable<Byte>` input.

This is a less intrusive way to insert RxRecorder into your project but of
course will not handle the back pressure problem.

### HelloWorldTest

This example demonstrates how to use RxRecorder in a unit test. The journal file
we created in the previous examples is used as input to test the `BytesToWordsProcessor`.
The results of `BytesToWordsProcessor` are fed into `RxValidator` which compares 
the output to the output which was recorded in the journal reporting any 
differences. 

We have effectively black boxed the inputs and outputs to `BytesToWordsProcessor` and can 
be confident that any changes we make to the processor will not break the existing 
behaviour.

### HelloWorldRemote

This example is designed to show how RxJOurnal can be used to tranfer data between JVMs.

Start `HelloWorldApp_JournalPlayThrough` but increase the `INTERVAL_MS` to 1000. Then
run `HelloWorldRemote`.

`HelloWorldRemote` has been configured with this option:

```` java
new PlayOptions().filter(HelloWorldApp_JounalAsObserver.INPUT_FILTER).playFromNow(true);
````

The playFromNow means that it will only consume current events and depending on how long 
a gap you have between starting the 2 programs you will see output which looks something 
like this:
 


### FastProducer SlowConsumer

In these example programs we deal with the situation where we find ourselves with a
fast producer and slow consumer. 

`FastProducer` produces `MarketData` which is consumer by `SlowConsumerObserver` (in
the case of Observable) or `SlowConsumerSubscriber` (in the case of Flowable).
 
