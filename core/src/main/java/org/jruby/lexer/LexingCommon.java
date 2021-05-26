package org.jruby.lexer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyRegexp;
import org.jruby.exceptions.RaiseException;
import org.jruby.lexer.yacc.StackState;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KCode;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.io.EncodingUtils;

/**
 * Code and constants common to both ripper and main parser.
 */
public abstract class LexingCommon {
    public static final int EXPR_BEG     = 1;
    public static final int EXPR_END     = 1<<1;
    public static final int EXPR_ENDARG  = 1<<2;
    public static final int EXPR_ENDFN   = 1<<3;
    public static final int EXPR_ARG     = 1<<4;
    public static final int EXPR_CMDARG  = 1<<5;
    public static final int EXPR_MID     = 1<<6;
    public static final int EXPR_FNAME   = 1<<7;
    public static final int EXPR_DOT     = 1<<8;
    public static final int EXPR_CLASS   = 1<<9;
    public static final int EXPR_LABEL   = 1<<10;
    public static final int EXPR_LABELED = 1<<11;
    public static final int EXPR_FITEM = 1<<12;
    public static final int EXPR_VALUE = EXPR_BEG;
    public static final int EXPR_BEG_ANY = EXPR_BEG | EXPR_MID | EXPR_CLASS;
    public static final int EXPR_ARG_ANY = EXPR_ARG | EXPR_CMDARG;
    public static final int EXPR_END_ANY = EXPR_END | EXPR_ENDARG | EXPR_ENDFN;

    public LexingCommon(LexerSource src) {
        this.src = src;
    }

    protected int braceNest = 0;
    public boolean commandStart;
    protected StackState conditionState = new StackState();
    protected StackState cmdArgumentState = new StackState();
    private ByteList current_arg;
    private Encoding current_enc;
    protected boolean __end__seen = false;
    public boolean eofp = false;
    protected boolean has_shebang = false;
    protected int heredoc_end = 0;
    protected int heredoc_indent = 0;
    protected int heredoc_line_indent = 0;
    public boolean inKwarg = false;
    protected int last_cr_line;
    protected int last_state;
    private int leftParenBegin = 0;
    public ByteList lexb = null;
    public ByteList lex_lastline = null;
    protected ByteList lex_nextline = null;
    public int lex_p = 0;                  // Where current position is in current line
    protected int lex_pbeg = 0;
    public int lex_pend = 0;               // Where line ends
    protected int lex_state;
    protected int line_count = 0;
    protected int line_offset = 0;
    protected int parenNest = 0;
    protected int ruby_sourceline = 0;
    protected LexerSource src;                // Stream of data that yylex() examines.
    protected int token;                      // Last token read via yylex().
    private int tokenCR;
    protected boolean tokenSeen = false;
    public int tokline;
    public int tokp = 0;                   // Where last token started
    protected Object yaccValue;               // Value of last token which had a value associated with it.

