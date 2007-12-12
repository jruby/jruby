/*
 **** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
 * Copyright (C) 2005 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;

import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;
import org.joni.encoding.Encoding;
import org.joni.encoding.specific.ASCIIEncoding;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Frame;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.Pack;
import org.jruby.util.Sprintf;

/**
 *
 * @author  jpetersen
 */
public class RubyString extends RubyObject {
    private static final ASCIIEncoding ASCII = ASCIIEncoding.INSTANCE;
    
    private static final int TMPLOCK_STR_F = 1 << 11;
    private static final int TMPLOCK_OR_FROZEN_STR_F = TMPLOCK_STR_F | FROZEN_F;

    // string doesn't have it's own ByteList (values) 
    private volatile boolean shared_buffer = false;

    // string has it's own ByteList, but it's pointing to a shared buffer (byte[])
    private volatile boolean shared_bytelist = false;
    
    private ByteList value;

    private static ObjectAllocator STRING_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyString newString = runtime.newString("");
            
            newString.setMetaClass(klass);
            
            return newString;
        }
    };
    
    public static RubyClass createStringClass(Ruby runtime) {
        RubyClass stringClass = runtime.defineClass("String", runtime.getObject(), STRING_ALLOCATOR);
        runtime.setString(stringClass);
        stringClass.index = ClassIndex.STRING;
        stringClass.kindOf = new RubyModule.KindOf() {
                public boolean isKindOf(IRubyObject obj, RubyModule type) {
                    return obj instanceof RubyString;
                }
            };
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyString.class);
        
        stringClass.includeModule(runtime.getComparable());
        stringClass.includeModule(runtime.getEnumerable());
        
        stringClass.defineAnnotatedMethods(RubyString.class);
        stringClass.dispatcher = callbackFactory.createDispatcher(stringClass);
        
        return stringClass;
    }

    /** short circuit for String key comparison
     * 
     */
    public final boolean eql(IRubyObject other) {
        return other instanceof RubyString && value.equal(((RubyString)other).value);
    }

    private RubyString(Ruby runtime, RubyClass rubyClass, CharSequence value) {
        super(runtime, rubyClass);
        assert value != null;
        this.value = new ByteList(ByteList.plain(value),false);
    }

    private RubyString(Ruby runtime, RubyClass rubyClass, byte[] value) {
        super(runtime, rubyClass);
        assert value != null;
        this.value = new ByteList(value);
    }

    private RubyString(Ruby runtime, RubyClass rubyClass, ByteList value) {
        super(runtime, rubyClass);
        assert value != null;
        this.value = value;
    }
    
    private RubyString(Ruby runtime, RubyClass rubyClass, ByteList value, boolean objectSpace) {
        super(runtime, rubyClass, objectSpace);
        assert value != null;
        this.value = value;
    }    

    public int getNativeTypeIndex() {
        return ClassIndex.STRING;
    }

    public Class getJavaClass() {
        return String.class;
    }

    public RubyString convertToString() {
        return this;
    }

    public String toString() {
        return value.toString();
    }

    /** rb_str_dup
     * 
     */
    public final RubyString strDup() {
        return strDup(getMetaClass());
    }

    final RubyString strDup(RubyClass clazz) {
        shared_bytelist = true;
        RubyString dup = new RubyString(getRuntime(), clazz, value);
        dup.shared_bytelist = true;

        dup.infectBy(this);
        return dup;
    }    

    public final RubyString makeShared(int index, int len) {
        if (len == 0) return newEmptyString(getRuntime(), getMetaClass());
        
        if (!shared_bytelist) shared_buffer = true;
        RubyString shared = new RubyString(getRuntime(), getMetaClass(), value.makeShared(index, len));
        shared.shared_buffer = true;

        shared.infectBy(this);
        return shared;
    }
    
    private void tmpLock() {
        if ((flags & TMPLOCK_STR_F) != 0) throw getRuntime().newRuntimeError("temporal locking already locked string");
        flags |= TMPLOCK_STR_F;
    }
    
    private void tmpUnlock() {
        if ((flags & TMPLOCK_STR_F) == 0) throw getRuntime().newRuntimeError("temporal unlocking already unlocked string");
        flags &= ~TMPLOCK_STR_F;
    }

    private final void modifyCheck() {
        if ((flags & TMPLOCK_OR_FROZEN_STR_F) != 0) {
            if ((flags & FROZEN_F) != 0) throw getRuntime().newFrozenError("string" + getMetaClass().getName());           
            if ((flags & TMPLOCK_STR_F) != 0) throw getRuntime().newTypeError("can't modify string; temporarily locked");
        }
        if (!isTaint() && getRuntime().getSafeLevel() >= 4) {
            throw getRuntime().newSecurityError("Insecure: can't modify string");
        }
    }
    
    private final void modifyCheck(byte[] b, int len) {
        if (value.bytes != b || value.realSize != len) throw getRuntime().newRuntimeError("string modified");
    }
    
    private final void frozenCheck() {
        if (isFrozen()) throw getRuntime().newRuntimeError("string frozen");
    }

    /** rb_str_modify
     * 
     */

    public final void modify() {
        modifyCheck();

        if (shared_buffer || shared_bytelist) {
            if (shared_bytelist) {
                value = value.dup();
            } else if (shared_buffer) {
                value.unshare();
            }
            shared_buffer = false;
            shared_bytelist = false;
        }

        value.invalidate();
    }
    
    public final void modify(int length) {
        modifyCheck();

        if (shared_buffer || shared_bytelist) {
            if (shared_bytelist) {
                value = value.dup(length);
            } else if (shared_buffer) {
                value.unshare(length);
            }
            shared_buffer = false;
            shared_bytelist = false;
        } else {
            value = value.dup(length);
        }
        value.invalidate();
    }        
    
    private final void view(ByteList bytes) {
        modifyCheck();

        value = bytes;
        shared_buffer = false;
        shared_bytelist = false;
    }

    private final void view(byte[]bytes) {
        modifyCheck();        

        value.replace(bytes);
        shared_buffer = false;
        shared_bytelist = false;

        value.invalidate();        
    }

    private final void view(int index, int len) {
        modifyCheck();

        if (shared_buffer || shared_bytelist) {
            if (shared_bytelist) {
                // if len == 0 then shared empty
                value = value.makeShared(index, len);
                shared_bytelist = false;
                shared_buffer = true;
            } else if (shared_buffer) {
                value.view(index, len);
            }
        } else {        
            value.view(index, len);
            // FIXME this below is temporary, but its much safer for COW (it prevents not shared Strings with begin != 0)
            // this allows now e.g.: ByteList#set not to be begin aware
            shared_buffer = true;
        }

        value.invalidate();
    }

    public static String bytesToString(byte[] bytes, int beg, int len) {
        return new String(ByteList.plain(bytes, beg, len));
    }

    public static String byteListToString(ByteList bytes) {
        return bytesToString(bytes.unsafeBytes(), bytes.begin(), bytes.length());
    }

    public static String bytesToString(byte[] bytes) {
        return bytesToString(bytes, 0, bytes.length);
    }

    public static byte[] stringToBytes(String string) {
        return ByteList.plain(string);
    }

    public static boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isUpper(int c) {
        return c >= 'A' && c <= 'Z';
    }

    public static boolean isLower(int c) {
        return c >= 'a' && c <= 'z';
    }

    public static boolean isLetter(int c) {
        return isUpper(c) || isLower(c);
    }

    public static boolean isAlnum(int c) {
        return isUpper(c) || isLower(c) || isDigit(c);
    }

    public static boolean isPrint(int c) {
        return c >= 0x20 && c <= 0x7E;
    }

    public RubyString asString() {
        return this;
    }

    public IRubyObject checkStringType() {
        return this;
    }

    @JRubyMethod(name = {"to_s", "to_str"})
    public IRubyObject to_s() {
        if (getMetaClass().getRealClass() != getRuntime().getString()) {
            return strDup(getRuntime().getString());
        }
        return this;
    }

    /* rb_str_cmp_m */
    @JRubyMethod(name = "<=>", required = 1)
    public IRubyObject op_cmp(IRubyObject other) {
        if (other instanceof RubyString) {
            return getRuntime().newFixnum(op_cmp((RubyString)other));
        }

        return getRuntime().getNil();
    }
        
    /**
     * 
     */
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if (this == other) return getRuntime().getTrue();
        if (!(other instanceof RubyString)) {
            if (!other.respondsTo("to_str")) return getRuntime().getFalse();
            Ruby runtime = getRuntime();
            return other.callMethod(runtime.getCurrentContext(), MethodIndex.EQUALEQUAL, "==", this).isTrue() ? runtime.getTrue() : runtime.getFalse();
        }
        return value.equal(((RubyString)other).value) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "+", required = 1)
    public IRubyObject op_plus(IRubyObject other) {
        RubyString str = other.convertToString();
        
        ByteList result = new ByteList(value.realSize + str.value.realSize);
        result.realSize = value.realSize + str.value.realSize;
        System.arraycopy(value.bytes, value.begin, result.bytes, 0, value.realSize);
        System.arraycopy(str.value.bytes, str.value.begin, result.bytes, value.realSize, str.value.realSize);
      
        RubyString resultStr = newString(getRuntime(), result);
        if (isTaint() || str.isTaint()) resultStr.setTaint(true);
        return resultStr;
    }

    @JRubyMethod(name = "*", required = 1)
    public IRubyObject op_mul(IRubyObject other) {
        RubyInteger otherInteger = (RubyInteger) other.convertToInteger();
        long len = otherInteger.getLongValue();

        if (len < 0) throw getRuntime().newArgumentError("negative argument");

        // we limit to int because ByteBuffer can only allocate int sizes
        if (len > 0 && Integer.MAX_VALUE / len < value.length()) {
            throw getRuntime().newArgumentError("argument too big");
        }
        ByteList newBytes = new ByteList(value.length() * (int)len);

        for (int i = 0; i < len; i++) {
            newBytes.append(value);
        }

        RubyString newString = newString(getRuntime(), newBytes);
        newString.setTaint(isTaint());
        return newString;
    }

    @JRubyMethod(name = "%", required = 1)
    public IRubyObject op_format(IRubyObject arg) {
        // FIXME: Should we make this work with platform's locale, or continue hardcoding US?
        return getRuntime().newString((ByteList)Sprintf.sprintf(Locale.US, value, arg));
    }

    @JRubyMethod(name = "hash")
    public RubyFixnum hash() {
        return getRuntime().newFixnum(value.hashCode());
    }

    public int hashCode() {
        return value.hashCode();
    }

    public boolean equals(Object other) {
        if (this == other) return true;

        if (other instanceof RubyString) {
            RubyString string = (RubyString) other;

            if (string.value.equal(value)) return true;
        }

        return false;
    }

    /** rb_obj_as_string
     *
     */
    public static RubyString objAsString(IRubyObject obj) {
        if (obj instanceof RubyString) return (RubyString) obj;

        IRubyObject str = obj.callMethod(obj.getRuntime().getCurrentContext(), MethodIndex.TO_S, "to_s");

        if (!(str instanceof RubyString)) return (RubyString) obj.anyToString();

        if (obj.isTaint()) str.setTaint(true);

        return (RubyString) str;
    }

    /** rb_str_cmp
     *
     */
    public int op_cmp(RubyString other) {
        return value.cmp(other.value);
    }

    /** rb_to_id
     *
     */
    public String asJavaString() {
        // TODO: This used to intern; but it didn't appear to change anything
        // turning that off, and it's unclear if it was needed. Plus, we intern
        // 
        return toString();
    }

    /** Create a new String which uses the same Ruby runtime and the same
     *  class like this String.
     *
     *  This method should be used to satisfy RCR #38.
     *
     */
    public RubyString newString(CharSequence s) {
        return new RubyString(getRuntime(), getType(), s);
    }

    /** Create a new String which uses the same Ruby runtime and the same
     *  class like this String.
     *
     *  This method should be used to satisfy RCR #38.
     *
     */
    public RubyString newString(ByteList s) {
        return new RubyString(getRuntime(), getMetaClass(), s);
    }

    // Methods of the String class (rb_str_*):

    /** rb_str_new2
     *
     */
    public static RubyString newString(Ruby runtime, CharSequence str) {
        return new RubyString(runtime, runtime.getString(), str);
    }
    
    public static RubyString newEmptyString(Ruby runtime) {
        return newEmptyString(runtime, runtime.getString());
    }

    public static RubyString newEmptyString(Ruby runtime, RubyClass metaClass) {
        RubyString empty = new RubyString(runtime, metaClass, ByteList.EMPTY_BYTELIST);
        empty.shared_bytelist = true;
        return empty;
    }

    public static RubyString newUnicodeString(Ruby runtime, String str) {
        try {
            return new RubyString(runtime, runtime.getString(), new ByteList(str.getBytes("UTF8"), false));
        } catch (UnsupportedEncodingException uee) {
            return new RubyString(runtime, runtime.getString(), str);
        }
    }

    public static RubyString newString(Ruby runtime, RubyClass clazz, CharSequence str) {
        return new RubyString(runtime, clazz, str);
    }

    public static RubyString newString(Ruby runtime, byte[] bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }

    public static RubyString newString(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes);
    }

    public static RubyString newStringLight(Ruby runtime, ByteList bytes) {
        return new RubyString(runtime, runtime.getString(), bytes, false);
    }

    public static RubyString newStringShared(Ruby runtime, RubyString orig) {
        orig.shared_bytelist = true;
        RubyString str = new RubyString(runtime, runtime.getString(), orig.value);
        str.shared_bytelist = true;
        return str;
    }       
    
    public static RubyString newStringShared(Ruby runtime, ByteList bytes) {
        return newStringShared(runtime, runtime.getString(), bytes);
    }    

    public static RubyString newStringShared(Ruby runtime, RubyClass clazz, ByteList bytes) {
        RubyString str = new RubyString(runtime, clazz, bytes);
        str.shared_bytelist = true;
        return str;
    }    

    public static RubyString newString(Ruby runtime, byte[] bytes, int start, int length) {
        byte[] bytes2 = new byte[length];
        System.arraycopy(bytes, start, bytes2, 0, length);
        return new RubyString(runtime, runtime.getString(), new ByteList(bytes2, false));
    }

    public IRubyObject doClone(){
        return newString(getRuntime(), value.dup());
    }

    // FIXME: cat methods should be more aware of sharing to prevent unnecessary reallocations in certain situations 
    public RubyString cat(byte[] str) {
        modify();
        value.append(str);
        return this;
    }

    public RubyString cat(byte[] str, int beg, int len) {
        modify();        
        value.append(str, beg, len);
        return this;
    }

    public RubyString cat(ByteList str) {
        modify();        
        value.append(str);
        return this;
    }

    public RubyString cat(byte ch) {
        modify();        
        value.append(ch);
        return this;
    }

    /** rb_str_replace_m
     *
     */
    @JRubyMethod(name = {"replace", "initialize_copy"}, required = 1)
    public RubyString replace(IRubyObject other) {
        modifyCheck();

        if (this == other) return this;
         
        RubyString otherStr =  stringValue(other);

        shared_bytelist = true;
        otherStr.shared_bytelist = true;
        
        value = otherStr.value;

        infectBy(other);
        return this;
    }

    @JRubyMethod(name = "reverse")
    public RubyString reverse() {
        if (value.length() <= 1) return strDup();

        ByteList buf = new ByteList(value.length()+2);
        buf.realSize = value.length();
        int src = value.length() - 1;
        int dst = 0;
        
        while (src >= 0) buf.set(dst++, value.get(src--));

        RubyString rev = new RubyString(getRuntime(), getMetaClass(), buf);
        rev.infectBy(this);
        return rev;
    }

    @JRubyMethod(name = "reverse!")
    public RubyString reverse_bang() {
        if (value.length() > 1) {
            modify();
            for (int i = 0; i < (value.length() / 2); i++) {
                byte b = (byte) value.get(i);
                
                value.set(i, value.get(value.length() - i - 1));
                value.set(value.length() - i - 1, b);
            }
        }
        
        return this;
    }

    /** rb_str_s_new
     *
     */
    public static RubyString newInstance(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString newString = newString(recv.getRuntime(), "");
        newString.setMetaClass((RubyClass) recv);
        newString.callInit(args, block);
        return newString;
    }

    @JRubyMethod(name = "initialize", optional = 1, frame = true)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        if (Arity.checkArgumentCount(getRuntime(), args, 0, 1) == 1) replace(args[0]);

        return this;
    }

    @JRubyMethod(name = "casecmp", required = 1)
    public IRubyObject casecmp(IRubyObject other) {
        int compare = value.caseInsensitiveCmp(stringValue(other).value);
        return RubyFixnum.newFixnum(getRuntime(), compare);
    }

    /** rb_str_match
     *
     */
    @JRubyMethod(name = "=~", required = 1)
    public IRubyObject op_match(IRubyObject other) {
        if (other instanceof RubyRegexp) return ((RubyRegexp) other).op_match(this);
        if (other instanceof RubyString) {
            throw getRuntime().newTypeError("type mismatch: String given");
        }
        return other.callMethod(getRuntime().getCurrentContext(), "=~", this);
    }

    /** rb_str_match2
     *
     */
    @JRubyMethod(name = "~")
    public IRubyObject op_match2() {
        return RubyRegexp.newRegexp(getRuntime(), value, 0).op_match2();
    }

    /**
     * String#match(pattern)
     *
     * rb_str_match_m
     *
     * @param pattern Regexp or String
     */
    @JRubyMethod(name = "match", required = 1)
    public IRubyObject match(IRubyObject pattern) {
        return getPattern(pattern, false).callMethod(getRuntime().getCurrentContext(), "match", this);
    }

    /** rb_str_capitalize
     *
     */
    @JRubyMethod(name = "capitalize")
    public IRubyObject capitalize() {
        RubyString str = strDup();
        str.capitalize_bang();
        return str;
    }

    /** rb_str_capitalize_bang
     *
     */
    @JRubyMethod(name = "capitalize!")
    public IRubyObject capitalize_bang() {
        if (value.realSize == 0) return getRuntime().getNil();
        
        modify();
        
        int s = value.begin;
        int send = s + value.realSize;
        byte[]buf = value.bytes;
        
        
        
        boolean modify = false;
        
        int c = buf[s] & 0xff;
        if (ASCII.isLower(c)) {
            buf[s] = (byte)ASCIIEncoding.asciiToUpper(c);
            modify = true;
        }
        
        while (++s < send) {
            c = (char)(buf[s] & 0xff);
            if (ASCII.isUpper(c)) {
                buf[s] = (byte)ASCIIEncoding.asciiToLower(c);
                modify = true;
            }
        }
        
        if (modify) return this;
        return getRuntime().getNil();
    }

    @JRubyMethod(name = ">=", required = 1)
    public IRubyObject op_ge(IRubyObject other) {
        if (other instanceof RubyString) {
            return getRuntime().newBoolean(op_cmp((RubyString) other) >= 0);
        }

        return RubyComparable.op_ge(this, other);
    }

    @JRubyMethod(name = ">", required = 1)
    public IRubyObject op_gt(IRubyObject other) {
        if (other instanceof RubyString) {
            return getRuntime().newBoolean(op_cmp((RubyString) other) > 0);
        }

        return RubyComparable.op_gt(this, other);
    }

    @JRubyMethod(name = "<=", required = 1)
    public IRubyObject op_le(IRubyObject other) {
        if (other instanceof RubyString) {
            return getRuntime().newBoolean(op_cmp((RubyString) other) <= 0);
        }

        return RubyComparable.op_le(this, other);
    }

    @JRubyMethod(name = "<", required = 1)
    public IRubyObject op_lt(IRubyObject other) {
        if (other instanceof RubyString) {
            return getRuntime().newBoolean(op_cmp((RubyString) other) < 0);
        }

        return RubyComparable.op_lt(this, other);
    }

    @JRubyMethod(name = "eql?", required = 1)
    public IRubyObject eql_p(IRubyObject other) {
        if (!(other instanceof RubyString)) return getRuntime().getFalse();
        RubyString otherString = (RubyString)other;
        return value.equal(otherString.value) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    /** rb_str_upcase
     *
     */
    @JRubyMethod(name = "upcase")
    public RubyString upcase() {
        RubyString str = strDup();
        str.upcase_bang();
        return str;
    }

    /** rb_str_upcase_bang
     *
     */
    @JRubyMethod(name = "upcase!")
    public IRubyObject upcase_bang() {
        if (value.realSize == 0)  return getRuntime().getNil();

        modify();

        int s = value.begin;
        int send = s + value.realSize;
        byte []buf = value.bytes;

        boolean modify = false;
        while (s < send) {
            int c = buf[s] & 0xff;
            if (ASCII.isLower(c)) {
                buf[s] = (byte)ASCIIEncoding.asciiToUpper(c);
                modify = true;
            }
            s++;
        }

        if (modify) return this;
        return getRuntime().getNil();        
    }

    /** rb_str_downcase
     *
     */
    @JRubyMethod(name = "downcase")
    public RubyString downcase() {
        RubyString str = strDup();
        str.downcase_bang();
        return str;
    }

    /** rb_str_downcase_bang
     *
     */
    @JRubyMethod(name = "downcase!")
    public IRubyObject downcase_bang() {
        if (value.realSize == 0)  return getRuntime().getNil();
        
        modify();
        
        int s = value.begin;
        int send = s + value.realSize;
        byte []buf = value.bytes;
        
        boolean modify = false;
        while (s < send) {
            int c = buf[s] & 0xff;
            if (ASCII.isUpper(c)) {
                buf[s] = (byte)ASCIIEncoding.asciiToLower(c);
                modify = true;
            }
            s++;
        }
        
        if (modify) return this;
        return getRuntime().getNil();
    }

    /** rb_str_swapcase
     *
     */
    @JRubyMethod(name = "swapcase")
    public RubyString swapcase() {
        RubyString str = strDup();
        str.swapcase_bang();
        return str;
    }

    /** rb_str_swapcase_bang
     *
     */
    @JRubyMethod(name = "swapcase!")
    public IRubyObject swapcase_bang() {
        if (value.realSize == 0)  return getRuntime().getNil();        
        
        modify();
        
        int s = value.begin;
        int send = s + value.realSize;
        byte[]buf = value.bytes;
        
        boolean modify = false;
        while (s < send) {
            int c = buf[s] & 0xff;
            if (ASCII.isUpper(c)) {
                buf[s] = (byte)ASCIIEncoding.asciiToLower(c);
                modify = true;
            } else if (ASCII.isLower(c)) {
                buf[s] = (byte)ASCIIEncoding.asciiToUpper(c);
                modify = true;
            }
            s++;
        }

        if (modify) return this;
        return getRuntime().getNil();
    }

    /** rb_str_dump
     *
     */
    @JRubyMethod(name = "dump")
    public IRubyObject dump() {
        return inspect();
    }

    @JRubyMethod(name = "insert", required = 2)
    public IRubyObject insert(IRubyObject indexArg, IRubyObject stringArg) {
        int index = (int) indexArg.convertToInteger().getLongValue();
        if (index < 0) index += value.length() + 1;

        if (index < 0 || index > value.length()) {
            throw getRuntime().newIndexError("index " + index + " out of range");
        }

        modify();
        
        ByteList insert = ((RubyString)stringArg.convertToString()).value;
        value.unsafeReplace(index, 0, insert);
        return this;
    }

    /** rb_str_inspect
     *
     */
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect() {
        final int length = value.length();
        Ruby runtime = getRuntime();
        ByteList sb = new ByteList(length + 2 + length / 100);

        sb.append('\"');

        // FIXME: This may not be unicode-safe
        for (int i = 0; i < length; i++) {
            int c = value.get(i) & 0xFF;
            if (isAlnum(c)) {
                sb.append((char)c);
            } else if (runtime.getKCode() == KCode.UTF8 && c == 0xEF) {
                // don't escape encoded UTF8 characters, leave them as bytes
                // append byte order mark plus two character bytes
                sb.append((char)c);
                sb.append((char)(value.get(++i) & 0xFF));
                sb.append((char)(value.get(++i) & 0xFF));
            } else if (c == '\"' || c == '\\') {
                sb.append('\\').append((char)c);
            } else if (c == '#' && isEVStr(i, length)) {
                sb.append('\\').append((char)c);
            } else if (isPrint(c)) {
                sb.append((char)c);
            } else if (c == '\n') {
                sb.append('\\').append('n');
            } else if (c == '\r') {
                sb.append('\\').append('r');
            } else if (c == '\t') {
                sb.append('\\').append('t');
            } else if (c == '\f') {
                sb.append('\\').append('f');
            } else if (c == '\u000B') {
                sb.append('\\').append('v');
            } else if (c == '\u0007') {
                sb.append('\\').append('a');
            } else if (c == '\u001B') {
                sb.append('\\').append('e');
            } else {
                sb.append(ByteList.plain(Sprintf.sprintf(runtime,"\\%.3o",c)));
            }
        }

        sb.append('\"');
        return getRuntime().newString(sb);
    }
    
    private boolean isEVStr(int i, int length) {
        if (i+1 >= length) return false;
        int c = value.get(i+1) & 0xFF;
        
        return c == '$' || c == '@' || c == '{';
    }

    /** rb_str_length
     *
     */
    @JRubyMethod(name = {"length", "size"})
    public RubyFixnum length() {
        return getRuntime().newFixnum(value.length());
    }

    /** rb_str_empty
     *
     */
    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p() {
        return isEmpty() ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    private boolean isEmpty() {
        return value.length() == 0;
    }

    /** rb_str_append
     *
     */
    public RubyString append(IRubyObject other) {
        infectBy(other);
        return cat(stringValue(other).value);
    }

    /** rb_str_concat
     *
     */
    @JRubyMethod(name = {"concat", "<<"}, required = 1)
    public RubyString concat(IRubyObject other) {
        if (other instanceof RubyFixnum) {
            long value = ((RubyFixnum) other).getLongValue();
            if (value >= 0 && value < 256) return cat((byte) value);
        }
        return append(other);
    }

    /** rb_str_crypt
     *
     */
    @JRubyMethod(name = "crypt", required = 1)
    public RubyString crypt(IRubyObject other) {
        ByteList salt = stringValue(other).getByteList();
        if (salt.realSize < 2) {
            throw getRuntime().newArgumentError("salt too short(need >=2 bytes)");
        }

        salt = salt.makeShared(0, 2);
        return RubyString.newStringShared(getRuntime(),getMetaClass(), JavaCrypt.crypt(salt, this.getByteList()));
    }

    public static class JavaCrypt {
        private static final int ITERATIONS = 16;

        private static final int con_salt[] = {
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
            0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
            0x0A, 0x0B, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A,
            0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12,
            0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A,
            0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22,
            0x23, 0x24, 0x25, 0x20, 0x21, 0x22, 0x23, 0x24,
            0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C,
            0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34,
            0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C,
            0x3D, 0x3E, 0x3F, 0x00, 0x00, 0x00, 0x00, 0x00,
        };

        private static final boolean shifts2[] = {
            false, false, true, true, true, true, true, true,
            false, true,  true, true, true, true, true, false };

        private static final int skb[][] = {
            {
                /* for C bits (numbered as per FIPS 46) 1 2 3 4 5 6 */
                0x00000000, 0x00000010, 0x20000000, 0x20000010,
                0x00010000, 0x00010010, 0x20010000, 0x20010010,
                0x00000800, 0x00000810, 0x20000800, 0x20000810,
                0x00010800, 0x00010810, 0x20010800, 0x20010810,
                0x00000020, 0x00000030, 0x20000020, 0x20000030,
                0x00010020, 0x00010030, 0x20010020, 0x20010030,
                0x00000820, 0x00000830, 0x20000820, 0x20000830,
                0x00010820, 0x00010830, 0x20010820, 0x20010830,
                0x00080000, 0x00080010, 0x20080000, 0x20080010,
                0x00090000, 0x00090010, 0x20090000, 0x20090010,
                0x00080800, 0x00080810, 0x20080800, 0x20080810,
                0x00090800, 0x00090810, 0x20090800, 0x20090810,
                0x00080020, 0x00080030, 0x20080020, 0x20080030,
                0x00090020, 0x00090030, 0x20090020, 0x20090030,
                0x00080820, 0x00080830, 0x20080820, 0x20080830,
                0x00090820, 0x00090830, 0x20090820, 0x20090830,
            },{
                /* for C bits (numbered as per FIPS 46) 7 8 10 11 12 13 */
                0x00000000, 0x02000000, 0x00002000, 0x02002000,
                0x00200000, 0x02200000, 0x00202000, 0x02202000,
                0x00000004, 0x02000004, 0x00002004, 0x02002004,
                0x00200004, 0x02200004, 0x00202004, 0x02202004,
                0x00000400, 0x02000400, 0x00002400, 0x02002400,
                0x00200400, 0x02200400, 0x00202400, 0x02202400,
                0x00000404, 0x02000404, 0x00002404, 0x02002404,
                0x00200404, 0x02200404, 0x00202404, 0x02202404,
                0x10000000, 0x12000000, 0x10002000, 0x12002000,
                0x10200000, 0x12200000, 0x10202000, 0x12202000,
                0x10000004, 0x12000004, 0x10002004, 0x12002004,
                0x10200004, 0x12200004, 0x10202004, 0x12202004,
                0x10000400, 0x12000400, 0x10002400, 0x12002400,
                0x10200400, 0x12200400, 0x10202400, 0x12202400,
                0x10000404, 0x12000404, 0x10002404, 0x12002404,
                0x10200404, 0x12200404, 0x10202404, 0x12202404,
            },{
                /* for C bits (numbered as per FIPS 46) 14 15 16 17 19 20 */
                0x00000000, 0x00000001, 0x00040000, 0x00040001,
                0x01000000, 0x01000001, 0x01040000, 0x01040001,
                0x00000002, 0x00000003, 0x00040002, 0x00040003,
                0x01000002, 0x01000003, 0x01040002, 0x01040003,
                0x00000200, 0x00000201, 0x00040200, 0x00040201,
                0x01000200, 0x01000201, 0x01040200, 0x01040201,
                0x00000202, 0x00000203, 0x00040202, 0x00040203,
                0x01000202, 0x01000203, 0x01040202, 0x01040203,
                0x08000000, 0x08000001, 0x08040000, 0x08040001,
                0x09000000, 0x09000001, 0x09040000, 0x09040001,
                0x08000002, 0x08000003, 0x08040002, 0x08040003,
                0x09000002, 0x09000003, 0x09040002, 0x09040003,
                0x08000200, 0x08000201, 0x08040200, 0x08040201,
                0x09000200, 0x09000201, 0x09040200, 0x09040201,
                0x08000202, 0x08000203, 0x08040202, 0x08040203,
                0x09000202, 0x09000203, 0x09040202, 0x09040203,
            },{
                /* for C bits (numbered as per FIPS 46) 21 23 24 26 27 28 */
                0x00000000, 0x00100000, 0x00000100, 0x00100100,
                0x00000008, 0x00100008, 0x00000108, 0x00100108,
                0x00001000, 0x00101000, 0x00001100, 0x00101100,
                0x00001008, 0x00101008, 0x00001108, 0x00101108,
                0x04000000, 0x04100000, 0x04000100, 0x04100100,
                0x04000008, 0x04100008, 0x04000108, 0x04100108,
                0x04001000, 0x04101000, 0x04001100, 0x04101100,
                0x04001008, 0x04101008, 0x04001108, 0x04101108,
                0x00020000, 0x00120000, 0x00020100, 0x00120100,
                0x00020008, 0x00120008, 0x00020108, 0x00120108,
                0x00021000, 0x00121000, 0x00021100, 0x00121100,
                0x00021008, 0x00121008, 0x00021108, 0x00121108,
                0x04020000, 0x04120000, 0x04020100, 0x04120100,
                0x04020008, 0x04120008, 0x04020108, 0x04120108,
                0x04021000, 0x04121000, 0x04021100, 0x04121100,
                0x04021008, 0x04121008, 0x04021108, 0x04121108,
            },{
                /* for D bits (numbered as per FIPS 46) 1 2 3 4 5 6 */
                0x00000000, 0x10000000, 0x00010000, 0x10010000,
                0x00000004, 0x10000004, 0x00010004, 0x10010004,
                0x20000000, 0x30000000, 0x20010000, 0x30010000,
                0x20000004, 0x30000004, 0x20010004, 0x30010004,
                0x00100000, 0x10100000, 0x00110000, 0x10110000,
                0x00100004, 0x10100004, 0x00110004, 0x10110004,
                0x20100000, 0x30100000, 0x20110000, 0x30110000,
                0x20100004, 0x30100004, 0x20110004, 0x30110004,
                0x00001000, 0x10001000, 0x00011000, 0x10011000,
                0x00001004, 0x10001004, 0x00011004, 0x10011004,
                0x20001000, 0x30001000, 0x20011000, 0x30011000,
                0x20001004, 0x30001004, 0x20011004, 0x30011004,
                0x00101000, 0x10101000, 0x00111000, 0x10111000,
                0x00101004, 0x10101004, 0x00111004, 0x10111004,
                0x20101000, 0x30101000, 0x20111000, 0x30111000,
                0x20101004, 0x30101004, 0x20111004, 0x30111004,
            },{
                /* for D bits (numbered as per FIPS 46) 8 9 11 12 13 14 */
                0x00000000, 0x08000000, 0x00000008, 0x08000008,
                0x00000400, 0x08000400, 0x00000408, 0x08000408,
                0x00020000, 0x08020000, 0x00020008, 0x08020008,
                0x00020400, 0x08020400, 0x00020408, 0x08020408,
                0x00000001, 0x08000001, 0x00000009, 0x08000009,
                0x00000401, 0x08000401, 0x00000409, 0x08000409,
                0x00020001, 0x08020001, 0x00020009, 0x08020009,
                0x00020401, 0x08020401, 0x00020409, 0x08020409,
                0x02000000, 0x0A000000, 0x02000008, 0x0A000008,
                0x02000400, 0x0A000400, 0x02000408, 0x0A000408,
                0x02020000, 0x0A020000, 0x02020008, 0x0A020008,
                0x02020400, 0x0A020400, 0x02020408, 0x0A020408,
                0x02000001, 0x0A000001, 0x02000009, 0x0A000009,
                0x02000401, 0x0A000401, 0x02000409, 0x0A000409,
                0x02020001, 0x0A020001, 0x02020009, 0x0A020009,
                0x02020401, 0x0A020401, 0x02020409, 0x0A020409,
            },{
                /* for D bits (numbered as per FIPS 46) 16 17 18 19 20 21 */
                0x00000000, 0x00000100, 0x00080000, 0x00080100,
                0x01000000, 0x01000100, 0x01080000, 0x01080100,
                0x00000010, 0x00000110, 0x00080010, 0x00080110,
                0x01000010, 0x01000110, 0x01080010, 0x01080110,
                0x00200000, 0x00200100, 0x00280000, 0x00280100,
                0x01200000, 0x01200100, 0x01280000, 0x01280100,
                0x00200010, 0x00200110, 0x00280010, 0x00280110,
                0x01200010, 0x01200110, 0x01280010, 0x01280110,
                0x00000200, 0x00000300, 0x00080200, 0x00080300,
                0x01000200, 0x01000300, 0x01080200, 0x01080300,
                0x00000210, 0x00000310, 0x00080210, 0x00080310,
                0x01000210, 0x01000310, 0x01080210, 0x01080310,
                0x00200200, 0x00200300, 0x00280200, 0x00280300,
                0x01200200, 0x01200300, 0x01280200, 0x01280300,
                0x00200210, 0x00200310, 0x00280210, 0x00280310,
                0x01200210, 0x01200310, 0x01280210, 0x01280310,
            },{
                /* for D bits (numbered as per FIPS 46) 22 23 24 25 27 28 */
                0x00000000, 0x04000000, 0x00040000, 0x04040000,
                0x00000002, 0x04000002, 0x00040002, 0x04040002,
                0x00002000, 0x04002000, 0x00042000, 0x04042000,
                0x00002002, 0x04002002, 0x00042002, 0x04042002,
                0x00000020, 0x04000020, 0x00040020, 0x04040020,
                0x00000022, 0x04000022, 0x00040022, 0x04040022,
                0x00002020, 0x04002020, 0x00042020, 0x04042020,
                0x00002022, 0x04002022, 0x00042022, 0x04042022,
                0x00000800, 0x04000800, 0x00040800, 0x04040800,
                0x00000802, 0x04000802, 0x00040802, 0x04040802,
                0x00002800, 0x04002800, 0x00042800, 0x04042800,
                0x00002802, 0x04002802, 0x00042802, 0x04042802,
                0x00000820, 0x04000820, 0x00040820, 0x04040820,
                0x00000822, 0x04000822, 0x00040822, 0x04040822,
                0x00002820, 0x04002820, 0x00042820, 0x04042820,
                0x00002822, 0x04002822, 0x00042822, 0x04042822,
            }
        };

        private static final int SPtrans[][] = {
            {
                /* nibble 0 */
                0x00820200, 0x00020000, 0x80800000, 0x80820200,
                0x00800000, 0x80020200, 0x80020000, 0x80800000,
                0x80020200, 0x00820200, 0x00820000, 0x80000200,
                0x80800200, 0x00800000, 0x00000000, 0x80020000,
                0x00020000, 0x80000000, 0x00800200, 0x00020200,
                0x80820200, 0x00820000, 0x80000200, 0x00800200,
                0x80000000, 0x00000200, 0x00020200, 0x80820000,
                0x00000200, 0x80800200, 0x80820000, 0x00000000,
                0x00000000, 0x80820200, 0x00800200, 0x80020000,
                0x00820200, 0x00020000, 0x80000200, 0x00800200,
                0x80820000, 0x00000200, 0x00020200, 0x80800000,
                0x80020200, 0x80000000, 0x80800000, 0x00820000,
                0x80820200, 0x00020200, 0x00820000, 0x80800200,
                0x00800000, 0x80000200, 0x80020000, 0x00000000,
                0x00020000, 0x00800000, 0x80800200, 0x00820200,
                0x80000000, 0x80820000, 0x00000200, 0x80020200,
            },{
                /* nibble 1 */
                0x10042004, 0x00000000, 0x00042000, 0x10040000,
                0x10000004, 0x00002004, 0x10002000, 0x00042000,
                0x00002000, 0x10040004, 0x00000004, 0x10002000,
                0x00040004, 0x10042000, 0x10040000, 0x00000004,
                0x00040000, 0x10002004, 0x10040004, 0x00002000,
                0x00042004, 0x10000000, 0x00000000, 0x00040004,
                0x10002004, 0x00042004, 0x10042000, 0x10000004,
                0x10000000, 0x00040000, 0x00002004, 0x10042004,
                0x00040004, 0x10042000, 0x10002000, 0x00042004,
                0x10042004, 0x00040004, 0x10000004, 0x00000000,
                0x10000000, 0x00002004, 0x00040000, 0x10040004,
                0x00002000, 0x10000000, 0x00042004, 0x10002004,
                0x10042000, 0x00002000, 0x00000000, 0x10000004,
                0x00000004, 0x10042004, 0x00042000, 0x10040000,
                0x10040004, 0x00040000, 0x00002004, 0x10002000,
                0x10002004, 0x00000004, 0x10040000, 0x00042000,
            },{
                /* nibble 2 */
                0x41000000, 0x01010040, 0x00000040, 0x41000040,
                0x40010000, 0x01000000, 0x41000040, 0x00010040,
                0x01000040, 0x00010000, 0x01010000, 0x40000000,
                0x41010040, 0x40000040, 0x40000000, 0x41010000,
                0x00000000, 0x40010000, 0x01010040, 0x00000040,
                0x40000040, 0x41010040, 0x00010000, 0x41000000,
                0x41010000, 0x01000040, 0x40010040, 0x01010000,
                0x00010040, 0x00000000, 0x01000000, 0x40010040,
                0x01010040, 0x00000040, 0x40000000, 0x00010000,
                0x40000040, 0x40010000, 0x01010000, 0x41000040,
                0x00000000, 0x01010040, 0x00010040, 0x41010000,
                0x40010000, 0x01000000, 0x41010040, 0x40000000,
                0x40010040, 0x41000000, 0x01000000, 0x41010040,
                0x00010000, 0x01000040, 0x41000040, 0x00010040,
                0x01000040, 0x00000000, 0x41010000, 0x40000040,
                0x41000000, 0x40010040, 0x00000040, 0x01010000,
            },{
                /* nibble 3 */
                0x00100402, 0x04000400, 0x00000002, 0x04100402,
                0x00000000, 0x04100000, 0x04000402, 0x00100002,
                0x04100400, 0x04000002, 0x04000000, 0x00000402,
                0x04000002, 0x00100402, 0x00100000, 0x04000000,
                0x04100002, 0x00100400, 0x00000400, 0x00000002,
                0x00100400, 0x04000402, 0x04100000, 0x00000400,
                0x00000402, 0x00000000, 0x00100002, 0x04100400,
                0x04000400, 0x04100002, 0x04100402, 0x00100000,
                0x04100002, 0x00000402, 0x00100000, 0x04000002,
                0x00100400, 0x04000400, 0x00000002, 0x04100000,
                0x04000402, 0x00000000, 0x00000400, 0x00100002,
                0x00000000, 0x04100002, 0x04100400, 0x00000400,
                0x04000000, 0x04100402, 0x00100402, 0x00100000,
                0x04100402, 0x00000002, 0x04000400, 0x00100402,
                0x00100002, 0x00100400, 0x04100000, 0x04000402,
                0x00000402, 0x04000000, 0x04000002, 0x04100400,
            },{
                /* nibble 4 */
                0x02000000, 0x00004000, 0x00000100, 0x02004108,
                0x02004008, 0x02000100, 0x00004108, 0x02004000,
                0x00004000, 0x00000008, 0x02000008, 0x00004100,
                0x02000108, 0x02004008, 0x02004100, 0x00000000,
                0x00004100, 0x02000000, 0x00004008, 0x00000108,
                0x02000100, 0x00004108, 0x00000000, 0x02000008,
                0x00000008, 0x02000108, 0x02004108, 0x00004008,
                0x02004000, 0x00000100, 0x00000108, 0x02004100,
                0x02004100, 0x02000108, 0x00004008, 0x02004000,
                0x00004000, 0x00000008, 0x02000008, 0x02000100,
                0x02000000, 0x00004100, 0x02004108, 0x00000000,
                0x00004108, 0x02000000, 0x00000100, 0x00004008,
                0x02000108, 0x00000100, 0x00000000, 0x02004108,
                0x02004008, 0x02004100, 0x00000108, 0x00004000,
                0x00004100, 0x02004008, 0x02000100, 0x00000108,
                0x00000008, 0x00004108, 0x02004000, 0x02000008,
            },{
                /* nibble 5 */
                0x20000010, 0x00080010, 0x00000000, 0x20080800,
                0x00080010, 0x00000800, 0x20000810, 0x00080000,
                0x00000810, 0x20080810, 0x00080800, 0x20000000,
                0x20000800, 0x20000010, 0x20080000, 0x00080810,
                0x00080000, 0x20000810, 0x20080010, 0x00000000,
                0x00000800, 0x00000010, 0x20080800, 0x20080010,
                0x20080810, 0x20080000, 0x20000000, 0x00000810,
                0x00000010, 0x00080800, 0x00080810, 0x20000800,
                0x00000810, 0x20000000, 0x20000800, 0x00080810,
                0x20080800, 0x00080010, 0x00000000, 0x20000800,
                0x20000000, 0x00000800, 0x20080010, 0x00080000,
                0x00080010, 0x20080810, 0x00080800, 0x00000010,
                0x20080810, 0x00080800, 0x00080000, 0x20000810,
                0x20000010, 0x20080000, 0x00080810, 0x00000000,
                0x00000800, 0x20000010, 0x20000810, 0x20080800,
                0x20080000, 0x00000810, 0x00000010, 0x20080010,
            },{
                /* nibble 6 */
                0x00001000, 0x00000080, 0x00400080, 0x00400001,
                0x00401081, 0x00001001, 0x00001080, 0x00000000,
                0x00400000, 0x00400081, 0x00000081, 0x00401000,
                0x00000001, 0x00401080, 0x00401000, 0x00000081,
                0x00400081, 0x00001000, 0x00001001, 0x00401081,
                0x00000000, 0x00400080, 0x00400001, 0x00001080,
                0x00401001, 0x00001081, 0x00401080, 0x00000001,
                0x00001081, 0x00401001, 0x00000080, 0x00400000,
                0x00001081, 0x00401000, 0x00401001, 0x00000081,
                0x00001000, 0x00000080, 0x00400000, 0x00401001,
                0x00400081, 0x00001081, 0x00001080, 0x00000000,
                0x00000080, 0x00400001, 0x00000001, 0x00400080,
                0x00000000, 0x00400081, 0x00400080, 0x00001080,
                0x00000081, 0x00001000, 0x00401081, 0x00400000,
                0x00401080, 0x00000001, 0x00001001, 0x00401081,
                0x00400001, 0x00401080, 0x00401000, 0x00001001,
            },{
                /* nibble 7 */
                0x08200020, 0x08208000, 0x00008020, 0x00000000,
                0x08008000, 0x00200020, 0x08200000, 0x08208020,
                0x00000020, 0x08000000, 0x00208000, 0x00008020,
                0x00208020, 0x08008020, 0x08000020, 0x08200000,
                0x00008000, 0x00208020, 0x00200020, 0x08008000,
                0x08208020, 0x08000020, 0x00000000, 0x00208000,
                0x08000000, 0x00200000, 0x08008020, 0x08200020,
                0x00200000, 0x00008000, 0x08208000, 0x00000020,
                0x00200000, 0x00008000, 0x08000020, 0x08208020,
                0x00008020, 0x08000000, 0x00000000, 0x00208000,
                0x08200020, 0x08008020, 0x08008000, 0x00200020,
                0x08208000, 0x00000020, 0x00200020, 0x08008000,
                0x08208020, 0x00200000, 0x08200000, 0x08000020,
                0x00208000, 0x00008020, 0x08008020, 0x08200000,
                0x00000020, 0x08208000, 0x00208020, 0x00000000,
                0x08000000, 0x08200020, 0x00008000, 0x00208020
            }
        };

        private static final int cov_2char[] = {
            0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35,
            0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44,
            0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C,
            0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54,
            0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x61, 0x62,
            0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69, 0x6A,
            0x6B, 0x6C, 0x6D, 0x6E, 0x6F, 0x70, 0x71, 0x72,
            0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A
        };

        private static final int byteToUnsigned(byte b) {
            return b & 0xFF;
        }

        private static int fourBytesToInt(byte b[], int offset) {
            int value;
            value  =  byteToUnsigned(b[offset++]);
            value |= (byteToUnsigned(b[offset++]) <<  8);
            value |= (byteToUnsigned(b[offset++]) << 16);
            value |= (byteToUnsigned(b[offset++]) << 24);
            return(value);
        }

        private static final void intToFourBytes(int iValue, byte b[], int offset) {
            b[offset++] = (byte)((iValue)        & 0xff);
            b[offset++] = (byte)((iValue >>> 8 ) & 0xff);
            b[offset++] = (byte)((iValue >>> 16) & 0xff);
            b[offset++] = (byte)((iValue >>> 24) & 0xff);
        }

        private static final void PERM_OP(int a, int b, int n, int m, int results[]) {
            int t;

            t = ((a >>> n) ^ b) & m;
            a ^= t << n;
            b ^= t;

            results[0] = a;
            results[1] = b;
        }

        private static final int HPERM_OP(int a, int n, int m) {
            int t;

            t = ((a << (16 - n)) ^ a) & m;
            a = a ^ t ^ (t >>> (16 - n));

            return a;
        }

        private static int [] des_set_key(byte key[]) {
            int schedule[] = new int[ITERATIONS * 2];

            int c = fourBytesToInt(key, 0);
            int d = fourBytesToInt(key, 4);

            int results[] = new int[2];

            PERM_OP(d, c, 4, 0x0f0f0f0f, results);
            d = results[0]; c = results[1];

            c = HPERM_OP(c, -2, 0xcccc0000);
            d = HPERM_OP(d, -2, 0xcccc0000);

            PERM_OP(d, c, 1, 0x55555555, results);
            d = results[0]; c = results[1];

            PERM_OP(c, d, 8, 0x00ff00ff, results);
            c = results[0]; d = results[1];

            PERM_OP(d, c, 1, 0x55555555, results);
            d = results[0]; c = results[1];

            d = (((d & 0x000000ff) <<  16) |  (d & 0x0000ff00)     |
                 ((d & 0x00ff0000) >>> 16) | ((c & 0xf0000000) >>> 4));
            c &= 0x0fffffff;

            int s, t;
            int j = 0;

            for(int i = 0; i < ITERATIONS; i ++) {
                if(shifts2[i]) {
                    c = (c >>> 2) | (c << 26);
                    d = (d >>> 2) | (d << 26);
                } else {
                    c = (c >>> 1) | (c << 27);
                    d = (d >>> 1) | (d << 27);
                }

                c &= 0x0fffffff;
                d &= 0x0fffffff;

                s = skb[0][ (c       ) & 0x3f                       ]|
                    skb[1][((c >>>  6) & 0x03) | ((c >>>  7) & 0x3c)]|
                    skb[2][((c >>> 13) & 0x0f) | ((c >>> 14) & 0x30)]|
                    skb[3][((c >>> 20) & 0x01) | ((c >>> 21) & 0x06) |
                           ((c >>> 22) & 0x38)];

                t = skb[4][ (d     )  & 0x3f                       ]|
                    skb[5][((d >>> 7) & 0x03) | ((d >>>  8) & 0x3c)]|
                    skb[6][ (d >>>15) & 0x3f                       ]|
                    skb[7][((d >>>21) & 0x0f) | ((d >>> 22) & 0x30)];

                schedule[j++] = ((t <<  16) | (s & 0x0000ffff)) & 0xffffffff;
                s             = ((s >>> 16) | (t & 0xffff0000));

                s             = (s << 4) | (s >>> 28);
                schedule[j++] = s & 0xffffffff;
            }
            return(schedule);
        }

        private static final int D_ENCRYPT(int L, int R, int S, int E0, int E1, int s[]) {
            int t, u, v;

            v = R ^ (R >>> 16);
            u = v & E0;
            v = v & E1;
            u = (u ^ (u << 16)) ^ R ^ s[S];
            t = (v ^ (v << 16)) ^ R ^ s[S + 1];
            t = (t >>> 4) | (t << 28);

            L ^= SPtrans[1][(t       ) & 0x3f] |
                SPtrans[3][(t >>>  8) & 0x3f] |
                SPtrans[5][(t >>> 16) & 0x3f] |
                SPtrans[7][(t >>> 24) & 0x3f] |
                SPtrans[0][(u       ) & 0x3f] |
                SPtrans[2][(u >>>  8) & 0x3f] |
                SPtrans[4][(u >>> 16) & 0x3f] |
                SPtrans[6][(u >>> 24) & 0x3f];

            return(L);
        }

        private static final int [] body(int schedule[], int Eswap0, int Eswap1) {
            int left = 0;
            int right = 0;
            int t     = 0;

            for(int j = 0; j < 25; j ++) {
                for(int i = 0; i < ITERATIONS * 2; i += 4) {
                    left  = D_ENCRYPT(left,  right, i,     Eswap0, Eswap1, schedule);
                    right = D_ENCRYPT(right, left,  i + 2, Eswap0, Eswap1, schedule);
                }
                t     = left;
                left  = right;
                right = t;
            }

            t = right;

            right = (left >>> 1) | (left << 31);
            left  = (t    >>> 1) | (t    << 31);

            left  &= 0xffffffff;
            right &= 0xffffffff;

            int results[] = new int[2];

            PERM_OP(right, left, 1, 0x55555555, results);
            right = results[0]; left = results[1];

            PERM_OP(left, right, 8, 0x00ff00ff, results);
            left = results[0]; right = results[1];

            PERM_OP(right, left, 2, 0x33333333, results);
            right = results[0]; left = results[1];

            PERM_OP(left, right, 16, 0x0000ffff, results);
            left = results[0]; right = results[1];

            PERM_OP(right, left, 4, 0x0f0f0f0f, results);
            right = results[0]; left = results[1];

            int out[] = new int[2];

            out[0] = left; out[1] = right;

            return(out);
        }

        public static final ByteList crypt(ByteList salt, ByteList original) {
            ByteList buffer = new ByteList(new byte[]{' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' '},false);

            byte charZero = salt.bytes[salt.begin];
            byte charOne  = salt.bytes[salt.begin+1];

            buffer.set(0,charZero);
            buffer.set(1,charOne);

            int Eswap0 = con_salt[(int)(charZero&0xFF)];
            int Eswap1 = con_salt[(int)(charOne&0xFF)] << 4;

            byte key[] = new byte[8];

            for(int i = 0; i < key.length; i ++) {
                key[i] = (byte)0;
            }

            for(int i = 0; i < key.length && i < original.length(); i ++) {
                int iChar = (int)(original.bytes[original.begin+i]&0xFF);

                key[i] = (byte)(iChar << 1);
            }

            int schedule[] = des_set_key(key);
            int out[]      = body(schedule, Eswap0, Eswap1);

            byte b[] = new byte[9];

            intToFourBytes(out[0], b, 0);
            intToFourBytes(out[1], b, 4);
            b[8] = 0;

            for(int i = 2, y = 0, u = 0x80; i < 13; i ++) {
                for(int j = 0, c = 0; j < 6; j ++) {
                    c <<= 1;

                    if(((int)b[y] & u) != 0)
                        c |= 1;

                    u >>>= 1;

                    if(u == 0) {
                        y++;
                        u = 0x80;
                    }
                    buffer.set(i, cov_2char[c]);
                }
            }
            return buffer;
        }
    }

    /* RubyString aka rb_string_value */
    public static RubyString stringValue(IRubyObject object) {
        return (RubyString) (object instanceof RubyString ? object :
            object.convertToString());
    }

    /** rb_str_sub
     *
     */
    @JRubyMethod(name = "sub", required = 1, optional = 1, frame = true)
    public IRubyObject sub(IRubyObject[] args, Block block) {
        RubyString str = strDup();
        str.sub_bang(args, block);
        return str;
    }

    /** rb_str_sub_bang
     *
     */
    @JRubyMethod(name = "sub!", required = 1, optional = 1, frame = true)
    public IRubyObject sub_bang(IRubyObject[] args, Block block) {
        
        RubyString repl = null;
        final boolean iter;
        boolean tainted = false;
        
        if(args.length == 1 && block.isGiven()) {
            iter = true;
            tainted = false;
        } else if(args.length == 2) {
            repl = args[1].convertToString();
            iter = false;
            if (repl.isTaint()) tainted = true;
        } else {
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + " for 2)");
        }

        RubyRegexp rubyRegex = getPattern(args[0], true);
        Regex regex = rubyRegex.getPattern();

        int range = value.begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, value.begin, range);
        
        ThreadContext context = getRuntime().getCurrentContext();
        Frame frame = context.getPreviousFrame();
        if (matcher.search(value.begin, range, Option.NONE) >= 0) {
            if (iter) {                
                byte[]bytes = value.bytes;
                int size = value.realSize;
                rubyRegex.updateBackRef(this, frame, matcher).use();
                if (regex.numberOfCaptures() == 0) {
                    repl = objAsString(block.yield(context, substr(matcher.getBegin(), matcher.getEnd() - matcher.getBegin())));
                } else {
                    Region region = matcher.getRegion();
                    repl = objAsString(block.yield(context, substr(region.beg[0], region.end[0] - region.beg[0])));
                }
                modifyCheck(bytes, size);
                frozenCheck();
                rubyRegex.updateBackRef(this, frame, matcher);
            } else {
                repl = rubyRegex.regsub(repl, this, matcher);
                rubyRegex.updateBackRef(this, frame, matcher);
            }

            final int beg, plen;
            if (regex.numberOfCaptures() == 0) {
                beg = matcher.getBegin();
                plen = matcher.getEnd() - beg;
            } else {
                Region region = matcher.getRegion();
                beg = region.beg[0];
                plen = region.end[0] - beg;
            }

            ByteList replValue = repl.value;
            if (replValue.realSize > plen) {
                modify(value.realSize + replValue.realSize - plen);
            } else {
                modify();
            }
            if (repl.isTaint()) tainted = true;            

            if (replValue.realSize != plen && (value.realSize - beg - plen) > 0) {
                int src = value.begin + beg + plen;
                int dst = value.begin + beg + replValue.realSize;
                int length = value.realSize - beg - plen;
                System.arraycopy(value.bytes, src, value.bytes, dst, length);
            }
            System.arraycopy(replValue.bytes, replValue.begin, value.bytes, value.begin + beg, replValue.realSize);
            value.realSize += replValue.realSize - plen;
            if (tainted) setTaint(true);
            return this;            
        } else {
            frame.setBackRef(getRuntime().getNil());
            return getRuntime().getNil();
        }
    }
    
    /** rb_str_gsub
     *
     */
    @JRubyMethod(name = "gsub", required = 1, optional = 1, frame = true)
    public IRubyObject gsub(IRubyObject[] args, Block block) {
        return gsub(args, block, false);
    }

    /** rb_str_gsub_bang
     *
     */
    @JRubyMethod(name = "gsub!", required = 1, optional = 1, frame = true)
    public IRubyObject gsub_bang(IRubyObject[] args, Block block) {
        return gsub(args, block, true);
    }

    private final IRubyObject gsub(IRubyObject[] args, Block block, final boolean bang) {
        IRubyObject repl;
        final boolean iter;
        boolean tainted = false;
        
        if (args.length == 1 && block.isGiven()) {
            iter = true;
            repl = null;
        } else if (args.length == 2) {
            iter = false;
            repl = args[1].convertToString();
            if (repl.isTaint()) tainted = true; 
        } else {
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + "for 2)");
        }
        
        RubyRegexp rubyRegex = getPattern(args[0], true);
        Regex regex = rubyRegex.getPattern();

        int begin = value.begin;
        int range = begin + value.realSize;
        Matcher matcher = regex.matcher(value.bytes, begin, range);
        
        int beg = matcher.search(begin, range, Option.NONE);

        if (beg < 0) return bang ? getRuntime().getNil() : strDup(); /* bang: true, no match, no substitution */
        
        int blen = value.realSize + 30; /* len + margin */
        ByteList dest = new ByteList(blen);
        dest.realSize = blen;
        int buf = 0, bp = 0;
        int cp = value.begin;
        
        ThreadContext context = getRuntime().getCurrentContext();
        Frame frame = context.getPreviousFrame();
        
        tmpLock();
        
        int n = 0;
        int offset = 0;
        RubyString val;

        while (beg >= 0) {
            n++;
            final int begz, endz;
            if (iter) {
                byte[]bytes = value.bytes;
                int size = value.realSize;
                rubyRegex.updateBackRef(this, frame, matcher).use();
                if (regex.numberOfCaptures() == 0) {
                    begz = matcher.getBegin();
                    endz = matcher.getEnd();
                    val = objAsString(block.yield(context, substr(begz, endz - begz)));
                } else {
                    Region region = matcher.getRegion();
                    begz = region.beg[0];
                    endz = region.end[0];
                    val = objAsString(block.yield(context, substr(begz, endz - begz)));
 
                }
                modifyCheck(bytes, size);
                if (bang) frozenCheck();
            } else {
                val = rubyRegex.regsub((RubyString)repl, this, matcher);
                if (regex.numberOfCaptures() == 0) {
                    begz = matcher.getBegin();
                    endz = matcher.getEnd();
                } else {
                    Region region = matcher.getRegion();
                    begz = region.beg[0];
                    endz = region.end[0];
                }
            }
            
            if (val.isTaint()) tainted = true;
            ByteList vbuf = val.value;
            int len = (bp - buf) + (beg - offset) + vbuf.realSize + 3;
            if (blen < len) {
                while (blen < len) blen <<= 1;
                len = bp - buf;
                dest.realloc(blen);
                dest.realSize = blen;
                bp = buf + len;
            }
            len = beg - offset; /* copy pre-match substr */
            System.arraycopy(value.bytes, cp, dest.bytes, bp, len);
            bp += len;
            System.arraycopy(vbuf.bytes, vbuf.begin, dest.bytes, bp, vbuf.realSize);
            bp += vbuf.realSize;
            offset = endz;
            
            if (begz == endz) {
                if (value.realSize <= endz) break;
                len = regex.getEncoding().length(value.bytes[begin + endz]);
                System.arraycopy(value.bytes, begin + endz, dest.bytes, bp, len);
                bp += len;
                offset = endz + len;
            }
            cp = begin + offset;
            if (offset > value.realSize) break;
            beg = matcher.search(cp, range, Option.NONE); 
        }
        
        if (value.realSize > offset) {
            int len = bp - buf;
            if (blen - len < value.realSize - offset) {
                blen = len + value.realSize - offset;
                dest.realloc(blen);
                bp = buf + len;
            }
            System.arraycopy(value.bytes, cp, dest.bytes, bp, value.realSize - offset);
            bp += value.realSize - offset;
        }        
        
        rubyRegex.updateBackRef(this, frame, matcher);
        tmpUnlock(); // MRI doesn't rb_ensure this

        dest.realSize = bp - buf;
        if (bang) {
            view(dest);
            if (tainted) setTaint(true);
            return this;
        } else {
            RubyString destStr = new RubyString(getRuntime(), getMetaClass(), dest);
            destStr.infectBy(this);
            if (tainted) destStr.setTaint(true);
            return destStr;
        }
    }

    /** rb_str_index_m
     *
     */
    @JRubyMethod(name = "index", required = 1, optional = 1)
    public IRubyObject index(IRubyObject[] args) {
        int pos = Arity.checkArgumentCount(getRuntime(), args, 1, 2) == 2 ? RubyNumeric.num2int(args[1]) : 0;  
        IRubyObject sub = args[0];
        
        if (pos < 0) {
            pos += value.realSize;
            if (pos < 0) {
                if (sub instanceof RubyRegexp) { 
                    getRuntime().getCurrentContext().getPreviousFrame().setBackRef(getRuntime().getNil());
                }
                return getRuntime().getNil();
            }
        }

        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp)sub;
            
            pos = regSub.adjustStartPos(this, pos, false);
            pos = regSub.search(this, pos, false);
        } else if (sub instanceof RubyFixnum) {
            byte c = (byte)RubyNumeric.fix2int(sub);
            byte[]bytes = value.bytes;
            int end = value.begin + value.realSize;

            pos += value.begin; 
            for (;pos<end; pos++) { 
                if (bytes[pos] == c) return RubyFixnum.newFixnum(getRuntime(), pos - value.begin);
            }
            return getRuntime().getNil();
        } else if (sub instanceof RubyString) {
            pos = strIndex((RubyString)sub, pos);
        } else {
            IRubyObject tmp = sub.checkStringType();
            
            if (tmp.isNil()) throw getRuntime().newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
            pos = strIndex((RubyString)tmp, pos);
        }
        
        return pos == -1  ? getRuntime().getNil() : RubyFixnum.newFixnum(getRuntime(), pos);        
    }
    
    private int strIndex(RubyString sub, int offset) {
        if (offset < 0) {
            offset += value.realSize;
            if (offset < 0) return -1;
        }
        
        if (value.realSize - offset < sub.value.realSize) return -1;
        if (sub.value.realSize == 0) return offset;
        return value.indexOf(sub.value, offset);
    }

    /** rb_str_rindex_m
     *
     */
    @JRubyMethod(name = "rindex", required = 1, optional = 1)
    public IRubyObject rindex(IRubyObject[] args) {
        int pos;
        final IRubyObject sub;
        
        if (Arity.checkArgumentCount(getRuntime(), args, 1, 2) == 2) {  
            sub = args[0];
            pos = RubyNumeric.num2int(args[1]);

            if (pos < 0) {
                pos += value.realSize;
                if (pos < 0) {
                    if (sub instanceof RubyRegexp) { 
                        getRuntime().getCurrentContext().getPreviousFrame().setBackRef(getRuntime().getNil());
                    }
                    return getRuntime().getNil();
                }
            }            
            if (pos > value.realSize) pos = value.realSize;
        } else {
            sub = args[0];
            pos = value.realSize;
        }
        
        if (sub instanceof RubyRegexp) {
            RubyRegexp regSub = (RubyRegexp)sub;
            if(regSub.length() > 0) {
                pos = regSub.adjustStartPos(this, pos, true);
                if (pos == value.realSize) pos--;
                pos = regSub.search(this, pos, true);
            }
            if (pos >= 0) return RubyFixnum.newFixnum(getRuntime(), pos);
        } else if (sub instanceof RubyString) {
            pos = strRindex((RubyString)sub, pos);
            if (pos >= 0) return RubyFixnum.newFixnum(getRuntime(), pos);
        } else if (sub instanceof RubyFixnum) {
            byte c = (byte)RubyNumeric.fix2int(sub);

            byte[]bytes = value.bytes;
            int pbeg = value.begin;
            int p = pbeg + pos;
            
            if (pos == value.realSize) {
                if (pos == 0) return getRuntime().getNil();
                --p;
            }
            while (pbeg <= p) {
                if (bytes[p] == c) return RubyFixnum.newFixnum(getRuntime(), p - value.begin);
                p--;
            }
            return getRuntime().getNil();
        } else {
            throw getRuntime().newTypeError("type mismatch: " + sub.getMetaClass().getName() + " given");
        }
        
        return getRuntime().getNil();
    }

    private int strRindex(RubyString sub, int pos) {
        int subLength = sub.value.realSize;
        
        /* substring longer than string */
        if (value.realSize < subLength) return -1;
        if (value.realSize - pos < subLength) pos = value.realSize - subLength;

        return value.lastIndexOf(sub.value, pos);
    }
    
    /* rb_str_substr */
    public IRubyObject substr(int beg, int len) {
        int length = value.length();
        if (len < 0 || beg > length) return getRuntime().getNil();

        if (beg < 0) {
            beg += length;
            if (beg < 0) return getRuntime().getNil();
        }
        
        int end = Math.min(length, beg + len);
        return makeShared(beg, end - beg);
    }

    /* rb_str_replace */
    public IRubyObject replace(int beg, int len, RubyString replaceWith) {
        if (beg + len >= value.length()) len = value.length() - beg;

        modify();
        value.unsafeReplace(beg,len,replaceWith.value);

        return infectBy(replaceWith);
    }

    /** rb_str_aref, rb_str_aref_m
     *
     */
    @JRubyMethod(name = {"[]", "slice"}, required = 1, optional = 1)
    public IRubyObject op_aref(IRubyObject[] args) {
        if (Arity.checkArgumentCount(getRuntime(), args, 1, 2) == 2) {
            if (args[0] instanceof RubyRegexp) {
                if(((RubyRegexp)args[0]).search(this,0,false) >= 0) {
                    return RubyRegexp.nth_match(RubyNumeric.fix2int(args[1]), getRuntime().getCurrentContext().getCurrentFrame().getBackRef());
                }
                return getRuntime().getNil();
            }
            return substr(RubyNumeric.fix2int(args[0]), RubyNumeric.fix2int(args[1]));
        }

        if (args[0] instanceof RubyRegexp) {
            if(((RubyRegexp)args[0]).search(this,0,false) >= 0) {
                return RubyRegexp.nth_match(0, getRuntime().getCurrentContext().getCurrentFrame().getBackRef());
            }
            return getRuntime().getNil();
        } else if (args[0] instanceof RubyString) {
            return value.indexOf(stringValue(args[0]).value) != -1 ?
                args[0] : getRuntime().getNil();
        } else if (args[0] instanceof RubyRange) {
            long[] begLen = ((RubyRange) args[0]).begLen(value.length(), 0);
            return begLen == null ? getRuntime().getNil() :
                substr((int) begLen[0], (int) begLen[1]);
        }
        int idx = (int) args[0].convertToInteger().getLongValue();
        
        if (idx < 0) idx += value.length();
        if (idx < 0 || idx >= value.length()) return getRuntime().getNil();

        return getRuntime().newFixnum(value.get(idx) & 0xFF);
    }

    /**
     * rb_str_subpat_set
     *
     */
    private void subpatSet(RubyRegexp regexp, int nth, IRubyObject repl) {
        RubyMatchData match;
        int start, end, len;        
        if (regexp.search(this, 0, false) < 0) throw getRuntime().newIndexError("regexp not matched");

        match = (RubyMatchData)getRuntime().getCurrentContext().getCurrentFrame().getBackRef();

        if (match.regs == null) {
            if (nth >= 1) throw getRuntime().newIndexError("index " + nth + " out of regexp");
            if (nth < 0) {
                if(-nth >= 1) throw getRuntime().newIndexError("index " + nth + " out of regexp");
                nth += 1;
            }
            start = match.begin;
            if(start == -1) throw getRuntime().newIndexError("regexp group " + nth + " not matched");
            end = match.end;
        } else {
            if(nth >= match.regs.numRegs) throw getRuntime().newIndexError("index " + nth + " out of regexp");
            if(nth < 0) {
                if(-nth >= match.regs.numRegs) throw getRuntime().newIndexError("index " + nth + " out of regexp");
                nth += match.regs.numRegs;
            }
            start = match.regs.beg[nth];
            if(start == -1) throw getRuntime().newIndexError("regexp group " + nth + " not matched");
            end = match.regs.end[nth];
        }
        
        len = end - start;
        replace(start, len, stringValue(repl));
    }

    /** rb_str_aset, rb_str_aset_m
     *
     */
    @JRubyMethod(name = "[]=", required = 2, optional = 1)
    public IRubyObject op_aset(IRubyObject[] args) {
        int strLen = value.length();
        if (Arity.checkArgumentCount(getRuntime(), args, 2, 3) == 3) {
            if (args[0] instanceof RubyFixnum) {
                RubyString repl = stringValue(args[2]);
                int beg = RubyNumeric.fix2int(args[0]);
                int len = RubyNumeric.fix2int(args[1]);
                if (len < 0) throw getRuntime().newIndexError("negative length");
                if (beg < 0) beg += strLen;

                if (beg < 0 || (beg > 0 && beg > strLen)) {
                    throw getRuntime().newIndexError("string index out of bounds");
                }
                if (beg + len > strLen) len = strLen - beg;

                replace(beg, len, repl);
                return repl;
            }
            if (args[0] instanceof RubyRegexp) {
                RubyString repl = stringValue(args[2]);
                int nth = RubyNumeric.fix2int(args[1]);
                subpatSet((RubyRegexp) args[0], nth, repl);
                return repl;
            }
        }
        if (args[0] instanceof RubyFixnum || args[0].respondsTo("to_int")) { // FIXME: RubyNumeric or RubyInteger instead?
            int idx = 0;

            // FIXME: second instanceof check adds overhead?
            if (!(args[0] instanceof RubyFixnum)) {
                // FIXME: ok to cast?
                idx = (int)args[0].convertToInteger().getLongValue();
            } else {
                idx = RubyNumeric.fix2int(args[0]); // num2int?
            }
            
            if (idx < 0) idx += value.length();

            if (idx < 0 || idx >= value.length()) {
                throw getRuntime().newIndexError("string index out of bounds");
            }
            if (args[1] instanceof RubyFixnum) {
                modify();
                value.set(idx, (byte) RubyNumeric.fix2int(args[1]));
            } else {
                replace(idx, 1, stringValue(args[1]));
            }
            return args[1];
        }
        if (args[0] instanceof RubyRegexp) {
            RubyString repl = stringValue(args[1]);
            subpatSet((RubyRegexp) args[0], 0, repl);
            return repl;
        }
        if (args[0] instanceof RubyString) {
            RubyString orig = stringValue(args[0]);
            int beg = value.indexOf(orig.value);
            if (beg != -1) {
                replace(beg, orig.value.length(), stringValue(args[1]));
            }
            return args[1];
        }
        if (args[0] instanceof RubyRange) {
            long[] idxs = ((RubyRange) args[0]).getBeginLength(value.length(), true, true);
            replace((int) idxs[0], (int) idxs[1], stringValue(args[1]));
            return args[1];
        }
        throw getRuntime().newTypeError("wrong argument type");
    }

    /** rb_str_slice_bang
     *
     */
    @JRubyMethod(name = "slice!", required = 1, optional = 1)
    public IRubyObject slice_bang(IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(getRuntime(), args, 1, 2);
        IRubyObject[] newArgs = new IRubyObject[argc + 1];
        newArgs[0] = args[0];
        if (argc > 1) newArgs[1] = args[1];

        newArgs[argc] = newString("");
        IRubyObject result = op_aref(args);
        if (result.isNil()) return result;

        op_aset(newArgs);
        return result;
    }

    @JRubyMethod(name = {"succ", "next"})
    public IRubyObject succ() {
        return strDup().succ_bang();
    }

    @JRubyMethod(name = {"succ!", "next!"})
    public IRubyObject succ_bang() {
        if (value.length() == 0) return this;

        modify();
        
        boolean alnumSeen = false;
        int pos = -1;
        int c = 0;
        int n = 0;
        for (int i = value.length() - 1; i >= 0; i--) {
            c = value.get(i) & 0xFF;
            if (isAlnum(c)) {
                alnumSeen = true;
                if ((isDigit(c) && c < '9') || (isLower(c) && c < 'z') || (isUpper(c) && c < 'Z')) {
                    value.set(i, (byte)(c + 1));
                    pos = -1;
                    break;
                }
                pos = i;
                n = isDigit(c) ? '1' : (isLower(c) ? 'a' : 'A');
                value.set(i, (byte)(isDigit(c) ? '0' : (isLower(c) ? 'a' : 'A')));
            }
        }
        if (!alnumSeen) {
            for (int i = value.length() - 1; i >= 0; i--) {
                c = value.get(i) & 0xFF;
                if (c < 0xff) {
                    value.set(i, (byte)(c + 1));
                    pos = -1;
                    break;
                }
                pos = i;
                n = '\u0001';
                value.set(i, 0);
            }
        }
        if (pos > -1) {
            // This represents left most digit in a set of incremented
            // values?  Therefore leftmost numeric must be '1' and not '0'
            // 999 -> 1000, not 999 -> 0000.  whereas chars should be
            // zzz -> aaaa and non-alnum byte values should be "\377" -> "\001\000"
            value.prepend((byte) n);
        }
        return this;
    }

    /** rb_str_upto_m
     *
     */
    @JRubyMethod(name = "upto", required = 1, frame = true)
    public IRubyObject upto(IRubyObject str, Block block) {
        return upto(str, false, block);
    }

    /* rb_str_upto */
    public IRubyObject upto(IRubyObject str, boolean excl, Block block) {
        // alias 'this' to 'beg' for ease of comparison with MRI
        RubyString beg = this;
        RubyString end = stringValue(str);

        int n = beg.op_cmp(end);
        if (n > 0 || (excl && n == 0)) return beg;

        RubyString afterEnd = stringValue(end.succ());
        RubyString current = beg;

        ThreadContext context = getRuntime().getCurrentContext();
        while (!current.equals(afterEnd)) {
            block.yield(context, current);
            if (!excl && current.equals(end)) break;

            current = (RubyString) current.succ();
            
            if (excl && current.equals(end)) break;

            if (current.length().getLongValue() > end.length().getLongValue()) break;
        }

        return beg;

    }

    /** rb_str_include
     *
     */
    @JRubyMethod(name = "include?", required = 1)
    public RubyBoolean include_p(IRubyObject obj) {
        if (obj instanceof RubyFixnum) {
            int c = RubyNumeric.fix2int(obj);
            for (int i = 0; i < value.length(); i++) {
                if (value.get(i) == (byte)c) {
                    return getRuntime().getTrue();
                }
            }
            return getRuntime().getFalse();
        }
        ByteList str = stringValue(obj).value;
        return getRuntime().newBoolean(value.indexOf(str) != -1);
    }

    /** rb_str_to_i
     *
     */
    @JRubyMethod(name = "to_i", optional = 1)
    public IRubyObject to_i(IRubyObject[] args) {
        long base = Arity.checkArgumentCount(getRuntime(), args, 0, 1) == 0 ? 10 : args[0].convertToInteger().getLongValue();
        return RubyNumeric.str2inum(getRuntime(), this, (int) base);
    }

    /** rb_str_oct
     *
     */
    @JRubyMethod(name = "oct")
    public IRubyObject oct() {
        if (isEmpty()) {
            return getRuntime().newFixnum(0);
        }

        int base = 8;

        int ix = value.begin;

        while(ix < value.begin+value.realSize && ASCII.isSpace(value.bytes[ix] & 0xff)) {
            ix++;
        }

        int pos = (value.bytes[ix] == '-' || value.bytes[ix] == '+') ? ix+1 : ix;
        if((pos+1) < value.begin+value.realSize && value.bytes[pos] == '0') {
            if(value.bytes[pos+1] == 'x' || value.bytes[pos+1] == 'X') {
                base = 16;
            } else if(value.bytes[pos+1] == 'b' || value.bytes[pos+1] == 'B') {
                base = 2;
            }
        }
        return RubyNumeric.str2inum(getRuntime(), this, base);
    }

    /** rb_str_hex
     *
     */
    @JRubyMethod(name = "hex")
    public IRubyObject hex() {
        return RubyNumeric.str2inum(getRuntime(), this, 16);
    }

    /** rb_str_to_f
     *
     */
    @JRubyMethod(name = "to_f")
    public IRubyObject to_f() {
        return RubyNumeric.str2fnum(getRuntime(), this);
    }

    /** rb_str_split_m
     *
     */
    @JRubyMethod(name = "split", optional = 2)
    public RubyArray split(IRubyObject[] args) {
        final int i, lim;
        boolean limit = false;
            
        if (Arity.checkArgumentCount(getRuntime(), args, 0, 2) == 2) {
            lim = RubyNumeric.fix2int(args[1]);
            if (lim == 1) return value.realSize == 0 ? getRuntime().newArray() : getRuntime().newArray(this);
            if (lim > 0) limit = true;
            i = 1;
        } else {
            i = 0;
            lim = 0;
        }
        
        IRubyObject spat = (args.length == 0 || args[0].isNil()) ? null : args[0];

        final RubyArray result;
        if (spat == null && (spat = getRuntime().getGlobalVariables().get("$;")).isNil()) {
            result = awkSplit(limit, lim, i);
        } else {
            if (spat instanceof RubyString && ((RubyString)spat).value.realSize == 1) {
                RubyString strSpat = (RubyString)spat;
                if (strSpat.value.bytes[strSpat.value.begin] == (byte)' ') {
                    result = awkSplit(limit, lim, i);
                } else {
                    result = split(spat, limit, lim, i);
                }
            } else {
                result = split(spat, limit, lim, i);
            }
        }

        if (!limit && lim == 0) {
            while (result.size() > 0 && ((RubyString)result.eltInternal(result.size() - 1)).value.realSize == 0)
                result.pop();
        }

        return result;
    }
    
    private RubyArray split(IRubyObject pat, boolean limit, int lim, int i) {
        Ruby runtime = getRuntime();
        
        final Regex regex = getPattern(pat, true).getPattern();
        int beg, end, start;
        
        int begin = value.begin;
        start = begin;
        beg = 0;
        
        int range = value.begin + value.realSize;
        final Matcher matcher = regex.matcher(value.bytes, value.begin, range);
        
        boolean lastNull = false;
        RubyArray result = runtime.newArray();
        if (regex.numberOfCaptures() == 0) { // shorter path, no captures defined, no region will be returned 
            while (start < range && (end = matcher.search(start, range, Option.NONE)) >= 0) {
                if (start == end + begin && matcher.getBegin() == matcher.getEnd()) {
                    if (value.realSize == 0) {
                        result.append(newEmptyString(runtime, getMetaClass()));
                        break;
                    } else if (lastNull) {
                        result.append(substr(beg, regex.getEncoding().length(value.bytes[begin + beg])));
                        beg = start - begin;
                    } else {
                        start += regex.getEncoding().length(value.bytes[start]);
                        lastNull = true;
                        continue;
                    }
                } else {
                    result.append(substr(beg, end - beg));
                    beg = matcher.getEnd();
                    start = begin + matcher.getEnd();
                }
                lastNull = false;
                if (limit && lim <= ++i) break;
            }
        } else {
            while (start < range && (end = matcher.search(start, range, Option.NONE)) >= 0) {
                final Region region = matcher.getRegion();
                if (start == end + begin && region.beg[0] == region.end[0]) {
                    if (value.realSize == 0) {                        
                        result.append(newEmptyString(runtime, getMetaClass()));
                        break;
                    } else if (lastNull) {
                        result.append(substr(beg, regex.getEncoding().length(value.bytes[begin + beg])));
                        beg = start - begin;
                    } else {
                        start += regex.getEncoding().length(value.bytes[start]);
                        lastNull = true;
                        continue;
                    }                    
                } else {
                    result.append(substr(beg, end - beg));
                    beg = start = region.end[0];
                    start += begin;
                }
                lastNull = false;
                
                for (int idx=1; idx<region.numRegs; idx++) {
                    if (region.beg[idx] == -1) continue;
                    if (region.beg[idx] == region.end[idx]) {
                        result.append(newEmptyString(runtime, getMetaClass()));
                    } else {
                        result.append(substr(region.beg[idx], region.end[idx] - region.beg[idx]));
                    }
                }
                if (limit && lim <= ++i) break;
            }
        }
        
        // only this case affects backrefs 
        runtime.getCurrentContext().getCurrentFrame().setBackRef(runtime.getNil());
        
        if (value.realSize > 0 && (limit || value.realSize > beg || lim < 0)) {
            if (value.realSize == beg) {
                result.append(newEmptyString(runtime, getMetaClass()));
            } else {
                result.append(substr(beg, value.realSize - beg));
            }
        }
        
        return result;
    }
    
    private RubyArray awkSplit(boolean limit, int lim, int i) {
        RubyArray result = getRuntime().newArray();
        
        byte[]bytes = value.bytes;
        int p = value.begin; 
        int endp = p + value.realSize;
                    
        boolean skip = true;
        
        int end, beg = 0;        
        for (end = beg = 0; p < endp; p++) {
            if (skip) {
                if (ASCII.isSpace(bytes[p] & 0xff)) {
                    beg++;
                } else {
                    end = beg + 1;
                    skip = false;
                    if (limit && lim <= i) break;
                }
            } else {
                if (ASCII.isSpace(bytes[p] & 0xff)) {
                    result.append(makeShared(beg, end - beg));
                    skip = true;
                    beg = end + 1;
                    if (limit) i++;
                } else {
                    end++;
                }
            }
        }
        
        if (value.realSize > 0 && (limit || value.realSize > beg || lim < 0)) {
            if (value.realSize == beg) {
                result.append(newEmptyString(getRuntime(), getMetaClass()));
            } else {
                result.append(makeShared(beg, value.realSize - beg));
            }
        }
        return result;
    }
    
    /** get_pat
     * 
     */
    private final RubyRegexp getPattern(IRubyObject obj, boolean quote) {
        if (obj instanceof RubyRegexp) {
            return (RubyRegexp)obj;
        } else if (!(obj instanceof RubyString)) {
            IRubyObject val = obj.checkStringType();
            if (val.isNil()) throw getRuntime().newTypeError("wrong argument type " + obj.getMetaClass() + " (expected Regexp)");
            obj = val; 
        }
        
        return ((RubyString)obj).getCachedPattern(quote);
    }

    /** rb_reg_regcomp
     * 
     */
    private final RubyRegexp getCachedPattern(boolean quote) {
        final HashMap<ByteList, RubyRegexp> cache = patternCache.get();
        RubyRegexp regexp = cache.get(value);
        if (regexp == null || regexp.getKCode() != getRuntime().getKCode()) {
            RubyString str = quote ? RubyRegexp.quote(this, (KCode)null) : this;
            regexp = RubyRegexp.newRegexp(getRuntime(), str.value, 0);
            cache.put(value, regexp);
        }
        return regexp;
    }
    
    // In future we should store joni Regexes (cross runtime cache)
    // for 1.9 cache, whole RubyString should be stored so the entry contains encoding information as well 
    private static final ThreadLocal<HashMap<ByteList, RubyRegexp>> patternCache = new ThreadLocal<HashMap<ByteList, RubyRegexp>>() {
        protected HashMap<ByteList, RubyRegexp> initialValue() {
            return new HashMap<ByteList, RubyRegexp>(5);
        }
    };
    
    /** rb_str_scan
     *
     */
    @JRubyMethod(name = "scan", required = 1, frame = true)
    public IRubyObject scan(IRubyObject arg, Block block) {
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        Frame frame = context.getPreviousFrame();
        
        
        final RubyRegexp rubyRegex = getPattern(arg, true);
        final Regex regex = rubyRegex.getPattern();
        
        int range = value.begin + value.realSize;
        final Matcher matcher = regex.matcher(value.bytes, value.begin, range);
        matcher.value = 0; // implicit start argument to scanOnce(NG)
        
        IRubyObject result;
        if (!block.isGiven()) {
            RubyArray ary = runtime.newArray();
            
            if (regex.numberOfCaptures() == 0) {
                while ((result = scanOnceNG(regex, matcher, range)) != null) ary.append(result);
            } else {
                while ((result = scanOnce(regex, matcher, range)) != null) ary.append(result);
            }
            rubyRegex.updateBackRef(this, frame, matcher);
            return ary;
        } else {
            byte[]bytes = value.bytes;
            int size = value.realSize;
            
            if (regex.numberOfCaptures() == 0) {
                while ((result = scanOnceNG(regex, matcher, range)) != null) {
                    rubyRegex.updateBackRef(this, frame, matcher).use();
                    block.yield(context, result);
                    modifyCheck(bytes, size);
                }
            } else {
                while ((result = scanOnce(regex, matcher, range)) != null) {
                    rubyRegex.updateBackRef(this, frame, matcher).use();
                    block.yield(context, result);
                    modifyCheck(bytes, size);
                }
            }
            rubyRegex.updateBackRef(this, frame, matcher);
            return this;
        }
    }

    /**
     * rb_enc_check
     */
    @SuppressWarnings("unused")
    private Encoding encodingCheck(RubyRegexp pattern) {
        // For 1.9 compatibility, should check encoding compat between string and pattern
        return pattern.getKCode().getEncoding();
    }
    
    // no group version
    private IRubyObject scanOnceNG(Regex regex, Matcher matcher, int range) {    
        if (matcher.search(matcher.value + value.begin, range, Option.NONE) >= 0) {
            int end = matcher.getEnd();
            if (matcher.getBegin() == end) {
                if (value.realSize > end) {
                    matcher.value = end + regex.getEncoding().length(value.bytes[value.begin + end]);
                } else {
                    matcher.value = end + 1;
                }
            } else {
                matcher.value = end;
            }
            return substr(matcher.getBegin(), end - matcher.getBegin());
        }
        return null;
    }
    
    // group version
    private IRubyObject scanOnce(Regex regex, Matcher matcher, int range) {    
        if (matcher.search(matcher.value + value.begin, range, Option.NONE) >= 0) {
            Region region = matcher.getRegion();
            int end = region.end[0];
            if (matcher.getBegin() == end) {
                if (value.realSize > end) {
                    matcher.value = end + regex.getEncoding().length(value.bytes[value.begin + end]);
                } else {
                    matcher.value = end + 1;
                }
            } else {
                matcher.value = end;
            }
            
            RubyArray result = getRuntime().newArray(region.numRegs);
            for (int i=1; i<region.numRegs; i++) {
                int beg = region.beg[i]; 
                if (beg == -1) {
                    result.append(getRuntime().getNil());
                } else {
                    result.append(substr(beg, region.end[i] - beg));
                }
            }
            return result;
        }
        return null;
    }    

    private static final ByteList SPACE_BYTELIST = new ByteList(ByteList.plain(" "));
    
    private final IRubyObject justify(IRubyObject[]args, char jflag) {
        Ruby runtime = getRuntime();        
        Arity.scanArgs(runtime, args, 1, 1);
        
        int width = RubyFixnum.num2int(args[0]);
        
        int f, flen = 0;
        byte[]fbuf;
        
        IRubyObject pad;

        if (args.length == 2) {
            pad = args[1].convertToString();
            ByteList fList = ((RubyString)pad).value;
            f = fList.begin;
            flen = fList.realSize;

            if (flen == 0) throw getRuntime().newArgumentError("zero width padding");
            
            fbuf = fList.bytes;
        } else {
            f = SPACE_BYTELIST.begin;
            flen = SPACE_BYTELIST.realSize;
            fbuf = SPACE_BYTELIST.bytes;
            pad = runtime.getNil();
        }
        
        if (width < 0 || value.realSize >= width) return strDup();
        
        ByteList res = new ByteList(width);
        res.realSize = width;
        
        int p = res.begin;
        int pend;
        byte[] pbuf = res.bytes;
        
        if (jflag != 'l') {
            int n = width - value.realSize;
            pend = p + ((jflag == 'r') ? n : n / 2);
            if (flen <= 1) {
                while (p < pend) pbuf[p++] = fbuf[f];
            } else {
                int q = f;
                while (p + flen <= pend) {
                    System.arraycopy(fbuf, f, pbuf, p, flen);
                    p += flen;
                }
                while (p < pend) pbuf[p++] = fbuf[q++];
            }
        }
        
        System.arraycopy(value.bytes, value.begin, pbuf, p, value.realSize);
        
        if (jflag != 'r') {
            p += value.realSize;
            pend = res.begin + width;
            if (flen <= 1) {
                while (p < pend) pbuf[p++] = fbuf[f];
            } else {
                while (p + flen <= pend) {
                    System.arraycopy(fbuf, f, pbuf, p, flen);
                    p += flen;
                }
                while (p < pend) pbuf[p++] = fbuf[f++];
            }
            
        }
        
        RubyString resStr = new RubyString(runtime, getMetaClass(), res);
        resStr.infectBy(this);
        if (flen > 0) resStr.infectBy(pad);
        return resStr;
        
    }

    /** rb_str_ljust
     *
     */
    @JRubyMethod(name = "ljust", required = 1, optional = 1)
    public IRubyObject ljust(IRubyObject [] args) {
        return justify(args, 'l');
    }

    /** rb_str_rjust
     *
     */
    @JRubyMethod(name = "rjust", required = 1, optional = 1)
    public IRubyObject rjust(IRubyObject [] args) {
        return justify(args, 'r');
    }

    @JRubyMethod(name = "center", required = 1, optional = 1)
    public IRubyObject center(IRubyObject[] args) {
        return justify(args, 'c');
    }

    @JRubyMethod(name = "chop")
    public IRubyObject chop() {
        RubyString str = strDup();
        str.chop_bang();
        return str;
    }

    /** rb_str_chop_bang
     * 
     */
    @JRubyMethod(name = "chop!")
    public IRubyObject chop_bang() {
        int end = value.realSize - 1;
        if (end < 0) return getRuntime().getNil(); 

        if ((value.bytes[value.begin + end]) == '\n') {
            if (end > 0 && (value.bytes[value.begin + end - 1]) == '\r') end--;
        }

        view(0, end);
        return this;
    }

    /** rb_str_chop
     * 
     */
    @JRubyMethod(name = "chomp", optional = 1)
    public RubyString chomp(IRubyObject[] args) {
        RubyString str = strDup();
        str.chomp_bang(args);
        return str;
    }

    /**
     * rb_str_chomp_bang
     *
     * In the common case, removes CR and LF characters in various ways depending on the value of
     *   the optional args[0].
     * If args.length==0 removes one instance of CR, CRLF or LF from the end of the string.
     * If args.length>0 and args[0] is "\n" then same behaviour as args.length==0 .
     * If args.length>0 and args[0] is "" then removes trailing multiple LF or CRLF (but no CRs at
     *   all(!)).
     * @param args See method description.
     */
    @JRubyMethod(name = "chomp!", optional = 1)
    public IRubyObject chomp_bang(IRubyObject[] args) {
        IRubyObject rsObj;

        if (Arity.checkArgumentCount(getRuntime(), args, 0, 1) == 0) {
            int len = value.length();
            if (len == 0) return getRuntime().getNil();
            byte[]buff = value.bytes;

            rsObj = getRuntime().getGlobalVariables().get("$/");

            if (rsObj == getRuntime().getGlobalVariables().getDefaultSeparator()) {
                int realSize = value.realSize;
                int begin = value.begin;
                if (buff[begin + len - 1] == (byte)'\n') {
                    realSize--;
                    if (realSize > 0 && buff[begin + realSize - 1] == (byte)'\r') realSize--;
                    view(0, realSize);
                } else if (buff[begin + len - 1] == (byte)'\r') {
                    realSize--;
                    view(0, realSize);
                } else {
                    modifyCheck();
                    return getRuntime().getNil();
                }
                return this;                
            }
        } else {
            rsObj = args[0];
        }

        if (rsObj.isNil()) return getRuntime().getNil();

        RubyString rs = rsObj.convertToString();
        int len = value.realSize;
        int begin = value.begin;
        if (len == 0) return getRuntime().getNil();
        byte[]buff = value.bytes;
        int rslen = rs.value.realSize;

        if (rslen == 0) {
            while (len > 0 && buff[begin + len - 1] == (byte)'\n') {
                len--;
                if (len > 0 && buff[begin + len - 1] == (byte)'\r') len--;
            }
            if (len < value.realSize) {
                view(0, len);
                return this;
            }
            return getRuntime().getNil();
        }

        if (rslen > len) return getRuntime().getNil();
        byte newline = rs.value.bytes[rslen - 1];

        if (rslen == 1 && newline == (byte)'\n') {
            buff = value.bytes;
            int realSize = value.realSize;
            if (buff[begin + len - 1] == (byte)'\n') {
                realSize--;
                if (realSize > 0 && buff[begin + realSize - 1] == (byte)'\r') realSize--;
                view(0, realSize);
            } else if (buff[begin + len - 1] == (byte)'\r') {
                realSize--;
                view(0, realSize);
            } else {
                modifyCheck();
                return getRuntime().getNil();
            }
            return this;                
        }

        if (buff[begin + len - 1] == newline && rslen <= 1 || value.endsWith(rs.value)) {
            view(0, value.realSize - rslen);
            return this;            
        }

        return getRuntime().getNil();
    }

    /** rb_str_lstrip
     * 
     */
    @JRubyMethod(name = "lstrip")
    public IRubyObject lstrip() {
        RubyString str = strDup();
        str.lstrip_bang();
        return str;
    }

    /** rb_str_lstrip_bang
     */
    @JRubyMethod(name = "lstrip!")
    public IRubyObject lstrip_bang() {
        if (value.realSize == 0) return getRuntime().getNil();
        
        int i=0;
        while (i < value.realSize && ASCII.isSpace(value.bytes[value.begin + i] & 0xff)) i++;
        
        if (i > 0) {
            view(i, value.realSize - i);
            return this;
        }
        
        return getRuntime().getNil();
    }

    /** rb_str_rstrip
     *  
     */
    @JRubyMethod(name = "rstrip")
    public IRubyObject rstrip() {
        RubyString str = strDup();
        str.rstrip_bang();
        return str;
    }

    /** rb_str_rstrip_bang
     */ 
    @JRubyMethod(name = "rstrip!")
    public IRubyObject rstrip_bang() {
        if (value.realSize == 0) return getRuntime().getNil();
        int i=value.realSize - 1;

        while (i >= 0 && value.bytes[value.begin+i] == 0) i--;
        while (i >= 0 && ASCII.isSpace(value.bytes[value.begin + i] & 0xff)) i--;

        if (i < value.realSize - 1) {
            view(0, i + 1);
            return this;
        }

        return getRuntime().getNil();
    }

    /** rb_str_strip
     *
     */
    @JRubyMethod(name = "strip")
    public IRubyObject strip() {
        RubyString str = strDup();
        str.strip_bang();
        return str;
        }

    /** rb_str_strip_bang
     */
    @JRubyMethod(name = "strip!")
    public IRubyObject strip_bang() {
        IRubyObject l = lstrip_bang();
        IRubyObject r = rstrip_bang();

        if(l.isNil() && r.isNil()) {
            return l;
        }
        return this;
    }

    /** rb_str_count
     *
     */
    @JRubyMethod(name = "count", required = 1, rest = true)
    public IRubyObject count(IRubyObject[] args) {
        if (args.length < 1) throw getRuntime().newArgumentError("wrong number of arguments");
        if (value.realSize == 0) return getRuntime().newFixnum(0);

        boolean[]table = new boolean[TRANS_SIZE];
        boolean init = true;
        for (int i=0; i<args.length; i++) {
            RubyString s = args[i].convertToString();
            s.setup_table(table, init);
            init = false;
        }

        int s = value.begin;
        int send = s + value.realSize;
        byte[]buf = value.bytes;
        int i = 0;

        while (s < send) if (table[buf[s++] & 0xff]) i++;

        return getRuntime().newFixnum(i);
    }

    /** rb_str_delete
     *
     */
    @JRubyMethod(name = "delete", required = 1, rest = true)
    public IRubyObject delete(IRubyObject[] args) {
        RubyString str = strDup();
        str.delete_bang(args);
        return str;
    }

    /** rb_str_delete_bang
     *
     */
    @JRubyMethod(name = "delete!", required = 1, rest = true)
    public IRubyObject delete_bang(IRubyObject[] args) {
        if (args.length < 1) throw getRuntime().newArgumentError("wrong number of arguments");
        
        boolean[]squeeze = new boolean[TRANS_SIZE];

        boolean init = true;
        for (int i=0; i<args.length; i++) {
            RubyString s = args[i].convertToString();
            s.setup_table(squeeze, init);
            init = false;
        }
        
        modify();
        
        if (value.realSize == 0) return getRuntime().getNil();
        int s = value.begin;
        int t = s;
        int send = s + value.realSize;
        byte[]buf = value.bytes;
        boolean modify = false;
        
        while (s < send) {
            if (squeeze[buf[s] & 0xff]) {
                modify = true;
            } else {
                buf[t++] = buf[s];
            }
            s++;
        }
        value.realSize = t - value.begin;
        
        if (modify) return this;
        return getRuntime().getNil();
    }

    /** rb_str_squeeze
     *
     */
    @JRubyMethod(name = "squeeze", rest = true)
    public IRubyObject squeeze(IRubyObject[] args) {
        RubyString str = strDup();
        str.squeeze_bang(args);        
        return str;        
    }

    /** rb_str_squeeze_bang
     *
     */
    @JRubyMethod(name = "squeeze!", rest = true)
    public IRubyObject squeeze_bang(IRubyObject[] args) {
        if (value.realSize == 0) return getRuntime().getNil();

        final boolean squeeze[] = new boolean[TRANS_SIZE];

        if (args.length == 0) {
            for (int i=0; i<TRANS_SIZE; i++) squeeze[i] = true;
        } else {
            boolean init = true;
            for (int i=0; i<args.length; i++) {
                RubyString s = args[i].convertToString();
                s.setup_table(squeeze, init);
                init = false;
            }
        }

        modify();

        int s = value.begin;
        int t = s;
        int send = s + value.realSize;
        byte[]buf = value.bytes;
        int save = -1;

        while (s < send) {
            int c = buf[s++] & 0xff;
            if (c != save || !squeeze[c]) buf[t++] = (byte)(save = c);
        }

        if (t - value.begin != value.realSize) { // modified
            value.realSize = t - value.begin; 
            return this;
        }

        return getRuntime().getNil();
    }

    /** rb_str_tr
     *
     */
    @JRubyMethod(name = "tr", required = 2)
    public IRubyObject tr(IRubyObject src, IRubyObject repl) {
        RubyString str = strDup();
        str.tr_trans(src, repl, false);        
        return str;        
    }
    
    /** rb_str_tr_bang
    *
    */
    @JRubyMethod(name = "tr!", required = 2)
    public IRubyObject tr_bang(IRubyObject src, IRubyObject repl) {
        return tr_trans(src, repl, false);
    }    
    
    private static final class TR {
        int gen, now, max;
        int p, pend;
        byte[]buf;
    }

    private static final int TRANS_SIZE = 256;
    
    /** tr_setup_table
     * 
     */
    private final void setup_table(boolean[]table, boolean init) {
        final boolean[]buf = new boolean[TRANS_SIZE];
        final TR tr = new TR();
        int c;
        
        boolean cflag = false;
        
        tr.p = value.begin;
        tr.pend = value.begin + value.realSize;
        tr.buf = value.bytes;
        tr.gen = tr.now = tr.max = 0;
        
        if (value.realSize > 1 && value.bytes[value.begin] == '^') {
            cflag = true;
            tr.p++;
        }
        
        if (init) for (int i=0; i<TRANS_SIZE; i++) table[i] = true;
        
        for (int i=0; i<TRANS_SIZE; i++) buf[i] = cflag;
        while ((c = trnext(tr)) >= 0) buf[c & 0xff] = !cflag;
        for (int i=0; i<TRANS_SIZE; i++) table[i] = table[i] && buf[i];
    }
    
    /** tr_trans
    *
    */    
    private final IRubyObject tr_trans(IRubyObject src, IRubyObject repl, boolean sflag) {
        if (value.realSize == 0) return getRuntime().getNil();
        
        ByteList replList = repl.convertToString().value;
        
        if (replList.realSize == 0) return delete_bang(new IRubyObject[]{src});

        ByteList srcList = src.convertToString().value;
        
        final TR trsrc = new TR();
        final TR trrepl = new TR();
        
        boolean cflag = false;
        boolean modify = false;
        
        trsrc.p = srcList.begin;
        trsrc.pend = srcList.begin + srcList.realSize;
        trsrc.buf = srcList.bytes;
        if (srcList.realSize >= 2 && srcList.bytes[srcList.begin] == '^') {
            cflag = true;
            trsrc.p++;
        }       
        
        trrepl.p = replList.begin;
        trrepl.pend = replList.begin + replList.realSize;
        trrepl.buf = replList.bytes;
        
        trsrc.gen = trrepl.gen = 0;
        trsrc.now = trrepl.now = 0;
        trsrc.max = trrepl.max = 0;
        
        int c;
        final int[]trans = new int[TRANS_SIZE];
        if (cflag) {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = 1;
            while ((c = trnext(trsrc)) >= 0) trans[c & 0xff] = -1;
            while ((c = trnext(trrepl)) >= 0); 
            for (int i=0; i<TRANS_SIZE; i++) {
                if (trans[i] >= 0) trans[i] = trrepl.now;
            }
        } else {
            for (int i=0; i<TRANS_SIZE; i++) trans[i] = -1;
            while ((c = trnext(trsrc)) >= 0) {
                int r = trnext(trrepl);
                if (r == -1) r = trrepl.now;
                trans[c & 0xff] = r;
            }
        }
        
        modify();
        
        int s = value.begin;
        int send = s + value.realSize;
        byte sbuf[] = value.bytes;
        
        if (sflag) {
            int t = s;
            int c0, last = -1;
            while (s < send) {
                c0 = sbuf[s++];
                if ((c = trans[c0 & 0xff]) >= 0) {
                    if (last == c) continue;
                    last = c;
                    sbuf[t++] = (byte)(c & 0xff);
                    modify = true;
                } else {
                    last = -1;
                    sbuf[t++] = (byte)c0;
                }
            }
            
            if (value.realSize > (t - value.begin)) {
                value.realSize = t - value.begin;
                modify = true;
            }
        } else {
            while (s < send) {
                if ((c = trans[sbuf[s] & 0xff]) >= 0) {
                    sbuf[s] = (byte)(c & 0xff);
                    modify = true;
                }
                s++;
            }
        }
        
        if (modify) return this;
        return getRuntime().getNil();
    }

    /** trnext
    *
    */    
    private final int trnext(TR t) {
        byte [] buf = t.buf;
        
        for (;;) {
            if (t.gen == 0) {
                if (t.p == t.pend) return -1;
                if (t.p < t.pend -1 && buf[t.p] == '\\') t.p++;
                t.now = buf[t.p++];
                if (t.p < t.pend - 1 && buf[t.p] == '-') {
                    t.p++;
                    if (t.p < t.pend) {
                        if (t.now > buf[t.p]) {
                            t.p++;
                            continue;
                        }
                        t.gen = 1;
                        t.max = buf[t.p++];
                    }
                }
                return t.now & 0xff;
            } else if (++t.now < t.max) {
                return t.now & 0xff;
            } else {
                t.gen = 0;
                return t.max & 0xff;
            }
        }
    }    

    /** rb_str_tr_s
     *
     */
    @JRubyMethod(name = "tr_s", required = 2)
    public IRubyObject tr_s(IRubyObject src, IRubyObject repl) {
        RubyString str = strDup();
        str.tr_trans(src, repl, true);        
        return str;        
    }

    /** rb_str_tr_s_bang
     *
     */
    @JRubyMethod(name = "tr_s!", required = 2)
    public IRubyObject tr_s_bang(IRubyObject src, IRubyObject repl) {
        return tr_trans(src, repl, true);
    }

    /** rb_str_each_line
     *
     */
    @JRubyMethod(name = {"each_line", "each"}, required = 0, optional = 1, frame = true)
    public IRubyObject each_line(IRubyObject[] args, Block block) {
        byte newline;
        int p = value.begin;
        int pend = p + value.realSize;
        int s;
        int ptr = p;
        int len = value.realSize;
        int rslen;
        IRubyObject line;
        

        IRubyObject _rsep;
        if (Arity.checkArgumentCount(getRuntime(), args, 0, 1) == 0) {
            _rsep = getRuntime().getGlobalVariables().get("$/");
        } else {
            _rsep = args[0];
        }

        ThreadContext tc = getRuntime().getCurrentContext();

        if(_rsep.isNil()) {
            block.yield(tc, this);
            return this;
        }
        
        RubyString rsep = stringValue(_rsep);
        ByteList rsepValue = rsep.value;
        byte[] strBytes = value.bytes;

        rslen = rsepValue.realSize;
        
        if(rslen == 0) {
            newline = '\n';
        } else {
            newline = rsepValue.bytes[rsepValue.begin + rslen-1];
        }

        s = p;
        p+=rslen;

        for(; p < pend; p++) {
            if(rslen == 0 && strBytes[p] == '\n') {
                if(strBytes[++p] != '\n') {
                    continue;
                }
                while(strBytes[p] == '\n') {
                    p++;
                }
            }
            if(ptr<p && strBytes[p-1] == newline &&
               (rslen <= 1 || 
                ByteList.memcmp(rsepValue.bytes, rsepValue.begin, rslen, strBytes, p-rslen, rslen) == 0)) {
                line = RubyString.newStringShared(getRuntime(), getMetaClass(), this.value.makeShared(s-ptr, p-s));
                line.infectBy(this);
                block.yield(tc, line);
                modifyCheck(strBytes,len);
                s = p;
            }
        }

        if(s != pend) {
            if(p > pend) {
                p = pend;
            }
            line = RubyString.newStringShared(getRuntime(), getMetaClass(), this.value.makeShared(s-ptr, p-s));
            line.infectBy(this);
            block.yield(tc, line);
        }

        return this;
    }

    /**
     * rb_str_each_byte
     */
    @JRubyMethod(name = "each_byte", frame = true)
    public RubyString each_byte(Block block) {
        int lLength = value.length();
        Ruby runtime = getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        for (int i = 0; i < lLength; i++) {
            block.yield(context, runtime.newFixnum(value.get(i) & 0xFF));
        }
        return this;
    }

    /** rb_str_intern
     *
     */
    public RubySymbol intern() {
        String s = toString();
        if (s.length() == 0) {
            throw getRuntime().newArgumentError("interning empty string");
        }
        if (s.indexOf('\0') >= 0) {
            throw getRuntime().newArgumentError("symbol string may not contain '\\0'");
        }
        return getRuntime().newSymbol(s);
    }

    @JRubyMethod(name = {"to_sym", "intern"})
    public RubySymbol to_sym() {
        return intern();
    }

    @JRubyMethod(name = "sum", optional = 1)
    public RubyInteger sum(IRubyObject[] args) {
        if (args.length > 1) {
            throw getRuntime().newArgumentError("wrong number of arguments (" + args.length + " for 1)");
        }
        
        long bitSize = 16;
        if (args.length == 1) {
            long bitSizeArg = ((RubyInteger) args[0].convertToInteger()).getLongValue();
            if (bitSizeArg > 0) {
                bitSize = bitSizeArg;
            }
        }

        long result = 0;
        for (int i = 0; i < value.length(); i++) {
            result += value.get(i) & 0xFF;
        }
        return getRuntime().newFixnum(bitSize == 0 ? result : result % (long) Math.pow(2, bitSize));
    }

    public static RubyString unmarshalFrom(UnmarshalStream input) throws java.io.IOException {
        RubyString result = newString(input.getRuntime(), input.unmarshalString());
        input.registerLinkTarget(result);
        return result;
    }

    /**
     * @see org.jruby.util.Pack#unpack
     */
    @JRubyMethod(name = "unpack", required = 1)
    public RubyArray unpack(IRubyObject obj) {
        return Pack.unpack(getRuntime(), this.value, stringValue(obj).value);
    }

    /**
     * Mutator for internal string representation.
     *
     * @param value The new java.lang.String this RubyString should encapsulate
     */
    public void setValue(CharSequence value) {
        view(ByteList.plain(value));
    }

    public void setValue(ByteList value) {
        view(value);
    }

    public CharSequence getValue() {
        return toString();
    }

    public byte[] getBytes() {
        return value.bytes();
    }

    public ByteList getByteList() {
        return value;
    }
}
