/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2007 William N Dortch <bill.dortch@gmail.com>
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

package org.jruby.util;

import org.jcodings.Config;
import org.jcodings.Encoding;
import org.jcodings.IntHolder;
import org.jcodings.unicode.UnicodeCodeRange;

public final class IdUtil {
    /**
     * rb_is_const_id and is_const_id
     */    
    public static boolean isConstant(String id) {
        return Character.isUpperCase(id.charAt(0));
    }

    /**
     * rb_is_class_id and is_class_id
     */    
    public static boolean isClassVariable(String id) {
        return id.length() > 1 && id.charAt(0) == '@' && id.charAt(1) == '@';
    }

    /**
     * rb_is_instance_id and is_instance_id
     */    
    public static boolean isInstanceVariable(String id) {
        return id.length() > 0 && id.charAt(0) == '@' && (id.length() < 2 || id.charAt(1) != '@');
    }
    
    /**
     * rb_is_global_id and is_global_id
     */    
    public static boolean isGlobal(String id) {
        return id.length()>0 && id.charAt(0) == '$';
    }

    public static boolean isPredicate(String id) {
        return id.endsWith("?");
    }
    
    /**
     * rb_is_local_id and is_local_id
     */    
    public static boolean isLocal(String id) {
        return !isGlobal(id) && !isClassVariable(id) && !isInstanceVariable(id) && !isConstant(id) && !isPredicate(id) && !isSpecial(id);
    }

    /**
     * We store IR special variables (e.g. %block) in scope and we want reflective Ruby methods to
     * not see these since they are not real variables...they're special.
     */
    public static boolean isSpecial(String id) {
        return id.startsWith("%");
    }

    public static boolean isAttrSet(String id) {
        return id.endsWith("=");
    }

    public static boolean isValidConstantName(String id) {
        char c;
        int len;
        if ((len = id.length()) > 0 && (c = id.charAt(0)) <= 'Z' && c >= 'A') {
            return isNameString(id, 1, len);
        }
        return false;
    }

    public static boolean isValidInstanceVariableName(String id) {
        int len;
        if ((len = id.length()) > 1 && '@' == id.charAt(0)) {
            if (isInitialCharacter(id.charAt(1))) {
                return isNameString(id, 2, len);
            }
        }
        return false;
    }
    
    public static boolean isValidClassVariableName(String id) {
        int len;
        if ((len = id.length()) > 2 && '@' == id.charAt(0) && '@' == id.charAt(1)) {
            if (isInitialCharacter(id.charAt(2))) {
                return isNameString(id, 3, len);
            }
        }
        return false;
    }

    public static boolean isInitialCharacter(int c) {
        return Character.isAlphabetic(c) || c == '_';
    }

    public static boolean isNameCharacter(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public static boolean isNameString(String id, int start, int limit) {
        for (int i = start; i < limit; i++) {
            if (!isNameCharacter(id.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check the syntax of a Ruby variable, including that it's longer
     * than zero characters, and starts with either an @ or a capital
     * letter.
     */
    public static final boolean isRubyVariable(String name) {
        char c;
        return name.length() > 0 && ((c = name.charAt(0)) == '@' || (c <= 'Z' && c >= 'A'));
    }

    // MRI: rb_sym_constant_char_p
    public static boolean isConstantInitial(ByteList byteList) {
        int c, len;
        int nlen = byteList.realSize();
        Encoding enc = byteList.getEncoding();

        if (nlen < 1) return false;
        int byte0 = byteList.get(0);
        if (byte0 < 128) return enc.isUpper(byte0);

        byte[] bytes = byteList.getUnsafeBytes();
        int begin = byteList.begin();
        int end = begin + nlen;
        c = StringSupport.preciseLength(enc, bytes, begin, end);
        if (!StringSupport.MBCLEN_CHARFOUND_P(c)) return false;
        len = StringSupport.MBCLEN_CHARFOUND_LEN(c);
        c = StringSupport.codePoint(enc, bytes, begin, end);
        if (enc.isUnicode()) {
            if (enc.isUpper(c)) return true;
            if (enc.isLower(c)) return false;
            if (enc.isCodeCType(c, UnicodeCodeRange.TITLECASELETTER.getCType())) return true;
        } else {
            /* fallback to case-folding */
            IntHolder holder = new IntHolder();
            holder.value = begin;
            byte[] fold = new byte[Config.ENC_GET_CASE_FOLD_CODES_MAX_NUM];
            int r = enc.mbcCaseFold(Config.CASE_FOLD, bytes, holder, end, fold);
            if (r > 0 && (r != len || ByteList.memcmp(fold, 0, bytes, begin, r) != 0))
                return true;
        }
        return false;
    }

    @Deprecated
    public static boolean isValidConstantName19(String id) {
        return isValidConstantName(id);
    }

    @Deprecated
    public static boolean isNameCharacter19(char c) {
        return isNameCharacter19(c);
    }

    @Deprecated
    public static boolean isNameString19(String id, int start, int limit) {
        return isNameString(id, start, limit);
    }

}