    public static final ByteList BACKTICK = new ByteList(new byte[] {'`'}, USASCIIEncoding.INSTANCE);
    public static final ByteList EQ_EQ_EQ = new ByteList(new byte[] {'=', '=', '='}, USASCIIEncoding.INSTANCE);
    public static final ByteList EQ_EQ = new ByteList(new byte[] {'=', '='}, USASCIIEncoding.INSTANCE);
    public static final ByteList EQ_TILDE = new ByteList(new byte[] {'=', '~'}, USASCIIEncoding.INSTANCE);
    public static final ByteList EQ_GT = new ByteList(new byte[] {'=', '>'}, USASCIIEncoding.INSTANCE);
    public static final ByteList EQ = new ByteList(new byte[] {'='}, USASCIIEncoding.INSTANCE);
    public static final ByteList AMPERSAND_AMPERSAND = CommonByteLists.AMPERSAND_AMPERSAND;
    public static final ByteList AMPERSAND = new ByteList(new byte[] {'&'}, USASCIIEncoding.INSTANCE);
    public static final ByteList AMPERSAND_DOT = new ByteList(new byte[] {'&', '.'}, USASCIIEncoding.INSTANCE);
    public static final ByteList BANG = new ByteList(new byte[] {'!'}, USASCIIEncoding.INSTANCE);
    public static final ByteList BANG_EQ = new ByteList(new byte[] {'!', '='}, USASCIIEncoding.INSTANCE);
    public static final ByteList BANG_TILDE = new ByteList(new byte[] {'!', '~'}, USASCIIEncoding.INSTANCE);
    public static final ByteList CARET = new ByteList(new byte[] {'^'}, USASCIIEncoding.INSTANCE);
    public static final ByteList COLON_COLON = new ByteList(new byte[] {':', ':'}, USASCIIEncoding.INSTANCE);
    public static final ByteList COLON = new ByteList(new byte[] {':'}, USASCIIEncoding.INSTANCE);
    public static final ByteList COMMA = new ByteList(new byte[] {','}, USASCIIEncoding.INSTANCE);
    public static final ByteList DOT_DOT_DOT = new ByteList(new byte[] {'.', '.', '.'}, USASCIIEncoding.INSTANCE);
    public static final ByteList DOT_DOT = new ByteList(new byte[] {'.', '.'}, USASCIIEncoding.INSTANCE);
    public static final ByteList DOT = new ByteList(new byte[] {'.'}, USASCIIEncoding.INSTANCE);
    public static final ByteList GT_EQ = new ByteList(new byte[] {'>', '='}, USASCIIEncoding.INSTANCE);
    public static final ByteList GT_GT = new ByteList(new byte[] {'>', '>'}, USASCIIEncoding.INSTANCE);
    public static final ByteList GT = new ByteList(new byte[] {'>'}, USASCIIEncoding.INSTANCE);
    public static final ByteList LBRACKET_RBRACKET_EQ = new ByteList(new byte[] {'[', ']', '='}, USASCIIEncoding.INSTANCE);
    public static final ByteList LBRACKET_RBRACKET = new ByteList(new byte[] {'[', ']'}, USASCIIEncoding.INSTANCE);
    public static final ByteList LBRACKET = new ByteList(new byte[] {'['}, USASCIIEncoding.INSTANCE);
    public static final ByteList LCURLY = new ByteList(new byte[] {'{'}, USASCIIEncoding.INSTANCE);
    public static final ByteList LT_EQ_RT = new ByteList(new byte[] {'<', '=', '>'}, USASCIIEncoding.INSTANCE);
    public static final ByteList LT_EQ = new ByteList(new byte[] {'<', '='}, USASCIIEncoding.INSTANCE);
    public static final ByteList LT_LT = new ByteList(new byte[] {'<', '<'}, USASCIIEncoding.INSTANCE);
    public static final ByteList LT = new ByteList(new byte[] {'<'}, USASCIIEncoding.INSTANCE);
    public static final ByteList MINUS_AT = new ByteList(new byte[] {'-', '@'}, USASCIIEncoding.INSTANCE);
    public static final ByteList MINUS = new ByteList(new byte[] {'-'}, USASCIIEncoding.INSTANCE);
    public static final ByteList MINUS_GT = new ByteList(new byte[] {'-', '>'}, USASCIIEncoding.INSTANCE);
    public static final ByteList PERCENT = new ByteList(new byte[] {'%'}, USASCIIEncoding.INSTANCE);
    public static final ByteList OR_OR = CommonByteLists.OR_OR;
    public static final ByteList OR = new ByteList(new byte[] {'|'}, USASCIIEncoding.INSTANCE);
    public static final ByteList PLUS_AT = new ByteList(new byte[] {'+', '@'}, USASCIIEncoding.INSTANCE);
    public static final ByteList PLUS = new ByteList(new byte[] {'+'}, USASCIIEncoding.INSTANCE);
    public static final ByteList QUESTION = new ByteList(new byte[] {'?'}, USASCIIEncoding.INSTANCE);
    public static final ByteList RBRACKET = new ByteList(new byte[] {']'}, USASCIIEncoding.INSTANCE);
    public static final ByteList RCURLY = new ByteList(new byte[] {'}'}, USASCIIEncoding.INSTANCE);
    public static final ByteList RPAREN = new ByteList(new byte[] {')'}, USASCIIEncoding.INSTANCE);
    public static final ByteList Q = new ByteList(new byte[] {'\''}, USASCIIEncoding.INSTANCE);
    public static final ByteList SLASH = new ByteList(new byte[] {'/'}, USASCIIEncoding.INSTANCE);
    public static final ByteList STAR = new ByteList(new byte[] {'*'}, USASCIIEncoding.INSTANCE);
    public static final ByteList STAR_STAR = new ByteList(new byte[] {'*', '*'}, USASCIIEncoding.INSTANCE);
    public static final ByteList TILDE = new ByteList(new byte[] {'~'}, USASCIIEncoding.INSTANCE);
    public static final ByteList QQ = new ByteList(new byte[] {'"'}, USASCIIEncoding.INSTANCE);
    public static final ByteList SEMICOLON = new ByteList(new byte[] {';'}, USASCIIEncoding.INSTANCE);
    public static final ByteList BACKSLASH = new ByteList(new byte[] {'\\'}, USASCIIEncoding.INSTANCE);
    public static final ByteList CALL = new ByteList(new byte[] {'c', 'a', 'l', 'l'}, USASCIIEncoding.INSTANCE);
    public static final ByteList DOLLAR_BANG = new ByteList(new byte[] {'$', '!'}, USASCIIEncoding.INSTANCE);
    public static final ByteList DOLLAR_UNDERSCORE = new ByteList(new byte[] {'$', '_'}, USASCIIEncoding.INSTANCE);
    public static final ByteList DOLLAR_DOT = new ByteList(new byte[] {'$', '_'}, USASCIIEncoding.INSTANCE);

    public int column() {
        return tokp - lex_pbeg;
    }

