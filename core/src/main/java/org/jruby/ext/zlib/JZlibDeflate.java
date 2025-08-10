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
import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.typeError;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.util.ByteList;

@JRubyClass(name = "Zlib::Deflate", parent = "Zlib::ZStream")
public class JZlibDeflate extends ZStream {
    public static final int BASE_SIZE = 100;
    
    private int level;
    private int windowBits;
    private int strategy;
    private byte[] collected;
    private int collectedIdx;
    private com.jcraft.jzlib.Deflater flater = null;
    private int flush = JZlib.Z_NO_FLUSH;

    @Deprecated(since = "9.4")
    public static IRubyObject s_deflate(IRubyObject recv, IRubyObject[] args) {
        return s_deflate(((RubyBasicObject) recv).getCurrentContext(), recv, args);
    }

    @JRubyMethod(name = "deflate", required = 1, optional = 1, checkArity = false, meta = true)
    public static IRubyObject s_deflate(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        args = Arity.scanArgs(context, args, 1, 1);
        int level = JZlib.Z_DEFAULT_COMPRESSION;
        if (!args[1].isNil()) level = checkLevel(context, toInt(context, args[1]));

        RubyClass klass = (RubyClass)(recv.isClass() ? recv : context.runtime.getClassFromPath("Zlib::Deflate"));
        JZlibDeflate deflate = (JZlibDeflate) klass.allocate(context);
        deflate.init(context, level, JZlib.DEF_WBITS, 8, JZlib.Z_DEFAULT_STRATEGY);

        try {
            IRubyObject result = deflate.deflate(context, args[0].convertToString().getByteList(), JZlib.Z_FINISH);
            deflate.close(context);
            return result;
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
    }

    public JZlibDeflate(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @Deprecated
    public IRubyObject _initialize(IRubyObject[] args) {
        return _initialize(getCurrentContext(), args);
    }

    @JRubyMethod(name = "initialize", optional = 4, checkArity = false, visibility = PRIVATE)
    public IRubyObject _initialize(ThreadContext context, IRubyObject[] args) {
        args = Arity.scanArgs(context, args, 0, 4);
        level = !args[0].isNil() ? checkLevel(context, toInt(context, args[0])) : -1;
        windowBits = !args[1].isNil() ? checkWindowBits(context, toInt(context, args[1]), false) : JZlib.MAX_WBITS;
        int memlevel = !args[2].isNil() ? toInt(context, args[2]) : 8; // ignored. Memory setting means nothing on Java.
        strategy = !args[3].isNil() ? toInt(context, args[3]) : 0;

        init(context, level, windowBits, memlevel, strategy);
        return this;
    }

    private void init(ThreadContext context, int level, int windowBits, int memlevel, int strategy) {
        flush = JZlib.Z_NO_FLUSH;
        flater = new com.jcraft.jzlib.Deflater();

        // TODO: Can we expect JZlib to check level, windowBits, and strategy here?
        // Then we should remove checkLevel, checkWindowsBits and checkStrategy.
        int err = flater.init(level, windowBits, memlevel);
        if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) throw RubyZlib.newStreamError(context, "stream error");

        err = flater.params(level, strategy);
        if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) throw RubyZlib.newStreamError(context, "stream error");

        collected = new byte[BASE_SIZE];
        collectedIdx = 0;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject _other) {
        if (!(_other instanceof JZlibDeflate other)) throw typeError(context, "Expecting an instance of class JZlibDeflate");
        if (this == _other) return this;

        this.level = other.level;
        this.windowBits = other.windowBits;
        this.strategy = other.strategy;
        this.collected = new byte[other.collected.length];
        System.arraycopy(other.collected, 0, this.collected, 0, other.collected.length);
        this.collectedIdx = other.collectedIdx;

        this.flush = other.flush;
        this.flater = new com.jcraft.jzlib.Deflater();
        int ret = this.flater.copy(other.flater);
        if (ret != com.jcraft.jzlib.JZlib.Z_OK) throw RubyZlib.newStreamError(context, "stream error");

        return this;
    }

    @Deprecated(since = "10.0")
    public IRubyObject append(IRubyObject arg) {
        return append(getCurrentContext(), arg);
    }

