/*
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
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
import java.util.HashMap;

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
    private static final int IS_STAR = -1;
    /** Native pack type.
     **/
    private static final String NATIVE_CODES = "sSiIlL";
    private static final String sTooFew = "too few arguments";
    private static final char hex_table[] = "0123456789ABCDEF".toCharArray();
    private static final char[] uu_table =
        "`!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_".toCharArray();
    private static final char[] b64_table =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
    private static final char[] sHexDigits = "0123456789abcdef0123456789ABCDEFx".toCharArray();
    private static HashMap converters = new HashMap();
    
    static {
        // short, little-endian (network)
        converters.put(new Character('v'), new Converter(2) { 
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, 
                        decodeShortUnsignedLittleEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
            	int s = (o == ruby.getNil() ? 0 :
						 (int) (RubyNumeric.num2long(o) & 0xffff));
           		encodeShortLittleEndian(result, s);
           	}});
    	// single precision, little-endian
        converters.put(new Character('e'), new Converter(4) { 
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFloat.newFloat(ruby, decodeFloatLittleEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                float f = (o == ruby.getNil() ? 0 :
                    (float)RubyNumeric.numericValue(o).getDoubleValue());
                encodeFloatLittleEndian(result, f);
            }});
        Converter tmp = new Converter(4) {
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFloat.newFloat(ruby, decodeFloatBigEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                float f = (o == ruby.getNil() ? 0 :
                    (float)RubyNumeric.numericValue(o).getDoubleValue());
                encodeFloatBigEndian(result, f);
            }
        };
        converters.put(new Character('f'), tmp); // single precision, native
        converters.put(new Character('g'), tmp); // single precision, native
        // double precision, little-endian
        converters.put(new Character('E'), new Converter(8) { 
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFloat.newFloat(ruby, decodeDoubleLittleEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                double d = (o == ruby.getNil() ? 0 :
                    RubyNumeric.numericValue(o).getDoubleValue());
                encodeDoubleLittleEndian(result, d);
            }});
        tmp = new Converter(8) {
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFloat.newFloat(ruby, decodeDoubleBigEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                double d = (o == ruby.getNil() ? 0 :
                    RubyNumeric.numericValue(o).getDoubleValue());
                encodeDoubleBigEndian(result, d);
            }
        }; 
        converters.put(new Character('d'), tmp); // double precision native
        converters.put(new Character('G'), tmp); // double precision bigendian 
        converters.put(new Character('s'), new Converter(2) { // signed short
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, decodeShortBigEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                int s = (o == ruby.getNil() ? 0 :
                    (int) (RubyNumeric.num2long(o) & 0xffff));
                encodeShortBigEndian(result, s);
            }});
        tmp = new Converter(2) {
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, 
                        decodeShortUnsignedBigEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                int s = (o == ruby.getNil() ? 0 :
                    (int) (RubyNumeric.num2long(o) & 0xffff));
                encodeShortBigEndian(result, s);
            }
        };
        converters.put(new Character('S'), tmp); // unsigned short
        converters.put(new Character('n'), tmp); // short network
        converters.put(new Character('c'), new Converter(1) { // signed char
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                int c = enc.nextChar();
                return RubyFixnum.newFixnum(ruby, c > (char) 127 ? c-256 : c);
            }	
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                char c = (o == ruby.getNil() ? 0 :
                    (char) (RubyNumeric.num2long(o) & 0xff));
                result.append(c);
            }});
        converters.put(new Character('C'), new Converter(1) { // unsigned char
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, (int)enc.nextChar());
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                char c = (o == ruby.getNil() ? 0 :
                    (char) (RubyNumeric.num2long(o) & 0xff));
                result.append(c);
            }});
        // long, little-endian 
        converters.put(new Character('V'), new Converter(4) { 
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, 
                        decodeIntUnsignedLittleEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                int s = (o == ruby.getNil() ? 0 : 
                    (int) (RubyNumeric.num2long(o)));
                encodeIntLittleEndian(result, s);
            }});
        tmp = new Converter(4) {
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, 
                        decodeIntUnsignedBigEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                int s = (o == ruby.getNil() ? 0 : 
                    (int) (RubyNumeric.num2long(o)));
                encodeIntBigEndian(result, s);
            }
        };
        converters.put(new Character('I'), tmp); // unsigned int, native 
        converters.put(new Character('L'), tmp); // unsigned long (bugs?)
        converters.put(new Character('N'), tmp); // long, network
        tmp = new Converter(4) {
            public IRubyObject decode(Ruby ruby, PtrList enc) {
                return RubyFixnum.newFixnum(ruby, decodeIntBigEndian(enc));
            }
            public void encode(Ruby ruby, IRubyObject o, StringBuffer result){
                int s = (o == ruby.getNil() ? 0 : 
                    (int) (RubyNumeric.num2long(o)));
                encodeIntBigEndian(result, s);
            }
        };
        converters.put(new Character('l'), tmp); // long, native 
        converters.put(new Character('i'), tmp); // int, native 
    }

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
    public static RubyArray unpack(String encodedString, 
            RubyString formatString) {
        Ruby ruby = formatString.getRuntime();
        RubyArray result = RubyArray.newArray(ruby);
        PtrList format = new PtrList(ruby, formatString.getValue());
        PtrList encode = new PtrList(ruby, encodedString);
        char type = format.nextChar(); // Type to be unpacked
        
        while(format.isAtEnd() == false) {
            // Possible next type, format of current type, occurrences of type
            char next = format.nextChar();
            
            // Next indicates to decode using native encoding format
            if (next == '_' || next == '!') {
                if (NATIVE_CODES.indexOf(type) == -1) {
                	throw new ArgumentError(ruby, "'" + next + 
                	        "' allowed only after types " + NATIVE_CODES);
                } 
                // We advance in case occurences follows
                next = format.nextChar();
            }
            
            // How many occurrences of 'type' we want
            int occurrences = 0;
            if (format.isAtEnd()) {
            	occurrences = 1;
            } else if (next == '*') {
            	occurrences = IS_STAR;
				next = format.nextChar();
            } else if (Character.isDigit(next)) {
				format.backup(1);
				occurrences = format.nextAsciiNumber();
				next = format.nextChar();
            } else {
                occurrences = type == '@' ? 0 : 1;
            }

            // See if we have a converter for the job...
            Converter converter = (Converter) converters.get(new Character(type));
            if (converter != null) {
            	decode(ruby, encode, occurrences, result, converter);
            	type = next;
            	continue;
            }

            // Otherwise the unpack should be here...
            switch (type) {
            	case '@' :
            	    encode.setPosition(occurrences);
            	    break;
                case '%' :
                    throw new ArgumentError(ruby, "% is not supported");
                case 'A' :
                	{
                    if (occurrences == IS_STAR || occurrences > encode.remaining())
                        occurrences = encode.remaining();

                    String potential = encode.nextSubstring(occurrences);
                    
                    for (int t = occurrences - 1; occurrences > 0; occurrences--, t--) {
						char c = potential.charAt(t);
                        
                       	if (c != '\0' && c != ' ') {
                       		break;
                       	}
                    }
                    
                    potential = potential.substring(0, occurrences);
                    result.append(RubyString.newString(ruby, potential));
                    }
                    break;
                case 'Z' :
                	{
                    if (occurrences == IS_STAR || occurrences > encode.remaining())
                        occurrences = encode.remaining();
                    
                    String potential = encode.nextSubstring(occurrences);
                    
                    for (int t = occurrences - 1; occurrences > 0; occurrences--, t--) {
                    	char c = potential.charAt(t);
                        
                       	if (c != '\0') {
                       		break;
                       	}
                    }
                    
                    potential = potential.substring(0, occurrences);
                    result.append(RubyString.newString(ruby, potential));
                	}
                    break;
                case 'a' :
                    if (occurrences == IS_STAR || occurrences > encode.remaining())
                        occurrences = encode.remaining();
                    result.append(RubyString.newString(ruby, encode.nextSubstring(occurrences)));
                    break;
                case 'b' :
                    {
                        if (occurrences == IS_STAR || occurrences > encode.remaining() * 8)
                            occurrences = encode.remaining() * 8;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(occurrences);
                        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
                            if ((lCurByte & 7) != 0)
                                bits >>>= 1;
                            else
                                bits = encode.nextChar();
                            lElem.append((bits & 1) != 0 ? '1' : '0');
                        }
                        result.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'B' :
                    {
                        if (occurrences == IS_STAR || occurrences > encode.remaining() * 8)
                            occurrences = encode.remaining() * 8;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(occurrences);
                        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
                            if ((lCurByte & 7) != 0)
                                bits <<= 1;
                            else
                                bits = encode.nextChar();
                            lElem.append((bits & 128) != 0 ? '1' : '0');
                        }
                        
                        result.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'h' :
                    {
                        if (occurrences == IS_STAR || occurrences > encode.remaining() * 2)
                            occurrences = encode.remaining() * 2;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(occurrences);
                        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
                            if ((lCurByte & 1) != 0)
                                bits >>>= 4;
                            else
                                bits = encode.nextChar();
                            lElem.append(sHexDigits[bits & 15]);
                        }
                        result.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'H' :
                    {
                        if (occurrences == IS_STAR || occurrences > encode.remaining() * 2)
                            occurrences = encode.remaining() * 2;
                        int bits = 0;
                        StringBuffer lElem = new StringBuffer(occurrences);
                        for (int lCurByte = 0; lCurByte < occurrences; lCurByte++) {
                            if ((lCurByte & 1) != 0)
                                bits <<= 4;
                            else
                                bits = encode.nextChar();
                            lElem.append(sHexDigits[(bits >>> 4) & 15]);
                        }
                        result.append(RubyString.newString(ruby, lElem.toString()));
                    }
                    break;
                case 'U' :
                    {
                        if (occurrences == IS_STAR || occurrences > encode.remaining())
                            occurrences = encode.remaining();
                        //get the correct substring
                        String toUnpack = encode.nextSubstring(occurrences);
                        String lUtf8 = null;
                        try {
                            lUtf8 = new String(toUnpack.getBytes("iso8859-1"), "UTF-8");
                        } catch (java.io.UnsupportedEncodingException e) {
                            Asserts.notReached("can't convert from UTF8");
                        }
                        char[] c = lUtf8.toCharArray();
                        for (int lCurCharIdx = 0; occurrences-- > 0 && lCurCharIdx < c.length; lCurCharIdx++)
                            result.append(RubyFixnum.newFixnum(ruby, c[lCurCharIdx]));
                    }
                    break;
                 case 'X':
                     if (occurrences == IS_STAR) {
                         occurrences = encode.getLength() - encode.remaining();
                     }
                     
                     try {
                         encode.backup(occurrences);
                     } catch (IllegalArgumentException e) {
                         throw new ArgumentError(ruby, "in `unpack': X outside of string");
                     }
                     break;
                 case 'x':
              		if (occurrences == IS_STAR) {
               			occurrences = encode.remaining();
              		}
              		
              		try {
              		    encode.nextSubstring(occurrences);
              		} catch (IllegalArgumentException e) {
              		    throw new ArgumentError(ruby, "in `unpack': x outside of string");
              		}

                 	break;
            }
			type = next;
        }
        return result;
    }
    
    public static void decode(Ruby ruby, PtrList encode, int occurrences, 
            RubyArray result, Converter converter) {
        int lPadLength = 0;
    	
        if (occurrences == IS_STAR) {
            occurrences = encode.remaining() / converter.size;
        } else if (occurrences > encode.remaining() / converter.size) {
            lPadLength = occurrences - encode.remaining() / converter.size;
            occurrences = encode.remaining() / converter.size;
        }
        for (; occurrences-- > 0;) {
            result.append(converter.decode(ruby, encode));
        }
        for (; lPadLength-- > 0;)
            result.append(ruby.getNil());
    }
   
    public static int encode(Ruby ruby, int occurrences, StringBuffer result, 
            ArrayList list, int index, Converter converter) {
        int listSize = list.size();

        while (occurrences-- > 0) {
            if (listSize-- <= 0) {
                throw new ArgumentError(ruby, sTooFew);
            }

            IRubyObject from = (IRubyObject) list.get(index++);

            converter.encode(ruby, from, result);
        }

        return index;
    }
    
    public abstract static class Converter {
    	public int size;
    	
    	public Converter(int size) {
    		this.size = size;
    	}
    	
    	public abstract IRubyObject decode(Ruby ruby, PtrList format);
    	public abstract void encode(Ruby ruby, IRubyObject from, 
    	        StringBuffer result);
    }
 
    static class PtrList {
        private Ruby ruby;
        private char[] buffer; // List to be managed
        private int index; // Pointer location in list

        public PtrList(Ruby ruby, String bufferString) {
            this.ruby = ruby;
            buffer = bufferString.toCharArray();
            index = 0;
        }

        /**
         * @return the number of elements between pointer and end of list
         */
        public int remaining() {
            return buffer.length - index;
        }

		/**
		 * <p>Get substring from current point of desired length and advance
		 * pointer.</p>
		 * 
		 * @param length of substring
		 * @return the substring
		 */
		public String nextSubstring(int length) {
		    // Cannot get substring off end of buffer
		    if (index + length > buffer.length) {
		        throw new IllegalArgumentException();
		    }
		    
			String substring = new String(buffer, index, length);
			
			index += length;
			
			return substring;
		}
		
		public void setPosition(int position) {
		    if (position < buffer.length) {
		        index = position;
		    }
		}

		/**
		 * @return numerical representation of ascii number at ptr
		 */
		public int nextAsciiNumber() {
            int i = index;

            for (; i < buffer.length; i++) {
            	if (!Character.isDigit(buffer[i])) {
                    break;
				}
			}
            
            // An exception will occur if no number is at ptr....
            int number = Integer.parseInt(new String(buffer, index, i - index));
            
            // An exception may occur here if an int can't hold this but ...
            index = i;
			return number;
        }

		/**
		 * @return length of list
		 */
		public int getLength() {
			return buffer.length;
		}

		/**
		 * @return char at the pointer (advancing the pointer) or '\0' if at end.
		 *
		 * Note: the pointer gets advanced one past last character to indicate
		 * that the whole buffer has been read.
		 */
		public char nextChar() {
		    char next = '\0';
		    		    
		    if (index < buffer.length) {
		        next = buffer[index++];
		    } else if (index == buffer.length) {
		        index++;
		    }
		    		        
    		return next;
    	}
		
		/**
		 * @return low byte of the char
		 */
		public int nextByte() {
			return nextChar() & 0xff;
		}
		
		/**
		 * <p>Backup the pointer occurrences times.</p>
		 * 
		 * @throws IllegalArgumentException if it backs up past beginning 
		 * of buffer	
		 */
		public void backup(int occurrences) {
		    index -= occurrences;

		    if (index < 0) {
		        throw new IllegalArgumentException();
		    }
		}

		/**
		 * @return true if index is at end of the buffer
		 */
	    public boolean isAtEnd() {
			return index > buffer.length;
        }

	    /**
	     * @return the current pointer location in buffer
	     */
		public int getIndex() {
	        return index;
	    }
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
        
        if (iLength < 0) {
            throw new IllegalArgumentException();
        }
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
    public static RubyString pack(ArrayList list, RubyString formatString) {
        Ruby ruby = formatString.getRuntime();
        PtrList format = new PtrList(ruby, formatString.getValue());
        StringBuffer result = new StringBuffer();
        int listSize = list.size();
        char type = format.nextChar();
        
        int idx = 0;
        String lCurElemString;
        
        while(format.isAtEnd() == false) {
        	// Possible next type, format of current type, occurrences of type
        	char next = format.nextChar();

            if (Character.isWhitespace(type)) { // skip all spaces
                type = next;
                continue;
            }
            
            if (next == '!' || next == '_') {
                if (NATIVE_CODES.indexOf(type) == -1) {
                	throw new ArgumentError(ruby, "'" + next +
                	        "' allowed only after types " + NATIVE_CODES);
                }

                next = format.nextChar();
            }

            // Determine how many of type are needed (default: 1)
            boolean isStar = false;
            int occurrences = 1;
            if (next == '*') {
                if ("@Xxu".indexOf(type) != -1) {
                    occurrences = 0;
                } else {
                    occurrences = listSize;
                    isStar = true;
                }
                next = format.nextChar();
            } else if (Character.isDigit(next)) {
                format.backup(1);
                // an exception may occur here if an int can't hold this but ...
                occurrences = format.nextAsciiNumber();
                next = format.nextChar();
            }

            Converter converter = (Converter) converters.get(new Character(type));

            if (converter != null) {
                idx = encode(ruby, occurrences, result, list, idx, converter);
                type = next;
                continue;
            }

            switch (type) {
                case '%' :
                    throw new ArgumentError(ruby, "% is not supported");
                case 'A' :
                case 'a' :
                case 'Z' :
                case 'B' :
                case 'b' :
                case 'H' :
                case 'h' :
                	{
                		if (listSize-- <= 0) {
                			throw new ArgumentError(ruby, sTooFew);
                		}
                		
                		IRubyObject from = (IRubyObject) list.get(idx++);
                		lCurElemString = (from == ruby.getNil() ? "" : 
										  convert2String(from));

                		if (isStar) {
                			occurrences = lCurElemString.length();
                		}
                    
                		switch (type) {
                			case 'a' :
                			case 'A' :
                			case 'Z' :
                				if (lCurElemString.length() >= occurrences) {
                					result.append(lCurElemString.toCharArray(), 0, occurrences);
                				} else {//need padding
                					//I'm fairly sure there is a library call to create a
                					//string filled with a given char with a given length but I couldn't find it
                					result.append(lCurElemString);
                					occurrences -= lCurElemString.length();
                					grow(result, (type == 'a') ? sNil10 : sSp10, occurrences);
                				}	
                            break;

                            //I believe there is a bug in the b and B case we skip a char too easily
                            case 'b' :
                            	{
                            		int currentByte = 0;
                            		int padLength = 0;

                            		if (occurrences > lCurElemString.length()) {
                            			padLength = occurrences - lCurElemString.length();
                            			occurrences = lCurElemString.length();
                            		}
                                
                            		for (int i = 0; i < occurrences;) {
                            			if ((lCurElemString.charAt(i++) & 1) != 0) {//if the low bit is set
                            				currentByte |= 128; //set the high bit of the result
                            			}
                            			
                            			if ((i & 7) == 0) {
                            				result.append((char) (currentByte & 0xff));
                            				currentByte = 0;
                            				continue;
                            			}
                            			
                           				//if the index is not a multiple of 8, we are not on a byte boundary
                           				currentByte >>= 1; //shift the byte
                            		}
                                
                            		if ((occurrences & 7) != 0) { //if the length is not a multiple of 8
                            			currentByte >>= 7 - (occurrences & 7); //we need to pad the last byte
                            			result.append((char) (currentByte & 0xff));
                            		}
                                
                            		//do some padding, I don't understand the padding strategy
                            		result.setLength(result.length() + padLength);
                            	}
                            break;
                            case 'B' :
                            	{
                            		int currentByte = 0;
                            		int padLength = 0;
                            		
                            		if (occurrences > lCurElemString.length()) {
                            			padLength = occurrences - lCurElemString.length();
                            			occurrences = lCurElemString.length();
                            		}
                            		
                            		for (int i = 0; i < occurrences;) {
                            			currentByte |= lCurElemString.charAt(i++) & 1;
                            			
                            			// we filled up current byte; append it and create next one
                            			if ((i & 7) == 0) {
                            				result.append((char) (currentByte & 0xff));
                            				currentByte = 0;
                            				continue;
                            			}
                            			
                            			//if the index is not a multiple of 8, we are not on a byte boundary
                            			currentByte <<= 1; 
                            		}
                            		
                            		if ((occurrences & 7) != 0) { //if the length is not a multiple of 8
                            			currentByte <<= 7 - (occurrences & 7); //we need to pad the last byte
                            			result.append((char) (currentByte & 0xff));
                            		}
                            		
                            		result.setLength(result.length() + padLength);
                            	}
                            break;
                            case 'h' :
                            	{
                            		int currentByte = 0;
                            		int padLength = 0;
                            		
                            		if (occurrences > lCurElemString.length()) {
                            			padLength = occurrences - lCurElemString.length();
                            			occurrences = lCurElemString.length();
                            		}
                            		
                            		for (int i = 0; i < occurrences;) {
                            			char currentChar = lCurElemString.charAt(i++);
                            			
                            			if (Character.isJavaIdentifierStart(currentChar)) {
                            				//this test may be too lax but it is the same as in MRI
                            				currentByte |= (((currentChar & 15) + 9) & 15) << 4;
                            			} else {
                            				currentByte |= (currentChar & 15) << 4;
                            			}
                            			
                            			if ((i & 1) != 0) {
                            				currentByte >>= 4;
                            			} else {
                            				result.append((char) (currentByte & 0xff));
                            				currentByte = 0;
                            			}
                            		}
                            		
                            		if ((occurrences & 1) != 0) {
                            			result.append((char) (currentByte & 0xff));
                            		}

                            		result.setLength(result.length() + padLength);
                            	}
                            break;
                            case 'H' :
                            	{
                            		int currentByte = 0;
                            		int padLength = 0;
                            		
                            		if (occurrences > lCurElemString.length()) {
                            			padLength = occurrences - lCurElemString.length();
                            			occurrences = lCurElemString.length();
                            		}
                            		
                            		for (int i = 0; i < occurrences;) {
                            			char currentChar = lCurElemString.charAt(i++);
                            			
                            			if (Character.isJavaIdentifierStart(currentChar)) {
                            				//this test may be too lax but it is the same as in MRI
                            				currentByte |= ((currentChar & 15) + 9) & 15;
                            			} else {
                            				currentByte |= (currentChar & 15);
                            			}
                            			
                            			if ((i & 1) != 0) {
                            				currentByte <<= 4;
                            			} else {
                            				result.append((char) (currentByte & 0xff));
                            				currentByte = 0;
                            			}
                            		}
                            		
                            		if ((occurrences & 1) != 0) {
                            			result.append((char) (currentByte & 0xff));
                            		}

                            		result.setLength(result.length() + padLength);
                            	}
                            break;
                		}
                		break;
                	}

                case 'x' :
                    grow(result, sNil10, occurrences);
                    break;
                case 'X' :
                    try {
                        shrink(result, occurrences);
                    } catch (IllegalArgumentException e) {
                        throw new ArgumentError(ruby, "in `pack': X outside of string");
                    }
                    break;
                case '@' :
                    occurrences -= result.length();
                    if (occurrences > 0)
                        grow(result, sNil10, occurrences);
                    occurrences = -occurrences;
                    if (occurrences > 0)
                        shrink(result, occurrences);
                    break;
                case 'u' :
                case 'm' :
                	{
                		if (listSize-- <= 0) {
                			throw new ArgumentError(ruby, sTooFew);
                		}
                        IRubyObject from = (IRubyObject) list.get(idx++);
                        lCurElemString = (from == ruby.getNil() ? "" :
										  convert2String(from));
                        occurrences = (occurrences <= 2 ? 45 :
									   occurrences / 3 * 3);

                        for (;;) {
                        	encodes(ruby, result, lCurElemString, occurrences, type);
                        	
                        	if (occurrences >= lCurElemString.length()) {
                        		break;
                        	}
                        	
                        	lCurElemString = lCurElemString.substring(occurrences);
                        }
                	}
                    break;
                case 'M' :
                	{
               		if (listSize-- <= 0) {
               			throw new ArgumentError(ruby, sTooFew);
               		}
                	
               		IRubyObject from = (IRubyObject) list.get(idx++);
               		lCurElemString = (from == ruby.getNil() ? "" :
									  convert2String(from));

               		if (occurrences <= 1) {
               			occurrences = 72;
               		}
                    
               		qpencode(result, lCurElemString, occurrences);
                	}
                    break;
                case 'U' :
               		char[] c = new char[occurrences];
               		for (int cIndex = 0; occurrences-- > 0; cIndex++) {
               			if (listSize-- <= 0) {
               				throw new ArgumentError(ruby, sTooFew);
               			}

               			IRubyObject from = (IRubyObject) list.get(idx++);
               			long l = (from == ruby.getNil() ? 0 :
								RubyNumeric.num2long(from));

               			c[cIndex] = (char) l;
               		}
                    
                    try {
                    	byte[] bytes = new String(c).getBytes("UTF-8");
                    	result.append(RubyString.bytesToString(bytes));
                    } catch (java.io.UnsupportedEncodingException e) {
                        Asserts.notReached("can't convert to UTF8");
                    }
                    break;
            }
            
            type = next;
        }
        return RubyString.newString(ruby, result.toString());
    }
    
    /**
     * Retrieve an encoded int in little endian starting at index in the 
     * string value.
     *  
     * @param encode string to get int from
     * @return the decoded integer
     */
    private static int decodeIntLittleEndian(PtrList encode) {
        return (encode.nextByte() + (encode.nextByte() << 8) + 
                (encode.nextByte() << 16) + (encode.nextByte() << 24));
    }

    /**
     * Retrieve an encoded int in little endian starting at index in the 
     * string value.
     *  
     * @param encode string to get int from
     * @return the decoded integer
     */
    private static int decodeIntBigEndian(PtrList encode) {
        return (encode.nextByte() << 24) + (encode.nextByte() << 16) +
        (encode.nextByte() << 8) + encode.nextByte();
    }
    
    /**
     * Retrieve an encoded int in big endian starting at index in the string 
     * value.
     *  
     * @param encode string to get int from
     * @return the decoded integer
     */
    private static long decodeIntUnsignedBigEndian(PtrList encode) {
        return (((long)encode.nextByte() << 24) + 
                ((long)encode.nextByte() << 16) + 
                ((long)encode.nextByte() << 8) + (long)encode.nextByte());
    }

    /**
     * Retrieve an encoded int in little endian starting at index in the 
     * string value.
     *  
     * @param encode the encoded string
     * @return the decoded integer
     */
    private static long decodeIntUnsignedLittleEndian(PtrList encode) {
        return ((long)encode.nextByte() + ((long)encode.nextByte() << 8) + 
                ((long)encode.nextByte() << 16) + 
                ((long)encode.nextByte() << 24));
    }
    
    /**
	 * Encode an int in little endian format into a packed representation.
	 *  
	 * @param result to be appended to
	 * @param s the integer to encode
	 */
    private static void encodeIntLittleEndian(StringBuffer result, int s) {
        result.append((char) (s & 0xff)).append((char) ((s >> 8) & 0xff));
        result.append((char) ((s>>16) & 0xff)).append((char) ((s>>24) &0xff));
    }

	/**
	 * Encode an int in big-endian format into a packed representation.
	 *  
	 * @param result to be appended to
	 * @param s the integer to encode
	 */
    private static void encodeIntBigEndian(StringBuffer result, int s) {
        result.append((char) ((s>>24) &0xff)).append((char) ((s>>16) &0xff));
        result.append((char) ((s >> 8) & 0xff)).append((char) (s & 0xff));
    }
	
	/**
	 * Decode a long in big-endian format from a packed value
	 * 
     * @param encode string to get int from
	 * @return the long value
	 */
	private static long decodeLongBigEndian(PtrList encode) {
	    int c1 = decodeIntBigEndian(encode);
	    int c2 = decodeIntBigEndian(encode);
		
		return ((long) c1 << 32) + (c2 & 0xffffffffL); 
	}

	/**
	 * Decode a long in little-endian format from a packed value
	 * 
     * @param encode string to get int from
	 * @return the long value
	 */
	private static long decodeLongLittleEndian(PtrList encode) {
	    int c1 = decodeIntLittleEndian(encode);
	    int c2 = decodeIntLittleEndian(encode);

	    return ((long) c2 << 32) + (c1 & 0xffffffffL); 
	}
	
	/**
	 * Encode a long in little-endian format into a packed value
	 * 
	 * @param result to pack long into
	 * @param l is the long to encode
	 */
	private static void encodeLongLittleEndian(StringBuffer result, long l) {
	    encodeIntLittleEndian(result, (int) (l & 0xffffffff));
	    encodeIntLittleEndian(result, (int) (l >>> 32));
	}
	
	/**
	 * Encode a long in big-endian format into a packed value
	 * 
	 * @param result to pack long into
	 * @param l is the long to encode
	 */
	private static void encodeLongBigEndian(StringBuffer result, long l) {
	    encodeIntBigEndian(result, (int) (l >>> 32)); 
	    encodeIntBigEndian(result, (int) (l & 0xffffffff)); 
	}
	
	/**
	 * Decode a double from a packed value
	 * 
     * @param encode string to get int from
	 * @return the double value
	 */
	private static double decodeDoubleLittleEndian(PtrList encode) {
	    return Double.longBitsToDouble(decodeLongLittleEndian(encode));
	}

	/**
	 * Decode a double in big-endian from a packed value
	 * 
     * @param encode string to get int from
	 * @return the double value
	 */
	private static double decodeDoubleBigEndian(PtrList encode) {
	    return Double.longBitsToDouble(decodeLongBigEndian(encode));
	}
	
	/**
	 * Encode a double in little endian format into a packed value
	 * 
	 * @param result to pack double into
	 * @param d is the double to encode
	 */
	private static void encodeDoubleLittleEndian(StringBuffer result, double d) {
	    encodeLongLittleEndian(result, Double.doubleToLongBits(d)); 
	}

	/**
	 * Encode a double in big-endian format into a packed value
	 * 
	 * @param result to pack double into
	 * @param d is the double to encode
	 */
	private static void encodeDoubleBigEndian(StringBuffer result, double d) {
	    encodeLongBigEndian(result, Double.doubleToLongBits(d)); 
	}
	
	/**
	 * Decode a float in big-endian from a packed value
	 * 
     * @param encode string to get int from
	 * @return the double value
	 */
	private static float decodeFloatBigEndian(PtrList encode) {
	    return Float.intBitsToFloat(decodeIntBigEndian(encode));
	}

	/**
	 * Decode a float in little-endian from a packed value
	 * 
	 * @param encode string to get int from
	 * @return the double value
	 */
	private static float decodeFloatLittleEndian(PtrList encode) {
	    return Float.intBitsToFloat(decodeIntLittleEndian(encode));
	}
	
	/**
	 * Encode a float in little endian format into a packed value
	 * @param result to pack float into
	 * @param d is the double to float
	 */
	private static void encodeFloatLittleEndian(StringBuffer result, float f) {
		encodeIntLittleEndian(result, Float.floatToIntBits(f)); 
	}

	/**
	 * Encode a float in big-endian format into a packed value
	 * @param result to pack float into
	 * @param f is the float to encode
	 */
	private static void encodeFloatBigEndian(StringBuffer result, float f) {
		encodeIntBigEndian(result, Float.floatToIntBits(f)); 
	}
	
	/**
	 * Decode a short in big-endian from a packed value
	 * 
     * @param encode string to get int from
	 * @return the short value
	 */
	private static int decodeShortUnsignedLittleEndian(PtrList encode) {
		return (encode.nextByte() + (encode.nextByte() << 8));
	}

	/**
	 * Decode a short in big-endian from a packed value
	 * 
     * @param encode string to get int from
	 * @return the short value
	 */
	private static int decodeShortUnsignedBigEndian(PtrList encode) {
		return ((encode.nextByte() << 8) + encode.nextByte());
	}

	/**
	 * Decode a short in big-endian from a packed value
	 * 
     * @param encode string to get int from
	 * @return the short value
	 */
	private static short decodeShortBigEndian(PtrList encode) {
		return (short) ((short) (encode.nextByte() << 8) + encode.nextByte());
	}
	
	/**
	 * Encode an short in little endian format into a packed representation.
	 *  
	 * @param result to be appended to
	 * @param s the short to encode
	 */
	private static void encodeShortLittleEndian(StringBuffer result, int s) {
		result.append((char) (s & 0xff)).append((char) ((s & 0xff00) >> 8));
	}
	
	/**
	 * Encode an shortin big-endian format into a packed representation.
	 *  
	 * @param result to be appended to
	 * @param s the short to encode
	 */
	private static void encodeShortBigEndian(StringBuffer result, int s) {
		result.append((char) ((s & 0xff00) >> 8)).append((char) (s & 0xff));
	}
}