    protected boolean comment_at_top() {
        int p = lex_pbeg;
        int pend = lex_p - 1;
        if (line_count != (has_shebang ? 2 : 1)) return false;
        while (p < pend) {
            if (!Character.isSpaceChar(p(p))) return false;
            p++;
        }
        return true;
    }

    public int getRubySourceline() {
        return ruby_sourceline;
    }

    public void setRubySourceline(int line) {
        ruby_sourceline = line;
    }

    public ByteList createTokenByteList() {
        return new ByteList(lexb.unsafeBytes(), lexb.begin() + tokp, lex_p - tokp, getEncoding(), true);
    }

    public ByteList createTokenByteList(int start) {
        return new ByteList(lexb.unsafeBytes(), lexb.begin() + start, lex_p - tokp - start, getEncoding(), false);
    }

    public String createTokenString(int start) {
        return createAsEncodedString(lexb.getUnsafeBytes(), lexb.begin() + start, lex_p - start);
    }

    public String createAsEncodedString(byte[] bytes, int start, int length) {
        // FIXME: We should be able to move some faster non-exception cache using Encoding.isDefined
        try {
            Charset charset = EncodingUtils.charsetForEncoding(getEncoding());
            if (charset != null) {
                if (charset == RubyEncoding.UTF8) {
                    return RubyEncoding.decodeUTF8(bytes, start, length);
                } else {
                    return new String(bytes, start, length, charset);
                }
            }
        } catch (UnsupportedCharsetException e) {}

        return new String(bytes, start, length);
    }

    public String createTokenString() {
        return createTokenString(tokp);
    }

    public static int dedent_string(ByteList string, int width) {
        long len = string.realSize();
        int i, col = 0;
        byte[] str = string.unsafeBytes();
        int begin = string.begin();

        for (i = 0; i < len && col < width; i++) {
            if (str[begin + i] == ' ') {
                col++;
            } else if (str[begin + i] == '\t') {
                int n = TAB_WIDTH * (col / TAB_WIDTH + 1);
                if (n > width) break;
                col = n;
            } else {
                break;
            }
        }

        string.setBegin(begin + i);
        string.setRealSize((int) len - i);
        return i;
    }

    protected void flush() {
        tokp = lex_p;
    }

    public int getBraceNest() {
        return braceNest;
    }

    public StackState getCmdArgumentState() {
        return cmdArgumentState;
    }

    public StackState getConditionState() {
        return conditionState;
    }

    public ByteList getCurrentArg() {
        return current_arg;
    }

    public String getCurrentLine() {
        return lex_lastline.toString();
    }

    public Encoding getEncoding() {
        return current_enc;
    }

    public String getFile() {
        return src.getFilename();
    }

    public int getHeredocIndent() {
        return heredoc_indent;
    }

    public int getHeredocLineIndent() {
        return heredoc_line_indent;
    }

    public int getLeftParenBegin() {
        return leftParenBegin;
    }

    public int getLineOffset() {
        return line_offset;
    }

    public int getState() {
        return lex_state;
    }

    public int getTokenCR() {
        return tokenCR;
    }

    public int incrementParenNest() {
        parenNest++;

        return parenNest;
    }

    public boolean isEndSeen() {
        return __end__seen;
    }

    // mri: parser_isascii
    public boolean isASCII() {
        return Encoding.isMbcAscii((byte) lexb.get(lex_p - 1));
    }

    public static boolean isASCII(int c) {
        return Encoding.isMbcAscii((byte) c);
    }

    // Return of 0 means failed to find anything.  Non-zero means return that from lexer.
    public int peekVariableName(int tSTRING_DVAR, int tSTRING_DBEG) throws IOException {
        int c = nextc(); // byte right after #
        int significant = -1;
        switch (c) {
            case '$': {  // we unread back to before the $ so next lex can read $foo
                int c2 = nextc();

                if (c2 == '-') {
                    int c3 = nextc();

                    if (c3 == EOF) {
                        pushback(c3); pushback(c2);
                        return 0;
                    }

                    significant = c3;                              // $-0 potentially
                    pushback(c3); pushback(c2);
                    break;
                } else if (isGlobalCharPunct(c2)) {          // $_ potentially
                    setValue("#" + (char) c2);

                    pushback(c2); pushback(c);
                    return tSTRING_DVAR;
                }

                significant = c2;                                  // $FOO potentially
                pushback(c2);
                break;
            }
            case '@': {  // we unread back to before the @ so next lex can read @foo
                int c2 = nextc();

                if (c2 == '@') {
                    int c3 = nextc();

                    if (c3 == EOF) {
                        pushback(c3); pushback(c2);
                        return 0;
                    }

                    significant = c3;                                // #@@foo potentially
                    pushback(c3); pushback(c2);
                    break;
                }

                significant = c2;                                    // #@foo potentially
                pushback(c2);
                break;
            }
            case '{':
                //setBraceNest(getBraceNest() + 1);
                setValue("#" + (char) c);
                commandStart = true;
                return tSTRING_DBEG;
            default:
                return 0;
        }

        // We found #@, #$, #@@ but we don't know what at this point (check for valid chars).
        if (significant != -1 && Character.isAlphabetic(significant) || significant == '_') {
            pushback(c);
            setValue("#" + significant);
            return tSTRING_DVAR;
        }

        return 0;
    }

