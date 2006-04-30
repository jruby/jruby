/**
 * 
 */
package org.jruby;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jruby.parser.ReOptions;

public class RegexpTranslator {

    private static final Pattern SHARP_IN_CHARACTER_CLASS_PATTERN = Pattern.compile("(\\[[^]]*)#(.*?])");
	private static final Pattern SPACE_IN_CHARACTER_CLASS_PATTERN = Pattern.compile("(\\[[^]]*) (.*?])");
	private static final Pattern COMMENT_PATTERN = Pattern.compile("\\(\\?#[^)]*\\)");
    private static final Pattern HEX_SINGLE_DIGIT_PATTERN = Pattern.compile("\\\\x(\\p{XDigit})(?!\\p{XDigit})");
    private static final Pattern OCTAL_SINGLE_ZERO_PATTERN = Pattern.compile("\\\\(0)(?![0-7])");
    private static final Pattern OCTAL_MISSING_ZERO_PATTERN = Pattern.compile("\\\\([1-7][0-7]{1,2})");
    private static final Pattern POSIX_NAME = Pattern.compile("\\[:(\\w+):\\]");
    
    public Pattern translate(String regex, int options, int javaRegexFlags) {
    	javaRegexFlags |= translateFlags(options);
		regex = translatePattern(regex, (javaRegexFlags & Pattern.COMMENTS) != 0);

		// If we return null, rather than die this ends up generating a TypeError (which is better
		// than crashing).
		try {
			return Pattern.compile(regex, javaRegexFlags);
		} catch (PatternSyntaxException e) {}
		
		return null;
	}
    
    // We do not check for pathological case of [:foo:] outside of [] (bug 1475096).
    private String translatePosixPattern(String regex) {
		for (Matcher matcher = POSIX_NAME.matcher(regex); matcher.find(); matcher = POSIX_NAME.matcher(regex)) {
			String value = matcher.group(1);

			if ("alnum".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Alnum}");
			} else if ("alpha".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Alpha}");
			} else if ("blank".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Blank}");
			} else if ("cntrl".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Cntrl}");
			} else if ("digit".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Digit}");
			} else if ("graph".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Graph}");
			} else if ("lower".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Lower}");
			} else if ("print".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Print}");
			} else if ("punct".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Punct}");
			} else if ("space".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Space}");
			} else if ("upper".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{Upper}");
			} else if ("xdigit".equals(value)) {
				regex = matcher.replaceFirst("\\\\p{XDigit}");
			} else {
				regex = matcher.replaceFirst("\\\\[:" + value + ":\\\\]");
			}
		}
		return regex;
    }

	String translatePattern(String regex, boolean commentsAllowed) {
		regex = COMMENT_PATTERN.matcher(regex).replaceAll("");
		regex = translatePosixPattern(regex); 
		regex = HEX_SINGLE_DIGIT_PATTERN.matcher(regex).replaceAll("\\\\"+"x0$1");
		regex = OCTAL_SINGLE_ZERO_PATTERN.matcher(regex).replaceAll("\\\\"+"0$1");
		regex = OCTAL_MISSING_ZERO_PATTERN.matcher(regex).replaceAll("\\\\"+"0$1");
		if (commentsAllowed) {
			regex = SPACE_IN_CHARACTER_CLASS_PATTERN.matcher(regex).replaceAll("$1\\\\x20$2");
			regex = SHARP_IN_CHARACTER_CLASS_PATTERN.matcher(regex).replaceAll("$1\\\\x23$2");
		}
		
		return regex;
	}
	
	int translateFlags(int options) {
		int flags = Pattern.MULTILINE;
        if ((options & ReOptions.RE_OPTION_IGNORECASE) > 0) {
            flags |= Pattern.CASE_INSENSITIVE;
        }
        if ((options & ReOptions.RE_OPTION_EXTENDED) > 0) {
        	flags |= Pattern.COMMENTS;
        }
        if ((options & ReOptions.RE_OPTION_MULTILINE) > 0) {
        	flags |= Pattern.DOTALL;
        }
		return flags;
	}

}