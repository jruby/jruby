package org.jruby.lexer;

import java.io.IOException;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Matcher;
import org.joni.Option;
import org.joni.Regex;
import org.jruby.Ruby;
import org.jruby.RubyRegexp;
import org.jruby.util.ByteList;

/**
 * Code and constants common to both ripper and main parser.
 */
public class LexingCommon {
    // ruby constants for strings (should this be moved somewhere else?)
    public static final int STR_FUNC_ESCAPE=0x01;
    public static final int STR_FUNC_EXPAND=0x02;
    public static final int STR_FUNC_REGEXP=0x04;
    public static final int STR_FUNC_QWORDS=0x08;
    public static final int STR_FUNC_SYMBOL=0x10;
    // When the heredoc identifier specifies <<-EOF that indents before ident. are ok (the '-').
    public static final int STR_FUNC_INDENT=0x20;

    public static final int str_squote = 0;
    public static final int str_dquote = STR_FUNC_EXPAND;
    public static final int str_xquote = STR_FUNC_EXPAND;
    public static final int str_regexp = STR_FUNC_REGEXP | STR_FUNC_ESCAPE | STR_FUNC_EXPAND;
    public static final int str_ssym   = STR_FUNC_SYMBOL;
    public static final int str_dsym   = STR_FUNC_SYMBOL | STR_FUNC_EXPAND;

    public static final int EOF = -1; // 0 in MRI

    public static ByteList END_MARKER = new ByteList(new byte[] {'_', '_', 'E', 'N', 'D', '_', '_'});
    public static ByteList BEGIN_DOC_MARKER = new ByteList(new byte[] {'b', 'e', 'g', 'i', 'n'});
    public static ByteList END_DOC_MARKER = new ByteList(new byte[] {'e', 'n', 'd'});
    public static ByteList CODING = new ByteList(new byte[] {'c', 'o', 'd', 'i', 'n', 'g'});

    public static final Encoding UTF8_ENCODING = UTF8Encoding.INSTANCE;
    public static final Encoding USASCII_ENCODING = USASCIIEncoding.INSTANCE;
    public static final Encoding ASCII8BIT_ENCODING = ASCIIEncoding.INSTANCE;

    public static final int SUFFIX_R = 1<<0;
    public static final int SUFFIX_I = 1<<1;
    public static final int SUFFIX_ALL = 3;

    /**
     * @param c the character to test
     * @return true if character is a hex value (0-9a-f)
     */
    public static boolean isHexChar(int c) {
        return Character.isDigit(c) || ('a' <= c && c <= 'f') || ('A' <= c && c <= 'F');
    }

    /**
     * @param c the character to test
     * @return true if character is an octal value (0-7)
     */
    public static boolean isOctChar(int c) {
        return '0' <= c && c <= '7';
    }

    /* MRI: magic_comment_marker */
    /* This impl is a little sucky.  We basically double scan the same bytelist twice.  Once here
     * and once in parseMagicComment.
     */
    public static int magicCommentMarker(ByteList str, int begin) {
        int i = begin;
        int len = str.length();

        while (i < len) {
            switch (str.charAt(i)) {
                case '-':
                    if (i >= 2 && str.charAt(i - 1) == '*' && str.charAt(i - 2) == '-') return i + 1;
                    i += 2;
                    break;
                case '*':
                    if (i + 1 >= len) return -1;

                    if (str.charAt(i + 1) != '-') {
                        i += 4;
                    } else if (str.charAt(i - 1) != '-') {
                        i += 2;
                    } else {
                        return i + 2;
                    }
                    break;
                default:
                    i += 3;
                    break;
            }
        }
        return -1;
    }

    public static final String magicString = "([^\\s\'\":;]+)\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|[^\"\\s;]+)[\\s;]*";
    public static final Regex magicRegexp = new Regex(magicString.getBytes(), 0, magicString.length(), 0, Encoding.load("ASCII"));

    // MRI: parser_magic_comment
    public static ByteList parseMagicComment(Ruby runtime, ByteList magicLine) throws IOException {
        int length = magicLine.length();

        if (length <= 7) return null;
        int beg = magicCommentMarker(magicLine, 0);
        if (beg < 0) return null;
        int end = magicCommentMarker(magicLine, beg);
        if (end < 0) return null;

        // We only use a regex if -*- ... -*- is found.
        length = end - beg - 3; // -3 is to skip past beg
        int realSize = magicLine.getRealSize();
        int begin = magicLine.getBegin();
        Matcher matcher = magicRegexp.matcher(magicLine.getUnsafeBytes(), begin, begin + realSize);
        int result = RubyRegexp.matcherSearch(runtime, matcher, begin, begin + realSize, Option.NONE);

        if (result < 0) return null;

        // Regexp is guarateed to have three matches
        int begs[] = matcher.getRegion().beg;
        int ends[] = matcher.getRegion().end;
        String name = magicLine.subSequence(begs[1], ends[1]).toString();
        if (!name.contains("ccoding")) return null;

        return magicLine.makeShared(begs[2], ends[2] - begs[2]);
    }

}
