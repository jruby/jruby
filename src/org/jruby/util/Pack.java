/*
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003 Thomas E Enebo <enebo@acm.org>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */

package org.jruby.util;

import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.builtin.IRubyObject;


public class Pack {
    private static final String sSp10 = "          ";
    private static final String sNil10 = "\000\000\000\000\000\000\000\000\000\000";
    /** Native pack type.
     **/
    private static final String sNatStr = "sSiIlL";
    private static final String sTooFew = "too few arguments";
    private static final char hex_table[] = "0123456789ABCDEF".toCharArray();
    private static final char[] uu_table =
        "`!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_".toCharArray();
    private static final char[] b64_table =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final char[] sHexDigits = "0123456789abcdef0123456789ABCDEFx".toCharArray();

    /**
     * encodes a String in base64 or its uuencode variant.
     * appends the result of the encoding in a StringBuffer
     * @param io2Append The StringBuffer which should receive the result
     * @param i2Encode The String to encode
     * @param iLength The max number of characters to encode
     * @param iType the type of encoding required (this is the same type as used by the pack method)
     * @return the io2Append buffer
     **/
    private static StringBuffer encodes(Ruby ruby, StringBuffer io2Append, String i2Encode, int iLength, char iType) {
        iLength = iLength < i2Encode.length() ? iLength : i2Encode.length();
        io2Append.ensureCapacity(iLength * 4 / 3 + 6);
        int i = 0;
        char[] lTranslationTable = iType == 'u' ? uu_table : b64_table;
        char lPadding;
        char[] l2Encode = i2Encode.toCharArray();
        if (iType == 'u') {
            if (iLength >= lTranslationTable.length)
                throw new ArgumentError(
                    ruby,
                    ""
                        + iLength
                        + " is not a correct value for the number of bytes per line in a u directive.  Correct values range from 0 to "
                        + lTranslationTable.length);
            io2Append.append(lTranslationTable[iLength]);
            lPadding = '`';
        } else {
            lPadding = '=';
        }
        while (iLength >= 3) {
            char lCurChar = l2Encode[i++];
            char lNextChar = l2Encode[i++];
            char lNextNextChar = l2Encode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >>> 4) & 017))]);
            io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | ((lNextNextChar >>> 6) & 03))]);
            io2Append.append(lTranslationTable[077 & lNextNextChar]);
            iLength -= 3;
        }
        if (iLength == 2) {
            char lCurChar = l2Encode[i++];
            char lNextChar = l2Encode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | ((lNextChar >> 4) & 017))]);
            io2Append.append(lTranslationTable[077 & (((lNextChar << 2) & 074) | (('\0' >> 6) & 03))]);
            io2Append.append(lPadding);
        } else if (iLength == 1) {
            char lCurChar = l2Encode[i++];
            io2Append.append(lTranslationTable[077 & (lCurChar >>> 2)]);
            io2Append.append(lTranslationTable[077 & (((lCurChar << 4) & 060) | (('\0' >>> 4) & 017))]);
            io2Append.append(lPadding);
            io2Append.append(lPadding);
        }
        io2Append.append('\n');
        return io2Append;
    }

    /**
     * encodes a String with the Quoted printable, MIME encoding (see RFC2045).
     * appends the result of the encoding in a StringBuffer
     * @param io2Append The StringBuffer which should receive the result
     * @param i2Encode The String to encode
     * @param iLength The max number of characters to encode
     * @return the io2Append buffer
     **/
    private static StringBuffer qpencode(StringBuffer io2Append, String i2Encode, int iLength) {
        io2Append.ensureCapacity(1024);
        int lCurLineLength = 0;
        int lPrevChar = -1;
        char[] l2Encode = i2Encode.toCharArray();
        try {
            for (int i = 0;; i++) {
                char lCurChar = l2Encode[i];
                if ((lCurChar > 126) || (lCurChar < 32 && lCurChar != '\n' && lCurChar != '\t') || (lCurChar == '=')) {
                    io2Append.append('=');
                    io2Append.append(hex_table[lCurChar >> 4]);
                    io2Append.append(hex_table[lCurChar & 0x0f]);
                    lCurLineLength += 3;
                    lPrevChar = -1;
                } else if (lCurChar == '\n') {
                    if (lPrevChar == ' ' || lPrevChar == '\t') {
                        io2Append.append('=');
                        io2Append.append(lCurChar);
                    }
                    io2Append.append(lCurChar);
                    lCurLineLength = 0;
                    lPrevChar = lCurChar;
                } else {
                    io2Append.append(lCurChar);
                    lCurLineLength++;
                    lPrevChar = lCurChar;
                }
                if (lCurLineLength > iLength) {
                    io2Append.append('=');
                    io2Append.append('\n');
                    lCurLineLength = 0;
                    lPrevChar = '\n';
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //normal exit, this should be faster than a test at each iterations for string with more than
            //about 40 char
        }

        if (lCurLineLength > 0) {
            io2Append.append('=');
            io2Append.append('\n');
        }
        return io2Append;
    }

    private static String convert2String(IRubyObject l2Conv) {
        Ruby ruby = l2Conv.getRuntime();
        if (l2Conv.getMetaClass() != ruby.getClasses().getStringClass()) {
            l2Conv = l2Conv.convertToType("String", "to_s", true); //we may need a false here, not sure
        }
        return ((RubyString) l2Conv).getValue();
    }

    /**
     *    Decodes <i>str</i> (which may contain binary data) according to the format
     *       string, returning an array of each value extracted.
     * 	  The format string consists of a sequence of single-character directives.<br/>
     * 	  Each directive may be followed by a number, indicating the number of times to repeat with this directive.  An asterisk (``<code>*</code>'') will use up all
     *       remaining elements.  <br/>
     * 	  The directives <code>sSiIlL</code> may each be followed by an underscore (``<code>_</code>'') to use the underlying platform's native size for the specified type; otherwise, it uses a platform-independent consistent size.  <br/>
     * 	  Spaces are ignored in the format string.
     *           @see RubyArray#pack
     *       <table border="2" width="500" bgcolor="#ffe0e0">
     *           <tr>
     *             <td>
     * <P></P>
     *         <b>Directives for <a href="ref_c_string.html#String.unpack">
     *                   <code>String#unpack</code>
     *                 </a>
     *               </b>        <table class="codebox" cellspacing="0" border="0" cellpadding="3">
     * <tr bgcolor="#ff9999">
     *   <td valign="top">
     *                     <b>Format</b>
     *                   </td>
     *   <td valign="top">
     *                     <b>Function</b>
     *                   </td>
     *   <td valign="top">
     *                     <b>Returns</b>
     *                   </td>
     * </tr>
     * <tr>
     *   <td valign="top">A</td>
     *   <td valign="top">String with trailing nulls and spaces removed.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">a</td>
     *   <td valign="top">String.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">B</td>
     *   <td valign="top">Extract bits from each character (msb first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">b</td>
     *   <td valign="top">Extract bits from each character (lsb first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">C</td>
     *   <td valign="top">Extract a character as an unsigned integer.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">c</td>
     *   <td valign="top">Extract a character as an integer.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">d</td>
     *   <td valign="top">Treat <em>sizeof(double)</em> characters as a native
     *           double.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">E</td>
     *   <td valign="top">Treat <em>sizeof(double)</em> characters as a double in
     *           little-endian byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">e</td>
     *   <td valign="top">Treat <em>sizeof(float)</em> characters as a float in
     *           little-endian byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">f</td>
     *   <td valign="top">Treat <em>sizeof(float)</em> characters as a native float.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">G</td>
     *   <td valign="top">Treat <em>sizeof(double)</em> characters as a double in
     *           network byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">g</td>
     *   <td valign="top">Treat <em>sizeof(float)</em> characters as a float in
     *           network byte order.</td>
     *   <td valign="top">Float</td>
     * </tr>
     * <tr>
     *   <td valign="top">H</td>
     *   <td valign="top">Extract hex nibbles from each character (most
     *           significant first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">h</td>
     *   <td valign="top">Extract hex nibbles from each character (least
     *           significant first).</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">I</td>
     *   <td valign="top">Treat <em>sizeof(int)</em>
     *                     <sup>1</sup> successive
     *           characters as an unsigned native integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">i</td>
     *   <td valign="top">Treat <em>sizeof(int)</em>
     *                     <sup>1</sup> successive
     *           characters as a signed native integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">L</td>
     *   <td valign="top">Treat four<sup>1</sup> successive
     *           characters as an unsigned native
     *           long integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">l</td>
     *   <td valign="top">Treat four<sup>1</sup> successive
     *           characters as a signed native
     *           long integer.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">M</td>
     *   <td valign="top">Extract a quoted-printable string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">m</td>
     *   <td valign="top">Extract a base64 encoded string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">N</td>
     *   <td valign="top">Treat four characters as an unsigned long in network
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">n</td>
     *   <td valign="top">Treat two characters as an unsigned short in network
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">P</td>
     *   <td valign="top">Treat <em>sizeof(char *)</em> characters as a pointer, and
     *           return <em>len</em> characters from the referenced location.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">p</td>
     *   <td valign="top">Treat <em>sizeof(char *)</em> characters as a pointer to a
     *           null-terminated string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">S</td>
     *   <td valign="top">Treat two<sup>1</sup> successive characters as an unsigned
     *           short in
     *           native byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">s</td>
     *   <td valign="top">Treat two<sup>1</sup> successive
     *           characters as a signed short in
     *           native byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">U</td>
     *   <td valign="top">Extract UTF-8 characters as unsigned integers.</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">u</td>
     *   <td valign="top">Extract a UU-encoded string.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">V</td>
     *   <td valign="top">Treat four characters as an unsigned long in little-endian
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">v</td>
     *   <td valign="top">Treat two characters as an unsigned short in little-endian
     *           byte order.</td>
     *   <td valign="top">Fixnum</td>
     * </tr>
     * <tr>
     *   <td valign="top">X</td>
     *   <td valign="top">Skip backward one character.</td>
     *   <td valign="top">---</td>
     * </tr>
     * <tr>
     *   <td valign="top">x</td>
     *   <td valign="top">Skip forward one character.</td>
     *   <td valign="top">---</td>
     * </tr>
     * <tr>
     *   <td valign="top">Z</td>
     *   <td valign="top">String with trailing nulls removed.</td>
     *   <td valign="top">String</td>
     * </tr>
     * <tr>
     *   <td valign="top">@</td>
     *   <td valign="top">Skip to the offset given by the length argument.</td>
     *   <td valign="top">---</td>
     * </tr>
     * <tr>
     *                   <td colspan="9" bgcolor="#ff9999" height="2"><img src="dot.gif" width="1" height="1"></td>
     *                 </tr>
     *               </table>
     * <P></P>
     *         <sup>1</sup>&nbsp;May be modified by appending ``_'' to the directive.
     * <P></P>
     *       </td>
     *           </tr>
     *         </table>
     *
     **/
    public static RubyArray unpack(String value, RubyString iFmt) {
        Ruby ruby = iFmt.getRuntime();

        char[] lFmt = iFmt.getValue().toCharArray();
        int lFmtLength = lFmt.length;
        RubyArray lResult = RubyArray.newArray(ruby);
        int lValueLength = value.length();
        int lCurValueIdx = 0;
        for (int i = 0; i < lFmtLength;) {
            int lLength = 0;
            char lType = lFmt[i++];
            char lNext = i < lFmtLength ? lFmt[i] : '\0';
            if (lNext == '_' || lNext == '!') {
                if (sNatStr.indexOf(lType) != -1) {
                    i++;
                    lNext = i < lFmtLength ? lFmt[i] : '\0';
                } else
                    throw new ArgumentError(ruby, "'" + lNext + "' allowed only after types " + sNatStr);

            }
            if (i > lFmtLength)
                lLength = 1;
            else if (lNext == '*') {
                lLength = lValueLength - lCurValueIdx;
                i++;
                lNext = i < lFmtLength ? lFmt[i] : '\0';
            } else if (Character.isDigit(lNext)) {
                int lEndIndex = i;
                for (; lEndIndex < lFmtLength; lEndIndex++)
                    if (!Character.isDigit(lFmt[lEndIndex]))
                        break;
                lLength = Integer.parseInt(new String(lFmt, i, lEndIndex - i));
                //an exception may occur here if an int can't hold this but ...
                i = lEndIndex;
                lNext = (i < lFmtLength) ? lFmt[i] : '\0';
            } else {
                lLength = lType == '@' ? 0 : 1;
            }
            switch (lType) {
                case '%' :
                    throw new ArgumentError(ruby, "% is not supported");
                case 'A' :
                    if (lLength > (lValueLength - lCurValueIdx))
                        lLength = (lValueLength - lCurValueIdx);
                    {
                        int end = lLength;
                        for (int t = lCurValueIdx + lLength - 1; lLength > 0; lLength--, t--)
                            if (value.charAt(t) != ' ' && value.charAt(t) != '\0')
                                break;
                        lResult.append(
                            RubyString.newString(ruby, value.substring(lCurValueIdx, lCurValueIdx + lLength)));
                        lCurValueIdx += end;
                    }

                    break;

                case 'Z' :
                    if (lLength > (lValueLength - lCurValueIdx))
                        lLength = (lValueLength - lCurValueIdx);
                    {
                        int end = lLength;
                        for (int t = lCurValueIdx + lLength - 1; lLength > 0; lLength--, t--)
                            if (value.charAt(t) != '\0')
                                break;
                        lResult.append(
                            RubyString.newString(ruby, value.substring(lCurValueIdx, lCurValueIdx + lLength)));
                        lCurValueIdx += end;
                    }
                    break;

                case 'a' :
                    if (lLength > (lValueLength - lCurValueIdx))
                        lLength = (lValueLength - lCurValueIdx);
                    lResult.append(RubyString.newString(ruby, value.substring(lCurValueIdx, lCurValueIdx + lLength)));
                    lCurValueIdx += lLength;
                    break;

                case 'b' :
                    {
                        if (lFmt[i - 1] == '*' || lLength > (lValueLength - lCurValueIdx) * 8)
                            lLength = (lValueLength - lCurValueIdx) * 8;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(lLength);
                        for (int lCurByte = 0; lCurByte < lLength; lCurByte++) {
                            if ((lCurByte & 7) != 0)
                                bits >>>= 1;
                            else
                                bits = value.charAt(lCurValueIdx++);
                            lElem.append((bits & 1) != 0 ? '1' : '0');
                        }
                        lResult.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;

                case 'B' :
                    {
                        if (lFmt[i - 1] == '*' || lLength > (lValueLength - lCurValueIdx) * 8)
                            lLength = (lValueLength - lCurValueIdx) * 8;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(lLength);
                        for (int lCurByte = 0; lCurByte < lLength; lCurByte++) {
                            if ((lCurByte & 7) != 0)
                                bits <<= 1;
                            else
                                bits = value.charAt(lCurValueIdx++);
                            lElem.append((bits & 128) != 0 ? '1' : '0');
                        }
                        lResult.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'h' :
                    {
                        if (lFmt[i - 1] == '*' || lLength > (lValueLength - lCurValueIdx) * 2)
                            lLength = (lValueLength - lCurValueIdx) * 2;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(lLength);
                        for (int lCurByte = 0; lCurByte < lLength; lCurByte++) {
                            if ((lCurByte & 1) != 0)
                                bits >>>= 4;
                            else
                                bits = value.charAt(lCurValueIdx++);
                            lElem.append(sHexDigits[bits & 15]);
                        }
                        lResult.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'H' :
                    {
                        if (lFmt[i - 1] == '*' || lLength > (lValueLength - lCurValueIdx) * 2)
                            lLength = (lValueLength - lCurValueIdx) * 2;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(lLength);
                        for (int lCurByte = 0; lCurByte < lLength; lCurByte++) {
                            if ((lCurByte & 1) != 0)
                                bits <<= 4;
                            else
                                bits = value.charAt(lCurValueIdx++);
                            lElem.append(sHexDigits[(bits >>> 4) & 15]);
                        }
                        lResult.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'c' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx)) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx);
                            lLength = (lValueLength - lCurValueIdx);
                        }
                        for (; lLength-- > 0;) {
                            int c = value.charAt(lCurValueIdx++);
                            if (c > (char) 127)
                                c -= 256;
                            lResult.append(RubyFixnum.newFixnum(ruby, c));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }

                    break;
                case 'C' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx)) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx);
                            lLength = (lValueLength - lCurValueIdx);
                        }
                        for (; lLength-- > 0;) {
                            int c = value.charAt(lCurValueIdx++);
                            lResult.append(RubyFixnum.newFixnum(ruby, c));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }
                    break;
                case 'd':
                	{
                		int lPadLength = 0;
                		if (lLength > (lValueLength - lCurValueIdx) / 8) {
                			if (lFmt[i - 1] != '*')
                				lPadLength = lLength - (lValueLength - lCurValueIdx) / 8;
                			lLength = (lValueLength - lCurValueIdx) / 8;
                		}
                		for (; lLength-- > 0;) {
                			long l = retrieveLong(value, lCurValueIdx);
                			lCurValueIdx += 8;
                			double d = Double.longBitsToDouble(l);

                			lResult.append(RubyFloat.newFloat(ruby, d));
                		}
                		for (; lPadLength-- > 0;)
                			lResult.append(ruby.getNil());
                	}
                	break;
                case 's' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx) / 2) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx) / 2;
                            lLength = (lValueLength - lCurValueIdx) / 2;
                        }
                        for (; lLength-- > 0;) {
                            short tmp = (short) (value.charAt(lCurValueIdx++) & 0xff);
                            short s = (short) (value.charAt(lCurValueIdx++) & 0xff);
                            s <<= 8;
                            s |= tmp;
                            lResult.append(RubyFixnum.newFixnum(ruby, s));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }

                    break;

                case 'S' :
                case 'v' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx) / 2) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx) / 2;
                            lLength = (lValueLength - lCurValueIdx) / 2;
                        }
                        for (; lLength-- > 0;) {
                            short tmp = (short) (value.charAt(lCurValueIdx++) & 0xff);
                            int s = (short) (value.charAt(lCurValueIdx++) & 0xff);
                            s <<= 8;
                            s |= tmp;
                            lResult.append(RubyFixnum.newFixnum(ruby, s));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }
                    break;

                case 'i' :
                case 'l' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx) / 4) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx) / 4;
                            lLength = (lValueLength - lCurValueIdx) / 4;
                        }
                        for (; lLength-- > 0;) {
                        	int ri = retrieveInt(value, lCurValueIdx);
                        	lCurValueIdx += 4;
                        	
                            lResult.append(RubyFixnum.newFixnum(ruby, ri));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }

                    break;

                case 'I' :
                case 'V' :
                case 'L' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx) / 4) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx) / 4;
                            lLength = (lValueLength - lCurValueIdx) / 4;
                        }
                        for (; lLength-- > 0;) {
                            int i1 = (value.charAt(lCurValueIdx++) & 0xff);
                            int i2 = (value.charAt(lCurValueIdx++) & 0xff);
                            int i3 = (value.charAt(lCurValueIdx++) & 0xff);
                            long i4 = (value.charAt(lCurValueIdx++) & 0xff);
                            i4 <<= 24;
                            i4 |= (i3 << 16);
                            i4 |= (i2 << 8);
                            i4 |= i1;
                            lResult.append(RubyFixnum.newFixnum(ruby, i4));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }
                    break;
                case 'N' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx) / 4) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx) / 4;
                            lLength = (lValueLength - lCurValueIdx) / 4;
                        }
                        for (; lLength-- > 0;) {
                            long i1 = (value.charAt(lCurValueIdx++) & 0xff);
                            i1 <<= 8;
                            i1 |= (value.charAt(lCurValueIdx++) & 0xff);
                            i1 <<= 8;
                            i1 |= (value.charAt(lCurValueIdx++) & 0xff);
                            i1 <<= 8;
                            i1 |= (value.charAt(lCurValueIdx++) & 0xff);
                            lResult.append(RubyFixnum.newFixnum(ruby, i1));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }
                    break;
                case 'n' :
                    {
                        int lPadLength = 0;
                        if (lLength > (lValueLength - lCurValueIdx) / 2) {
                            if (lFmt[i - 1] != '*')
                                lPadLength = lLength - (lValueLength - lCurValueIdx) / 2;
                            lLength = (lValueLength - lCurValueIdx) / 2;
                        }
                        for (; lLength-- > 0;) {
                            int i1 = (value.charAt(lCurValueIdx++) & 0xff);
                            i1 <<= 8;
                            i1 |= (value.charAt(lCurValueIdx++) & 0xff);
                            lResult.append(RubyFixnum.newFixnum(ruby, i1));
                        }
                        for (; lPadLength-- > 0;)
                            lResult.append(ruby.getNil());
                    }
                    break;
                case 'U' :
                    {
                        if (lLength > lValueLength - lCurValueIdx)
                            lLength = lValueLength - lCurValueIdx;
                        //get the correct substring
                        String toUnpack = value.substring(lCurValueIdx);
                        String lUtf8 = null;
                        try {
                            lUtf8 = new String(toUnpack.getBytes("iso8859-1"), "UTF-8");
                        } catch (java.io.UnsupportedEncodingException e) {
                            Asserts.notReached("can't convert from UTF8");
                        }
                        char[] c = lUtf8.toCharArray();
                        int lNbChar = c.length;
                        for (int lCurCharIdx = 0; lLength-- > 0 && lCurCharIdx < lNbChar; lCurCharIdx++)
                            lResult.append(RubyFixnum.newFixnum(ruby, c[lCurCharIdx]));
                    }
                    break;
                 case 'x':
                 	{
                 		if (lLength > (lValueLength - lCurValueIdx))
                 			lLength = (lValueLength - lCurValueIdx);
                 		lCurValueIdx+=lLength;
                 	}
                 	break;
                 
            }
        }
        return lResult;
    }

 
	/**
     * shrinks a stringbuffer.
     * shrinks a stringbuffer by a number of characters.
     * @param i2Shrink the stringbuffer
     * @param iLength how much to shrink
     * @return the stringbuffer
     **/
    private static final StringBuffer shrink(StringBuffer i2Shrink, int iLength) {
        iLength = i2Shrink.length() - iLength;
        i2Shrink.setLength(iLength);
        return i2Shrink;
    }

    /**
     * grows a stringbuffer.
     * uses the Strings to pad the buffer for a certain length
     * @param i2Grow the buffer to grow
     * @param iPads the string used as padding
     * @param iLength how much padding is needed
     * @return the padded buffer
     **/
    private static final StringBuffer grow(StringBuffer i2Grow, String iPads, int iLength) {
        int lPadLength = iPads.length();
        while (iLength >= lPadLength) {
            i2Grow.append(iPads);
            iLength -= lPadLength;
        }
        i2Grow.append(iPads.substring(0, iLength));
        return i2Grow;
    }

    /**
     * pack_pack
     *
     * Template characters for Array#pack Directive  Meaning
     * 	         <table class="codebox" cellspacing="0" border="0" cellpadding="3">
     * <tr bgcolor="#ff9999">
     *   <td valign="top">
     *                     <b>Directive</b>
     *                   </td>
     *   <td valign="top">
     *                     <b>Meaning</b>
     *                   </td>
     * </tr>
     * <tr>
     *   <td valign="top">@</td>
     *   <td valign="top">Moves to absolute position</td>
     * </tr>
     * <tr>
     *   <td valign="top">A</td>
     *   <td valign="top">ASCII string (space padded, count is width)</td>
     * </tr>
     * <tr>
     *   <td valign="top">a</td>
     *   <td valign="top">ASCII string (null padded, count is width)</td>
     * </tr>
     * <tr>
     *   <td valign="top">B</td>
     *   <td valign="top">Bit string (descending bit order)</td>
     * </tr>
     * <tr>
     *   <td valign="top">b</td>
     *   <td valign="top">Bit string (ascending bit order)</td>
     * </tr>
     * <tr>
     *   <td valign="top">C</td>
     *   <td valign="top">Unsigned char</td>
     * </tr>
     * <tr>
     *   <td valign="top">c</td>
     *   <td valign="top">Char</td>
     * </tr>
     * <tr>
     *   <td valign="top">d</td>
     *   <td valign="top">Double-precision float, native format</td>
     * </tr>
     * <tr>
     *   <td valign="top">E</td>
     *   <td valign="top">Double-precision float, little-endian byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">e</td>
     *   <td valign="top">Single-precision float, little-endian byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">f</td>
     *   <td valign="top">Single-precision float, native format</td>
     * </tr>
     * <tr>
     *   <td valign="top">G</td>
     *   <td valign="top">Double-precision float, network (big-endian) byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">g</td>
     *   <td valign="top">Single-precision float, network (big-endian) byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">H</td>
     *   <td valign="top">Hex string (high nibble first)</td>
     * </tr>
     * <tr>
     *   <td valign="top">h</td>
     *   <td valign="top">Hex string (low nibble first)</td>
     * </tr>
     * <tr>
     *   <td valign="top">I</td>
     *   <td valign="top">Unsigned integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">i</td>
     *   <td valign="top">Integer</td>
     * </tr>
     * <tr>
     *   <td valign="top">L</td>
     *   <td valign="top">Unsigned long</td>
     * </tr>
     * <tr>
     *   <td valign="top">l</td>
     *   <td valign="top">Long</td>
     * </tr>
     * <tr>
     *   <td valign="top">M</td>
     *   <td valign="top">Quoted printable, MIME encoding (see RFC2045)</td>
     * </tr>
     * <tr>
     *   <td valign="top">m</td>
     *   <td valign="top">Base64 encoded string</td>
     * </tr>
     * <tr>
     *   <td valign="top">N</td>
     *   <td valign="top">Long, network (big-endian) byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">n</td>
     *   <td valign="top">Short, network (big-endian) byte-order</td>
     * </tr>
     * <tr>
     *   <td valign="top">P</td>
     *   <td valign="top">Pointer to a structure (fixed-length string)</td>
     * </tr>
     * <tr>
     *   <td valign="top">p</td>
     *   <td valign="top">Pointer to a null-terminated string</td>
     * </tr>
     * <tr>
     *   <td valign="top">S</td>
     *   <td valign="top">Unsigned short</td>
     * </tr>
     * <tr>
     *   <td valign="top">s</td>
     *   <td valign="top">Short</td>
     * </tr>
     * <tr>
     *   <td valign="top">U</td>
     *   <td valign="top">UTF-8</td>
     * </tr>
     * <tr>
     *   <td valign="top">u</td>
     *   <td valign="top">UU-encoded string</td>
     * </tr>
     * <tr>
     *   <td valign="top">V</td>
     *   <td valign="top">Long, little-endian byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">v</td>
     *   <td valign="top">Short, little-endian byte order</td>
     * </tr>
     * <tr>
     *   <td valign="top">X</td>
     *   <td valign="top">Back up a byte</td>
     * </tr>
     * <tr>
     *   <td valign="top">x</td>
     *   <td valign="top">Null byte</td>
     * </tr>
     * <tr>
     *   <td valign="top">Z</td>
     *   <td valign="top">Same as ``A''</td>
     * </tr>
     * <tr>
     *                   <td colspan="9" bgcolor="#ff9999" height="2"><img src="dot.gif" width="1" height="1"></td>
     *                 </tr>
     *               </table>
     *
     *
     * Packs the contents of arr into a binary sequence according to the directives in
     * aTemplateString (see preceding table).
     * Directives ``A,'' ``a,'' and ``Z'' may be followed by a count, which gives the
     * width of the resulting field.
     * The remaining directives also may take a count, indicating the number of array
     * elements to convert.
     * If the count is an asterisk (``*''), all remaining array elements will be
     * converted.
     * Any of the directives ``sSiIlL'' may be followed by an underscore (``_'') to use
     * the underlying platform's native size for the specified type; otherwise, they
     * use a platform-independent size. Spaces are ignored in the template string.
     * @see RubyString#unpack
     **/
    public static RubyString pack(ArrayList list, RubyString iFmt) {
        Ruby ruby = iFmt.getRuntime();

        char[] lFmt = iFmt.getValue().toCharArray();
        int lFmtLength = lFmt.length;
        int idx = 0;
        int lLeftInArray = list.size();
        StringBuffer lResult = new StringBuffer();
        IRubyObject lFrom;
        String lCurElemString;
        for (int i = 0; i < lFmtLength;) {
            int lLength = 1;
            //first skip all spaces
            char lType = lFmt[i++];
            if (Character.isWhitespace(lType))
                continue;
            char lNext = i < lFmtLength ? lFmt[i] : '\0';
            if (lNext == '!' || lNext == '_') {
                if (sNatStr.indexOf(lType) != -1) {
                    lNext = ++i < lFmtLength ? lFmt[i] : '\0';
                } else
                    throw new ArgumentError(ruby, "'" + lNext + "' allowed only after types " + sNatStr);
            }
            if (lNext == '*') {
                lLength = "@Xxu".indexOf(lType) == -1 ? lLeftInArray : 0;
                lNext = ++i < lFmtLength ? lFmt[i] : '\0';
            } else if (Character.isDigit(lNext)) {
                int lEndIndex = i;
                for (; lEndIndex < lFmtLength; lEndIndex++)
                    if (!Character.isDigit(lFmt[lEndIndex]))
                        break;
                lLength = Integer.parseInt(new String(lFmt, i, lEndIndex - i));
                //an exception may occur here if an int can't hold this but ...
                i = lEndIndex;
                lNext = i < lFmtLength ? lFmt[i] : '\0';
            } //no else, the original value of length is correct
            switch (lType) {
                case '%' :
                    throw new ArgumentError(ruby, "% is not supported");

                case 'A' :
                case 'a' :
                case 'Z' :
                case 'B' :
                case 'b' :
                case 'H' :
                case 'h' :
                    if (lLeftInArray-- > 0)
                        lFrom = (IRubyObject) list.get(idx++);
                    else
                        throw new ArgumentError(ruby, sTooFew);
                    if (lFrom == ruby.getNil())
                        lCurElemString = "";
                    else
                        lCurElemString = convert2String(lFrom);
                    if (lFmt[i - 1] == '*')
                        lLength = lCurElemString.length();
                    switch (lType) {
                        case 'a' :
                        case 'A' :
                        case 'Z' :
                            if (lCurElemString.length() >= lLength)
                                lResult.append(lCurElemString.toCharArray(), 0, lLength);
                            else //need padding
                                { //I'm fairly sure there is a library call to create a
                                //string filled with a given char with a given length but I couldn't find it
                                lResult.append(lCurElemString);
                                lLength -= lCurElemString.length();
                                grow(lResult, (lType == 'a') ? sNil10 : sSp10, lLength);
                            }
                            break;

                            //I believe there is a bug in the b and B case we skip a char too easily
                        case 'b' :
                            {
                                int lByte = 0;
                                int lIndex = 0;
                                char lCurChar;
                                int lPadLength = 0;
                                if (lLength > lCurElemString.length()) { //I don't understand this, why divide by 2
                                    lPadLength = (lLength - lCurElemString.length() + 1) / 2;
                                    lLength = lCurElemString.length();
                                }
                                for (lIndex = 0; lIndex < lLength;) {
                                    lCurChar = lCurElemString.charAt(lIndex++);
                                    if ((lCurChar & 1) != 0) //if the low bit of the current char is set
                                        lByte |= 128; //set the high bit of the result
                                    if ((lIndex & 7) != 0)
                                        //if the index is not a multiple of 8, we are not on a byte boundary
                                        lByte >>= 1; //shift the byte
                                    else { //we are done with one byte, append it to the result and go for the next
                                        lResult.append((char) (lByte & 0xff));
                                        lByte = 0;
                                    }
                                }
                                if ((lLength & 7) != 0) //if the length is not a multiple of 8
                                    { //we need to pad the last byte
                                    lByte >>= 7 - (lLength & 7);
                                    lResult.append((char) (lByte & 0xff));
                                }
                                //do some padding, I don't understand the padding strategy
                                lLength = lResult.length();
                                lResult.setLength(lLength + lPadLength);
                            }
                            break;

                        case 'B' :
                            {
                                int lByte = 0;
                                int lIndex = 0;
                                char lCurChar;
                                int lPadLength = 0;
                                if (lLength > lCurElemString.length()) { //I don't understand this, why divide by 2
                                    lPadLength = (lLength - lCurElemString.length() + 1) / 2;
                                    lLength = lCurElemString.length();
                                }
                                for (lIndex = 0; lIndex < lLength;) {
                                    lCurChar = lCurElemString.charAt(lIndex++);
                                    lByte |= lCurChar & 1;
                                    if ((lIndex & 7) != 0)
                                        //if the index is not a multiple of 8, we are not on a byte boundary
                                        lByte <<= 1; //shift the byte
                                    else { //we are done with one byte, append it to the result and go for the next
                                        lResult.append((char) (lByte & 0xff));
                                        lByte = 0;
                                    }
                                }
                                if ((lLength & 7) != 0) //if the length is not a multiple of 8
                                    { //we need to pad the last byte
                                    lByte <<= 7 - (lLength & 7);
                                    lResult.append((char) (lByte & 0xff));
                                }
                                //do some padding, I don't understand the padding strategy
                                lLength = lResult.length();
                                lResult.setLength(lLength + lPadLength);
                            }
                            break;

                        case 'h' :
                            {
                                int lByte = 0;
                                int lIndex = 0;
                                char lCurChar;
                                int lPadLength = 0;
                                if (lLength > lCurElemString.length()) { //I don't undestand this why divide by 2
                                    lPadLength = (lLength - lCurElemString.length() + 1) / 2;
                                    lLength = lCurElemString.length();
                                }
                                for (lIndex = 0; lIndex < lLength;) {
                                    lCurChar = lCurElemString.charAt(lIndex++);
                                    if (Character.isJavaIdentifierStart(lCurChar))
                                        //this test may be too lax but it is the same as in MRI
                                        lByte |= (((lCurChar & 15) + 9) & 15) << 4;
                                    else
                                        lByte |= (lCurChar & 15) << 4;
                                    if ((lIndex & 1) != 0)
                                        lByte >>= 4;
                                    else {
                                        lResult.append((char) (lByte & 0xff));
                                        lByte = 0;
                                    }
                                }
                                if ((lLength & 1) != 0) {
                                    lResult.append((char) (lByte & 0xff));
                                }

                                //do some padding, I don't understand the padding strategy
                                lLength = lResult.length();
                                lResult.setLength(lLength + lPadLength);
                            }
                            break;

                        case 'H' :
                            {
                                int lByte = 0;
                                int lIndex = 0;
                                char lCurChar;
                                int lPadLength = 0;
                                if (lLength > lCurElemString.length()) { //I don't undestand this why divide by 2
                                    lPadLength = (lLength - lCurElemString.length() + 1) / 2;
                                    lLength = lCurElemString.length();
                                }
                                for (lIndex = 0; lIndex < lLength;) {
                                    lCurChar = lCurElemString.charAt(lIndex++);
                                    if (Character.isJavaIdentifierStart(lCurChar))
                                        //this test may be too lax but it is the same as in MRI
                                        lByte |= ((lCurChar & 15) + 9) & 15;
                                    else
                                        lByte |= (lCurChar & 15);
                                    if ((lIndex & 1) != 0)
                                        lByte <<= 4;
                                    else {
                                        lResult.append((char) (lByte & 0xff));
                                        lByte = 0;
                                    }
                                }
                                if ((lLength & 1) != 0) {
                                    lResult.append((char) (lByte & 0xff));
                                }

                                //do some padding, I don't understand the padding strategy
                                lLength = lResult.length();
                                lResult.setLength(lLength + lPadLength);
                            }
                            break;
                    }
                    break;

                case 'x' :
                    grow(lResult, sNil10, lLength);
                    break;

                case 'X' :
                    shrink(lResult, lLength);
                    break;

                case '@' :
                    lLength -= lResult.length();
                    if (lLength > 0)
                        grow(lResult, sNil10, lLength);
                    lLength = -lLength;
                    if (lLength > 0)
                        shrink(lResult, lLength);
                    break;

                case 'c' :
                case 'C' :
                    while (lLength-- > 0) {
                        char c;
                        if (lLeftInArray-- > 0)
                            lFrom = (IRubyObject) list.get(idx++);
                        else
                            throw new ArgumentError(ruby, sTooFew);
                        if (lFrom == ruby.getNil())
                            c = 0;
                        else {
                            c = (char) (RubyNumeric.num2long(lFrom) & 0xff);
                        }
                        lResult.append(c);
                    }
                    break;
                case 'd':
                	while (lLength-- > 0) {
                		long d;
                		if (lLeftInArray-- > 0)
                			lFrom = (IRubyObject) list.get(idx++);
                		else
                			throw new ArgumentError(ruby, sTooFew);
                		if (lFrom == ruby.getNil())
                			d = 0;
                		else {
                			d = Double.doubleToLongBits(RubyNumeric.numericValue(lFrom).getDoubleValue());
                		}
                		appendInt(lResult, (int) (d & 0xffffffff));
                		appendInt(lResult, (int) (d >>> 32));
                	}
                	break;
                case 's' :
                case 'v' :
                case 'S' :
                    while (lLength-- > 0) {
                        int s;
                        if (lLeftInArray-- > 0)
                            lFrom = (IRubyObject) list.get(idx++);
                        else
                            throw new ArgumentError(ruby, sTooFew);
                        if (lFrom == ruby.getNil())
                            s = 0;
                        else {
                            s = (int) (RubyNumeric.num2long(lFrom) & 0xffff);
                        }
                        lResult.append((char) (s & 0xff));
                        lResult.append((char) ((s & 0xff00) >> 8));

                    }
                    break;
                case 'n' :
                    while (lLength-- > 0) {
                        int s;
                        if (lLeftInArray-- > 0)
                            lFrom = (IRubyObject) list.get(idx++);
                        else
                            throw new ArgumentError(ruby, sTooFew);
                        if (lFrom == ruby.getNil())
                            s = 0;
                        else {
                            s = (int) (RubyNumeric.num2long(lFrom) & 0xffff);
                        }
                        lResult.append((char) ((s & 0xff00) >> 8));
                        lResult.append((char) (s & 0xff));

                    }
                    break;

                case 'i' :
                case 'I' :
                case 'l' :
                case 'L' :
                case 'V' :
                    while (lLength-- > 0) {
                        if (lLeftInArray-- > 0)
                            lFrom = (IRubyObject) list.get(idx++);
                        else
                            throw new ArgumentError(ruby, sTooFew);
                        
                        int s = (lFrom == ruby.getNil() ? 
							     0 : (int) (RubyNumeric.num2long(lFrom)));
                        appendInt(lResult, s);
                    }
                    break;
                case 'N' :
                    while (lLength-- > 0) {
                        int s;
                        if (lLeftInArray-- > 0)
                            lFrom = (IRubyObject) list.get(idx++);
                        else
                            throw new ArgumentError(ruby, sTooFew);
                        if (lFrom == ruby.getNil())
                            s = 0;
                        else {
                            s = (int) (RubyNumeric.num2long(lFrom));
                        }
                        lResult.append((char) ((s >> 24) & 0xff));
                        lResult.append((char) ((s >> 16) & 0xff));
                        lResult.append((char) ((s >> 8) & 0xff));
                        lResult.append((char) (s & 0xff));

                    }
                    break;
                case 'u' :
                case 'm' :
                    if (lLeftInArray-- > 0)
                        lFrom = (IRubyObject) list.get(idx++);
                    else
                        throw new ArgumentError(ruby, sTooFew);
                    if (lFrom == ruby.getNil())
                        lCurElemString = "";
                    else
                        lCurElemString = convert2String(lFrom);

                    if (lLength <= 2)
                        lLength = 45;
                    else
                        lLength = lLength / 3 * 3;
                    for (;;) {
                        encodes(ruby, lResult, lCurElemString, lLength, lType);
                        if (lLength < lCurElemString.length())
                            lCurElemString = lCurElemString.substring(lLength);
                        else
                            break;
                    }
                    break;

                case 'M' :
                    if (lLeftInArray-- > 0)
                        lFrom = (IRubyObject) list.get(idx++);
                    else
                        throw new ArgumentError(ruby, sTooFew);
                    if (lFrom == ruby.getNil())
                        lCurElemString = "";
                    else
                        lCurElemString = convert2String(lFrom);

                    if (lLength <= 1)
                        lLength = 72;
                    qpencode(lResult, lCurElemString, lLength);
                    break;

                case 'U' :
                    char[] c = new char[lLength];
                    for (int lCurCharIdx = 0; lLength-- > 0; lCurCharIdx++) {
                        long l;
                        if (lLeftInArray-- > 0)
                            lFrom = (IRubyObject) list.get(idx++);
                        else
                            throw new ArgumentError(ruby, sTooFew);

                        if (lFrom == ruby.getNil())
                            l = 0;
                        else {
                            l = RubyNumeric.num2long(lFrom);
                        }
                        c[lCurCharIdx] = (char) l;
                    }
                    String s = new String(c);
                    try {
                        lResult.append(RubyString.bytesToString(s.getBytes("UTF-8")));
                    } catch (java.io.UnsupportedEncodingException e) {
                        Asserts.notReached("can't convert to UTF8");
                    }
                    break;
            }
        }
        return RubyString.newString(ruby, lResult.toString());
    }

	/**
	 * Append a packed integer representation onto end of a stringbuffer.
	 *  
	 * @param result to be appended to
	 * @param s the integer to encode
	 */
	private static void appendInt(StringBuffer result, int s) {
		result.append((char) (s & 0xff));
		result.append((char) ((s >> 8) & 0xff));
		result.append((char) ((s >> 16) & 0xff));
		result.append((char) ((s >> 24) & 0xff));
	}
	
	/**
	 * Retrieve an encoded int starting at index in the string value.
	 *  
	 * @param value to get Into from
	 * @param index where encoded int starts at
	 * @return the decoded integer
	 */
	private static int retrieveInt(String value, int index) {
		int i1 = (value.charAt(index) & 0xff);
		int i2 = (value.charAt(index + 1) & 0xff);
		int i3 = (value.charAt(index + 2) & 0xff);
		int i4 = (value.charAt(index + 3) & 0xff);
		
		i4 <<= 24;
		i4 |= (i3 << 16);
		i4 |= (i2 << 8);
		i4 |= i1;
		
		return i4;
	}
	
	private static long retrieveLong(String value, int index) {
		int c1 = retrieveInt(value, index);
		int c2 = retrieveInt(value, index + 4);
		
		return  (c1 & 0xffffffffL) + ((long) c2 << 32);
	}
}
