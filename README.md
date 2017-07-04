# ReactiveJournal

ReactiveJournal supports [Reactive](http://www.reactive-streams.org/) libraries by adding 
functionality to record and replay reactive streams. 

## Downloading the project

### Maven
ReactiveJournal is a Maven project so you can `git clone` the project and build in the usual way.

The intention is for this project to make its way into Maven Central (work in progress).

### Download the jar
Go to the releases section of the project. With each release there will be an uber jar that you 
can download with the ReactiveJournal classes and all dependencies.

Once downloaded you can test that it works by running:

//todo update this when release
````
java -cp ./reactivejournal-x.x.x.jar org.reactivejournal.examples.helloworld.HelloWorld 
````
## Why ReactiveJournal??

Picture this scenario...

You've just released your shiny new Reactive project. Streams of data start to flow into the system,
your 'Reactives' are busy processing the data, enriching it, making decisions based on it, creating
new streams of data for Reactives further down the line to consume. It's the well oiled engine of
an F1 car, a
thing of beauty, poetry in motion. Exactly what a Reactive system was designed to do.

But then...

Something starts going wrong. Perhaps data received isn't quite as clean as it should be, 
the system gets dirty, components clog up and fail, data backs up. Processes slow down,
before long we have total melt down. Our F1 engine is a heap a smouldering metal.

But what went wrong... this is what we need to be able to our system:

* Recreate the exact situation so that can step through it and understand
where the root of the problem is.

* Capture that scenario fix it and include it in a test case so that it never 
happens again.

* Monitor the slowdown of the system by observing the queues build up. Recover
the system without losing any data in the meantime.

* Deal with the surge in data that caused the component to start runing
out of memory. 

* Run multiple processes to process our data rather than relying on the one
which went over.

* All the above without slowing the system down or having to do any development.

Journaling addresses all these points. ReactiveJournal was designed to makes journaling for 
Reactive programs as simple as it could possible be.
 

## Primary Motivations Behind ReactiveJournal

### 1. Testing

Testing is a primary motivation for ReactiveJournal. ReactiveJournal allows developers to
black box test their code by recording all inputs and outputs in and out of their programs.

An obvious use case are unit tests where ReactiveJournal recordings can be used to create
comprehensive tests (see [HelloWorldTest](https://github.com/danielshaya/reactivejournal/blob/master/src/main/java/org/reactivejournal/examples/helloworld/HelloWorldTest.java) for an example). This example makes use of
`ReactiveValidator` which allows unit tests to compare their results against previously
recorded results in the journal.

Another powerful use case is to enable users to replay production data into test systems. 
By simply copying over the journal file from a production system and replaying 
all or part of the file
into a test system the exact conditions of the primary system will be reproduced.

### 2. Remote Connections

ReactiveJournal can be recorded on one JVM and can be replayed (in real-time if required) 
on one or more 
JVMs provided they have access to the journal file location.  

The remote connection can either read from the beginning of the recording or just start with live 
updates from the recorder. The remote connection (the 'listener') can optionally write back to the 
journal effecting a two way conversation or RPC. There can be multiple readers and writers to the
journal.

ReactiveJournal uses [Chronicle-Queue](https://github.com/OpenHFT/Chronicle-Queue/blob/master/README.adoc) (a memory mapped file solution) 
serialisation meaning that
the process of moving data from one JVM to another is exceedingly efficient and can be achieved 
in single digit micro seconds. 

If you need to pass data between JVMs on the same machine this is not only the most efficient way 
to do so but you will also provide you with a full recording of the data that is 
transferred between the JVMs.

Setting up remote listeners allows a system to have live (hot) backup processes.  Another
powerful strategy is to set
up processes running different code bases so that you can live test new code or play with 
alternative algorithms and compare results with the exisiting sytem.

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

In such scenarios you can publish your data to ReactiveJournal which shouldn't have any problems
keeping up with most feeds (typically writing in the order of 1,000,000/s). You can then read
your data from ReactiveJournal knowing that the data is safely stored on disk without having to
keep any data in memory buffers.

See more about this topic below in the [Examples](https://github.com/danielshaya/reactivejournal/tree/master/src/main/java/org/reactivejournal/examples) section

## Design Goals

- Recording to the journal is transactional i.e. no data will be lost if the 
program crashes
- Recording and playback is so fast that it won't slow down the host program.
- Recording and playback can be achieved without any gc overhead
- RxRecorder can be easily added (or even retro-fitted) into any `Reactive` project

# Quick Start
## Creating a Journal

An `ReactiveJournal` is created as follows:

    ReactiveJournal reactiveJournal = new ReactiveJournal(String dir);

The directory is the location where the serialised file will be created 

## Recording a reactive stream
`ReactiveRecorder` allows any Reactive [`Publisher`](https://github.com/reactive-streams/reactive-streams-jvm/blob/master/api/src/main/java/org/reactivestreams/Publisher.java) to be journaled to disk using 
the `record` function:
    
    ReactiveRecorder reactiveRecorder = reactiveJournal.createReactiveRecorder();
    reactiveRexcorder.record(Publisher)

For notes on threading see FAQ below.

## Playing back a reactive stream

`ReactivePlayer` is used to playback the journal recording:

    ReactivePlayer reactivePlayer = reactiveJournal.createReactivePlayer();
    Publisher = reactivePlayer.play(new PlayOptions());
    
There are a number of options that can be configured using `PlayOptions`. These
include filtering the stream by time and stream. Playback speed can also be
controlled using this configuration.

## Viewing the contents of a journal

`ReactiveJournal` is created and stored to disk using the low latency Chronicle-Queue library.
The data can be examined in plain ASCII using the writeToDisk function:

    reactiveJournal.writeToDisk(String fileName, boolean printToSdout)

## Putting it together with HelloWorld

Full code example code [HelloWorldApp](https://github.com/danielshaya/ReactiveJournal/blob/master/src/main/java/org/ReactiveJournal/examples/helloworld/HelloWorld.java).

```java
package org.reactivejournal.examples.helloworld;

import org.reactivejournal.impl.PlayOptions;
import org.reactivejournal.impl.ReactiveJournal;
import org.reactivejournal.impl.ReactiveRecorder;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.File;
import java.io.IOException;

/**
 * Simple Demo Program
 */
public class HelloWorld {
    private static final String tmpDir = System.getProperty("java.io.tmpdir");

    public static void main(String[] args) throws IOException {
        //1. Create the reactiveRecorder and delete any previous content by clearing the cache
        ReactiveJournal reactiveJournal = new ReactiveJournal(tmpDir + File.separator + "HW");
        reactiveJournal.clearCache();

        //2. Create a HelloWorld Publisher
        Publisher<String> helloWorldFlowable = subscriber -> {
            subscriber.onNext("Hello World!");
            subscriber.onComplete();
        };

        //3. Pass the Publisher into the reactiveRecorder which will subscribe to it and record all events.
        ReactiveRecorder reactiveRecorder = reactiveJournal.createReactiveRecorder();
        reactiveRecorder.record(helloWorldFlowable, "");

        //4. Subscribe to ReactiveJournal and print out results
        Publisher recordedObservable = reactiveJournal.createReactivePlayer().play(new PlayOptions());

        recordedObservable.subscribe(new Subscriber() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Object o) {
                System.out.println(o);
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println(throwable);
            }

            @Override
            public void onComplete() {
                System.out.println("Hello World Complete");
            }
        });

        //5. Sometimes useful to see the recording written to a file
        reactiveJournal.writeToFile(tmpDir + File.separator + "/hw.txt",true);
    }
}
```    
The results of running this program can be seen below:

````
[main] INFO org.reactivejournal.impl.ReactiveJournal - Deleting existing recording [/tmp/HW/.reactiveJournal]
Hello World!
[main] INFO org.reactivejournal.impl.ReactiveJournal - Writing recording to dir [/tmp//hw.txt]
Hello World Complete
[main] INFO org.reactivejournal.impl.ReactiveJournal - VALID	1	1499202194782		Hello World!
[main] INFO org.reactivejournal.impl.ReactiveJournal - COMPLETE	2	1499202194784		EndOfStream{}
[main] INFO org.reactivejournal.impl.ReactiveJournal - Writing to dir complete
````

## FAQ

### What types of data can be serialised by ReactiveJournal

Items that can be serialised to ReactiveJournal are those that can be serialised to 
Chronicle-Queue.

These are:
* AutoBoxed primitives, Strings and byte[]
* Classes implementing `Serialisable`
* Classes implementing `Externalizable`
* Classes implementing `Marshallable`

See [Chronicle Queue Docs](https://github.com/OpenHFT/Chronicle-Queue#restrictions-on-topics-and-messages) for full documentation

### How to use in conjunction with Reactive implementations such as RxJava and React?

`ReactiveJava` has been designed be used with any [Reactive](https://github.com/reactive-streams) implentation 
such as [RxJava](https://github.com/ReactiveX/RxJava) and [Reactor](https://projectreactor.io/).

For example if you had an RxJava `Flowable`, because `Flowable` implements `Publisher`, 
you could use the `Flowable` as input to 
`ReactiveRecorder.record()` which takes a `Publisher`. ()If you have an RxJava
`Observable` you would have to convert it to a `Flowable` with `Observable.toFlowable()`
first.)

`ReactivePlayer` returns a `Publisher` that can be converted to `Flowable` with
 `Flowable.fromPublisher(reactivePlayer.play(options))`. There is utility class
 `RxPlayer` that does this for the RxJava user. 


### ReactiveJournal on the critical path or as another subscriber 
 
There are 2 ways you might want to set up your `ReactiveJournal`.

1. Record your `Publisher` input into `ReactiveJournal` and then subscribe to
`ReactiveJournal` for its stream of events. This effectively inserts `ReactiveJournal` 
into the critical path of
your program. This will certainly be the setup if you are using ReactiveJournal 
to handle back pressure.
This is demonstrated in the example program [HelloWorldApp_JournalPlayThrough](https://github.com/danielshaya/reactivejournal/blob/master/src/main/java/org/reactivejournal/examples/helloworld/HelloWorldApp_JournalPlayThrough.java)

2. Have `ReactiveJournal` as a second subscriber to your `Publisher`. This has the benefit
of keeping all functions on the same thread. This might be the setup if you are using `ReactiveJournal`
to record data for testing purposes. If you are using RxJava you might want to use 
the `ConnectableObservable` paradigm
as you won't want ReactiveRecorder kicking off the connection until
all the other connections have been setup. 
This is demonstrated in the example program [HelloWorldApp_JounalAsObserver](https://github.com/danielshaya/reactivejournal/blob/master/src/main/java/org/reactivejournal/examples/helloworld/HelloWorldApp_JounalAsObserver.java)

### Play the stream in actual time or fast

The `RxPlayer` can `play` in two modes:
* `ACTUAL_TIME` This plays back the stream preserving the time gaps between the events. This is
important for back testing and reproducing exact conditions in unit tests.
* `FAST` This plays the events as soon as they are received. This mode should be set when you are using 
ReactiveJournal for remote connections or when using RxJounal to deal with back pressure.

### Can ReactiveJournal be used in a low latency environment

The intention is for `ReactiveJournal` to support low latency programs. The two main features to allow
for this are:
* Dedicating a CPU core to RxPlayer by using the `PlayOptions.setPauseStrategy(PauseStrategy.SPIN)` 
setting so that we don't have any context switching on the thread reading from the journal.
* Setting the `PlayOptions.using()` so that there is no allocation for new events. This should enable
programs to be written that have minimal GC impact, critical for reliable low latency.

## Examples

There are few core example applications in the code that work through the typical
use cases and are worth considering in more detail.

### HelloWorldApp_JournalPlayThrough

This program demonstrates how to set up a simple 'play through' example.
 
We have an input `Flowable` with a stream of `Byte`s. These are recorded in the journal 
by `ReactiveRecorder`.

We then subscribe to `ReactiveJournal` with `ReactivePlayer` giving us an `Flowable` of `Byte`s
which are processed by the `BytesToWordsProcessor`. The output of the processor is 
also recorded into `ReactiveJournal` so we have a full record of all our input and outputs to
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
subscribing to `ReactiveJournal` it subscribes directly to the `Observable<Byte>` input.

This is a less intrusive way to insert RxRecorder into your project but of
course will not handle the back pressure problem.

### HelloWorldTest

This example demonstrates how to use RxRecorder in a unit test. The journal file
we created in the previous examples is used as input to test the `BytesToWordsProcessor`.
The results of `BytesToWordsProcessor` are fed into `ReactiveValidator` which compares 
the output to the output which was recorded in the journal reporting any 
differences. 

We have effectively black boxed the inputs and outputs to `BytesToWordsProcessor` and can 
be confident that any changes we make to the processor will not break the existing 
behaviour.

### HelloWorldRemote

This example is designed to show how ReactiveJournal can be used to tranfer data between JVMs.

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

#### ReactiveJournalBackPressureBuffer

In this program we set up `ReactiveJournal` to handle back pressure in the buffer mode 
but solving all the problems that we saw with the standard RxJava `BUFFER` mode.

The FastProducer can be created with
the `BackpressureStrategy.MISSING` because we don't expect that the producer will ever be
slowed down by the consumer, which in this case is `RxRecorder`.

The Consumer, rather than subscribing directly to the FastProducer, subscribes to 
`RxPlayer`.  Note that `RxPlayer.play` returns an `Obserable` as there is no need for it to
handle back pressure because bakc pressure has already been applied using `ReactiveJournal` as the 
buffer.

Lets look at the problems `BackpressureStrategy.MISSING` and see how they are solved.
    
* Even if the program crashes everything written to ReactiveJournal is safe. The events will be stored
    to disk and you can just restart the program and carry on consuming the queue at the point 
    you crashed.  If there is a OS/Machine level issue it is possible that a few messages might
    get lost that are waiting to be written to disk.  
    If that is a problem you should make sure that replication is setup on your system.
* There is no in-memory buffer so there is no need to run with extra heap memory and the program
certainly won't run out memory because of `ReactiveJournal`. 
* When you call `RxPlayer.play` one of the the options is `using`. This allows you to pass in the
object that will be used for every event. This means that no new objects will be allocated even
if you have millions of items in your stream. (Of course if you want to hole a reference to the
event you will need to clone).

In addition to those benefits you will have the ususal benefits of using 'RxJornal' in that you
will have a full record of the stream to use in testing and you will be able to use remote
JVMs.

#### ReactiveJournalBackPressureLatest

As its name implies this demo program shows you how to handle back pressure using `ReactiveJournal`
but rather than buffer you just want the latest item on the queue.

All you have to do is set up the program exactly as we did in the previous example 
`ReactiveJournalBackPressureBuffer` but rather than the slow subscriber subscribing to the Observable
that comes from `RxPlayer.play` we insert a `Flowable` inbetween. The `Flowable` is created 
with `BackpressureStrategy.LATEST`.

See code snippet from the example below:

```java
    //1. Get the stream of events from the RxPlayer
    ConnectableObservable journalInput = reactiveJournal.createRxPlayer().play(options).publish();

    //2. Create a Flowable with LATEST back pressure strategy from the ReactiveJournal stream
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
sure it uses the LATEST strategy we also record the values we actaully consumer into ReactiveJournal.

As with the plain RxJava implementation of LATEST (without ReactiveJournal) the Slow Consumer
only sees the latest updates from the Fast Producer. However if you use RxRecorder (as in this
example) you have:
* A full record of all the events that were emitted by the Fast Producer. 
* A full record of all the events that were actaully consumed by the Slow Producer.

Both these streams can be played back with `RxPlayer` by specifying the appropriate filter
in the `PlayOptions` when calling `play`.

This leads to being ablse to try the following...

#### ReactiveJournalBackPressureTestingFasterConsumer

In this example we experiment by replaying the event stream recorded in `ReactiveJournal` and 
observing the effects of lowering the latency of the SlowConsumer.

We have a recording of the FastProducer created whilst running `ReactiveJournalBackPressureBuffer`.
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

| Option        | Are           | Cool  |
| ------------- |:-------------:| -----:|
| replayRate      | right-aligned | $1600 |
| filter    | centered      |   $12 |
| using | are neat      |    $1 |

## Acknowlegments

Special thanks to my friend and ex-collegue [Peter Lawrey](https://stackoverflow.com/users/57695/peter-lawrey)
for inspiring me with his [Chronicle libraries](https://github.com/OpenHFT) which underpin ReactiveJournal.

To those behind [RxJava](https://github.com/ReactiveX/RxJava) in particular to 
[Tomasz Nurkiewicz](http://www.nurkiewicz.com) for his talks and book which
opened my eyes to RxJava.
 
