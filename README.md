# RxJournal

RxJournal augments the popular [RxJava](https://github.com/ReactiveX/RxJava) library by adding functionality to record
and play reactive streams. 

## Primary Motivations Behind RxJournal

### 1. Testing

Testing is a primary motivation for RxJournal. It effectively allows developers to
blackbox test their code by recording all inputs and outputs in and out of their programs.

One possible use case are unit tests where RxJournal recordings can be used to create
comprehensive tests (see [RxPlayerTest] for an example where this is done in this project).

Another powerful use case is to enable users to replay production data into test systems. 
By simply copying over the journal file from a production system and replaying all or part of the file
into a test system the exact conditions of the primary system should be able to be reproduced.

### 2. Remote Connnections

RxJournal can be recorded on one JVM and can be replayed on a another JVM that has access to
the file location.  

The remote connection can read from the beginning of the recording or just start with live 
updates from the recorder. The remote connection (the 'listener') can write back to the 
journal effecting a two way conversation. There can be multiple readers and writers to the
journal.

The journal is serialised using Chronicle-Queue to a memory mapped file so
the process of moving data from one JVM to another is exceedingly efficient and can be achieved 
in single digit micro seconds. 

If you need to pass data between JVMs on the same machine this is not only the most efficient way 
to do so but you will also have a full recording of the data that goes between the JVMs.

### 3. Slow consumers (handling back pressure)

If you have a fast producer that you can't slow down but your consumer can't keep up
there are a few options available to your system.

Most often you end up implementing strategies that hold buffers of data in memory until the
consumer catches up. The problem with those sort of strategies are one, if your process
crashes you lose all the data in your buffer, if you need to consume the fast data in a 
transactional manner this will not be an option. Two, you may run out of memory if the 
buffers get really big. At the very least you will probably need to run your JVM with a large
memory setting that many be ineffecient. For latency sensitive applications it will 
put pressure on the GC.


## Design Goals

- Recording to the journal is transactional i.e. no data will be lost if the 
program crashes
- Recording and playback is so fast that it won't slow down the host program.
- Recording and playback can be achieved without any gc overhead
- RxRecorder can be eaily fitted into any RxJava project

# Quick Start
## Creating a Journal

An RxJournal is created as follows:

    RxJournal rxJournal = new RxJournal(String dir);

The directory is the location where the serialised file will be created 

## Recording a reactive stream
`RxRecorder` allows any RxJava `Observable`/`Flowable` to be journalled to disk using 
the record function:
    
    RxRecorder rxRecorder = rxJournal.createRxRecorder();
    rxRexcorder.record(Observable)

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
    
There are 3 primary envisaged purposes for RxRecorder.

## Putting it together (full code sample)


Full code example code org.rxrecorder.examples.HelloWorldApp.

Thread 1: HelloWorldApp has an Observable<Bytes> that produces a stream of bytes. 
RxRecorder subscribes to and records the bytes that are produced. 

Thread2: HelloWorldApp has a BytesToWords processor which subscribes to RxRecorder 
creating a stream of words which also recorded by RXRecorder.

See diagram below.

Since the input has been recorded we are able to test the BytesToWordsProcessor directly from RxRecorder without
the original Observable<Bytes>. Furthermore since the output was recorded we can validate
the output against the original output of the program.
