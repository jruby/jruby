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

import org.jcodings.Encoding;
import org.jcodings.transcode.EConv;
import org.joda.time.DateTime;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.IOEncodable;

/**
 *
 */
@JRubyClass(name = "Zlib::GzipFile")
public class RubyGzipFile extends RubyObject implements IOEncodable {
    @JRubyClass(name = "Zlib::GzipFile::Error", parent = "Zlib::Error")
    public static class Error {}

    @JRubyClass(name = "Zlib::GzipFile::CRCError", parent = "Zlib::GzipFile::Error")
    public static class CRCError extends Error {}

    @JRubyClass(name = "Zlib::GzipFile::NoFooter", parent = "Zlib::GzipFile::Error")
    public static class NoFooter extends Error {}

    @JRubyClass(name = "Zlib::GzipFile::LengthError", parent = "Zlib::GzipFile::Error")
    public static class LengthError extends Error {}

    static IRubyObject wrapBlock(ThreadContext context, RubyGzipFile instance, Block block) {
        if (block.isGiven()) {
            try {
                return block.yield(context, instance);
            } finally {
                if (!instance.isClosed()) {
                    instance.close();
                }
            }
        }
        return instance;
    }

    public static IRubyObject wrap(ThreadContext context, IRubyObject recv, IRubyObject io, Block block) {
        return wrap19(context, recv, new IRubyObject[]{io}, block);
    }
    
    @JRubyMethod(meta = true, name = "wrap", required = 1, optional = 1)
    public static IRubyObject wrap19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyGzipFile instance;

        // TODO: People extending GzipWriter/reader will break.  Find better way here.
        if (recv == runtime.getModule("Zlib").getClass("GzipWriter")) {
            instance = JZlibRubyGzipWriter.newInstance(recv, args);
        } else {
            instance = JZlibRubyGzipReader.newInstance(recv, args);
        }

