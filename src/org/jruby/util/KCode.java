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

import java.util.regex.Pattern;
import org.jruby.IRuby;
import org.jruby.runtime.builtin.IRubyObject;

public class KCode {
    public static final KCode NIL = new KCode(null);
    public static final KCode NONE = new KCode("none");
    public static final KCode UTF8 = new KCode("utf8");
    public static final KCode SJIS = new KCode("sjis");

    private String kcode;

    private KCode(String kcode) {
        this.kcode = kcode;
    }

    public static KCode create(IRuby runtime, String lang) {
        if (lang == null) {
            return NIL;
        } else if (lang.charAt(0) == 'n' || lang.charAt(0) == 'N') {
            return NONE;
        } else if (lang.charAt(0) == 'u' || lang.charAt(0) == 'U') {
            return UTF8;
        } else if (lang.charAt(0) == 's' || lang.charAt(0) == 'S') {
            runtime.getWarnings().warn("JRuby supports only Unicode regexp.");
            return SJIS;
        }
        return NIL;
    }

    public IRubyObject kcode(IRuby runtime) {
        if (kcode == null) {
            return runtime.getNil();
        }
        return runtime.newString(kcode);
    }
    
    public String encoding() {
        if (this == UTF8) {
            return "UTF-8";
        } 
        
        return "ISO8859-1";
    }

    public int flags() {
        int flags = 0;
        if (this == UTF8) {
            flags |= Pattern.UNICODE_CASE;
        }
        flags |= Pattern.UNIX_LINES;

        return flags;
    }
}
	
