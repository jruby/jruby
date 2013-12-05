/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.PRIVATE;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
@JRubyClass(name = "Zlib::ZStream")
public abstract class ZStream extends RubyObject {

    protected boolean closed = false;

    protected abstract int internalTotalIn();

    protected abstract int internalTotalOut();

    protected abstract boolean internalStreamEndP();

    protected abstract void internalReset();

    protected abstract boolean internalFinished();

    protected abstract long internalAdler();

    // TODO: eliminate?
    protected abstract IRubyObject internalFinish();

    protected abstract void internalClose();

    public ZStream(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize(Block unusedBlock) {
        return this;
    }

    @JRubyMethod
    public IRubyObject flush_next_out(ThreadContext context) {
        return RubyString.newEmptyString(context.getRuntime());
    }

    @JRubyMethod
    public IRubyObject total_out() {
        checkClosed();
        return getRuntime().newFixnum(internalTotalOut());
    }

    @JRubyMethod(name = "stream_end?")
    public IRubyObject stream_end_p() {
        return internalStreamEndP() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "data_type")
    public IRubyObject data_type() {
        checkClosed();
        return getRuntime().getModule("Zlib").getConstant("UNKNOWN");
    }

    @JRubyMethod(name = {"closed?", "ended?"})
    public IRubyObject closed_p() {
        return closed ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "reset")
    public IRubyObject reset() {
        checkClosed();
        internalReset();
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "avail_out")
    public IRubyObject avail_out() {
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = "avail_out=", required = 1)
    public IRubyObject set_avail_out(IRubyObject p1) {
        checkClosed();
        
        return p1;
    }

    @JRubyMethod(name = "adler")
    public IRubyObject adler() {
        checkClosed();
        
        return getRuntime().newFixnum(internalAdler());
    }

    @JRubyMethod(name = "finish")
    public IRubyObject finish(ThreadContext context) {
        checkClosed();
        
        IRubyObject result = internalFinish();
        
        return result;
    }

    @JRubyMethod(name = "avail_in")
    public IRubyObject avail_in() {
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = "flush_next_in")
    public IRubyObject flush_next_in(ThreadContext context) {
        return RubyString.newEmptyString(context.getRuntime());
    }

    @JRubyMethod(name = "total_in")
    public IRubyObject total_in() {
        checkClosed();
        
        return getRuntime().newFixnum(internalTotalIn());
    }

    @JRubyMethod(name = "finished?")
    public IRubyObject finished_p(ThreadContext context) {
        checkClosed();
        Ruby runtime = context.getRuntime();
        return internalFinished() ? runtime.getTrue() : runtime.getFalse();
    }

    @JRubyMethod(name = {"close", "end"})
    public IRubyObject close() {
        checkClosed();
        internalClose();
        closed = true;
        return getRuntime().getNil();
    }

    void checkClosed() {
        if (closed) throw RubyZlib.newZlibError(getRuntime(), "stream is not ready");
    }

    // TODO: remove when JZlib checks the given level
    static void checkLevel(Ruby runtime, int level) {
        if ((level < 0 || level > 9) && level != JZlib.Z_DEFAULT_COMPRESSION) {
            throw RubyZlib.newStreamError(runtime, "stream error: invalid level");
        }
    }

    /**
     * We only do windowBits=15(32K buffer, LZ77 algorithm) since java.util.zip
     * only allows it. NOTE: deflateInit2 of zlib.c also accepts MAX_WBITS +
     * 16(gzip compression). inflateInit2 also accepts MAX_WBITS + 16(gzip
     * decompression) and MAX_WBITS + 32(automatic detection of gzip and LZ77).
     */
    // TODO: remove when JZlib checks the given windowBits
    static void checkWindowBits(Ruby runtime, int wbits, boolean forInflate) {
        wbits = Math.abs(wbits);
        if ((wbits & 0xf) < 8) {
            throw RubyZlib.newStreamError(runtime, "stream error: invalid window bits");
        }
        if ((wbits & 0xf) != 0xf) {
            // windowBits < 15 for reducing memory is meaningless on Java platform. 
            runtime.getWarnings().warn("windowBits < 15 is ignored on this platform");
            // continue
        }
        if (forInflate && wbits > JZlib.MAX_WBITS + 32) {
            throw RubyZlib.newStreamError(runtime, "stream error: invalid window bits");
        } else if (!forInflate && wbits > JZlib.MAX_WBITS + 16) {
            throw RubyZlib.newStreamError(runtime, "stream error: invalid window bits");
        }
    }

    // TODO: remove when JZlib checks the given strategy
    static void checkStrategy(Ruby runtime, int strategy) {
        switch (strategy) {
            case JZlib.Z_DEFAULT_STRATEGY:
            case JZlib.Z_FILTERED:
            case JZlib.Z_HUFFMAN_ONLY:
                break;
            default:
                throw RubyZlib.newStreamError(runtime, "stream error: invalid strategy");
        }
    }
}