        return wrapBlock(context, instance, block);
    }

    @JRubyMethod(name = "new", meta = true)
    public static RubyGzipFile newInstance(IRubyObject recv, Block block) {
        RubyClass klass = (RubyClass) recv;

        RubyGzipFile result = (RubyGzipFile) klass.allocate();

        result.callInit(IRubyObject.NULL_ARRAY, block);

        return result;
    }

    // These methods are here to avoid defining a singleton #path on every instance, as in MRI

    @JRubyMethod
    public IRubyObject path(ThreadContext context) {
        return this.realIo.callMethod(context, "path");
    }

    @JRubyMethod(name = "respond_to?", frame = true)
    public IRubyObject respond_to(ThreadContext context, IRubyObject name) {
        if (name.asJavaString().equals("path")) {
            return sites(context).reader_respond_to.call(context, this, this.realIo, name);
        }

        return Helpers.invokeSuper(context, this, name, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "respond_to?", frame = true)
    public IRubyObject respond_to(ThreadContext context, IRubyObject name, IRubyObject includePrivate) {
        if (name.asJavaString().equals("path")) {
            return sites(context).reader_respond_to.call(context, this, this.realIo, name, includePrivate);
        }

        return Helpers.invokeSuper(context, this, name, Block.NULL_BLOCK);
    }
    
    public RubyGzipFile(Ruby runtime, RubyClass type) {
        super(runtime, type);
        mtime = RubyTime.newTime(runtime, new DateTime());
        enc = null;
        enc2 = null;
    }
    
    // rb_gzfile_ecopts
    protected void ecopts(ThreadContext context, IRubyObject opts) {
        if (!opts.isNil()) {
            EncodingUtils.ioExtractEncodingOption(context, this, opts, null);
        }
        if (enc2 != null) {
            IRubyObject[] outOpts = new IRubyObject[]{opts};
            ecflags = EncodingUtils.econvPrepareOpts(context, opts, outOpts);
            ec = EncodingUtils.econvOpenOpts(context, enc.getName(), enc2.getName(), ecflags, opts);
            ecopts = opts;
        }
    }
    
    public Encoding getReadEncoding() {
        return enc == null ? getRuntime().getDefaultExternalEncoding() : enc;
    }
    
    public Encoding getEnc() {
        return enc;
    }
    
    public Encoding getInternalEncoding() {
        return enc2 == null ? getEnc() : enc2;
    }
    
    public Encoding getEnc2() {
        return enc2;
    }

    // c: gzfile_newstr
    protected RubyString newStr(Ruby runtime, ByteList value) {
        if (enc2 == null) {
            return RubyString.newString(runtime, value, getReadEncoding());
        }

        if (ec != null && enc2.isDummy()) {
            value = EncodingUtils.econvStrConvert(runtime.getCurrentContext(), ec, value, 0);
            return RubyString.newString(runtime, value, getEnc());
        }

        return EncodingUtils.strConvEncOpts(runtime.getCurrentContext(), RubyString.newString(runtime, value), enc2, enc, ecflags, ecopts);
    }

    @JRubyMethod(name = "os_code")
    public IRubyObject os_code() {
        return getRuntime().newFixnum(osCode & 0xff);
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        return closed ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    protected boolean isClosed() {
        return closed;
    }

    @JRubyMethod(name = "orig_name")
    public IRubyObject orig_name() {
        if (closed) {
            throw RubyZlib.newGzipFileError(getRuntime(), "closed gzip stream");
        }
        return nullFreeOrigName == null ? getRuntime().getNil() : nullFreeOrigName;
    }

    @JRubyMethod(name = "to_io")
    public IRubyObject to_io() {
        return realIo;
    }

    @JRubyMethod(name = "comment")
    public IRubyObject comment() {
        if (closed) {
            throw RubyZlib.newGzipFileError(getRuntime(), "closed gzip stream");
        }
        return nullFreeComment == null ? getRuntime().getNil() : nullFreeComment;
    }

    @JRubyMethod(name = "crc")
    public IRubyObject crc() {
        return getRuntime().newFixnum(0);
    }

    @JRubyMethod(name = "mtime")
    public IRubyObject mtime() {
        return mtime;
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync() {
        return sync ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "finish")
    public IRubyObject finish() {
        if (!finished) {
            //io.finish();
        }
        finished = true;
        return realIo;
    }

    @JRubyMethod(name = "close")
    public IRubyObject close() {
        return realIo;
    }

    @JRubyMethod(name = "level")
    public IRubyObject level() {
        return getRuntime().newFixnum(level);
    }

    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject set_sync(IRubyObject arg) {
        sync = ((RubyBoolean) arg).isTrue();
        return sync ? getRuntime().getTrue() : getRuntime().getFalse();
    }
    
    @Override
    public void setEnc(Encoding readEncoding) {
        this.enc = readEncoding;
    }
    
    @Override
    public void setEnc2(Encoding writeEncoding) {
        this.enc2 = writeEncoding;
    }
    
    @Override
    public void setEcflags(int ecflags) {
        this.ecflags = ecflags;
    }
    
    @Override
    public int getEcflags() {
        return ecflags;
    }
    
    @Override
    public void setEcopts(IRubyObject ecopts) {
        this.ecopts = ecopts;
    }
    
    @Override
    public IRubyObject getEcopts() {
        return ecopts;
    }
    
    @Override
    public void setBOM(boolean bom) {
        this.hasBOM = bom;
    }
    
    @Override
    public boolean getBOM() {
        return hasBOM;
    }

    private static JavaSites.ZlibSites sites(ThreadContext context) {
        return context.sites.Zlib;
    }
    
    protected boolean closed = false;
    protected boolean finished = false;
    protected boolean hasBOM;
    protected final byte osCode = Zlib.OS_UNKNOWN;
    protected int level = -1;
    protected RubyString nullFreeOrigName;
    protected RubyString nullFreeComment;
    protected IRubyObject realIo;
    protected RubyTime mtime;
    protected Encoding enc;
    protected Encoding enc2;
    protected int ecflags;
    protected IRubyObject ecopts;
    protected EConv ec;
    protected boolean sync = false;
    protected EConv readTranscoder = null;
    protected EConv writeTranscoder = null;
}