    // FIXME: I added number gvars here and they did not.
    public boolean isGlobalCharPunct(int c) {
        switch (c) {
            case '_': case '~': case '*': case '$': case '?': case '!': case '@':
            case '/': case '\\': case ';': case ',': case '.': case '=': case ':':
            case '<': case '>': case '\"': case '-': case '&': case '`': case '\'':
            case '+': case '1': case '2': case '3': case '4': case '5': case '6':
            case '7': case '8': case '9': case '0':
                return true;
        }
        return isIdentifierChar(c);
    }

    /**
     * This is a valid character for an identifier?
     *
     * @param c is character to be compared
     * @return whether c is an identifier or not
     *
     * mri: is_identchar
     */
    public static boolean isIdentifierChar(int c) {
        return c != EOF && (Character.isLetterOrDigit(c) || c == '_' || !isASCII(c));
    }

    public void lex_goto_eol() {
        lex_p = lex_pend;
    }

    public int lineno() {
        return ruby_sourceline + src.getLineOffset();
    }

    protected void magicCommentEncoding(ByteList encoding) {
        if (!comment_at_top()) return;

        setEncoding(encoding);
    }

    // FIXME: We significantly different from MRI in that we are just mucking
    // with lex_p pointers and not alloc'ing our own buffer (or using bytelist).
    // In most cases this does not matter much but for ripper or a place where
    // we remove actual source characters (like extra '"') then this acts differently.
    public void newtok(boolean unreadOnce) {
        tokline = ruby_sourceline;
        // We assume all idents are 7BIT until they aren't.
        tokenCR = StringSupport.CR_7BIT;

        tokp = lex_p - (unreadOnce ? 1 : 0); // We use tokp of ripper to mark beginning of tokens.
    }

    protected int numberLiteralSuffix(int mask) {
        int c;
        int result = 0;
        int last = lex_p;

        while((c = nextc()) != EOF) {
            if ((mask & SUFFIX_I) != 0 && c == 'i') {
                result |= (mask & SUFFIX_I);
                mask &= ~SUFFIX_R; // 'r' cannot come after an 'i'.
                continue;
            }

            if ((mask & SUFFIX_R) != 0 && c == 'r') {
                result |= (mask & SUFFIX_R);
                mask &= ~SUFFIX_R;
                continue;
            }

            if (!isASCII(c) || getEncoding().isAlpha(c) || c == '_') {
                lex_p = last;
                return 0;
            }

            if (c == '.') {
                int c2 = nextc();
                if (Character.isDigit(c2)) {
                    compile_error("unexpected fraction part after numeric literal");
                    do { // Ripper does not stop so we follow MRI here and read over next word...
                        c2 = nextc();
                    } while (isIdentifierChar(c2));
                } else {
                    pushback(c2);
                }
            }
            pushback(c);

            break;
        }

        return result;
    }

    public void parser_prepare() {
        int c = nextc();

        switch(c) {
            case '#':
                if (peek('!')) has_shebang = true;
                break;
            case 0xef:
                if (lex_pend - lex_p >= 2 && p(lex_p) == 0xbb && p(lex_p + 1) == 0xbf) {
                    setEncoding(UTF8_ENCODING);
                    lex_p += 2;
                    lex_pbeg = lex_p;
                    return;
                }
                break;
            case EOF:
                return;
        }
        pushback(c);

        current_enc = lex_lastline.getEncoding();
    }

    public int p(int offset) {
        return lexb.get(offset) & 0xff;
    }

    public boolean peek(int c) {
        return peek(c, 0);
    }

    protected boolean peek(int c, int n) {
        return lex_p+n < lex_pend && p(lex_p+n) == c;
    }

    public int precise_mbclen() {
        byte[] data = lexb.getUnsafeBytes();
        int begin = lexb.begin();

        // we subtract one since we have read past first byte by time we are calling this.
        return current_enc.length(data, begin + lex_p - 1, begin + lex_pend);
    }

    public void printState() {
        if (lex_state == 0) {
            System.out.println("NULL");
        } else {
            System.out.println(lex_state);
        }
    }

    public void pushback(int c) {
        if (c == -1) return;

        lex_p--;

        if (lex_p > lex_pbeg && p(lex_p) == '\n' && p(lex_p-1) == '\r') {
            lex_p--;
        }
    }

    public void reset() {
        braceNest = 0;
        commandStart = true;
        heredoc_indent = 0;
        heredoc_line_indent = 0;
        last_cr_line = -1;
        parenNest = 0;
        ruby_sourceline = 0;
        token = 0;
        tokenSeen = false;
        tokp = 0;
        yaccValue = null;

        setState(0);
        resetStacks();
    }

    public void resetStacks() {
        conditionState.reset();
        cmdArgumentState.reset();
    }

