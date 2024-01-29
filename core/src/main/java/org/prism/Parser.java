package org.prism;

public abstract class Parser {

    public static void loadLibrary(String path) {
        System.load(path);
    }

    public static native byte[] parseAndSerialize(byte[] source, byte[] options);

    public static byte[] parseAndSerialize(byte[] source) {
        return parseAndSerialize(source, null);
    }

}
