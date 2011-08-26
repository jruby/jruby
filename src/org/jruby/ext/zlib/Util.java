package org.jruby.ext.zlib;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.joda.time.DateTime;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.ByteList;

import static org.jruby.ext.zlib.Zlib.*;

public final class Util {
    private Util() {
    }

    public static byte[] dumpHeader(String origName, String comment, int level, byte osCode,
            long modifiedTime) throws IOException {
        // See http://www.gzip.org/zlib/rfc-gzip.html
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte flags = 0, extraflags = 0;
        if (origName != null) {
            flags |= GZ_FLAG_ORIG_NAME;
        }
        if (comment != null) {
            flags |= GZ_FLAG_COMMENT;
        }
        if (level == Z_BEST_SPEED) {
            extraflags |= GZ_EXTRAFLAG_FAST;
        } else if (level == Z_BEST_COMPRESSION) {
            extraflags |= GZ_EXTRAFLAG_SLOW;
        }
        byte header[] = { GZ_MAGIC_ID_1, GZ_MAGIC_ID_2, GZ_METHOD_DEFLATE, flags,
                (byte) (modifiedTime), (byte) (modifiedTime >> 8), (byte) (modifiedTime >> 16),
                (byte) (modifiedTime >> 24), extraflags, OS_CODE };
        out.write(header);
        if (origName != null) {
            out.write(origName.toString().getBytes());
            out.write('\0');
        }
        if (comment != null) {
            out.write(comment.toString().getBytes());
            out.write('\0');
        }
        return out.toByteArray();
    }

    public static byte[] dumpTrailer(int originalDataSize, int checksumInt) {
        return new byte[] { (byte) (checksumInt), (byte) (checksumInt >> 8),
                (byte) (checksumInt >> 16), (byte) (checksumInt >> 24), (byte) (originalDataSize),
                (byte) (originalDataSize >> 8), (byte) (originalDataSize >> 16),
                (byte) (originalDataSize >> 24) };
    }

    public static class GzipHeader {
        public DateTime mtime;
        public int level;
        public byte osCode;
        public String origName;
        public String comment;
        public int length;

        public GzipHeader() {
        }
    }

    public static GzipHeader readHeader(Ruby runtime, InputStream in) {
        GzipHeader header = new GzipHeader();
        try {
            if ((byte) readUByte(in) != GZ_MAGIC_ID_1) {
                return null;
            }
            if ((byte) readUByte(in) != GZ_MAGIC_ID_2) {
                return null;
            }
            byte b = (byte) readUByte(in);
            if ((byte) b != GZ_METHOD_DEFLATE) {
                throw newGzipFileError(runtime, "unsupported compression method " + b);
            }
            int flags = readUByte(in);
            if ((flags & GZ_FLAG_MULTIPART) != 0) {
                throw newGzipFileError(runtime,
                        "multi-part gzip file is not supported");
            } else if ((flags & GZ_FLAG_ENCRYPT) != 0) {
                throw newGzipFileError(runtime, "encrypted gzip file is not supported");
            } else if ((flags & GZ_FLAG_UNKNOWN_MASK) != 0) {
                throw newGzipFileError(runtime, "unknown flags " + flags);
            }
            header.mtime = new DateTime(readUInt(in) * 1000);
            int extraflags = readUByte(in);
            if ((extraflags & GZ_EXTRAFLAG_FAST) != 0) {
                header.level = Z_BEST_SPEED;
            } else if ((extraflags & GZ_EXTRAFLAG_SLOW) != 0) {
                header.level = Z_BEST_COMPRESSION;
            } else {
                header.level = Z_DEFAULT_COMPRESSION;
            }
            header.osCode = (byte) readUByte(in);
            header.length += 10;
            if ((flags & GZ_FLAG_EXTRA) != 0) {
                int size = readUShort(in);
                byte[] extra = new byte[2 + size];
                // just discards it
                readBytes(in, extra);
                header.length += 2 + extra.length;
            }
            if ((flags & GZ_FLAG_ORIG_NAME) != 0) {
                header.origName = readNullTerminateString(in);
                header.length += header.origName.getBytes().length + 1;
            }
            if ((flags & GZ_FLAG_COMMENT) != 0) {
                header.comment = readNullTerminateString(in);
                header.length += header.comment.getBytes().length + 1;
            }
        } catch (IOException ioe) {
            throw newGzipFileError(runtime, ioe.getMessage());
        }
        // TODO: should check header CRC (cruby-zlib doesn't do for now)
        return header;
    }

