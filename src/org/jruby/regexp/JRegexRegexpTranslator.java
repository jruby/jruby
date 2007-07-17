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
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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
package org.jruby.regexp;

import jregex.MatchResult;
import jregex.Pattern;
import jregex.REFlags;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;

import org.jruby.parser.ReOptions;
import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class JRegexRegexpTranslator {
    public final static JRegexRegexpTranslator INSTANCE = new JRegexRegexpTranslator();
    private JRegexRegexpTranslator() {}

    private static final Pattern SHARP_IN_CHARACTER_CLASS_PATTERN = new Pattern("(\\[[^]]*)#(.*?])");
    private static final Pattern SPACE_IN_CHARACTER_CLASS_PATTERN = new Pattern("(\\[[^]]*) (.*?])");
    private static final Pattern COMMENT_PATTERN = new Pattern("\\(\\?#[^)]*\\)");
    private static final Pattern COMMENT2_PATTERN = new Pattern("(?<!\\\\)#.*");
    private static final Pattern HEX_SINGLE_DIGIT_PATTERN = new Pattern("\\\\x(\\p{XDigit})(?!\\p{XDigit})");
    private static final Pattern OCTAL_SINGLE_ZERO_PATTERN = new Pattern("\\\\(0)(?![0-7])");
    private static final Pattern OCTAL_MISSING_ZERO_PATTERN = new Pattern("\\\\([1-7][0-7]{1,2})");
    private static final Pattern POSIX_NAME = new Pattern("\\[:(\\w+):\\]");
    
    public Pattern translate(String regex, int options, int javaRegexFlags) {
        javaRegexFlags |= translateFlags(options);
        regex = translatePattern(regex, (javaRegexFlags & REFlags.IGNORE_SPACES) != 0);
        return new Pattern(regex, javaRegexFlags);
    }
    
    public Pattern translate(ByteList regex, int options, int javaRegexFlags) {
        javaRegexFlags |= translateFlags(options);
        String regexString;
        if ((options & ReOptions.RE_UNICODE) == 0) {
            regexString = regex.toString();
        } else {
            regexString = regex.toUtf8String();
        }
        String newRegex = translatePattern(regexString, (javaRegexFlags & REFlags.IGNORE_SPACES) != 0);
        return new Pattern(newRegex, javaRegexFlags);
    }
    
    public int flagsFor(int options, int javaRegexFlags) {
        return (javaRegexFlags | translateFlags(options)) & ~(16|32|48|64); // Remove languages from flags.
    }
    
    // We do not check for pathological case of [:foo:] outside of [] (bug 1475096).
    private static String translatePosixPattern(String regex) {
        Substitution posix=new Substitution(){
            public void appendSubstitution(MatchResult match, TextBuffer dest){
                String value = match.group(1);
                if ("alnum".equals(value)) {
                    dest.append("\\p{Alnum}");
                } else if ("alpha".equals(value)) {
                    dest.append("\\p{Alpha}");
                } else if ("blank".equals(value)) {
                    dest.append("\\p{Blank}");
                } else if ("cntrl".equals(value)) {
                    dest.append("\\p{Cntrl}");
                } else if ("digit".equals(value)) {
                    dest.append("\\p{Digit}");
                } else if ("graph".equals(value)) {
                    dest.append("\\p{Graph}");
                } else if ("lower".equals(value)) {
                    dest.append("\\p{Lower}");
                } else if ("print".equals(value)) {
                    dest.append("\\p{Print}");
                } else if ("punct".equals(value)) {
                    dest.append("\\p{Punct}");
                } else if ("space".equals(value)) {
                    dest.append("\\p{Space}");
                } else if ("upper".equals(value)) {
                    dest.append("\\p{Upper}");
                } else if ("xdigit".equals(value)) {
                    dest.append("\\p{XDigit}");
                } else {
                    dest.append("\\[:" + value + ":\\]");
                }
            }
        };
        Replacer r=POSIX_NAME.replacer(posix);
        return r.replace(regex);
    }
    
    public static String translatePattern(String regex, boolean commentsAllowed) {
        regex = COMMENT_PATTERN.replacer("").replace(regex);
        regex = translatePosixPattern(regex);
        regex = HEX_SINGLE_DIGIT_PATTERN.replacer("\\\\"+"x0$1").replace(regex);
        regex = OCTAL_SINGLE_ZERO_PATTERN.replacer("\\\\"+"0$1").replace(regex);
        regex = OCTAL_MISSING_ZERO_PATTERN.replacer("\\\\"+"0$1").replace(regex);
        if (commentsAllowed) {
            regex = SPACE_IN_CHARACTER_CLASS_PATTERN.replacer("$1\\\\x20$2").replace(regex);
            regex = SHARP_IN_CHARACTER_CLASS_PATTERN.replacer("$1\\\\x23$2").replace(regex);
            regex = COMMENT2_PATTERN.replacer("").replace(regex);
        }
        return regex;
    }
    
    public static int translateFlags(int options) {
        int flags = REFlags.MULTILINE;
        if ((options & ReOptions.RE_OPTION_IGNORECASE) > 0) {
            flags |= REFlags.IGNORE_CASE;
        }
        if ((options & ReOptions.RE_OPTION_EXTENDED) > 0) {
            flags |= REFlags.IGNORE_SPACES;
        }
        if ((options & ReOptions.RE_OPTION_MULTILINE) > 0) {
            flags |= REFlags.DOTALL;
        }
        // FIXME: This may be useful for something, but doesn't appear to be right
        // for Ruby. It turns \w, \s, etc into Unicode forms, but that appears to
        // break some test cases for us
        //if ((options & ReOptions.RE_UNICODE) > 0) {
        //    flags |= REFlags.UNICODE;
        //}
        return flags;
    }
}// JRegexRegexpTranslator
