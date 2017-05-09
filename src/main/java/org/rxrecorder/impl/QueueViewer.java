package org.rxrecorder.impl;

import java.io.IOException;

/**
 * Created by daniel on 03/05/17.
 */
public class QueueViewer {
    public static void main(String[] args) throws IOException {
        String fileName = args.length == 1 ? args[0] : "/tmp/MarketData";
        RxRecorder rxRecorder = new RxRecorder();
        rxRecorder.init(fileName, false);
        rxRecorder.writeToFile("/tmp/MarketData/out.txt", true);
    }
}
