/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.truffle.parser;

public interface ReOptions {
    int RE_OPTION_NONE         = 0;
    int RE_OPTION_IGNORECASE   = 1;
    int RE_OPTION_EXTENDED     = 2;
    int RE_OPTION_MULTILINE    = 4;
    int RE_OPTION_SINGLELINE   = 8;
    int RE_OPTION_POSIXLINE    = (RE_OPTION_MULTILINE | RE_OPTION_SINGLELINE);
    int RE_FIXED               = 16;
    int RE_NONE                = 32;
    int RE_UNICODE             = 64;
    int RE_OPTION_ONCE         = 128; // odd...but it is odd in ruby too.    
    int RE_LITERAL             = 256; // reusing regexp_options since we used 
                                      // and we won't escape regexp_options.
    int RE_DEFAULT = 512; // Only for RubyRegexp. for kcode default

    @Deprecated
    int RE_OPTION_LONGEST      = 16;
    @Deprecated
    int RE_MAY_IGNORECASE      = 32;

    int ARG_ENCODING_FIXED     = RE_FIXED;
    int ARG_ENCODING_NONE      = RE_NONE;
}
