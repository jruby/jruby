package org.jruby.ext.zlib;

public final class Zlib {
    private Zlib() {}
    
    // wbits
    public final static int MIN_WBITS = 8;
    public final static int MAX_WBITS = 15;
    // flush
    public static final byte Z_NO_FLUSH = (byte) 0;
    public static final byte Z_SYNC_FLUSH = (byte) 2;
    public static final byte Z_FULL_FLUSH = (byte) 3;
    public static final byte Z_FINISH = (byte) 4;
    // compression level
    public static final byte Z_NO_COMPRESSION = (byte) 0x0;
    public static final byte Z_BEST_SPEED = (byte) 0x1;
    public static final byte Z_BEST_COMPRESSION = (byte) 0x9;
    public static final byte Z_DEFAULT_COMPRESSION = (byte) -1;
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
    // strategy
    public static final byte Z_FILTERED = (byte) 1;
    public static final byte Z_HUFFMAN_ONLY = (byte) 2;
    public static final byte Z_DEFAULT_STRATEGY = (byte) 0;
    // data_type
    public static final byte Z_BINARY = (byte) 0;
    public static final byte Z_ASCII = (byte) 1;
    public static final byte Z_UNKNOWN = (byte) 2;
    // from zlib.c in ruby
    public static final byte GZ_MAGIC_ID_1 = (byte) 0x1f;
    public static final byte GZ_MAGIC_ID_2 = (byte) 0x8b;
    public static final byte GZ_METHOD_DEFLATE = (byte) 8;
    public static final byte GZ_FLAG_MULTIPART = (byte) 0x2;
    public static final byte GZ_FLAG_EXTRA = (byte) 0x4;
    public static final byte GZ_FLAG_ORIG_NAME = (byte) 0x8;
    public static final byte GZ_FLAG_COMMENT = (byte) 0x10;
    public static final byte GZ_FLAG_ENCRYPT = (byte) 0x20;
    public static final byte GZ_FLAG_UNKNOWN_MASK = (byte) 0xc0;
    public static final byte GZ_EXTRAFLAG_FAST = (byte) 0x4;
    public static final byte GZ_EXTRAFLAG_SLOW = (byte) 0x2;
}