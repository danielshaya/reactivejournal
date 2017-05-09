# RxRecorder

RxRecorder is a utility library that can be used in conjunction with RxJava.

Simply put it allows any Observable to be journalled to disk using the record function:

    rxRexcorder.record(Observable)
    
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