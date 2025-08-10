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
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Access.getModule;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "Zlib::ZStream")
public abstract class ZStream extends RubyObject {

    protected boolean closed = false;

    protected abstract int internalTotalIn();

    protected abstract int internalTotalOut();

    protected abstract boolean internalStreamEndP();

    @Deprecated(since = "10.0")
    protected void internalReset() {
        internalReset(getCurrentContext());
    }

    protected void internalReset(ThreadContext context) {
        throw new RuntimeException("Missing internalReset Implementation");
    }

    protected abstract boolean internalFinished();

    protected abstract long internalAdler();

    @Deprecated(since = "10.0")
    protected IRubyObject internalFinish(Block block) {
        return internalFinish(getCurrentContext(), block);
    }

    protected IRubyObject internalFinish(ThreadContext context, Block block) {
        throw new RuntimeException("Missing internalFinish Implementation");
    }


    protected abstract void internalClose();

    public ZStream(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(Block unusedBlock) {
        return this;
    }

    @JRubyMethod
    public IRubyObject flush_next_out(ThreadContext context, Block block) {
        return RubyString.newEmptyBinaryString(context.getRuntime());
    }

    @Deprecated
    public IRubyObject total_out() {
        return total_out(getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject total_out(ThreadContext context) {
        checkClosed(context);
        return asFixnum(context, internalTotalOut());
    }

    @Deprecated(since = "10.0")
    public IRubyObject stream_end_p() {
        return stream_end_p(getCurrentContext());
    }

    @JRubyMethod(name = "stream_end?")
    public IRubyObject stream_end_p(ThreadContext context) {
        return internalStreamEndP() ? context.tru : context.fals;
    }

    @Deprecated(since = "10.0")
    public IRubyObject data_type() {
        return data_type(getCurrentContext());
    }

    @JRubyMethod(name = "data_type")
    public IRubyObject data_type(ThreadContext context) {
        checkClosed(context);
        return getModule(context, "Zlib").getConstant(context, "UNKNOWN");
    }

    @Deprecated(since = "10.0")
    public IRubyObject closed_p() {
        return closed_p(getCurrentContext());
    }

    @JRubyMethod(name = {"closed?", "ended?"})
    public IRubyObject closed_p(ThreadContext context) {
        return closed ? context.tru : context.fals;
    }

    @Deprecated(since = "10.0")
    public IRubyObject reset() {
        return reset(getCurrentContext());
    }

    @JRubyMethod(name = "reset")
    public IRubyObject reset(ThreadContext context) {
        checkClosed(context);
        internalReset(context);
        
        return context.nil;
    }

    @Deprecated(since = "10.0")
    public IRubyObject avail_out() {
        return avail_out(getCurrentContext());
    }

    @JRubyMethod(name = "avail_out")
    public IRubyObject avail_out(ThreadContext context) {
        return asFixnum(context, 0);
    }

    @Deprecated(since = "10.0")
    public IRubyObject set_avail_out(IRubyObject p1) {
        return set_avail_out(getCurrentContext(), p1);
    }

    @JRubyMethod(name = "avail_out=")
    public IRubyObject set_avail_out(ThreadContext context, IRubyObject p1) {
        checkClosed(context);
        
        return p1;
    }

    @JRubyMethod(name = "adler")
    public IRubyObject adler(ThreadContext context) {
        checkClosed(context);

        return asFixnum(context, internalAdler());
    }

    @Deprecated
    public IRubyObject adler() {
        return adler(getCurrentContext());
    }

    @JRubyMethod(name = "finish")
    public IRubyObject finish(ThreadContext context, Block block) {
        checkClosed(context);
        
        IRubyObject result = internalFinish(context, block);
        
        return result;
    }

    @Deprecated(since = "10.0")
    public IRubyObject avail_in() {
        return avail_in(getCurrentContext());
    }

    @JRubyMethod(name = "avail_in")
    public IRubyObject avail_in(ThreadContext context) {
        return asFixnum(context, 0);
    }

    @JRubyMethod(name = "flush_next_in")
    public IRubyObject flush_next_in(ThreadContext context) {
        return RubyString.newEmptyBinaryString(context.getRuntime());
    }

    @Deprecated
    public IRubyObject total_in() {
        return total_in(getCurrentContext());
    }

    @JRubyMethod(name = "total_in")
    public IRubyObject total_in(ThreadContext context) {
        checkClosed(context);
        return asFixnum(context, internalTotalIn());
    }

    @JRubyMethod(name = "finished?")
    public IRubyObject finished_p(ThreadContext context) {
        checkClosed(context);
        return asBoolean(context, internalFinished());
    }

    @Deprecated(since = "10.0")
    public IRubyObject close() {
        return close(getCurrentContext());
    }

    @JRubyMethod(name = {"close", "end"})
    public IRubyObject close(ThreadContext context) {
        checkClosed(context);
        internalClose();
        closed = true;
        return context.nil;
    }

    @Deprecated(since = "10.0")
    void checkClosed() {
        checkClosed(getCurrentContext());
    }

    void checkClosed(ThreadContext context) {
        if (closed) throw RubyZlib.newZlibError(context, "stream is not ready");
    }

    // TODO: remove when JZlib checks the given level
    static int checkLevel(ThreadContext context, int level) {
        if ((level < 0 || level > 9) && level != JZlib.Z_DEFAULT_COMPRESSION) {
            throw RubyZlib.newStreamError(context, "stream error: invalid level");
        }
        return level;
    }

    /**
     * We only do windowBits=15(32K buffer, LZ77 algorithm) since java.util.zip
     * only allows it. NOTE: deflateInit2 of zlib.c also accepts MAX_WBITS +
     * 16(gzip compression). inflateInit2 also accepts MAX_WBITS + 16(gzip
     * decompression) and MAX_WBITS + 32(automatic detection of gzip and LZ77).
     */
    // TODO: remove when JZlib checks the given windowBits
    static int checkWindowBits(ThreadContext context, int value, boolean forInflate) {
        int wbits = Math.abs(value);
        if ((wbits & 0xf) < 8) {
            throw RubyZlib.newStreamError(context, "stream error: invalid window bits");
        }
        if ((wbits & 0xf) != 0xf) {
            // windowBits < 15 for reducing memory is meaningless on Java platform. 
            warn(context, "windowBits < 15 is ignored on this platform");
            // continue
        }
        if (forInflate && wbits > JZlib.MAX_WBITS + 32) {
            throw RubyZlib.newStreamError(context, "stream error: invalid window bits");
        } else if (!forInflate && wbits > JZlib.MAX_WBITS + 16) {
            throw RubyZlib.newStreamError(context, "stream error: invalid window bits");
        }

        return value;
    }

    @Deprecated(since = "10.0")
    static void checkStrategy(Ruby runtime, int strategy) {
        checkStrategy(runtime.getCurrentContext(), strategy);
    }

    // TODO: remove when JZlib checks the given strategy
    static void checkStrategy(ThreadContext context, int strategy) {
        switch (strategy) {
            case JZlib.Z_DEFAULT_STRATEGY, JZlib.Z_FILTERED, JZlib.Z_HUFFMAN_ONLY -> {}
            default -> throw RubyZlib.newStreamError(context, "stream error: invalid strategy");
        }
    }
}