    protected char scanOct(int count) throws IOException {
        char value = '\0';

        for (int i = 0; i < count; i++) {
            int c = nextc();

            if (!isOctChar(c)) {
                pushback(c);
                break;
            }

            value <<= 3;
            value |= Integer.parseInt(String.valueOf((char) c), 8);
        }

        return value;
    }

    public void setCurrentArg(ByteList current_arg) {
        this.current_arg = current_arg;
    }

    // FIXME: This is icky.  Ripper is setting encoding immediately but in Parsers lexer we are not.
    public void setCurrentEncoding(Encoding encoding) {
        current_enc = encoding;
    }
    // FIXME: This is mucked up...current line knows it's own encoding so that must be changed.  but we also have two
    // other sources.  I am thinking current_enc should be removed in favor of src since it needs to know encoding to
    // provide next line.
    public void setEncoding(Encoding encoding) {
        setCurrentEncoding(encoding);
        src.setEncoding(encoding);
        lexb.setEncoding(encoding);
    }

    protected void set_file_encoding(int str, int send) {
        boolean sep = false;
        for (;;) {
            if (send - str <= 6) return;

            switch(p(str+6)) {
                case 'C': case 'c': str += 6; continue;
                case 'O': case 'o': str += 5; continue;
                case 'D': case 'd': str += 4; continue;
                case 'I': case 'i': str += 3; continue;
                case 'N': case 'n': str += 2; continue;
                case 'G': case 'g': str += 1; continue;
                case '=': case ':':
                    sep = true;
                    str += 6;
                    break;
                default:
                    str += 6;
                    if (Character.isSpaceChar(p(str))) break;
                    continue;
            }
            if (lexb.makeShared(str - 6, 6).caseInsensitiveCmp(CODING) == 0) break;
        }

        for(;;) {
            do {
                str++;
                if (str >= send) return;
            } while(Character.isSpaceChar(p(str)));
            if (sep) break;

            if (p(str) != '=' && p(str) != ':') return;
            sep = true;
            str++;
        }

        int beg = str;
        while ((p(str) == '-' || p(str) == '_' || Character.isLetterOrDigit(p(str))) && ++str < send) {}
        setEncoding(lexb.makeShared(beg, str - beg));
    }

    public void setHeredocLineIndent(int heredoc_line_indent) {
        this.heredoc_line_indent = heredoc_line_indent;
    }

    public void setHeredocIndent(int heredoc_indent) {
        this.heredoc_indent = heredoc_indent;
    }

    public void setBraceNest(int nest) {
        braceNest = nest;
    }

    public void setLeftParenBegin(int value) {
        leftParenBegin = value;
    }

    /**
     * Allow the parser to set the source for its lexer.
     *
     * @param source where the lexer gets raw data
     */
    public void setSource(LexerSource source) {
        this.src = source;
    }

    public void setState(int state) {
        this.lex_state = state;
    }

    public void setValue(Object yaccValue) {
        this.yaccValue = yaccValue;
    }

    protected boolean strncmp(ByteList one, ByteList two, int length) {
        if (one.length() < length || two.length() < length) return false;

        return one.makeShared(0, length).equal(two.makeShared(0, length));
    }

    public void tokAdd(int first_byte, ByteList buffer) {
        buffer.append((byte) first_byte);
    }

    public void tokCopy(int length, ByteList buffer) {
        buffer.append(lexb, lex_p - length, length);
    }

    public boolean tokadd_ident(int c) {
        do {
            if (!tokadd_mbchar(c)) return false;
            c = nextc();
        } while (isIdentifierChar(c));
        pushback(c);

        return true;
    }

    // mri: parser_tokadd_mbchar
    /**
     * This differs from MRI in a few ways.  This version does not apply value to a separate token buffer.
     * It is for use when we know we will not be omitting or including ant non-syntactical characters.  Use
     * tokadd_mbchar(int, ByteList) if the string differs from actual source.  Secondly, this returns a boolean
     * instead of the first byte passed.  MRI only used the return value as a success/failure code to return
     * EOF.
     *
     * Because this version does not use a separate token buffer we only just increment lex_p.  When we reach
     * end of the token it will just get the bytes directly from source directly.
     */
    public boolean tokadd_mbchar(int first_byte) {
        int length = precise_mbclen();

        if (length <= 0) {
            compile_error("invalid multibyte char (" + getEncoding() + ")");
            return false;
        } else if (length > 1) {
            tokenCR = StringSupport.CR_VALID;
        }

        lex_p += length - 1;  // we already read first byte so advance pointer for remainder

        return true;
    }

    // mri: parser_tokadd_mbchar
    public boolean tokadd_mbchar(int first_byte, ByteList buffer) {
        int length = precise_mbclen();

        if (length <= 0) {
            compile_error("invalid multibyte char (" + getEncoding() + ")");
            return false;
        }

        tokAdd(first_byte, buffer);                  // add first byte since we have it.
        lex_p += length - 1;                         // we already read first byte so advance pointer for remainder
        if (length > 1) tokCopy(length - 1, buffer); // copy next n bytes over.

        return true;
    }