    public static void checkTrailer(Ruby runtime, byte[] trailer, long bytesWritten, long checksum) {
        long uint = bytesToUInt(trailer, 0);
        if (uint != checksum) {
            throw newCRCError(runtime, "invalid compressed data -- crc error");
        }
        uint = bytesToUInt(trailer, 4);
        if (uint != bytesWritten) {
            throw newLengthError(runtime, "invalid compressed data -- length error");
        }
    }

    public static void resetBuffer(ByteList l) {
        l.setBegin(0);
        l.setRealSize(0);
        l.invalidate();
    }

    public static RaiseException newZlibError(Ruby runtime, String message) {
        return newZlibError(runtime, "Error", message);
    }

    public static RaiseException newBufError(Ruby runtime, String message) {
        return newZlibError(runtime, "BufError", message);
    }
    
    public static RaiseException newDictError(Ruby runtime, String message) {
        return newZlibError(runtime, "NeedDict", message);
    }

    public static RaiseException newStreamError(Ruby runtime, String message) {
        return newZlibError(runtime, "StreamError", message);
    }

    public static RaiseException newDataError(Ruby runtime, String message) {
        return newZlibError(runtime, "DataError", message);
    }

    private static RaiseException newZlibError(Ruby runtime, String klass, String message) {
        RubyClass errorClass = runtime.getModule("Zlib").getClass(klass);
        return new RaiseException(RubyException.newException(runtime, errorClass, message), true);
    }

    public static RaiseException newGzipFileError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "Error", message);
    }

    public static RaiseException newCRCError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "CRCError", message);
    }

    public static RaiseException newNoFooter(Ruby runtime, String message) {
        return newGzipFileError(runtime, "NoFooter", message);
    }

    public static RaiseException newLengthError(Ruby runtime, String message) {
        return newGzipFileError(runtime, "LengthError", message);
    }

    private static RaiseException newGzipFileError(Ruby runtime, String klass, String message) {
        RubyClass errorClass = runtime.getModule("Zlib").getClass("GzipFile").getClass(klass);
        return new RaiseException(RubyException.newException(runtime, errorClass, message), true);
    }

    private static int readUByte(InputStream in) throws IOException {
        int ret = in.read();
        if (ret == -1) {
            throw new EOFException();
        }
        return ret & 0xff;
    }

    private static int readUShort(InputStream in) throws IOException {
        return (readUByte(in) | (readUByte(in) << 8)) & 0xffff;
    }

    private static long readUInt(InputStream in) throws IOException {
        return (readUShort(in) | (readUShort(in) << 16)) & 0xffffffffL;
    }

    private static void readBytes(InputStream in, byte[] bytes) throws IOException {
        readBytes(in, bytes, 0, bytes.length);
    }

    private static void readBytes(InputStream in, byte[] bytes, int pos, int len)
            throws IOException {
        if (bytes.length < pos + len) {
            throw new IllegalArgumentException();
        }
        while (len > 0) {
            int ret = in.read(bytes, pos, len);
            if (ret == -1) {
                throw new EOFException();
            }
            pos += ret;
            len -= ret;
        }
    }

    private static String readNullTerminateString(InputStream in) throws IOException {
        StringBuilder builder = new StringBuilder();
        int c;
        while ((c = readUByte(in)) != '\0') {
            builder.append((char) c);
        }
        return builder.toString();
    }

    private static long bytesToUInt(byte[] bytes, int pos) {
        if (bytes.length < pos + 4) {
            throw new IllegalArgumentException();
        }
        return (bytes[pos++] & 0xff | (bytes[pos++] & 0xff) << 8 | (bytes[pos++] & 0xff) << 16 | (bytes[pos++] & 0xff) << 24) & 0xffffffffL;
    }
}
