/*
 */
package org.jruby.ext.zlib;

import com.jcraft.jzlib.JZlib;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
 @JRubyClass(name = "Zlib::Inflate", parent = "Zlib::ZStream")
public class JZlibInflate extends ZStream {

    public static final int BASE_SIZE = 100;
    private int windowBits;
    private byte[] collected;
    private int collectedIdx;
    private ByteList input;
    private com.jcraft.jzlib.Inflater flater = null;
    protected static final ObjectAllocator INFLATE_ALLOCATOR = new ObjectAllocator() {

        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new JZlibInflate(runtime, klass);
        }
    };

    public JZlibInflate(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "inflate", required = 1, meta = true)
    public static IRubyObject s_inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
        RubyClass klass = (RubyClass) recv;
        JZlibInflate inflate = (JZlibInflate) klass.allocate();
        inflate.init(JZlib.DEF_WBITS);

        IRubyObject result;
        try {
            inflate.append(string.convertToString().getByteList());
        } finally {
            result = inflate.finish(context);
            inflate.close();
        }
        return result;
    }

    @JRubyMethod(name = "initialize", optional = 1, visibility = PRIVATE)
    public IRubyObject _initialize(IRubyObject[] args) {
        windowBits = JZlib.DEF_WBITS;

        if (args.length > 0 && !args[0].isNil()) {
            windowBits = RubyNumeric.fix2int(args[0]);
            checkWindowBits(getRuntime(), windowBits, true);
        }

        init(windowBits);
        return this;
    }

    private void init(int windowBits) {
        flater = new com.jcraft.jzlib.Inflater();
        flater.init(windowBits);
        collected = new byte[BASE_SIZE];
        collectedIdx = 0;
        input = new ByteList();
    }

    @Override
    @JRubyMethod(name = "flush_next_out")
    public IRubyObject flush_next_out(ThreadContext context) {
        return flushOutput(context.getRuntime());
    }

    private RubyString flushOutput(Ruby runtime) {
        if (collectedIdx > 0) {
            RubyString res = RubyString.newString(runtime, collected, 0, collectedIdx);
            collectedIdx = 0;
            flater.setOutput(collected);
            return res;
        }
        return RubyString.newEmptyString(runtime);
    }

    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        checkClosed();
        if (arg.isNil()) {
            run(true);
        } else {
            append(arg.convertToString().getByteList());
        }
        return this;
    }

    public void append(ByteList obj) {
        if (!internalFinished()) {
            flater.setInput(obj.bytes(), true);
        } else {
            input.append(obj);
        }
        run(false);
    }

    @JRubyMethod(name = "sync_point?")
    public IRubyObject sync_point_p() {
        return sync_point();
    }

    public IRubyObject sync_point() {
        int ret = flater.syncPoint();
        switch (ret) {
            case 1:
                return getRuntime().getTrue();
            case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                throw RubyZlib.newStreamError(getRuntime(), "stream error");
            default:
                return getRuntime().getFalse();
        }
    }

    @JRubyMethod(name = "set_dictionary", required = 1)
    public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) {
        try {
            return set_dictionary(arg);
        } catch (IllegalArgumentException iae) {
            throw RubyZlib.newStreamError(context.getRuntime(), "stream error: " + iae.getMessage());
        }
    }

    private IRubyObject set_dictionary(IRubyObject str) {
        byte[] tmp = str.convertToString().getBytes();
        int ret = flater.setDictionary(tmp, tmp.length);
        switch (ret) {
            case com.jcraft.jzlib.JZlib.Z_STREAM_ERROR:
                throw RubyZlib.newStreamError(getRuntime(), "stream error");
            case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                throw RubyZlib.newDataError(getRuntime(), "wrong dictionary");
            default:
        }
        run(false);
        return str;
    }

    @JRubyMethod(name = "inflate", required = 1)
    public IRubyObject inflate(ThreadContext context, IRubyObject string) {
        ByteList data = null;
        if (!string.isNil()) {
            data = string.convertToString().getByteList();
        }
        return inflate(context, data);
    }

    public IRubyObject inflate(ThreadContext context, ByteList str) {
        if (null == str) {
            return internalFinish();
        } else {
            append(str);
            return flushOutput(context.getRuntime());
        }
    }

    @JRubyMethod(name = "sync", required = 1)
    public IRubyObject sync(ThreadContext context, IRubyObject string) {
        if (flater.avail_in > 0) {
            switch (flater.sync()) {
                case com.jcraft.jzlib.JZlib.Z_OK:
                    flater.setInput(string.convertToString().getByteList().bytes(),
                            true);
                    return getRuntime().getTrue();
                case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                    break;
                default:
                    throw RubyZlib.newStreamError(getRuntime(), "stream error");
            }
        }
        if (string.convertToString().getByteList().length() <= 0) {
            return getRuntime().getFalse();
        }
        flater.setInput(string.convertToString().getByteList().bytes(), true);
        switch (flater.sync()) {
            case com.jcraft.jzlib.JZlib.Z_OK:
                return getRuntime().getTrue();
            case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                return getRuntime().getFalse();
            default:
                throw RubyZlib.newStreamError(getRuntime(), "stream error");
        }
    }

    private void run(boolean finish) {
        int resultLength = -1;
        Ruby runtime = getRuntime();

        while (!internalFinished() && resultLength != 0) {
            // MRI behavior
            boolean needsInput = flater.avail_in < 0;
            if (finish && needsInput) {
                throw RubyZlib.newBufError(runtime, "buffer error");
            }

            flater.setOutput(collected, collectedIdx, collected.length - collectedIdx);
            int ret = flater.inflate(com.jcraft.jzlib.JZlib.Z_NO_FLUSH);
            resultLength = flater.next_out_index - collectedIdx;
            collectedIdx = flater.next_out_index;
            switch (ret) {
                case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                    /*
                     * resultLength = flater.next_out_index; if(resultLength>0){
                     * // error has been occurred, // but some data has been
                     * inflated successfully. collected.append(outp, 0,
                     * resultLength); }
                     */
                    throw RubyZlib.newDataError(runtime, flater.getMessage());
                case com.jcraft.jzlib.JZlib.Z_NEED_DICT:
                    throw RubyZlib.newDictError(runtime, "need dictionary");
                case com.jcraft.jzlib.JZlib.Z_STREAM_END:
                    if (flater.avail_in > 0) {
                        // MRI behavior: pass-through
                        input.append(flater.next_in,
                                flater.next_in_index, flater.avail_in);
                        flater.setInput("".getBytes());
                    }
                case com.jcraft.jzlib.JZlib.Z_OK:
                    resultLength = flater.next_out_index;
                    break;
                default:
                    resultLength = 0;
            }
            if (collected.length == collectedIdx && !internalFinished()) {
                byte[] tmp = new byte[collected.length * 3];
                System.arraycopy(collected, 0, tmp, 0, collected.length);
                collected = tmp;
            }
        }
        if (finish) {
            if (!internalFinished()) {
                int err = flater.inflate(com.jcraft.jzlib.JZlib.Z_FINISH);
                if (err != com.jcraft.jzlib.JZlib.Z_OK) {
                    throw RubyZlib.newBufError(getRuntime(), "buffer error");
                }
            }
        }
    }

    @Override
    protected int internalTotalIn() {
        return (int) flater.total_in;
    }

    @Override
    protected int internalTotalOut() {
        return (int) flater.total_out;
    }

    @Override
    protected boolean internalStreamEndP() {
        return flater.finished();
    }

    @Override
    protected void internalReset() {
        init(windowBits);
    }

    @Override
    protected boolean internalFinished() {
        return flater.finished();
    }

    @Override
    protected long internalAdler() {
        return flater.getAdler();
    }

    @Override
    protected IRubyObject internalFinish() {
        run(true);
        // MRI behavior: in finished mode, we work as pass-through
        if (internalFinished()) {
            if (input.getRealSize() > 0) {
                if (collected.length - collectedIdx < input.length()) {
                    byte[] tmp = new byte[collected.length + input.length()];
                    System.arraycopy(collected, 0, tmp, 0, collectedIdx);
                    collected = tmp;
                }
                System.arraycopy(input.getUnsafeBytes(), input.begin(), collected, collectedIdx, input.length());
                collectedIdx += input.length();
                resetBuffer(input);
            }
        }
        return flushOutput(getRuntime());
    }

    @Override
    protected void internalClose() {
        flater.end();
    }

    @Override
    public IRubyObject avail_in() {
        return getRuntime().newFixnum(flater.avail_in);
    }

    private static void resetBuffer(ByteList l) {
        l.setBegin(0);
        l.setRealSize(0);
        l.invalidate();
    }
}