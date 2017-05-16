# RxJournal

RxJournal augments the popular RxJava library by adding functionality to record
and play reactive streams. 

## Primary Motivations Behind RxJournal

### Testing

Testing is a primary motivation for RxRecorder. It effectively allows developers to
blackbox test their code by recording all inputs and outputs into their programs.

It can be using for unit testing.

It also allows users to replay production data into test systems by just copying over a file.

### Remote Connnections
### Slow consumers (handling back pressure)

## Design Goals

- Recording to the journal is transactional i.e. no data will be lost if the 
program crashes
- Recording and playback is so fast that it won't slow down the host program.
- Recording and playback can be achieved without any gc overhead
- RxRecorder can be eaily fitted into any RxJava project

#Quick Start
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

##Putting it together (full code sample)


Full code example code org.rxrecorder.examples.HelloWorldApp.

Thread 1: HelloWorldApp has an Observable<Bytes> that produces a stream of bytes. 
RxRecorder subscribes to and records the bytes that are produced. 

Thread2: HelloWorldApp has a BytesToWords processor which subscribes to RxRecorder 
creating a stream of words which also recorded by RXRecorder.

See diagram below.

Since the input has been recorded we are able to test the BytesToWordsProcessor directly from RxRecorder without
the original Observable<Bytes>. Furthermore since the output was recorded we can validate
the output against the original output of the program.
