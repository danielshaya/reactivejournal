# RxRecorder

RxRecorder enhances RxJava by facilitating record/play functionality. 

## Design Goals

- Recording is transactional i.e. no data will be lost if the program crashes
- Recording and playback is extremely fast
- Recording and playback can be achieved without any gc overhead
- RxRecorder can be eaily fitted into any RxJava project

#Quick Start
## Creating a Recorder

An RxRecorder is created as follows:

    RxRecorder rxRecorder = new RxRecorder(String dir, boolean clearCacheFlag)

The directory is the location where the serialised file will be created
The clearCache flag determines whether 

## Recording
RxRecorder allows any RxJava Observable/Flowable to be journalled to disk using the record function:

    rxRexcorder.record(Observable)

The 

the Observable can then be played from disk using the play function:

    rxRecorder.play(PlayOptons)

The journal is created and stored to disk using the low latency Chronicle-Queue library.
The data can be examined in plain ASCII using the writeToDisk function:

    rxRecorder.writeToDisk(File)
    
There are 3 primary envisaged purposes for RxRecorder.

### Testing

Testing is a primary motivation for RxRecorder. It effectively allows developers to
blackbox test their code by recording all inputs and outputs into their programs.

It can be using for unit testing.

It also allows users to replay production data into test systems by just copying over a file.

####Quick Start


Full code example code org.rxrecorder.examples.HelloWorldApp.

Thread 1: HelloWorldApp has an Observable<Bytes> that produces a stream of bytes. 
RxRecorder subscribes to and records the bytes that are produced. 

Thread2: HelloWorldApp has a BytesToWords processor which subscribes to RxRecorder 
creating a stream of words which also recorded by RXRecorder.

See diagram below.

Since the input has been recorded we are able to test the BytesToWordsProcessor directly from RxRecorder without
the original Observable<Bytes>. Furthermore since the output was recorded we can validate
the output against the original output of the program.
### Remote Connnections
### Slow consumers