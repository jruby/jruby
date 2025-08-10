package org.jruby.util;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;

import static org.jruby.util.RubyStringBuilder.inspectIdentifierByteList;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * Helpers for working with bytelists.
 */
public class ByteListHelper {
    public interface CodePoint {
        /**
         * call a command pattern caller with index of and actual codepoint.  If false is
         * returned it will let users of this interface to stop walking the codepoints.
         * @param index the index of which codepoint we are visiting.
         * @param codepoint we are visiting.
         * @return true to continue walking and false to stop.
         */
        boolean call(int index, int codepoint, Encoding encoding);
    }

    // Note: perhaps there is some great way of doing this with streams but I did not want to box index
    // with value or make someone try and deal with that outside of the closure.
    public interface Visit<T, U> {
        U call(int index, T value, U module);
    }

    /**
     * This method assumes the ByteList will be a valid string for the encoding which it is marked as.
     * It also assumes slow path mbc walking.  If you know you have an ASCII ByteList you should do something
     * else.
     * @param bytelist of the mbc-laden bytes
     * @param each the closure which walks the codepoints
     * @return true if it walks the whole bytelist or false if it stops.
     */
    public static boolean eachCodePoint(ByteList bytelist, CodePoint each) {
        byte[] bytes = bytelist.unsafeBytes();
        int len = bytelist.getRealSize();
        Encoding encoding = bytelist.getEncoding();
        int begin = bytelist.begin();
        int end = begin + len;
        int n;

        for (int i = 0; i < len; i += n) {
            int realIndex = begin + i;
            n = StringSupport.encFastMBCLen(bytes, realIndex, end, encoding);
            if (!each.call(i, encoding.mbcToCode(bytes, realIndex, end), encoding)) return false;
        }

        return true;
    }

    /**
     * If you know you have an ASCII ByteList you should do something else.  This will continue walking the
     * bytelist 'while' as long as each continues to be true.  When it stops being true it will return the
     * last byte index processed (on full walk it will be length otherwise the beginning of the codepoint
     * which did not satisfy each).
     *
     * @param bytelist of the mbc-laden bytes
     * @param offset place in bytes to search past begin
     * @param each the closure which walks the codepoints
     * @return length if all codepoints match.  index (ignoring begin) if not.
     */
    public static int eachCodePointWhile(Ruby runtime, ByteList bytelist, int offset, CodePoint each) {
        Encoding encoding = bytelist.getEncoding();

        if (encoding != USASCIIEncoding.INSTANCE) {
            return eachMBCCodePointWhile(bytelist, offset, each);
        }

        byte[] bytes = bytelist.unsafeBytes();
        int len = bytelist.getRealSize();
        int begin = bytelist.begin();
        int end = begin + len;

        for (int i = offset; i < end; i++) {
            byte c = bytes[i];
            if (!Encoding.isAscii(c)) throw runtime.newEncodingError(str(runtime, "invalid symbol in encoding " + encoding + " :" , inspectIdentifierByteList(runtime, bytelist)));
            if (!each.call(i, bytes[i] & 0xff, encoding)) return i;
        }

        return len;
    }

    // Should also call through eachCodePointWhile since it will fast path US-ASCII.
    private static int eachMBCCodePointWhile(ByteList bytelist, int offset, CodePoint each) {
        Encoding encoding = bytelist.getEncoding();
        byte[] bytes = bytelist.unsafeBytes();
        int len = bytelist.getRealSize();
        int begin = bytelist.begin();
        int end = begin + len;
        int n;

        for (int i = 0, p = begin + offset; p < end; i++, p += n) {
            n = StringSupport.length(encoding, bytes, p, end);
            if (!each.call(i, encoding.mbcToCode(bytes, p, end), encoding)) {
                return p;
            }
        }

        return len;
    }

    /**
     * This method will split a string and call a visitor for each segment between the split pattern.
     *
     * @param value to be split
     * @param pattern the pattern to split value with
     * @param bodyVisitor visitor for all but last segment
     * @param headVisitor visitor for the last segment (if null if will use bodyVisitor).
     * @param <T> Return type of visitor
     * @return last T from headVisitor
     * @deprecated This was only used by Module#const_defined, but was difficult to match MRI's equivalent in this form
     */
    @Deprecated(since = "9.4-")
    public static <T> T split(ByteList value, ByteList pattern, Visit<ByteList, T> bodyVisitor, Visit<ByteList, T> headVisitor) {
        if (headVisitor == null) headVisitor = bodyVisitor;

        Encoding enc = pattern.getEncoding();
        byte[] bytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int realSize = value.getRealSize();
        int end = begin + realSize;
        int currentOffset = 0;
        int patternIndex;
        int i = 0;
        T current = null;

        for (; currentOffset < realSize && (patternIndex = value.indexOf(pattern, currentOffset)) >= 0; i++) {
            int t = enc.rightAdjustCharHead(bytes, currentOffset + begin, patternIndex + begin, end) - begin;
            if (t != patternIndex) {
                currentOffset = t;
                continue;
            }

            current = bodyVisitor.call(i, value.makeShared(currentOffset, patternIndex - currentOffset), current);
            if (current == null) return null;

            currentOffset = patternIndex + pattern.getRealSize();
        }

        return headVisitor.call(i, value.makeShared(currentOffset, realSize - currentOffset), current);
    }
}

