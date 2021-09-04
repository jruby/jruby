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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.EUCJPEncoding;
import org.jcodings.specific.SJISEncoding;
import org.jcodings.specific.UTF8Encoding;

public enum KCode {
    NIL(null, ASCIIEncoding.INSTANCE, 0),
    NONE("NONE", ASCIIEncoding.INSTANCE, 0),
    UTF8("UTF8", UTF8Encoding.INSTANCE, 64),
    SJIS("SJIS", SJISEncoding.INSTANCE, 48),
    EUC("EUC", EUCJPEncoding.INSTANCE, 32);

    private final String kcode;
    private final Encoding encoding;
    private final int code;

    private KCode(String kcode, Encoding encoding, int code) {
        this.kcode = kcode;
        this.encoding = encoding;
        this.code = code;
    }

    public static KCode create(String lang) {
        if (lang == null) return NIL;
        if (lang.length() == 0) return NONE;

        switch (lang.charAt(0)) {
        case 'E':
        case 'e':
            return EUC;
        case 'S':
        case 's':
            return SJIS;
        case 'U':
        case 'u':
            return UTF8;
        case 'N':
        case 'n':
        case 'A':
        case 'a':
            return NONE;
        }
        return NIL;
    }

    public String getKCode() {
        return kcode;
    }

    public int bits() {
        return code;
    }

    public static KCode fromBits(int bits) {
        if ((bits & 64) != 0) return UTF8;
        if ((bits & 48) == 48) return SJIS;
        if ((bits & 32) != 0) return EUC;

        return NONE;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public String toString() {
        return name();
    }
}
