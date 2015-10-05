/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2003-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Derek Berner <derek.berner@state.nm.us>
 * Copyright (C) 2006 Evan Buswell <ebuswell@gmail.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

public class PackUtils {

    private static final byte[] hex_table;

    static {
        hex_table = ByteList.plain("0123456789ABCDEF");
    }

    /**
     * encodes a String with the Quoted printable, MIME encoding (see RFC2045).
     * appends the result of the encoding in a StringBuffer
     * @param io2Append The StringBuffer which should receive the result
     * @param i2Encode The String to encode
     * @param iLength The max number of characters to encode
     * @return the io2Append buffer
     **/
    public static ByteList qpencode(ByteList io2Append, ByteList i2Encode, int iLength) {
        io2Append.ensure(1024);
        int lCurLineLength = 0;
        int lPrevChar = -1;
        byte[] l2Encode = i2Encode.getUnsafeBytes();
        try {
            int end = i2Encode.getBegin() + i2Encode.getRealSize();
            for (int i = i2Encode.getBegin(); i < end; i++) {
                int lCurChar = l2Encode[i] & 0xff;
                if (lCurChar > 126 || (lCurChar < 32 && lCurChar != '\n' && lCurChar != '\t') || lCurChar == '=') {
                    io2Append.append('=');
                    io2Append.append(hex_table[lCurChar >>> 4]);
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
}
