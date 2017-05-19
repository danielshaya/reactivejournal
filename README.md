# RxJournal

RxJournal augments the popular [RxJava](https://github.com/ReactiveX/RxJava) library by adding 
functionality to record and replay reactive streams. 

## Primary Motivations Behind RxJournal

### 1. Testing

Testing is a primary motivation for RxJournal. RxJournal allows developers to
blackbox test their code by recording all inputs and outputs in and out of their programs.

An obvious use case are unit tests where RxJournal recordings can be used to create
comprehensive tests (see [RxPlayerTest] for an example where this is done in this project).

Another powerful use case is to enable users to replay production data into test systems. 
By simply copying over the journal file from a production system and replaying all or part of the file
into a test system the exact conditions of the primary system will be reproduced.

### 2. Remote Connnections

RxJournal can be recorded on one JVM and can be replayed on a another JVM that has access to
the journal file location.  

The remote connection can read from the beginning of the recording or just start with live 
updates from the recorder. The remote connection (the 'listener') can optionally write back to the 
journal effecting a two way conversation or RPC. There can be multiple readers and writers to the
journal.

RxJournal uses Chronicle-Queue (a memory mapped file solution) serialisation meaning that
the process of moving data from one JVM to another is exceedingly efficient and can be achieved 
in single digit micro seconds. 

If you need to pass data between JVMs on the same machine this is not only the most efficient way 
to do so but you will also provide you with a full recording of the data that is transferred between the JVMs.

### 3. Slow consumers (handling back pressure)

If you have a fast producer that you can't slow down but your consumer can't keep up
there are a few options available to your system.

Most often you end up implementing strategies that hold buffers of data in memory until the
consumer catches up. The problem with those sort of strategies are one, if your process
crashes you lose all the data in your buffer. Therefore if you need to consume the fast data in a 
transactional manner this will not be an option. Two, you may run out of memory if the 
buffers get really big. At the very least you will probably need to run your JVM with a large
memory setting that many be ineffecient. For latency sensitive applications it will 
put pressure on the GC which will not be acceptable.


## Design Goals

- Recording to the journal is transactional i.e. no data will be lost if the 
program crashes
- Recording and playback is so fast that it won't slow down the host program.
- Recording and playback can be achieved without any gc overhead
- RxRecorder can be eaily added (or even retro-fitted) into any RxJava project

# Quick Start
## Creating a Journal

An RxJournal is created as follows:

    RxJournal rxJournal = new RxJournal(String dir);

The directory is the location where the serialised file will be created 

## Recording a reactive stream
`RxRecorder` allows any RxJava `Observable`/`Flowable` to be journalled to disk using 
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

### Threading
 
Thread 1: HelloWorldApp has an Observable<Bytes> that produces a stream of bytes. 
RxRecorder subscribes to and records the bytes that are produced. 

Thread2: HelloWorldApp has a BytesToWords processor which subscribes to RxRecorder 
creating a stream of words which also recorded by RXRecorder.

See diagram below.

Since the input has been recorded we are able to test the BytesToWordsProcessor directly from RxRecorder without
the original Observable<Bytes>. Furthermore since the output was recorded we can validate
the output against the original output of the program.

## Examples

### HelloWorldApp