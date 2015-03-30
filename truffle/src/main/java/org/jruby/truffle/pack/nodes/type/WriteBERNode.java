package org.jruby.truffle.pack.nodes.type;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.jruby.truffle.pack.nodes.PackNode;
import org.jruby.truffle.pack.runtime.CantCompressNegativeException;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.util.ByteList;

import java.math.BigInteger;

@NodeChildren({
        @NodeChild(value = "value", type = PackNode.class),
})
public abstract class WriteBERNode extends PackNode {

    @Specialization
    public Object doWrite(VirtualFrame frame, long value) {
        if (value < 0) {
            CompilerDirectives.transferToInterpreter();
            throw new CantCompressNegativeException();
        }

        writeBytes(frame, encode(value));

        return null;
    }

    @Specialization
    public Object doWrite(VirtualFrame frame, BigInteger value) {
        if (value.signum() < 0) {
            CompilerDirectives.transferToInterpreter();
            throw new CantCompressNegativeException();
        }

        writeBytes(frame, encode(value));

        return null;
    }

    @CompilerDirectives.TruffleBoundary
    private ByteList encode(Object from) {
        // TODO CS 30-Mar-15 should write our own optimisable version of BER

        ByteList buf = new ByteList();

        long l;

        if (from instanceof BigInteger) {
            BigInteger big128 = BigInteger.valueOf(128);
            while (true) {
                BigInteger bignum = (BigInteger)from;
                BigInteger[] ary = bignum.divideAndRemainder(big128);
                buf.append((byte)(ary[1].longValue() | 0x80) & 0xff);

                if (ary[0].compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0) {
                    l = ary[0].longValue();
                    break;
                }
                from = ary[0];
            }
        } else if (from instanceof Integer) {
            l = (int) from;
        } else if (from instanceof Long) {
            l = (long) from;
        } else {
            throw new UnsupportedOperationException();
        }

        while(l != 0) {
            buf.append((byte)(((l & 0x7f) | 0x80) & 0xff));
            l >>= 7;
        }

        int left = 0;
        int right = buf.getRealSize() - 1;

        if (right >= 0) {
            buf.getUnsafeBytes()[0] &= 0x7F;
        } else {
            buf.append(0);
        }

        while (left < right) {
            byte tmp = buf.getUnsafeBytes()[left];
            buf.getUnsafeBytes()[left] = buf.getUnsafeBytes()[right];
            buf.getUnsafeBytes()[right] = tmp;

            left++;
            right--;
        }

        return buf;
    }

}
