package org.yarp;

public class Parser {

    public static void loadLibrary(String path) {
        System.load(path);
    }

    public static native byte[] parseAndSerialize(byte[] source);

}
