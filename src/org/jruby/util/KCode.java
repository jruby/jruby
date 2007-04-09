/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

package org.jruby.util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

public class KCode {
    public static final KCode NIL = new KCode(null);
    public static final KCode NONE = new KCode("NONE");
    public static final KCode UTF8 = new KCode("UTF8");
    public static final KCode SJIS = new KCode("SJIS");
    public static final KCode EUC = new KCode("EUC");

    private String kcode;

    private KCode(String kcode) {
        this.kcode = kcode;
    }

    public static KCode create(Ruby runtime, String lang) {
        if(lang == null) {
            return NIL;
        }

        switch(lang.charAt(0)) {
        case 'E':
        case 'e':
            runtime.getWarnings().warn("JRuby supports only Unicode regexp.");
            return EUC;
        case 'S':
        case 's':
            runtime.getWarnings().warn("JRuby supports only Unicode regexp.");
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

    public IRubyObject kcode(Ruby runtime) {
        if (kcode == null) {
            return runtime.getNil();
        }
        return runtime.newString(kcode);
    }
    
    public CharsetDecoder decoder() {
        if (this == UTF8) {
            return Charset.forName("UTF-8").newDecoder();
        } 
        
        return Charset.forName("ISO-8859-1").newDecoder();
    }
    
    public CharsetEncoder encoder() {
        if (this == UTF8) {
            return Charset.forName("UTF-8").newEncoder();
        }
        
        return Charset.forName("ISO-8859-1").newEncoder();
    }

    public int flags() {
        return 0;
    }
}
	