    /**
     *  This looks deceptively like tokadd_mbchar(int, ByteList) but it differs in that it uses
     *  the bytelists encoding and the first parameter is a full codepoint and not the first byte
     *  of a mbc sequence.
     */
    public void tokaddmbc(int codepoint, ByteList buffer) {
        Encoding encoding = buffer.getEncoding();
        int length = encoding.codeToMbcLength(codepoint);
        buffer.ensure(buffer.getRealSize() + length);
        encoding.codeToMbc(codepoint, buffer.getUnsafeBytes(), buffer.begin() + buffer.getRealSize());
        buffer.setRealSize(buffer.getRealSize() + length);
    }

    /**
     * Last token read from the lexer at the end of a call to yylex()
     *
     * @return last token read
     */
    public int token() {
        return token;
    }

    public boolean update_heredoc_indent(int c) {
        if (heredoc_line_indent == -1) {
            if (c == '\n') heredoc_line_indent = 0;
        } else if (c == ' ') {
            heredoc_line_indent++;
            return true;
        } else if (c == '\t') {
            int w = (heredoc_line_indent / TAB_WIDTH) + 1;
            heredoc_line_indent = w * TAB_WIDTH;
            return true;
        } else if (c != '\n') {
            if (heredoc_indent > heredoc_line_indent) heredoc_indent = heredoc_line_indent;
            heredoc_line_indent = -1;
        }

        return false;
    }

    public void validateFormalIdentifier(ByteList identifier) {
        char first = identifier.charAt(0);

        if (Character.isUpperCase(first)) {
            compile_error("formal argument cannot be a constant");
        }

        switch(first) {
            case '@':
                if (identifier.charAt(1) == '@') {
                    compile_error("formal argument cannot be a class variable");
                } else {
                    compile_error("formal argument cannot be an instance variable");
                }
                break;
            case '$':
                compile_error("formal argument cannot be a global variable");
                break;
            default:
                // This mechanism feels a tad dicey but at this point we are dealing with a valid
                // method name at least so we should not need to check the entire string...
                char last = identifier.charAt(identifier.length() - 1);

                if (last == '=' || last == '?' || last == '!') {
                    compile_error("formal argument must be local variable");
                }
        }
    }

    @Deprecated
    public void validateFormalIdentifier(String identifier) {
        char first = identifier.charAt(0);

        if (Character.isUpperCase(first)) {
            compile_error("formal argument cannot be a constant");
        }

        switch(first) {
            case '@':
                if (identifier.charAt(1) == '@') {
                    compile_error("formal argument cannot be a class variable");
                } else {
                    compile_error("formal argument cannot be an instance variable");
                }
                break;
            case '$':
                compile_error("formal argument cannot be a global variable");
                break;
            default:
                // This mechanism feels a tad dicey but at this point we are dealing with a valid
                // method name at least so we should not need to check the entire string...
                char last = identifier.charAt(identifier.length() - 1);

                if (last == '=' || last == '?' || last == '!') {
                    compile_error("formal argument must be local variable");
                }
        }
    }

    /**
     * Value of last token (if it is a token which has a value).
     *
     * @return value of last value-laden token
     */
    public Object value() {
        return yaccValue;
    }

    protected void warn_balanced(int c, boolean spaceSeen, String op, String syn) {
        if (!isLexState(last_state, EXPR_CLASS|EXPR_DOT|EXPR_FNAME|EXPR_ENDFN) && spaceSeen && !Character.isWhitespace(c)) {
            ambiguousOperator(op, syn);
        }
    }

    public boolean was_bol() {
        return lex_p == lex_pbeg + 1;
    }

    public boolean whole_match_p(ByteList eos, boolean indent) {
        int len = eos.length();
        int p = lex_pbeg;

        if (indent) {
            for (int i = 0; i < lex_pend; i++) {
                if (!Character.isWhitespace(p(i+p))) {
                    p += i;
                    break;
                }
            }
        }
        int n = lex_pend - (p + len);
        if (n < 0) return false;
        if (n > 0 && p(p+len) != '\n') {
            if (p(p+len) != '\r') return false;
            if (n == 1 || p(p+len+1) != '\n') return false;
        }

        return strncmp(eos, lexb.makeShared(p, len), len);
    }

    protected abstract void ambiguousOperator(String op, String syn);
    public abstract void compile_error(String message);
    public abstract int nextc();
    protected abstract void setCompileOptionFlag(String name, ByteList value);
    protected abstract void setEncoding(ByteList name);
    protected abstract void setTokenInfo(String name, ByteList value);
    public abstract int tokenize_ident(int result);

    public static final int TAB_WIDTH = 8;

