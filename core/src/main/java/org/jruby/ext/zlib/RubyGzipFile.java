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
import org.jruby.RubyBasicObject;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyTime;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.api.Access;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.IOEncodable;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;

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

    @Deprecated(since = "10.0")
    public static IRubyObject wrap(ThreadContext context, IRubyObject recv, IRubyObject io, Block block) {
        return wrap(context, recv, new IRubyObject[]{io}, block);
    }

    @Deprecated(since = "10.0")
    public static IRubyObject wrap19(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return wrap(context, recv, args, block);
    }
    
    @JRubyMethod(meta = true, name = "wrap", required = 1, optional = 1, checkArity = false)
    public static IRubyObject wrap(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyGzipFile instance = ((RubyModule) recv).isKindOfModule(Access.getClass(context, "Zlib", "GzipWriter")) ?
                JZlibRubyGzipWriter.newInstance(context, (RubyClass) recv, args) :
                JZlibRubyGzipReader.newInstance(context, (RubyClass) recv, args);

        return wrapBlock(context, instance, block);
    }

    @Deprecated(since = "10.0")
    public static RubyGzipFile newInstance(IRubyObject recv, Block block) {
        return newInstance(((RubyBasicObject) recv).getCurrentContext(), recv, block);
    }

    @JRubyMethod(name = "new", meta = true)
    public static RubyGzipFile newInstance(ThreadContext context, IRubyObject recv, Block block) {
        RubyClass klass = (RubyClass) recv;

        RubyGzipFile result = (RubyGzipFile) klass.allocate(context);

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

    @Deprecated(since = "10.0")
    public Encoding getReadEncoding() {
        return getReadEncoding(getCurrentContext());
    }

    public Encoding getReadEncoding(ThreadContext context) {
        return enc == null ? context.runtime.getDefaultExternalEncoding() : enc;
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

    @Deprecated(since = "10.0")
    protected RubyString newStr(Ruby runtime, ByteList value) {
        return newStr(runtime.getCurrentContext(), value);
    }

    // c: gzfile_newstr
    protected RubyString newStr(ThreadContext context, ByteList value) {
        if (enc2 == null) return newString(context, value, getReadEncoding(context));

        return ec != null && enc2.isDummy() ?
                newString(context, EncodingUtils.econvStrConvert(context, ec, value, 0), getEnc()) :
                EncodingUtils.strConvEncOpts(context, newString(context, value), enc2, enc, ecflags, ecopts);
    }

    @Deprecated
    public IRubyObject os_code() {
        return os_code(getCurrentContext());
    }

    @JRubyMethod(name = "os_code")
    public IRubyObject os_code(ThreadContext context) {
        return asFixnum(context, osCode & 0xff);
    }

    @Deprecated
    public IRubyObject closed_p() {
        return closed_p(getCurrentContext());
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p(ThreadContext context) {
        return closed ? context.tru : context.fals;
    }

    protected boolean isClosed() {
        return closed;
    }

    @Deprecated(since = "10.0")
    public IRubyObject orig_name() {
        return orig_name(getCurrentContext());
    }

    @JRubyMethod(name = "orig_name")
    public IRubyObject orig_name(ThreadContext context) {
        if (closed) throw RubyZlib.newGzipFileError(context, "closed gzip stream");

        return nullFreeOrigName == null ? context.nil : nullFreeOrigName;
    }

    @Deprecated(since = "10.0")
    public IRubyObject to_io() {
        return to_io(getCurrentContext());
    }

    @JRubyMethod(name = "to_io")
    public IRubyObject to_io(ThreadContext context) {
        return realIo;
    }

    @Deprecated(since = "10.0")
    public IRubyObject comment() {
        return comment(getCurrentContext());
    }

    @JRubyMethod(name = "comment")
    public IRubyObject comment(ThreadContext context) {
        if (closed) throw RubyZlib.newGzipFileError(context, "closed gzip stream");

        return nullFreeComment == null ? context.nil : nullFreeComment;
    }

    @Deprecated
    public IRubyObject crc() {
        return crc(getCurrentContext());
    }

    @JRubyMethod(name = "crc")
    public IRubyObject crc(ThreadContext context) {
        return asFixnum(context, 0);
    }

    @JRubyMethod(name = "mtime")
    public IRubyObject mtime() {
        return mtime;
    }

    @Deprecated
    public IRubyObject sync() {
        return sync(getCurrentContext());
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync(ThreadContext context) {
        return sync ? context.tru : context.fals;
    }

    @Deprecated(since = "10.0")
    public IRubyObject finish() {
        return finish(getCurrentContext());
    }

    @JRubyMethod(name = "finish")
    public IRubyObject finish(ThreadContext context) {
        if (!finished) {
            //io.finish();
        }
        finished = true;
        return realIo;
    }

    @Deprecated(since = "10.0")
    public IRubyObject close() {
        return close(getCurrentContext());
    }

    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        return realIo;
    }

    @Deprecated
    public IRubyObject level() {
        return level(getCurrentContext());
    }

    @JRubyMethod(name = "level")
    public IRubyObject level(ThreadContext context) {
        return asFixnum(context, level);
    }

    @Deprecated(since = "10.0")
    public IRubyObject set_sync(IRubyObject arg) {
        return set_sync(getCurrentContext(), arg);
    }

    @JRubyMethod(name = "sync=")
    public IRubyObject set_sync(ThreadContext context, IRubyObject arg) {
        sync = arg.isTrue();
        return sync ? context.tru : context.fals;
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