    @JRubyMethod(name = "<<")
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        checkClosed(context);
        try {
            append(context, arg.convertToString().getByteList());
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
        }
        return this;
    }

    @JRubyMethod(name = "params")
    public IRubyObject params(ThreadContext context, IRubyObject level, IRubyObject strategy) {
        int l = toInt(context, level);
        checkLevel(context, l);

        int s = toInt(context, strategy);
        checkStrategy(context, s);

        if (flater.next_out == null) flater.setOutput(ByteList.NULL_ARRAY);

        int err = flater.params(l, s);
        if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) throw RubyZlib.newStreamError(context, "stream error");

        if (collectedIdx != flater.next_out_index) collectedIdx = flater.next_out_index;

        run(context);
        return context.nil;
    }

    @JRubyMethod(name = "set_dictionary")
    public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) {
        try {
            byte[] tmp = arg.convertToString().getBytes();
            int err = flater.setDictionary(tmp, tmp.length);
            if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) {
                throw RubyZlib.newStreamError(context, "stream error: ");
            }
            run(context);
            return arg;
        } catch (IllegalArgumentException iae) {
            throw RubyZlib.newStreamError(context, "stream error: " + iae.getMessage());
        }
    }

    @Deprecated
    public IRubyObject flush(IRubyObject[] args) {
        return flush(getCurrentContext(), args);
    }

    @JRubyMethod(name = "flush", optional = 1, checkArity = false)
    public IRubyObject flush(ThreadContext context, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        int flush = args.length == 1 && !args[0].isNil() ? toInt(context, args[0]) : 2; // SYNC_FLUSH

        return flush(context, flush);
    }

    @Deprecated
    public IRubyObject deflate(IRubyObject[] args) {
        return deflate(getCurrentContext(), args);
    }

    @JRubyMethod(name = "deflate", required = 1, optional = 1, checkArity = false)
    public IRubyObject deflate(ThreadContext context, IRubyObject[] args) {
        args = Arity.scanArgs(context, args, 1, 1);
        if (internalFinished()) throw RubyZlib.newStreamError(context, "stream error");

        ByteList data = !args[0].isNil() ? args[0].convertToString().getByteList() : null;
        int flush = !args[1].isNil() ? toInt(context, args[1]) : JZlib.Z_NO_FLUSH;

        try {
            return deflate(context, data, flush);
        } catch (IOException ioe) {
            throw context.runtime.newIOErrorFromException(ioe);
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
        init(context, level, windowBits, 8, strategy);
    }

    @Override
    public boolean internalFinished() {
        return flater.finished();
    }

    @Override
    protected long internalAdler() {
        return flater.getAdler();
    }

    @Override
    protected IRubyObject internalFinish(ThreadContext context, Block block) {
        return finish(context);
    }

    @Override
    protected void internalClose() {
        flater.end();
    }

    private void append(ThreadContext context, ByteList obj) throws IOException {
        flater.setInput(obj.getUnsafeBytes(), obj.getBegin(), obj.getRealSize(), true);
        run(context);
    }

    private IRubyObject flush(ThreadContext context, int flush) {
        int last_flush = this.flush;
        this.flush = flush;
        if (flush == JZlib.Z_NO_FLUSH) return RubyString.newEmptyBinaryString(context.runtime);

        run(context);
        this.flush = last_flush;
        IRubyObject obj = newString(context, collected, 0, collectedIdx);
        collectedIdx = 0;
        flater.setOutput(collected);
        return obj;
    }

    private IRubyObject deflate(ThreadContext context, ByteList str, int flush) throws IOException {
        if (null != str) append(context, str);

        return flush(context, flush);
    }

    private IRubyObject finish(ThreadContext context) {
        return flush(context, JZlib.Z_FINISH);
    }

    private void run(ThreadContext context) {
        if (internalFinished()) return;

        while (!internalFinished()) {
            flater.setOutput(collected, collectedIdx, collected.length - collectedIdx);

            int err = flater.deflate(flush);
            if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) throw RubyZlib.newStreamError(context, "stream error: ");

            if (collectedIdx == flater.next_out_index) break;

            collectedIdx = flater.next_out_index;
            
            if (collected.length == collectedIdx && !internalFinished()) {
                byte[] tmp = new byte[collected.length * 3];
                System.arraycopy(collected, 0, tmp, 0, collected.length);
                collected = tmp;
            }
        }
    }
}
