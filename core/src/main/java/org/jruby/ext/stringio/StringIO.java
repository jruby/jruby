/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Ryan Bell <ryan.l.bell@gmail.com>
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2008 Vladimir Sizikov <vsizikov@gmail.com>
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

package org.jruby.ext.stringio;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.*;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.runtime.marshal.DataType;
import org.jruby.util.ArraySupport;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.Getline;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;

import java.util.Arrays;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.runtime.Visibility.PRIVATE;

@JRubyClass(name="StringIO")
public class StringIO extends RubyObject implements EncodingCapable, DataType {
    static class StringIOData {
        /**
         * ATTN: the value of internal might be reset to null
         * (during StringIO.open with block), so watch out for that.
         */
        RubyString string;
        Encoding enc;
        int pos;
        int lineno;
        int flags;
    }
    StringIOData ptr;

    private static final int STRIO_READABLE = ObjectFlags.STRIO_READABLE;
    private static final int STRIO_WRITABLE = ObjectFlags.STRIO_WRITABLE;
    private static final int STRIO_READWRITE = (STRIO_READABLE | STRIO_WRITABLE);

    public static RubyClass createStringIOClass(final Ruby runtime) {
        RubyClass stringIOClass = runtime.defineClass(
                "StringIO", runtime.getData(), StringIO::new);

        stringIOClass.defineAnnotatedMethods(StringIO.class);
        stringIOClass.includeModule(runtime.getEnumerable());

        if (runtime.getObject().isConstantDefined("Java")) {
            stringIOClass.defineAnnotatedMethods(IOJavaAddons.AnyIO.class);
        }

        RubyModule genericReadable = runtime.getIO().defineOrGetModuleUnder("GenericReadable");
        genericReadable.defineAnnotatedMethods(GenericReadable.class);
        stringIOClass.includeModule(genericReadable);

        RubyModule genericWritable = runtime.getIO().defineOrGetModuleUnder("GenericWritable");
        genericWritable.defineAnnotatedMethods(GenericWritable.class);
        stringIOClass.includeModule(genericWritable);

        return stringIOClass;
    }

    // mri: get_enc
    public Encoding getEncoding() {
        return ptr.enc != null ? ptr.enc : ptr.string.getEncoding();
    }

    public void setEncoding(Encoding enc) {
        ptr.enc = enc;
    }

    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return RubyIO.newInstance(context, recv, args, block);
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        StringIO strio = (StringIO)((RubyClass)recv).newInstance(context, args, Block.NULL_BLOCK);
        IRubyObject val = strio;

