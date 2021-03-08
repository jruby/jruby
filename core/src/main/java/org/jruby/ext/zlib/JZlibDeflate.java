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
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.util.ByteList;

/**
 */
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

    @JRubyMethod(name = "deflate", required = 1, optional = 1, meta = true)
    public static IRubyObject s_deflate(IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = recv.getRuntime();
        args = Arity.scanArgs(runtime, args, 1, 1);
        int level = JZlib.Z_DEFAULT_COMPRESSION;
        if (!args[1].isNil()) {
            level = RubyNumeric.fix2int(args[1]);
            checkLevel(runtime, level);
        }

        RubyClass klass = (RubyClass)(recv.isClass() ? recv : runtime.getClassFromPath("Zlib::Deflate"));
        JZlibDeflate deflate = (JZlibDeflate) klass.allocate();
        deflate.init(level, JZlib.DEF_WBITS, 8, JZlib.Z_DEFAULT_STRATEGY);

        try {
            IRubyObject result = deflate.deflate(args[0].convertToString().getByteList(), JZlib.Z_FINISH);
            deflate.close();
            return result;
        } catch (IOException ioe) {
            throw runtime.newIOErrorFromException(ioe);
        }
    }

    public JZlibDeflate(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "initialize", optional = 4, visibility = PRIVATE)
    public IRubyObject _initialize(IRubyObject[] args) {
        args = Arity.scanArgs(getRuntime(), args, 0, 4);
        level = -1;
        windowBits = JZlib.MAX_WBITS;
        int memlevel = 8;
        strategy = 0;
        if (!args[0].isNil()) {
            level = RubyNumeric.fix2int(args[0]);
            checkLevel(getRuntime(), level);
        }
        if (!args[1].isNil()) {
            windowBits = RubyNumeric.fix2int(args[1]);
            checkWindowBits(getRuntime(), windowBits, false);
        }
        if (!args[2].isNil()) {
            memlevel = RubyNumeric.fix2int(args[2]);
            // We accepts any memlevel and ignores it. Memory setting means nothing on Java platform.
        }
        if (!args[3].isNil()) {
            strategy = RubyNumeric.fix2int(args[3]);
        }
        init(level, windowBits, memlevel, strategy);
        return this;
    }

    private void init(int level, int windowBits, int memlevel, int strategy) {
        flush = JZlib.Z_NO_FLUSH;
        flater = new com.jcraft.jzlib.Deflater();

        // TODO: Can we expect JZlib to check level, windowBits, and strategy here?
        // Then we should remove checkLevel, checkWindowsBits and checkStrategy.
        int err = flater.init(level, windowBits, memlevel);
        if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) {
            throw RubyZlib.newStreamError(getRuntime(), "stream error");
        }
        err = flater.params(level, strategy);
        if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) {
            throw RubyZlib.newStreamError(getRuntime(), "stream error");
        }

        collected = new byte[BASE_SIZE];
        collectedIdx = 0;
    }

    @Override
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize_copy(IRubyObject _other) {
        if (!(_other instanceof JZlibDeflate)) {
            throw getRuntime().newTypeError("Expecting an instance of class JZlibDeflate");
        }

        if (this == _other) {
            return this;
        }

        JZlibDeflate other = (JZlibDeflate) _other;

        this.level = other.level;
        this.windowBits = other.windowBits;
        this.strategy = other.strategy;
        this.collected = new byte[other.collected.length];
        System.arraycopy(other.collected, 0, this.collected, 0, other.collected.length);
        this.collectedIdx = other.collectedIdx;

        this.flush = other.flush;
        this.flater = new com.jcraft.jzlib.Deflater();
        int ret = this.flater.copy(other.flater);
        if (ret != com.jcraft.jzlib.JZlib.Z_OK) {
            throw RubyZlib.newStreamError(getRuntime(), "stream error");
        }

        return (IRubyObject) this;
    }

    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject append(IRubyObject arg) {
        checkClosed();
        try {
            append(arg.convertToString().getByteList());
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
        }
        return this;
    }

    @JRubyMethod(name = "params", required = 2)
    public IRubyObject params(ThreadContext context, IRubyObject level, IRubyObject strategy) {
        int l = RubyNumeric.fix2int(level);
        checkLevel(getRuntime(), l);
        
        int s = RubyNumeric.fix2int(strategy);
        checkStrategy(getRuntime(), s);
        
        if (flater.next_out == null) flater.setOutput(ByteList.NULL_ARRAY);

        int err = flater.params(l, s);
        if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) {
            throw RubyZlib.newStreamError(getRuntime(), "stream error");
        }
        
        if (collectedIdx != flater.next_out_index) collectedIdx = flater.next_out_index;

        run();
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "set_dictionary", required = 1)
    public IRubyObject set_dictionary(ThreadContext context, IRubyObject arg) {
        try {
            byte[] tmp = arg.convertToString().getBytes();
            int err = flater.setDictionary(tmp, tmp.length);
            if (err == com.jcraft.jzlib.JZlib.Z_STREAM_ERROR) {
                throw RubyZlib.newStreamError(context.getRuntime(), "stream error: ");
            }
            run();
            return arg;
        } catch (IllegalArgumentException iae) {
            throw RubyZlib.newStreamError(context.getRuntime(), "stream error: " + iae.getMessage());
        }
    }

    @JRubyMethod(name = "flush", optional = 1)
    public IRubyObject flush(IRubyObject[] args) {
        int flush = 2; // SYNC_FLUSH
        
        if (args.length == 1 && !args[0].isNil()) flush = RubyNumeric.fix2int(args[0]);

        return flush(flush);
    }

    @JRubyMethod(name = "deflate", required = 1, optional = 1)
    public IRubyObject deflate(IRubyObject[] args) {
        args = Arity.scanArgs(getRuntime(), args, 1, 1);
        if (internalFinished()) throw RubyZlib.newStreamError(getRuntime(), "stream error");

        ByteList data = null;
        if (!args[0].isNil()) data = args[0].convertToString().getByteList();

        int flush = JZlib.Z_NO_FLUSH;
        if (!args[1].isNil()) flush = RubyNumeric.fix2int(args[1]);
        
        try {
            return deflate(data, flush);
        } catch (IOException ioe) {
            throw getRuntime().newIOErrorFromException(ioe);
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
        init(level, windowBits, 8, strategy);
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
    protected IRubyObject internalFinish(Block block) {
        return finish();
    }

    @Override
    protected void internalClose() {
        flater.end();
    }

    private void append(ByteList obj) throws IOException {
        flater.setInput(obj.getUnsafeBytes(), obj.getBegin(), obj.getRealSize(), true);
        run();
    }

    private IRubyObject flush(int flush) {
        int last_flush = this.flush;
        this.flush = flush;
        if (flush == JZlib.Z_NO_FLUSH) return RubyString.newEmptyString(getRuntime());

        run();
        this.flush = last_flush;
        IRubyObject obj = RubyString.newString(getRuntime(), collected, 0, collectedIdx);
        collectedIdx = 0;
        flater.setOutput(collected);
        return obj;
    }

    private IRubyObject deflate(ByteList str, int flush) throws IOException {
        if (null != str) append(str);

        return flush(flush);
    }

    private IRubyObject finish() {
        return flush(JZlib.Z_FINISH);
    }

    private void run() {
        if (internalFinished()) return;

        while (!internalFinished()) {
            flater.setOutput(collected, collectedIdx, collected.length - collectedIdx);

            int err = flater.deflate(flush);
            switch (err) {
                case com.jcraft.jzlib.JZlib.Z_STREAM_ERROR:
                    throw RubyZlib.newStreamError(getRuntime(), "stream error: ");
                default:
            }
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
