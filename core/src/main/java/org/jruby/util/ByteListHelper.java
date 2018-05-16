package org.jruby.util;

import org.jcodings.Encoding;

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
     * This method will split a string and call a visitor for each segment between the split pattern.
     *
     * @param value to be split
     * @param pattern the pattern to split value with
     * @param bodyVisitor visitor for all but last segment
     * @param headVisitor visitor for the last segment (if null if will use bodyVisitor).
     * @param <T> Return type of visitor
     * @return last T from headVisitor
     */
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