        if (block.isGiven()) {
            try {
                val = block.yield(context, strio);
            } finally {
                strio.ptr.string = null;
                strio.flags &= ~STRIO_READWRITE;
            }
        }
        return val;
    }

    protected StringIO(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        if (ptr == null) {
            ptr = new StringIOData();
        }

        // does not dispatch quite right and is not really necessary for us
        //Helpers.invokeSuper(context, this, metaClass, "initialize", IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        strioInit(context, args);
        return this;
    }

    // MRI: strio_init
    private void strioInit(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyString string;
        IRubyObject mode;

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            switch (args.length) {
                case 2:
                    mode = args[1];
                    final boolean trunc;
                    if (mode instanceof RubyFixnum) {
                        int flags = RubyFixnum.fix2int(mode);
                        ptr.flags = ModeFlags.getOpenFileFlagsFor(flags);
                        trunc = (flags & ModeFlags.TRUNC) != 0;
                    } else {
                        String m = args[1].convertToString().toString();
                        ptr.flags = OpenFile.ioModestrFmode(runtime, m);
                        trunc = m.length() > 0 && m.charAt(0) == 'w';
                    }
                    string = args[0].convertToString();
                    if ((ptr.flags & OpenFile.WRITABLE) != 0 && string.isFrozen()) {
                        throw runtime.newErrnoEACCESError("Permission denied");
                    }
                    if (trunc) {
                        string.resize(0);
                    }
                    break;
                case 1:
                    string = args[0].convertToString();
                    ptr.flags = string.isFrozen() ? OpenFile.READABLE : OpenFile.READWRITE;
                    break;
                case 0:
                    string = RubyString.newEmptyString(runtime, runtime.getDefaultExternalEncoding());
                    ptr.flags = OpenFile.READWRITE;
                    break;
                default:
                    throw runtime.newArgumentError(args.length, 2);
            }

            ptr.string = string;
            ptr.enc = null;
            ptr.pos = 0;
            ptr.lineno = 0;
            // funky way of shifting readwrite flags into object flags
            flags |= (ptr.flags & OpenFile.READWRITE) * (STRIO_READABLE / OpenFile.READABLE);
        }
    }

    // MRI: strio_copy
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        StringIO otherIO = (StringIO) TypeConverter.convertToType(other,
                context.runtime.getClass("StringIO"), "to_strio");

        if (this == otherIO) return this;

        ptr = otherIO.ptr;
        infectBy(otherIO);
        flags &= ~STRIO_READWRITE;
        flags |= otherIO.flags & STRIO_READWRITE;

        return this;
    }

    @JRubyMethod
    public IRubyObject binmode(ThreadContext context) {
        ptr.enc = EncodingUtils.ascii8bitEncoding(context.runtime);
        if (writable()) ptr.string.setEncoding(ptr.enc);

        return this;
    }

    @JRubyMethod(name = "flush")
    public IRubyObject strio_self() {
        return this;
    }

    @JRubyMethod(name = {"fcntl"}, rest = true)
    public IRubyObject strio_unimpl(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("");
    }

    @JRubyMethod(name = {"fsync"})
    public IRubyObject strioZero(ThreadContext context) {
        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(name = {"sync="})
    public IRubyObject strioFirst(IRubyObject arg) {
        checkInitialized();
        return arg;
    }

    @JRubyMethod(name = {"isatty", "tty?"})
    public IRubyObject strioFalse(ThreadContext context) {
        return context.fals;
    }

    @JRubyMethod(name = {"pid", "fileno"})
    public IRubyObject strioNil(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        checkInitialized();
        if ( closed() ) return context.nil;

        // NOTE: This is 2.0 behavior to allow dup'ed StringIO to remain open when original is closed
        flags &= ~STRIO_READWRITE;

        return context.nil;
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(closed());
    }

    @JRubyMethod
    public IRubyObject close_read(ThreadContext context) {
        // ~ checkReadable() :
        checkInitialized();
        if ( (ptr.flags & OpenFile.READABLE) == 0 ) {
            throw context.runtime.newIOError("not opened for reading");
        }
        if ( ( flags & STRIO_READABLE ) != 0 ) {
            flags &= ~STRIO_READABLE;
        }
        return context.nil;
    }

    @JRubyMethod(name = "closed_read?")
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(!readable());
    }

    @JRubyMethod
    public IRubyObject close_write(ThreadContext context) {
        // ~ checkWritable() :
        checkInitialized();
        if ( (ptr.flags & OpenFile.WRITABLE) == 0 ) {
            throw context.runtime.newIOError("not opened for writing");
        }
        if ( ( flags & STRIO_WRITABLE ) != 0 ) {
            flags &= ~STRIO_WRITABLE;
        }
        return context.nil;
    }

    @JRubyMethod(name = "closed_write?")
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(!writable());
    }

    // MRI: strio_each
    @JRubyMethod(name = "each", writes = FrameField.LASTLINE)
    public IRubyObject each(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each");

        return Getline.getlineCall(context, GETLINE_YIELD, this, getEncoding(), 0, null, null, null, block);
    }

    // MRI: strio_each
    @JRubyMethod(name = "each", writes = FrameField.LASTLINE)
    public IRubyObject each(ThreadContext context, IRubyObject arg0, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each", arg0);

        return Getline.getlineCall(context, GETLINE_YIELD, this, getEncoding(), 1, arg0, null, null, block);
    }

    // MRI: strio_each
    @JRubyMethod(name = "each", writes = FrameField.LASTLINE)
    public IRubyObject each(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each", Helpers.arrayOf(arg0, arg1));

        return Getline.getlineCall(context, GETLINE_YIELD, this, getEncoding(), 2, arg0, arg1, null, block);
    }

    // MRI: strio_each
    @JRubyMethod(name = "each")
    public IRubyObject each(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each", Helpers.arrayOf(arg0, arg1, arg2));

        return Getline.getlineCall(context, GETLINE_YIELD, this, getEncoding(), 3, arg0, arg1, arg2, block);
    }

    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each", args);
        switch (args.length) {
            case 0:
                return each(context, block);
            case 1:
                return each(context, args[0], block);
            case 2:
                return each(context, args[0], args[1], block);
            case 3:
                return each(context, args[0], args[1], args[2], block);
            default:
                Arity.raiseArgumentError(context, args.length, 0, 3);
                throw new AssertionError("BUG");
        }
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line");

        return each(context, block);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg0, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line", arg0);

        return each(context, arg0, block);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line", arg0, arg1);

        return each(context, arg0, arg1, block);
    }

    @JRubyMethod(name = "each_line")
    public IRubyObject each_line(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line", arg0, arg1, arg2);

        return each(context, arg0, arg1, arg2, block);
    }

    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line", args);
        switch (args.length) {
            case 0:
                return each_line(context, block);
            case 1:
                return each_line(context, args[0], block);
            case 2:
                return each_line(context, args[0], args[1], block);
            case 3:
                return each_line(context, args[0], args[1], args[2], block);
            default:
                Arity.raiseArgumentError(context, args.length, 0, 3);
                throw new AssertionError("BUG");
        }
    }

    @JRubyMethod(name = "lines", optional = 2)
    public IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block) {
        context.runtime.getWarnings().warn("StringIO#lines is deprecated; use #each_line instead");
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.runtime, this, "each_line", args);
    }

    @JRubyMethod(name = {"each_byte", "bytes"})
    public IRubyObject each_byte(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_byte");

        checkReadable();

        Ruby runtime = context.runtime;
        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            ByteList bytes = ptr.string.getByteList();

            // Check the length every iteration, since
            // the block can modify this string.
            while (ptr.pos < bytes.length()) {
                block.yield(context, runtime.newFixnum(bytes.get(ptr.pos++) & 0xFF));
            }
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_char");

        IRubyObject c;
        while (!(c = getc(context)).isNil()) {
            block.yieldSpecific(context, c);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject chars(final ThreadContext context, final Block block) {
        context.runtime.getWarnings().warn("StringIO#chars is deprecated; use #each_char instead");

        return each_char(context, block);
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public IRubyObject eof(ThreadContext context) {
        checkReadable();
        if (ptr.pos < ptr.string.size()) return context.fals;
        return context.tru;
    }

    private boolean isEndOfString() {
        return ptr.pos >= ptr.string.size();
    }

    @JRubyMethod(name = "getc")
    public IRubyObject getc(ThreadContext context) {
        checkReadable();

        if (isEndOfString()) return context.nil;

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            int start = ptr.pos;
            int total = 1 + StringSupport.bytesToFixBrokenTrailingCharacter(ptr.string.getByteList(), start + 1);

            ptr.pos += total;

            return context.runtime.newString(ptr.string.getByteList().makeShared(start, total));
        }
    }

    @JRubyMethod(name = "getbyte")
    public IRubyObject getbyte(ThreadContext context) {
        checkReadable();

        if (isEndOfString()) return context.nil;

        int c;
        StringIOData ptr = this.ptr;
        synchronized (ptr) {
            c = ptr.string.getByteList().get(this.ptr.pos++) & 0xFF;
        }

        return context.runtime.newFixnum(c);
    }

    private RubyString strioSubstr(Ruby runtime, int pos, int len, Encoding enc) {
        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            final RubyString string = ptr.string;
            final ByteList stringBytes = string.getByteList();
            int rlen = string.size() - pos;

            if (len > rlen) len = rlen;
            if (len < 0) len = 0;

            if (len == 0) return RubyString.newEmptyString(runtime, enc);
            string.setByteListShared(); // we only share the byte[] buffer but its easier this way
            return RubyString.newStringShared(runtime, stringBytes.getUnsafeBytes(), stringBytes.getBegin() + pos, len, enc);
        }
    }

    private static final int CHAR_BIT = 8;

    private static void bm_init_skip(int[] skip, byte[] pat, int patPtr, int m) {
        int c;

        for (c = 0; c < (1 << CHAR_BIT); c++) {
            skip[c] = m;
        }
        while ((--m) > 0) {
            skip[pat[patPtr++]] = m;
        }
    }

    // Note that this is substantially more complex in 2.0 (Onigmo)
    private static int bm_search(byte[] little, int lstart, int llen, byte[] big, int bstart, int blen, int[] skip) {
        int i, j, k;

        i = llen - 1;
        while (i < blen) {
            k = i;
            j = llen - 1;
            while (j >= 0 && big[k + bstart] == little[j + lstart]) {
                k--;
                j--;
            }
            if (j < 0) return k + 1;
            i += skip[big[i + bstart] & 0xFF];
        }
        return -1;
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context) {
        return Getline.getlineCall(context, GETLINE, this, getEncoding());
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject arg0) {
        return Getline.getlineCall(context, GETLINE, this, getEncoding(), arg0);
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return Getline.getlineCall(context, GETLINE, this, getEncoding(), arg0, arg1);
    }

    @JRubyMethod(name = "gets", writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Getline.getlineCall(context, GETLINE, this, getEncoding(), arg0, arg1, arg2);
    }

    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return gets(context);
            case 1:
                return gets(context, args[0]);
            case 2:
                return gets(context, args[0], args[1]);
            case 3:
                return gets(context, args[0], args[1], args[2]);
            default:
                Arity.raiseArgumentError(context, args.length, 0, 3);
                throw new AssertionError("BUG");
        }
    }

    private static final Getline.Callback<StringIO, IRubyObject> GETLINE = new Getline.Callback<StringIO, IRubyObject>() {
        @Override
        public IRubyObject getline(ThreadContext context, StringIO self, IRubyObject rs, int limit, boolean chomp, Block block) {
            if (limit == 0) {
                return RubyString.newEmptyString(context.runtime, self.getEncoding());
            }

            IRubyObject result = self.getline(context, rs, limit, chomp);

            context.setLastLine(result);

            return result;
        }
    };

    private static final Getline.Callback<StringIO, StringIO> GETLINE_YIELD = new Getline.Callback<StringIO, StringIO>() {
        @Override
        public StringIO getline(ThreadContext context, StringIO self, IRubyObject rs, int limit, boolean chomp, Block block) {
            IRubyObject line;

            if (limit == 0) {
                throw context.runtime.newArgumentError("invalid limit: 0 for each_line");
            }

            while (!(line = self.getline(context, rs, limit, chomp)).isNil()) {
                block.yieldSpecific(context, line);
            }

            return self;
        }
    };

    private static final Getline.Callback<StringIO, RubyArray> GETLINE_ARY = new Getline.Callback<StringIO, RubyArray>() {
        @Override
        public RubyArray getline(ThreadContext context, StringIO self, IRubyObject rs, int limit, boolean chomp, Block block) {
            RubyArray ary = context.runtime.newArray();
            IRubyObject line;

            if (limit == 0) {
                throw context.runtime.newArgumentError("invalid limit: 0 for readlines");
            }

            while (!(line = self.getline(context, rs, limit, chomp)).isNil()) {
                ary.append(line);
            }

            return ary;
        }
    };

    // strio_getline
    private IRubyObject getline(ThreadContext context, final IRubyObject rs, int limit, boolean chomp) {
        Ruby runtime = context.runtime;

        RubyString str;

        checkReadable();

        int n;

        if (isEndOfString()) {
            return context.nil;
        }

        StringIOData ptr = this.ptr;
        Encoding enc = getEncoding();

        synchronized (ptr) {
            final ByteList string = ptr.string.getByteList();
            final byte[] stringBytes = string.getUnsafeBytes();
            int begin = string.getBegin();
            int s = begin + ptr.pos;
            int e = begin + string.getRealSize();
            int p;
            int w = 0;

            if (limit > 0 && s + limit < e) {
                e = getEncoding().rightAdjustCharHead(stringBytes, s, s + limit, e);
            }
            if (rs == context.nil) {
                if (chomp) {
                    w = chompNewlineWidth(stringBytes, s, e);
                }
                str = strioSubstr(runtime, ptr.pos, e - s - w, enc);
            } else if ((n = ((RubyString) rs).size()) == 0) {
                // this is not an exact port; the original confused me
                // in MRI, the next loop appears to have a complicated boolean to determine the index, but in actuality
                // it just resolves to p (+ 0) as below. We theorize that the MRI logic may have originally been
                // intended to skip all \n and \r, but because p does not get incremented before the \r check that
                // logic never fires. We use the original logic that did not have these strange flaws.
                // See https://github.com/ruby/ruby/commit/30540c567569d3486ccbf59b59d903d5778f04d5
                p = s;
                while (stringBytes[p] == '\n') {
                    if (++p == e) {
                        return context.nil;
                    }
                }
                s = p;
                while ((p = StringSupport.memchr(stringBytes, p, '\n', e - p)) != -1 && (p != e)) {
                    p += 1;
                    if (p == e) break;

                    if (stringBytes[p] == '\n') {
                        e = p + 1;
                        w = (chomp ? 1 : 0);
                        break;
                    }
            	    else if (stringBytes[p] == '\r' && p < e && stringBytes[p + 1] == '\n') {
                        e = p + 2;
                        w = (chomp ? 2 : 0);
                        break;
                    }
                }
                if (w == 0 && chomp) {
                    w = chompNewlineWidth(stringBytes, s, e);
                }
                str = strioSubstr(runtime, s - begin, e - s - w, enc);
            } else if (n == 1) {
                RubyString strStr = (RubyString) rs;
                ByteList strByteList = strStr.getByteList();
                if ((p = StringSupport.memchr(stringBytes, s, strByteList.get(0), e - s)) != -1) {
                    e = p + 1;
                    w = (chomp ? ((p > s && stringBytes[p-1] == '\r')?1:0) + 1 : 0);
                }
                str = strioSubstr(runtime, ptr.pos, e - s - w, enc);
            } else {
                if (n < e - s) {
                    RubyString rsStr = (RubyString) rs;
                    ByteList rsByteList = rsStr.getByteList();
                    byte[] rsBytes = rsByteList.getUnsafeBytes();

                    int[] skip = new int[1 << CHAR_BIT];
                    int pos;
                    p = rsByteList.getBegin();
                    bm_init_skip(skip, rsBytes, p, n);
                    if ((pos = bm_search(rsBytes, p, n, stringBytes, s, e - s, skip)) >= 0) {
                        e = s + pos + n;
                    }
                }
                str = strioSubstr(runtime, ptr.pos, e - s - w, enc);
            }
            ptr.pos = e - begin;
            ptr.lineno++;
        }

        return str;
    }

    private static int chompNewlineWidth(byte[] bytes, int s, int e) {
        if (e > s && bytes[--e] == '\n') {
            if (e > s && bytes[--e] == '\r') return 2;
            return 1;
        }
        return 0;
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length() {
        checkInitialized();
        checkFinalized();
        return getRuntime().newFixnum(ptr.string.size());
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno(ThreadContext context) {
        return context.runtime.newFixnum(ptr.lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(ThreadContext context, IRubyObject arg) {
        ptr.lineno = RubyNumeric.fix2int(arg);

        return context.nil;
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos(ThreadContext context) {
        checkInitialized();

        return context.runtime.newFixnum(ptr.pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject arg) {
        checkInitialized();

        long p = RubyNumeric.fix2long(arg);

        if (p < 0) throw getRuntime().newErrnoEINVALError(arg.toString());

        if (p > Integer.MAX_VALUE) throw getRuntime().newArgumentError("JRuby does not support StringIO larger than " + Integer.MAX_VALUE + " bytes");

        ptr.pos = (int)p;

        return arg;
    }

    private void strioExtend(int pos, int len) {
        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            final int olen = ptr.string.size();
            if (pos + len > olen) {
                ptr.string.resize(pos + len);
                if (pos > olen) {
                    ptr.string.modify19();
                    ByteList ptrByteList = ptr.string.getByteList();
                    // zero the gap
                    Arrays.fill(ptrByteList.getUnsafeBytes(),
                            ptrByteList.getBegin() + olen,
                            ptrByteList.getBegin() + pos,
                            (byte) 0);
                }
            } else {
                ptr.string.modify19();
            }
        }
    }

    // MRI: strio_putc
    @JRubyMethod(name = "putc")
    public IRubyObject putc(ThreadContext context, IRubyObject ch) {
        Ruby runtime = context.runtime;
        checkWritable();
        IRubyObject str;

        checkModifiable();
        if (ch instanceof RubyString) {
            str = ((RubyString)ch).substr19(runtime, 0, 1);
        }
        else {
            byte c = RubyNumeric.num2chr(ch);
            str = RubyString.newString(runtime, new byte[]{c});
        }
        write(context, str);
        return ch;
    }

    public static final ByteList NEWLINE = ByteList.create("\n");

    @JRubyMethod(name = "read", optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        final Ruby runtime = context.runtime;
        IRubyObject str = context.nil;
        int len;
        boolean binary = false;

        StringIOData ptr = this.ptr;
        final RubyString string;

        synchronized (ptr) {
            switch (args.length) {
                case 2:
                    str = args[1];
                    if (!str.isNil()) {
                        str = str.convertToString();
                        ((RubyString) str).modify();
                    }
                case 1:
                    if (!args[0].isNil()) {
                        len = RubyNumeric.fix2int(args[0]);

                        if (len < 0) {
                            throw runtime.newArgumentError("negative length " + len + " given");
                        }
                        if (len > 0 && isEndOfString()) {
                            if (!str.isNil()) ((RubyString) str).resize(0);
                            return context.nil;
                        }
                        binary = true;
                        break;
                    }
                case 0:
                    len = ptr.string.size();
                    if (len <= ptr.pos) {
                        Encoding enc = binary ? ASCIIEncoding.INSTANCE : getEncoding();
                        if (str.isNil()) {
                            str = runtime.newString();
                        } else {
                            ((RubyString) str).resize(0);
                        }
                        ((RubyString) str).setEncoding(enc);
                        return str;
                    } else {
                        len -= ptr.pos;
                    }
                    break;
                default:
                    throw runtime.newArgumentError(args.length, 0);
            }

            if (str.isNil()) {
                Encoding enc = binary ? ASCIIEncoding.INSTANCE : getEncoding();
                string = strioSubstr(runtime, ptr.pos, len, enc);
            } else {
                string = (RubyString) str;
                int rest = ptr.string.size() - ptr.pos;
                if (len > rest) len = rest;
                string.resize(len);
                ByteList strByteList = string.getByteList();
                byte[] strBytes = strByteList.getUnsafeBytes();
                ByteList dataByteList = ptr.string.getByteList();
                byte[] dataBytes = dataByteList.getUnsafeBytes();
                System.arraycopy(dataBytes, dataByteList.getBegin() + ptr.pos, strBytes, strByteList.getBegin(), len);
                if (binary) {
                    string.setEncoding(ASCIIEncoding.INSTANCE);
                } else {
                    string.setEncoding(ptr.string.getEncoding());
                }
            }
            ptr.pos += string.size();
        }

        return string;
    }

    @JRubyMethod(name = "readlines")
    public IRubyObject readlines(ThreadContext context) {
        return Getline.getlineCall(context, GETLINE_ARY, this, getEncoding());
    }

    @JRubyMethod(name = "readlines")
    public IRubyObject readlines(ThreadContext context, IRubyObject arg0) {
        return Getline.getlineCall(context, GETLINE_ARY, this, getEncoding(), arg0);
    }

    @JRubyMethod(name = "readlines")
    public IRubyObject readlines(ThreadContext context, IRubyObject arg0, IRubyObject arg1) {
        return Getline.getlineCall(context, GETLINE_ARY, this, getEncoding(), arg0, arg1);
    }

    @JRubyMethod(name = "readlines")
    public IRubyObject readlines(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return Getline.getlineCall(context, GETLINE_ARY, this, getEncoding(), arg0, arg1, arg2);
    }

    public IRubyObject readlines(ThreadContext context, IRubyObject[] args) {
        switch (args.length) {
            case 0:
                return readlines(context);
            case 1:
                return readlines(context, args[0]);
            case 2:
                return readlines(context, args[0], args[1]);
            case 3:
                return readlines(context, args[0], args[1], args[2]);
            default:
                Arity.raiseArgumentError(context, args.length, 0, 3);
                throw new AssertionError("BUG");
        }
    }

    // MRI: strio_reopen
    @JRubyMethod(name = "reopen", required = 0, optional = 2)
    public IRubyObject reopen(ThreadContext context, IRubyObject[] args) {
        checkFrozen();

        if (args.length == 1 && !(args[0] instanceof RubyString)) {
            return initialize_copy(context, args[0]);
        }

        // reset the state
        strioInit(context, args);
        return this;
    }

    @JRubyMethod(name = "rewind")
    public IRubyObject rewind(ThreadContext context) {
        checkInitialized();

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            ptr.pos = 0;
            ptr.lineno = 0;
        }

        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(required = 1, optional = 1)
    public IRubyObject seek(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        checkFrozen();
        checkFinalized();

        int offset = RubyNumeric.num2int(args[0]);
        IRubyObject whence = context.nil;

        if (args.length > 1 && !args[0].isNil()) whence = args[1];

        checkOpen();

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            switch (whence.isNil() ? 0 : RubyNumeric.num2int(whence)) {
                case 0:
                    break;
                case 1:
                    offset += ptr.pos;
                    break;
                case 2:
                    offset += ptr.string.size();
                    break;
                default:
                    throw runtime.newErrnoEINVALError("invalid whence");
            }

            if (offset < 0) throw runtime.newErrnoEINVALError("invalid seek value");

            ptr.pos = offset;
        }

        return RubyFixnum.zero(runtime);
    }

    @JRubyMethod(name = "string=", required = 1)
    public IRubyObject set_string(IRubyObject arg) {
        checkFrozen();
        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            ptr.flags &= ~OpenFile.READWRITE;
            RubyString str = arg.convertToString();
            ptr.flags = str.isFrozen() ? OpenFile.READABLE : OpenFile.READWRITE;
            ptr.pos = 0;
            ptr.lineno = 0;
            return ptr.string = str;
        }
    }

    @JRubyMethod(name = "string")
    public IRubyObject string(ThreadContext context) {
        RubyString string = ptr.string;
        if (string == null) return context.nil;

        return string;
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync(ThreadContext context) {
        checkInitialized();
        return context.tru;
    }

    // only here for the fake-out class in org.jruby
    public IRubyObject sysread(IRubyObject[] args) {
        return GenericReadable.sysread(getRuntime().getCurrentContext(), this, args);
    }

    @JRubyMethod(name = "truncate", required = 1)
    public IRubyObject truncate(IRubyObject len) {
        checkWritable();

        int l = RubyFixnum.fix2int(len);
        StringIOData ptr = this.ptr;
        RubyString string = ptr.string;

        synchronized (ptr) {
            int plen = string.size();
            if (l < 0) {
                throw getRuntime().newErrnoEINVALError("negative legnth");
            }
            string.resize(l);
            ByteList buf = string.getByteList();
            if (plen < l) {
                // zero the gap
                Arrays.fill(buf.getUnsafeBytes(), buf.getBegin() + plen, buf.getBegin() + l, (byte) 0);
            }
        }

        return len;
    }

    @JRubyMethod(name = "ungetc")
    public IRubyObject ungetc(ThreadContext context, IRubyObject arg) {
        Encoding enc, enc2;

        checkModifiable();
        checkReadable();

        if (arg.isNil()) return arg;
        if (arg instanceof RubyInteger) {
            int len, cc = RubyNumeric.num2int(arg);
            byte[] buf = new byte[16];

            enc = getEncoding();
            len = enc.codeToMbcLength(cc);
            if (len <= 0) EncodingUtils.encUintChr(context, cc, enc);
            enc.codeToMbc(cc, buf, 0);
            ungetbyteCommon(buf, 0, len);
            return context.nil;
        } else {
            arg = arg.convertToString();
            enc = getEncoding();
            RubyString argStr = (RubyString) arg;
            enc2 = argStr.getEncoding();
            if (enc != enc2 && enc != ASCIIEncoding.INSTANCE) {
                argStr = EncodingUtils.strConvEnc(context, argStr, enc2, enc);
            }
            ByteList argBytes = argStr.getByteList();
            ungetbyteCommon(argBytes.unsafeBytes(), argBytes.begin(), argBytes.realSize());
            return context.nil;
        }
    }

    private void ungetbyteCommon(int c) {
        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            ptr.string.modify();
            ptr.pos--;

            ByteList bytes = ptr.string.getByteList();

            if (isEndOfString()) bytes.length(ptr.pos + 1);

            if (ptr.pos == -1) {
                bytes.prepend((byte) c);
                ptr.pos = 0;
            } else {
                bytes.set(ptr.pos, c);
            }
        }
    }

    private void ungetbyteCommon(RubyString ungetBytes) {
        ByteList ungetByteList = ungetBytes.getByteList();
        ungetbyteCommon(ungetByteList.unsafeBytes(), ungetByteList.begin(), ungetByteList.realSize());
    }

    private void ungetbyteCommon(byte[] ungetBytes, int ungetBegin, int ungetLen) {
        final int start; // = ptr.pos;

        if (ungetLen == 0) return;

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            ptr.string.modify();

            if (ungetLen > ptr.pos) {
                start = 0;
            } else {
                start = ptr.pos - ungetLen;
            }

            ByteList byteList = ptr.string.getByteList();

            if (isEndOfString()) byteList.length(Math.max(ptr.pos, ungetLen));

            byteList.replace(start, ptr.pos - start, ungetBytes, ungetBegin, ungetLen);

            ptr.pos = start;
        }
    }

    @JRubyMethod
    public IRubyObject ungetbyte(ThreadContext context, IRubyObject arg) {
        // TODO: Not a line-by-line port.
        checkReadable();

        if (arg.isNil()) return arg;

        checkModifiable();

        if (arg instanceof RubyInteger) {
            ungetbyteCommon(((RubyInteger) ((RubyInteger) arg).op_mod(context, 256)).getIntValue());
        } else {
            ungetbyteCommon(arg.convertToString());
        }

        return context.nil;
    }

    // MRI: strio_write
    @JRubyMethod(name = "write")
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        Ruby runtime = context.runtime;
        return RubyFixnum.newFixnum(runtime, stringIOWrite(context, runtime, arg));
    }

    @JRubyMethod(name = "write", required = 1, rest = true)
    public IRubyObject write(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        long len = 0;
        for (IRubyObject arg : args) {
            len += stringIOWrite(context, runtime, arg);
        }
        return RubyFixnum.newFixnum(runtime, len);
    }

    // MRI: strio_write
    private long stringIOWrite(ThreadContext context, Ruby runtime, IRubyObject arg) {
        checkWritable();

        RubyString str = arg.asString();
        int len, olen;

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            final Encoding enc = getEncoding();
            final Encoding encStr = str.getEncoding();
            if (enc != encStr && enc != EncodingUtils.ascii8bitEncoding(runtime)
                    // this is a hack because we don't seem to handle incoming ASCII-8BIT properly in transcoder
                    && encStr != ASCIIEncoding.INSTANCE) {
                str = EncodingUtils.strConvEnc(context, str, encStr, enc);
            }
            final ByteList strByteList = str.getByteList();
            len = str.size();
            if (len == 0) return 0;
            checkModifiable();
            olen = ptr.string.size();
            if ((ptr.flags & OpenFile.APPEND) != 0) {
                ptr.pos = olen;
            }
            if (ptr.pos == olen) {
                if (enc == EncodingUtils.ascii8bitEncoding(runtime) || encStr == EncodingUtils.ascii8bitEncoding(runtime)) {
                    EncodingUtils.encStrBufCat(runtime, ptr.string, strByteList, enc);
                    ptr.string.infectBy(str);
                } else {
                    ptr.string.cat19(str);
                }
            } else {
                strioExtend(ptr.pos, len);
                ByteList ptrByteList = ptr.string.getByteList();
                System.arraycopy(strByteList.getUnsafeBytes(), strByteList.getBegin(), ptrByteList.getUnsafeBytes(), ptrByteList.begin() + ptr.pos, len);
                ptr.string.infectBy(str);
            }
            ptr.string.infectBy(this);
            ptr.pos += len;
        }

        return len;
    }

    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject ext_enc) {
        final Encoding enc;
        if ( ext_enc.isNil() ) {
            enc = EncodingUtils.defaultExternalEncoding(context.runtime);
        } else {
            enc = EncodingUtils.rbToEncoding(context, ext_enc);
        }

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            ptr.enc = enc;

            // in read-only mode, StringIO#set_encoding no longer sets the encoding
            RubyString string;
            if (writable() && (string = ptr.string).getEncoding() != enc) {
                string.modify();
                string.setEncoding(enc);
            }
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc, IRubyObject ignored) {
        return set_encoding(context, enc);
    }

    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc, IRubyObject ignored1, IRubyObject ignored2) {
        return set_encoding(context, enc);
    }

    @JRubyMethod
    public IRubyObject external_encoding(ThreadContext context) {
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(getEncoding());
    }

    @JRubyMethod
    public IRubyObject internal_encoding(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "each_codepoint")
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;

        if (!block.isGiven()) return enumeratorize(runtime, this, "each_codepoint");

        checkReadable();

        StringIOData ptr = this.ptr;

        synchronized (ptr) {
            final Encoding enc = getEncoding();
            final ByteList string = ptr.string.getByteList();
            final byte[] stringBytes = string.getUnsafeBytes();
            int begin = string.getBegin();
            for (; ; ) {
                if (ptr.pos >= ptr.string.size()) return this;

                int c = StringSupport.codePoint(runtime, enc, stringBytes, begin + ptr.pos, stringBytes.length);
                int n = StringSupport.codeLength(enc, c);
                block.yield(context, runtime.newFixnum(c));
                ptr.pos += n;
            }
        }
    }

    @JRubyMethod(name = "codepoints")
    public IRubyObject codepoints(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        runtime.getWarnings().warn("StringIO#codepoints is deprecated; use #each_codepoint");

        if (!block.isGiven()) return enumeratorize(runtime, this, "each_codepoint");

        return each_codepoint(context, block);
    }

    public static class GenericReadable {
        @JRubyMethod(name = "readchar")
        public static IRubyObject readchar(ThreadContext context, IRubyObject self) {
            IRubyObject c = self.callMethod(context, "getc");

            if (c.isNil()) throw context.runtime.newEOFError();

            return c;
        }

        @JRubyMethod(name = "readbyte")
        public static IRubyObject readbyte(ThreadContext context, IRubyObject self) {
            IRubyObject b = self.callMethod(context, "getbyte");

            if (b.isNil()) throw context.runtime.newEOFError();

            return b;
        }

        @JRubyMethod(name = "readline", optional = 1, writes = FrameField.LASTLINE)
        public static IRubyObject readline(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            IRubyObject line = self.callMethod(context, "gets", args);

            if (line.isNil()) throw context.runtime.newEOFError();

            return line;
        }

        @JRubyMethod(name = {"sysread", "readpartial"}, optional = 2)
        public static IRubyObject sysread(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            IRubyObject val = self.callMethod(context, "read", args);

            if (val.isNil()) throw context.runtime.newEOFError();

            return val;
        }

        @JRubyMethod(name = "read_nonblock", required = 1, optional = 2)
        public static IRubyObject read_nonblock(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            final Ruby runtime = context.runtime;

            boolean exception = true;
            IRubyObject opts = ArgsUtil.getOptionsArg(runtime, args);
            if (opts != context.nil) {
                args = ArraySupport.newCopy(args, args.length - 1);
                exception = Helpers.extractExceptionOnlyArg(context, (RubyHash) opts);
            }

            IRubyObject val = self.callMethod(context, "read", args);
            if (val == context.nil) {
                if (!exception) return context.nil;
                throw runtime.newEOFError();
            }

            return val;
        }
    }

    public static class GenericWritable {
        @JRubyMethod(name = "<<", required = 1)
        public static IRubyObject append(ThreadContext context, IRubyObject self, IRubyObject arg) {
            // Claims conversion is done via 'to_s' in docs.
            self.callMethod(context, "write", arg);

            return self;
        }

        @JRubyMethod(name = "print", rest = true, writes = FrameField.LASTLINE)
        public static IRubyObject print(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            return RubyIO.print(context, self, args);
        }

        @JRubyMethod(name = "printf", required = 1, rest = true)
        public static IRubyObject printf(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            self.callMethod(context, "write", RubyKernel.sprintf(context, self, args));
            return context.nil;
        }

        @JRubyMethod(name = "puts", rest = true)
        public static IRubyObject puts(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
            // TODO: This should defer to RubyIO logic, but we don't have puts right there for 1.9
            Ruby runtime = context.runtime;
            if (args.length == 0) {
                RubyIO.write(context, maybeIO, RubyString.newStringShared(runtime, NEWLINE));
                return runtime.getNil();
            }

            for (int i = 0; i < args.length; i++) {
                RubyString line = null;

                if (!args[i].isNil()) {
                    IRubyObject tmp = args[i].checkArrayType();
                    if (!tmp.isNil()) {
                        RubyArray arr = (RubyArray) tmp;
                        if (runtime.isInspecting(arr)) {
                            line = runtime.newString("[...]");
                        } else {
                            inspectPuts(context, maybeIO, arr);
                            continue;
                        }
                    } else {
                        if (args[i] instanceof RubyString) {
                            line = (RubyString) args[i];
                        } else {
                            line = args[i].asString();
                        }
                    }
                }

                if (line != null) RubyIO.write(context, maybeIO, line);

                if (line == null || !line.getByteList().endsWith(NEWLINE)) {
                    RubyIO.write(context, maybeIO, RubyString.newStringShared(runtime, NEWLINE));
                }
            }

            return runtime.getNil();
        }

        private static IRubyObject inspectPuts(ThreadContext context, IRubyObject maybeIO, RubyArray array) {
            Ruby runtime = context.runtime;
            try {
                runtime.registerInspecting(array);
                return puts(context, maybeIO, array.toJavaArray());
            }
            finally {
                runtime.unregisterInspecting(array);
            }
        }

        @JRubyMethod(name = "syswrite", required = 1)
        public static IRubyObject syswrite(ThreadContext context, IRubyObject self, IRubyObject arg) {
            return RubyIO.write(context, self, arg);
        }

        @JRubyMethod(name = "write_nonblock", required = 1, optional = 1)
        public static IRubyObject syswrite_nonblock(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            Ruby runtime = context.runtime;

            ArgsUtil.getOptionsArg(runtime, args); // ignored as in MRI

            return syswrite(context, self, args[0]);
        }
    }

    public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
        return GenericWritable.puts(context, this, args);
    }

    /* rb: check_modifiable */
    public void checkFrozen() {
        super.checkFrozen();
        checkInitialized();
    }

    private boolean readable() {
        return (flags & STRIO_READABLE) != 0
                && (ptr.flags & OpenFile.READABLE) != 0;
    }

    private boolean writable() {
        return (flags & STRIO_WRITABLE) != 0
                && (ptr.flags & OpenFile.WRITABLE) != 0;
    }

    private boolean closed() {
        return !((flags & STRIO_READWRITE) != 0
                && (ptr.flags & OpenFile.READWRITE) != 0);
    }

    /* rb: readable */
    private void checkReadable() {
        checkInitialized();
        if (!readable()) {
            throw getRuntime().newIOError("not opened for reading");
        }
    }

    /* rb: writable */
    private void checkWritable() {
        checkInitialized();
        if (!writable()) {
            throw getRuntime().newIOError("not opened for writing");
        }

        // Tainting here if we ever want it. (secure 4)
    }

    private void checkModifiable() {
        checkFrozen();
        if (ptr.string.isFrozen()) throw getRuntime().newIOError("not modifiable string");
    }

    private void checkInitialized() {
        if (ptr == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }

    private void checkFinalized() {
        if (ptr.string == null) {
            throw getRuntime().newIOError("not opened");
        }
    }

    private void checkOpen() {
        if (closed()) {
            throw getRuntime().newIOError(RubyIO.CLOSED_STREAM_MSG);
        }
    }
}
