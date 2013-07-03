package org.jruby.ext.zlib;

public final class Zlib {
    private Zlib() {}

    // TODO: Those are defined at com.jcraft.jzlib.Deflate but private
    public static final byte Z_BINARY = (byte) 0;
    public static final byte Z_ASCII = (byte) 1;
    public static final byte Z_UNKNOWN = (byte) 2;
    // os_code
    public static final byte OS_MSDOS = (byte) 0x00;
    public static final byte OS_AMIGA = (byte) 0x01;
    public static final byte OS_VMS = (byte) 0x02;
    public static final byte OS_UNIX = (byte) 0x03;
    public static final byte OS_ATARI = (byte) 0x05;
    public static final byte OS_OS2 = (byte) 0x06;
    public static final byte OS_MACOS = (byte) 0x07;
    public static final byte OS_TOPS20 = (byte) 0x0a;
    public static final byte OS_WIN32 = (byte) 0x0b;
    public static final byte OS_VMCMS = (byte) 0x04;
    public static final byte OS_ZSYSTEM = (byte) 0x08;
    public static final byte OS_CPM = (byte) 0x09;
    public static final byte OS_QDOS = (byte) 0x0c;
    public static final byte OS_RISCOS = (byte) 0x0d;
    public static final byte OS_UNKNOWN = (byte) 0xff;
    public static final byte OS_CODE = OS_WIN32; // TODO: why we define OS_CODE to OS_WIN32?
}