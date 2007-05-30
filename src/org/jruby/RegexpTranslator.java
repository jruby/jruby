/**
 *
 */
package org.jruby;

import jregex.MatchResult;
import jregex.Pattern;
import jregex.REFlags;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;

import org.jruby.parser.ReOptions;
import org.jruby.util.ByteList;

public class RegexpTranslator {
    
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
        return javaRegexFlags | translateFlags(options);
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
}