    // ruby constants for strings (should this be moved somewhere else?)
    public static final int STR_FUNC_ESCAPE=0x01;
    public static final int STR_FUNC_EXPAND=0x02;
    public static final int STR_FUNC_REGEXP=0x04;
    public static final int STR_FUNC_QWORDS=0x08;
    public static final int STR_FUNC_SYMBOL=0x10;
    // When the heredoc identifier specifies <<-EOF that indents before ident. are ok (the '-').
    public static final int STR_FUNC_INDENT=0x20;
    public static final int STR_FUNC_LABEL=0x40;
    public static final int STR_FUNC_LIST=0x4000;
    public static final int STR_FUNC_TERM=0x8000;

    public static final int str_label = STR_FUNC_LABEL;
    public static final int str_squote = 0;
    public static final int str_dquote = STR_FUNC_EXPAND;
    public static final int str_xquote = STR_FUNC_EXPAND;
    public static final int str_regexp = STR_FUNC_REGEXP | STR_FUNC_ESCAPE | STR_FUNC_EXPAND;
    public static final int str_sword = STR_FUNC_QWORDS | STR_FUNC_LIST;
    public static final int str_dword = STR_FUNC_QWORDS | STR_FUNC_EXPAND | STR_FUNC_LIST;
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

    public static boolean isLexState(int state, int mask) {
        return (mask & state) != 0;
    }

    protected boolean isLexStateAll(int state, int mask) {
        return (mask & state) == mask;
    }

    protected boolean isARG() {
        return isLexState(lex_state, EXPR_ARG_ANY);
    }

    protected boolean isBEG() {
        return isLexState(lex_state, EXPR_BEG_ANY) || isLexStateAll(lex_state, EXPR_ARG|EXPR_LABELED);
    }

    protected boolean isEND() {
        return isLexState(lex_state, EXPR_END_ANY);
    }

    protected boolean isLabelPossible(boolean commandState) {
        return (isLexState(lex_state, EXPR_LABEL|EXPR_ENDFN) && !commandState) || isARG();
    }

    public boolean isLabelSuffix() {
        return peek(':') && !peek(':', 1);
    }

    protected boolean isAfterOperator() {
        return isLexState(lex_state, EXPR_FNAME|EXPR_DOT);
    }

    protected boolean isNext_identchar() throws IOException {
        int c = nextc();
        pushback(c);

        return c != EOF && (Character.isLetterOrDigit(c) || c == '_');
    }

    /**
     * @param c the character to test
     * @return true if character is an octal value (0-7)
     */
    public static boolean isOctChar(int c) {
        return '0' <= c && c <= '7';
    }

    public static boolean isSpace(int c) {
        return c == ' ' || ('\t' <= c && c <= '\r');
    }

