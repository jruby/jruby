/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.ext.zlib;

import com.jcraft.jzlib.JZlib;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newString;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

 @JRubyClass(name = "Zlib::Inflate", parent = "Zlib::ZStream")
public class JZlibInflate extends ZStream {

    public static final int BASE_SIZE = 100;
    private int windowBits;
    private byte[] collected;
    private int collectedIdx;
    private ByteList input;
    private com.jcraft.jzlib.Inflater flater = null;

     public JZlibInflate(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "inflate", meta = true)
    public static IRubyObject s_inflate(ThreadContext context, IRubyObject recv, IRubyObject string) {
        RubyClass klass = (RubyClass)(recv.isClass() ? recv : context.runtime.getClassFromPath("Zlib::Inflate"));
        JZlibInflate inflate = (JZlibInflate) klass.allocate(context);
        inflate.init(JZlib.DEF_WBITS);

        try {
            return inflate.inflate(context, string, Block.NULL_BLOCK);
        } finally {
            inflate.finish(context, Block.NULL_BLOCK);
            inflate.close(context);
        }
    }

    @Deprecated
    public IRubyObject _initialize(IRubyObject[] args) {
         return _initialize(getCurrentContext(), args);
    }

    @JRubyMethod(name = "initialize", optional = 1, checkArity = false, visibility = PRIVATE)
    public IRubyObject _initialize(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 1);

        windowBits = argc > 0 && !args[0].isNil() ?
                checkWindowBits(context, toInt(context, args[0]), true) : JZlib.DEF_WBITS;

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
    public IRubyObject flush_next_out(ThreadContext context, Block block) {
        return flushOutput(context, block);
    }

    private IRubyObject flushOutput(ThreadContext context, Block block) {
        if (collectedIdx > 0) {
            RubyString res = newString(context, collected, 0, collectedIdx);
            collectedIdx = 0;
            flater.setOutput(collected);

            if (block.isGiven()) {
                block.yield(context, res);
                return context.nil;
            }
            return res;
        }
        return RubyString.newEmptyBinaryString(context.runtime);
    }

    @JRubyMethod(name = "<<")
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        checkClosed(context);
        if (arg.isNil()) {
            run(context, true);
        } else {
            append(context, arg.convertToString().getByteList());
        }
        return this;
    }

    @Deprecated(since = "10.0")
     public void append(ByteList obj) {
        append(getCurrentContext(), obj);
     }

     public void append(ThreadContext context, ByteList obj) {
        if (!internalFinished()) {
            flater.setInput(obj.bytes(), true);
        } else {
            input.append(obj);
        }
        run(context, false);
    }

     @Deprecated(since = "10.0")
     public IRubyObject sync_point_p() {
         return sync_point_p(getCurrentContext());
     }

     @JRubyMethod(name = "sync_point?")
     public IRubyObject sync_point_p(ThreadContext context) {
         return sync_point(context);
     }

     @Deprecated(since = "10.0")
     public IRubyObject sync_point() {
         return sync_point(getCurrentContext());
     }

     public IRubyObject sync_point(ThreadContext context) {
        int ret = flater.syncPoint();
        return switch (ret) {
             case 1 -> context.tru;
             case JZlib.Z_DATA_ERROR -> throw RubyZlib.newStreamError(context, "stream error");
             default -> context.fals;
        };
    }

    @JRubyMethod(name = "set_dictionary")
    public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) {
        try {
            byte[] tmp = arg.convertToString().getBytes();
            int ret = flater.setDictionary(tmp, tmp.length);
            switch (ret) {
                case JZlib.Z_STREAM_ERROR -> throw RubyZlib.newStreamError(context, "stream error");
                case JZlib.Z_DATA_ERROR -> throw RubyZlib.newDataError(context, "wrong dictionary");
            }
            run(context, false);
            return arg;
        } catch (IllegalArgumentException iae) {
            throw RubyZlib.newStreamError(context, "stream error: " + iae.getMessage());
        }
    }

    @JRubyMethod(name = "inflate")
    public IRubyObject inflate(ThreadContext context, IRubyObject string, Block block) {
        ByteList data = null;
        if (!string.isNil()) {
            data = string.convertToString().getByteList();
        }
        return inflate(context, data, block);
    }

    public IRubyObject inflate(ThreadContext context, ByteList str, Block block) {
        if (str == null) return internalFinish(context, block);

        append(context, str);
        return flushOutput(context, block);
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync(ThreadContext context, IRubyObject string) {
        if (flater.avail_in > 0) {
            switch (flater.sync()) {
                case JZlib.Z_OK -> {
                    flater.setInput(string.convertToString().getByteList().bytes(), true);
                    return context.tru;
                }
                case JZlib.Z_DATA_ERROR -> {}
                default -> throw RubyZlib.newStreamError(context, "stream error");
            }
        }
        if (string.convertToString().getByteList().length() <= 0) return context.fals;

        flater.setInput(string.convertToString().getByteList().bytes(), true);
        switch (flater.sync()) {
            case com.jcraft.jzlib.JZlib.Z_OK:
                return context.tru;
            case com.jcraft.jzlib.JZlib.Z_DATA_ERROR:
                return context.fals;
            default:
                throw RubyZlib.newStreamError(context, "stream error");
        }
    }

    private void run(ThreadContext context, boolean finish) {
        int resultLength = -1;

        while (!internalFinished() && resultLength != 0) {
            // MRI behavior
            boolean needsInput = flater.avail_in < 0;
            if (finish && needsInput) {
                throw RubyZlib.newBufError(context, "buffer error");
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
                    throw RubyZlib.newDataError(context, flater.getMessage());
                case com.jcraft.jzlib.JZlib.Z_NEED_DICT:
                    throw RubyZlib.newDictError(context, "need dictionary");
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
                    throw RubyZlib.newBufError(context, "buffer error");
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
    protected void internalReset(ThreadContext context) {
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
    protected IRubyObject internalFinish(ThreadContext context, Block block) {
        run(context, true);
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
        return flushOutput(context, block);
    }

    @Override
    protected void internalClose() {
        flater.end();
    }

    @Override
    public IRubyObject avail_in(ThreadContext context) {
        return asFixnum(context, flater.avail_in);
    }

    private static void resetBuffer(ByteList l) {
        l.setBegin(0);
        l.setRealSize(0);
        l.invalidate();
    }
}