# RxJournal

##Note README needs to be updated for latest version of code which will be done shortly.
###RxJounal now supports all Reactive implementations not just RxJava

RxJournal augments the popular [RxJava](https://github.com/ReactiveX/RxJava) library by adding 
functionality to record and replay reactive streams. 

## Downloading the project

### Maven
RxJournal is a Maven project so you can clone the project and build in the usual way.

The intention is for this project to make its way into Maven Central (work in progress).

### Download the jar
Go to the releases section of the project. With each release there will be an uber jar that you 
can download with the RXJournal classes and all dependencies.

Once downloaded you can test that it works by running:

````
java -cp ./rxjournal-x.x.x.jar org.rxjournal.examples.helloworld.HelloWorld 
````

## Primary Motivations Behind RxJournal

### 1. Testing

Testing is a primary motivation for RxJournal. RxJournal allows developers to
black box test their code by recording all inputs and outputs in and out of their programs.

An obvious use case are unit tests where RxJournal recordings can be used to create
comprehensive tests (see [HelloWorldTest](todo) for an example). This example makes use of
`RxValidator` which allows unit tests to compare their results against previously
recorded results in the journal.

Another powerful use case is to enable users to replay production data into test systems. 
By simply copying over the journal file from a production system and replaying all or part of the file
into a test system the exact conditions of the primary system will be reproduced.

### 2. Remote Connections

`RxJournal` can be recorded on one JVM and can be replayed (in real-time if required) on one or more 
JVMs provided they have access to the journal file location.  

The remote connection can either read from the beginning of the recording or just start with live 
updates from the recorder. The remote connection (the 'listener') can optionally write back to the 
journal effecting a two way conversation or RPC. There can be multiple readers and writers to the
journal.

`RxJournal` uses [Chronicle-Queue](todo) (a memory mapped file solution) serialisation meaning that
the process of moving data from one JVM to another is exceedingly efficient and can be achieved 
in single digit micro seconds. 

If you need to pass data between JVMs on the same machine this is not only the most efficient way 
to do so but you will also provide you with a full recording of the data that is 
transferred between the JVMs.

### 3. Slow consumers (handling back pressure)

If you have a fast producer that you can't slow down but your consumer can't keep up
there are a few options available to your system.

Most often you end up implementing strategies that hold buffers of data in memory allowing the
consumer to catch up eventually. The problem with those sort of strategies are one, if your process
crashes you lose all the data in your buffer. Therefore if you need to consume the fast data in a 
transactional manner this will not be an option. Two, you may run out of memory if the 
buffers get really big. At the very least you will probably need to run your JVM with a large
memory setting that many be inefficient. For latency sensitive applications it will 
put pressure on the GC which will not be acceptable.

See more about this topic below in the [Examples](todo) section

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


Full code example code [HelloWorldApp](https://github.com/danielshaya/rxjournal/blob/master/src/main/java/org/rxjournal/examples/helloworld/HelloWorld.java).

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
* AutoBoxed primitives, Strings and byte[]
* Classes implementing `Serialisable`
* Classes implementing `Externalizable`
* Classes implementing `Marshallable`

See [Chronicle Queue Docs](https://github.com/OpenHFT/Chronicle-Queue#restrictions-on-topics-and-messages) for full documentation

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
 
There are 2 ways you might want to set up your `RxJournal`.

1. Record your `Observable`/`Flowable` input into `RxJournal` and then have your processor subscribe to
`RxJournal` for its stream of events. This effectively inserts `RxJournal` into the critical path of
your program. This will certainly be the setup if you are using RxJava to handle back pressure.
This is demonstrated in the example program [HelloWorldApp_JournalPlayThrough](todo)

2. Have `RxJournal` as a second subscriber to your `Observable` input data. This has the benefit
of keeping all functions on the same thread. This might be the setup if you are using `RxJournal`
to record data for testing purposes. You might want to use the `ConnectableObservable` paradigm
for cold Observables as you probably don't want RxRecorder kicking off the connection until
all the other connections have been setup. 
This is demonstrated in the example program [HelloWorldApp_JounalAsObserver](todo)

### Play the stream in actual time or fast

The `RxPlayer` can `play` in two modes:
* `ACTUAL_TIME` This plays back the stream preserving the time gaps between the events. This is
important for back testing and reproducing exact conditions in unit tests.
* `FAST` This plays the events as soon as they are recieved. Use this when you are using 
RxJournal for remote connections or when using RxJounal to deal with back pressure.

### Can RxJournal be used in a low latency environment

The intention is for `RxJournal` to support low latency programs. The two main features to allow
for this are:
* Dedicating a CPU core to RxPlayer by using the FAST setting described above so that we don't have 
any context switching.
* Setting the PlayOptions.using() so that there is no allocation for new events. This should enable
programs to be written that have minimal GC impact, critical for reliable low latency.

## Examples

There are few core example applications in the code that work through the typical
use cases and are worth considering in more detail.

### HelloWorldApp_JournalPlayThrough

This program demonstrates how to set up a simple 'play through' example.
 
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
 


### Fast Producer Slow Consumer

In these example programs we deal with the situation where we find ourselves with a
fast producer and slow consumer. 

In all these example we setup a scenario in [`FastProducerSlowConsumer`]() where the
producer emits `Long` values every millisecond. We also create a `Consumer` which
processes the Long values with a variable delay which is significantly slower
than the rate that they are being produced.

In other words we have the classic Fast Producer Slow Consumer scenario which needs
to be handled by applying back pressure.

The following example programs have all been written to 'solve' the back pressure 
problem we have created.

#### RxJavaBackPressure

Firstly let's consider how RxJava handles back pressure out of the box. 

A quick reminder, in RxJava2 the code was split into 2 sections:
* `Observable` - no back pressure. Use when back pressure is not an issue because the
code is more efficient not having to deal with this complication.
* `Flowable` - handles back pressure. Use when you have to address the back pressure issue.
 
Clearly we will only be looking at the `Flowable` part of RxJava2 in this example.

This example program demonstrates how the 5 `BackpressureStrategy` modes handle back
pressure.
* `BUFFER` this will, as its name implies, hold the items in an in-memory buffer waiting
for availablility on the consumer to process them. This is good choice for handling spikes
in event traffic where the consumer will eventually be able to catch up with the 
producer. The problems using this strategy are:

    * If the program crashes the events in the buffer will be lost. Even if the 
    program terminates normally careful attention has o be paid to draining the buffer.
    * If the queue builds up too much the JVM will run out of memory and crash.
    * It forces the program to run with a large memory setting to hold the buffer
    which can be a problem for programs where latency is an issue especially coupled
    with the next point.
    * The program will not be able to be designed in an allocation-free manner. Every
    item will have to be created in a 'new' object which will then put pressure on the GC.

* `LATEST` and `DROP` deal with back pressure by making the slow consumer keep up with
the fast producer. This is done by dropping events from the stream. This is a good choice
where events on the stream are replaceable and you don't need to process every item. The
problems with this straegy are:

    * If you want to back test your program against all the values in the stream to see
      if you might get better results by processing more events.
    * As with buffer you can't write GC friendly code.
    
* `ERROR` and `MISSING` deal with back pressure by putting the program into an error state
as soon as back pressure is encountered. This is useful when you don't expect any back pressure
and you want the program to error on encountering back pressure. 

#### RxJournalBackPressureBuffer

In this program we set up `RxJournal` to handle back pressure in the buffer mode 
but solving all the problems that we saw with the standard RxJava `BUFFER` mode.

The FastProducer can be created with
the `BackpressureStrategy.MISSING` because we don't expect that the producer will ever be
slowed down by the consumer, which in this case is `RxRecorder`.

The Consumer, rather than subscribing directly to the FastProducer, subscribes to 
`RxPlayer`.  Note that `RxPlayer.play` returns an `Obserable` as there is no need for it to
handle back pressure because bakc pressure has already been applied using `RxJournal` as the 
buffer.

Lets look at the problems `BackpressureStrategy.MISSING` and see how they are solved.
    
* Even if the program crashes everything written to RxJournal is safe. The events will be stored
    to disk and you can just restart the program and carry on consuming the queue at the point 
    you crashed.  If there is a OS/Machine level issue it is possible that a few messages might
    get lost that are waiting to be written to disk.  
    If that is a problem you should make sure that replication is setup on your system.
* There is no in-memory buffer so there is no need to run with extra heap memory and the program
certainly won't run out memory because of `RxJournal`. 
* When you call `RxPlayer.play` one of the the options is `using`. This allows you to pass in the
object that will be used for every event. This means that no new objects will be allocated even
if you have millions of items in your stream. (Of course if you want to hole a reference to the
event you will need to clone).

In addition to those benefits you will have the ususal benefits of using 'RxJornal' in that you
will have a full record of the stream to use in testing and you will be able to use remote
JVMs.

#### RxJournalBackPressureLatest

As its name implies this demo program shows you how to handle back pressure using `RxJournal`
but rather than buffer you just want the latest item on the queue.

All you have to do is set up the program exactly as we did in the previous example 
`RxJournalBackPressureBuffer` but rather than the slow subscriber subscribing to the Observable
that comes from `RxPlayer.play` we insert a `Flowable` inbetween. The `Flowable` is created 
with `BackpressureStrategy.LATEST`.

See code snippet from the example below:

```java
    //1. Get the stream of events from the RxPlayer
    ConnectableObservable journalInput = rxJournal.createRxPlayer().play(options).publish();

    //2. Create a Flowable with LATEST back pressure strategy from the RxJournal stream
    Flowable flowable = journalInput.toFlowable(BackpressureStrategy.LATEST);

    //3. Record the output of the Flowable into the journal (note the different filter name)
    recorder.record(flowable, "consumed");

    long startTime = System.currentTimeMillis();
    
    //4. The slow consumer subscribes to the Flowable 
    flowable.observeOn(Schedulers.io()).subscribe(onNextSlowConsumer::accept,
            e -> System.out.println("RxRecorder " + " " + e),
            () -> System.out.println("RxRecorder complete [" + (System.currentTimeMillis()-startTime) + "]")
    );
```

You might have noticed that as well as the Slow Consumer subscribing to the Flowable to make
sure it uses the LATEST strategy we also record the values we actaully consumer into RxJournal.

As with the plain RxJava implementation of LATEST (without RxJournal) the Slow Consumer
only sees the latest updates from the Fast Producer. However if you use RxRecorder (as in this
example) you have:
* A full record of all the events that were emitted by the Fast Producer. 
* A full record of all the events that were actaully consumed by the Slow Producer.

Both these streams can be played back with `RxPlayer` by specifying the appropriate filter
in the `PlayOptions` when calling `play`.

This leads to being ablse to try the following...

#### RxJournalBackPressureTestingFasterConsumer

In this example we experiment by replaying the event stream recorded in `RxJournal` and 
observing the effects of lowering the latency of the SlowConsumer.

We have a recording of the FastProducer created whilst running `RxJournalBackPressureBuffer`.
The SlowConsumer subscribes to this using a `Flowable` with `BackpressureStrategy.LATEST`
as in the provious example.

When we run with the SlowConsumer at a latency of 5ms we get this result:

````
Received [100] items. Published item[100]
Received [200] items. Published item[391]
Received [300] items. Published item[791]
Received [400] items. Published item[1175]
Received [500] items. Published item[1560]
Received [600] items. Published item[1946]
Received [700] items. Published item[2340]
RxRecorder complete [3909ms]
````

The Slow Consumer has managed to consume about 700 events.
 
If we reduce the latency of SlowConsumer to 3ms we get this result:

````
Received [100] items. Published item[100]
Received [200] items. Published item[265]
Received [300] items. Published item[491]
Received [400] items. Published item[719]
Received [500] items. Published item[958]
Received [600] items. Published item[1192]
Received [700] items. Published item[1428]
Received [800] items. Published item[1664]
Received [900] items. Published item[2046]
Received [1000] items. Published item[2288]
RxRecorder complete [3666ms]
````

The Slow Consumer has now managed to consume about 1000 events.
 
Whilst this is a trivial example I'll let your imagination extend the scenarios
to real world situations where this sort of ability to replay data against real
load will be invaluable.

## Acknowlegments

Special thanks to my friend and ex-collegue [Peter Lawrey](https://stackoverflow.com/users/57695/peter-lawrey)
for inspiring me with his [Chronicle libraries](https://github.com/OpenHFT) which underpin RxJournal.

To those behind [RxJava](https://github.com/ReactiveX/RxJava) in particular to 
[Tomasz Nurkiewicz](http://www.nurkiewicz.com) for his talks and book which
opened my eyes to RxJava.
 