    protected boolean isSpaceArg(int c, boolean spaceSeen) {
        return isARG() && spaceSeen && !Character.isWhitespace(c);
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

    public boolean parser_magic_comment(ByteList magicLine) {
        boolean indicator = false;
        int vbeg, vend;
        int length = magicLine.realSize();
        int str = 0;
        int end;

        if (length <= 7) return false;
        int beg = magicCommentMarker(magicLine, 0);
        if (beg >= 0) {
            end = magicCommentMarker(magicLine, beg);
            if (end < 0) return false;
            indicator = true;
            str = beg;
            length = end - beg - 3; // -3 is to backup over end just found
        }

        /* %r"([^\\s\'\":;]+)\\s*:\\s*(\"(?:\\\\.|[^\"])*\"|[^\"\\s;]+)[\\s;]*" */
        while (length > 0) {
            for (; length > 0; str++, --length) {
                char c = magicLine.charAt(str);

                switch (c) {
                    case '\'': case '"': case ':': case ';': continue;
                }
                if (!Character.isWhitespace(c)) break;
            }

            for (beg = str; length > 0; str++, --length) {
                char c = magicLine.charAt(str);

                switch (c) {
                    case '\'': case '"': case ':': case ';': break;
                    default:
                        if (Character.isWhitespace(c)) break;
                    continue;
                }
                break;
            }
            for (end = str; length > 0 && Character.isWhitespace(magicLine.charAt(str)); str++, --length);
            if (length == 0) break;
            char c = magicLine.charAt(str);
            if (c != ':') {
                if (!indicator) return false;
                continue;
            }

            do {
                str++;
            } while (--length > 0 && Character.isWhitespace(magicLine.charAt(str)));
            if (length == 0) break;
            if (magicLine.charAt(str) == '"') {
                for (vbeg = ++str; --length > 0 && str < length && magicLine.charAt(str) != '"'; str++) {
                    if (magicLine.charAt(str) == '\\') {
                        --length;
                        ++str;
                    }
                }
                vend = str;
                if (length > 0) {
                    --length;
                    ++str;
                }
            } else {
                for (vbeg = str; length > 0 && magicLine.charAt(str) != '"' && magicLine.charAt(str) != ';' && !Character.isWhitespace(magicLine.charAt(str)); --length, str++);
                vend = str;
            }
            if (indicator) {
                while (length > 0 && (magicLine.charAt(str) == ';' || Character.isWhitespace(magicLine.charAt(str)))) {
                    --length;
                    str++;
                }
            } else {
                while (length > 0 && Character.isWhitespace(magicLine.charAt(str))) {
                    --length;
                    str++;
                }
                if (length > 0) return false;
            }

            String name = magicLine.subSequence(beg, end).toString().replace('-', '_');
            ByteList value = magicLine.makeShared(vbeg, vend - vbeg);

            if (!onMagicComment(name, value)) return false;
        }

        return true;
    }

    protected boolean onMagicComment(String name, ByteList value) {
        if ("coding".equalsIgnoreCase(name) || "encoding".equalsIgnoreCase(name)) {
            magicCommentEncoding(value);
            return true;
        } else if ("frozen_string_literal".equalsIgnoreCase(name)) {
            setCompileOptionFlag(name, value);
            return true;
        } else if ("warn_indent".equalsIgnoreCase(name)) {
            setTokenInfo(name, value);
            return true;
        }
        return false;
    }

    protected abstract RegexpOptions parseRegexpFlags() throws IOException;

    protected RegexpOptions parseRegexpFlags(StringBuilder unknownFlags) throws IOException {
        RegexpOptions options = new RegexpOptions();
        int c;

        newtok(true);
        for (c = nextc(); c != EOF && Character.isLetter(c); c = nextc()) {
            switch (c) {
            case 'i':
                options.setIgnorecase(true);
                break;
            case 'x':
                options.setExtended(true);
                break;
            case 'm':
                options.setMultiline(true);
                break;
            case 'o':
                options.setOnce(true);
                break;
            case 'n':
                options.setExplicitKCode(KCode.NONE);
                break;
            case 'e':
                options.setExplicitKCode(KCode.EUC);
                break;
            case 's':
                options.setExplicitKCode(KCode.SJIS);
                break;
            case 'u':
                options.setExplicitKCode(KCode.UTF8);
                break;
            case 'j':
                options.setJava(true);
                break;
            default:
                unknownFlags.append((char) c);
                break;
            }
        }
        pushback(c);

        return options;
    }

    public void checkRegexpFragment(Ruby runtime, ByteList value, RegexpOptions options) {
        setRegexpEncoding(runtime, value, options);
        ThreadContext context = runtime.getCurrentContext();
        IRubyObject $ex = context.getErrorInfo();
        try {
            RubyRegexp.preprocessCheck(runtime, value);
        } catch (RaiseException re) {
            context.setErrorInfo($ex);
            compile_error(re.getMessage());
        }
    }

    public void checkRegexpSyntax(Ruby runtime, ByteList value, RegexpOptions options) {
        final String stringValue = value.toString();
        // Joni doesn't support these modifiers - but we can fix up in some cases - let the error delay until we try that
        if (stringValue.startsWith("(?u)") || stringValue.startsWith("(?a)") || stringValue.startsWith("(?d)"))
            return;

        ThreadContext context = runtime.getCurrentContext();
        IRubyObject $ex = context.getErrorInfo();
        try {
            // This is only for syntax checking but this will as a side-effect create an entry in the regexp cache.
            RubyRegexp.newRegexpParser(runtime, value, (RegexpOptions)options.clone());
        } catch (RaiseException re) {
            context.setErrorInfo($ex);
            compile_error(re.getMessage());
        }
    }

    protected abstract void mismatchedRegexpEncodingError(Encoding optionEncoding, Encoding encoding);

    // MRI: reg_fragment_setenc_gen
    public void setRegexpEncoding(Ruby runtime, ByteList value, RegexpOptions options) {
        Encoding optionsEncoding = options.setup(runtime);

        // Change encoding to one specified by regexp options as long as the string is compatible.
        if (optionsEncoding != null) {
            if (optionsEncoding != value.getEncoding() && !is7BitASCII(value)) {
                mismatchedRegexpEncodingError(optionsEncoding, value.getEncoding());
            }

            value.setEncoding(optionsEncoding);
        } else if (options.isEncodingNone()) {
            if (value.getEncoding() != ASCII8BIT_ENCODING && !is7BitASCII(value)) {
                mismatchedRegexpEncodingError(optionsEncoding, value.getEncoding());
            }
            value.setEncoding(ASCII8BIT_ENCODING);
        } else if (getEncoding() == USASCIIEncoding.INSTANCE) {
            if (!is7BitASCII(value)) {
                value.setEncoding(USASCIIEncoding.INSTANCE); // This will raise later
            } else {
                value.setEncoding(ASCII8BIT_ENCODING);
            }
        }
    }

    private boolean is7BitASCII(ByteList value) {
      return StringSupport.codeRangeScan(value.getEncoding(), value) == StringSupport.CR_7BIT;
    }

    // TODO: Put somewhere more consolidated (similiar
    protected char optionsEncodingChar(Encoding optionEncoding) {
        if (optionEncoding == USASCIIEncoding.INSTANCE) return 'n';
        if (optionEncoding == org.jcodings.specific.EUCJPEncoding.INSTANCE) return 'e';
        if (optionEncoding == org.jcodings.specific.SJISEncoding.INSTANCE) return 's';
        if (optionEncoding == UTF8_ENCODING) return 'u';

        return ' ';
    }
}
