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
 * Copyright (C) 2013 The JRuby Team (jruby@jruby.org)
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
package org.jruby.ext.ripper;

import java.io.IOException;
import org.jcodings.Encoding;
import org.jruby.ext.ripper.SyntaxException.PID;
import org.jruby.parser.Tokens;
import org.jruby.util.ByteList;

public class StringTerm extends StrTerm {
    // Expand variables, Indentation of final marker
    private int flags;

    // Start of string ([, (, {, <, ', ", \n) 
    private final char begin;

    // End of string (], ), }, >, ', ", \0)
    private final char end;

    // How many strings are nested in the current string term
    private int nest;

    public StringTerm(int flags, int begin, int end) {
        this.flags = flags;
        this.begin = (char) begin;
        this.end   = (char) end;
        this.nest  = 0;
    }

    protected ByteList createByteList(RipperLexer lexer) {
        return new ByteList(new byte[]{}, lexer.getEncoding());
    }

    private int endFound(RipperLexer lexer, LexerSource src, ByteList buffer) throws IOException {
            if ((flags & RipperLexer.STR_FUNC_QWORDS) != 0) {
                flags = -1;
                lexer.getPosition();
                buffer.append(end);
                lexer.setValue(new Token(buffer, lexer.getPosition()));
                return ' ';
            }

            if ((flags & RipperLexer.STR_FUNC_REGEXP) != 0) {
                String options = parseRegexpFlags(lexer, src);
                buffer.append(options.getBytes());

                lexer.setValue(new Token(buffer, lexer.getPosition()));
                return Tokens.tREGEXP_END;
            }

            buffer.append(end);
            lexer.setValue(new Token(buffer, lexer.getPosition()));
            return Tokens.tSTRING_END;
    }

    @Override
    public int parseString(RipperLexer lexer, LexerSource src) throws IOException {
        boolean spaceSeen = false;
        int c;

        // FIXME: How much more obtuse can this be?
        // Heredoc already parsed this and saved string...Do not parse..just return
        if (flags == -1) {
            lexer.setValue(new Token("" + end, lexer.getPosition()));
            lexer.ignoreNextScanEvent = true;
            return Tokens.tSTRING_END;
        }
        
        ByteList buffer = createByteList(lexer);        

        c = src.read();
        if ((flags & RipperLexer.STR_FUNC_QWORDS) != 0 && Character.isWhitespace(c)) {
            do { 
                buffer.append((char) c);
                c = src.read();
            } while (Character.isWhitespace(c));
            spaceSeen = true;
        }

        if (c == end && nest == 0) {
            return endFound(lexer, src, buffer);
        }
        
        if (spaceSeen) {
            src.unread(c);
            lexer.getPosition();
            lexer.setValue(new Token(buffer, lexer.getPosition()));
            return ' ';
        }        

        if ((flags & RipperLexer.STR_FUNC_EXPAND) != 0 && c == '#') {
            c = src.read();
            switch (c) {
            case '$':
            case '@':
                src.unread(c);
                lexer.setValue(new Token("#", lexer.getPosition()));
                return Tokens.tSTRING_DVAR;
            case '{':
                lexer.setValue(new Token("#{", lexer.getPosition())); 
                return Tokens.tSTRING_DBEG;
            }
            buffer.append((byte) '#');
        }
        src.unread(c);
        
        if (parseStringIntoBuffer(lexer, src, buffer) == RipperLexer.EOF) {
            throw new SyntaxException(PID.STRING_HITS_EOF, lexer.getPosition(),
                    src.getCurrentLine(), "unterminated string meets end of file");
        }

        lexer.setValue(lexer.createStr(lexer.getPosition(), buffer, flags));
        return Tokens.tSTRING_CONTENT;
    }

    private String parseRegexpFlags(RipperLexer lexer, LexerSource src) throws IOException {
        StringBuilder buf = new StringBuilder(""+end);

        int c;
        StringBuilder unknownFlags = new StringBuilder(10);

        for (c = src.read(); c != RipperLexer.EOF
                && Character.isLetter(c); c = src.read()) {
            switch (c) {
                case 'i': case 'x': case 'm': case 'o': case 'n':
                case 'e': case 's': case 'u':
                    buf.append((char) c);
                break;
            default:
                unknownFlags.append((char) c);
                break;
            }
        }
        src.unread(c);
        if (unknownFlags.length() != 0) {
            throw new SyntaxException(PID.REGEXP_UNKNOWN_OPTION, lexer.getPosition(), "unknown regexp option"
                    + (unknownFlags.length() > 1 ? "s" : "") + " - "
                    + unknownFlags.toString(), unknownFlags.toString());
        }
        return buf.toString();
    }

    private void mixedEscape(RipperLexer lexer, Encoding foundEncoding, Encoding parserEncoding) {
        throw new SyntaxException(PID.MIXED_ENCODING,lexer.getPosition(), "",
                foundEncoding + " mixed within " + parserEncoding);
    }

    // mri: parser_tokadd_string
    public int parseStringIntoBuffer(RipperLexer lexer, LexerSource src, ByteList buffer) throws IOException {
        boolean qwords = (flags & RipperLexer.STR_FUNC_QWORDS) != 0;
        boolean expand = (flags & RipperLexer.STR_FUNC_EXPAND) != 0;
        boolean escape = (flags & RipperLexer.STR_FUNC_ESCAPE) != 0;
        boolean regexp = (flags & RipperLexer.STR_FUNC_REGEXP) != 0;
        boolean symbol = (flags & RipperLexer.STR_FUNC_SYMBOL) != 0;
        boolean hasNonAscii = false;
        int c;
        Encoding encoding = lexer.getEncoding();

        while ((c = src.read()) != RipperLexer.EOF) {
            if (begin != '\0' && c == begin) {
                nest++;
            } else if (c == end) {
                if (nest == 0) {
                    src.unread(c);
                    break;
                }
                nest--;
            } else if (expand && c == '#' && !src.peek('\n')) {
                int c2 = src.read();

                if (c2 == '$' || c2 == '@' || c2 == '{') {
                    src.unread(c2);
                    src.unread(c);
                    break;
                }
                src.unread(c2);
            } else if (c == '\\') {
                c = src.read();
                switch (c) {
                case '\n':
                    if (qwords) break;
                    if (expand) continue;
                    buffer.append('\\');
                    break;

                case '\\':
                    if (escape) buffer.append(c);
                    break;

                case 'u':
                    if (!expand) {
                        buffer.append('\\');
                        break;
                    }

                    if (regexp) {
                        lexer.readUTFEscapeRegexpLiteral(buffer);
                    } else {
                        lexer.readUTFEscape(buffer, true, symbol);
                    }

                    if (hasNonAscii && buffer.getEncoding() != encoding) {
                        mixedEscape(lexer, buffer.getEncoding(), encoding);
                    }

                    continue;
                default:
                    if (regexp) {
                        src.unread(c);
                        parseEscapeIntoBuffer(lexer, src, buffer);

                        if (hasNonAscii && buffer.getEncoding() != encoding) {
                            mixedEscape(lexer, buffer.getEncoding(), encoding);
                        }
                        
                        continue;
                    } else if (expand) {
                        src.unread(c);
                        if (escape) buffer.append('\\');
                        c = lexer.readEscape();
                    } else if (qwords && Character.isWhitespace(c)) {
                        /* ignore backslashed spaces in %w */
                    } else if (c != end && !(begin != '\0' && c == begin)) {
                        buffer.append('\\');
                    }
                }
            } else if (!Encoding.isAscii((byte) c)) {
                if (buffer.getEncoding() != encoding) {
                    mixedEscape(lexer, buffer.getEncoding(), encoding);
                }
                c = src.readCodepoint(c, encoding);
                if (c == -2) { // FIXME: Hack
                    throw new SyntaxException(PID.INVALID_MULTIBYTE_CHAR, lexer.getPosition(),
                            null, "invalid multibyte char (" + encoding + ")");
                }

                // FIXME: We basically go from bytes to codepoint back to bytes to append them...fix this
                if (lexer.tokenAddMBC(c, buffer) == RipperLexer.EOF) return RipperLexer.EOF;

                continue;
            } else if (qwords && Character.isWhitespace(c)) {
                src.unread(c);
                break;
            }

            // Hmm did they change this?
/*                if (c == '\0' && symbol) {
                    throw new SyntaxException(PID.NUL_IN_SYMBOL, lexer.getPosition(),
                            src.getCurrentLine(), "symbol cannot contain '\\0'");
                            * } else*/
            if ((c & 0x80) != 0) {
                hasNonAscii = true;
                if (buffer.getEncoding() != encoding) {
                    mixedEscape(lexer, buffer.getEncoding(), encoding);
                }
            }
            buffer.append(c);
        }
        
        return c;
    }

    // Was a goto in original ruby lexer
    private void escaped(RipperLexer lexer, LexerSource src, ByteList buffer) throws java.io.IOException {
        int c;

        switch (c = src.read()) {
        case '\\':
            parseEscapeIntoBuffer(lexer, src, buffer);
            break;
        case RipperLexer.EOF:
            throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, lexer.getPosition(),
                    src.getCurrentLine(), "Invalid escape character syntax");
        default:
            buffer.append(c);
        }
    }

    private void parseEscapeIntoBuffer(RipperLexer lexer, LexerSource src, ByteList buffer) throws java.io.IOException {
        int c;

        switch (c = src.read()) {
        case '\n':
            break; /* just ignore */
        case '0':
        case '1':
        case '2':
        case '3': /* octal constant */
        case '4':
        case '5':
        case '6':
        case '7':
            buffer.append('\\');
            buffer.append(c);
            for (int i = 0; i < 2; i++) {
                c = src.read();
                if (c == RipperLexer.EOF) {
                    throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, lexer.getPosition(),
                            src.getCurrentLine(), "Invalid escape character syntax");
                }
                if (!RipperLexer.isOctChar(c)) {
                    src.unread(c);
                    break;
                }
                buffer.append(c);
            }
            break;
        case 'x': /* hex constant */
            buffer.append('\\');
            buffer.append(c);
            c = src.read();
            if (!RipperLexer.isHexChar(c)) {
                throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, lexer.getPosition(),
                        src.getCurrentLine(), "Invalid escape character syntax");
            }
            buffer.append(c);
            c = src.read();
            if (RipperLexer.isHexChar(c)) {
                buffer.append(c);
            } else {
                src.unread(c);
            }
            break;
        case 'M':
            if ((c = src.read()) != '-') {
                throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, lexer.getPosition(),
                        src.getCurrentLine(), "Invalid escape character syntax");
            }
            buffer.append(new byte[] { '\\', 'M', '-' });
            escaped(lexer, src, buffer);
            break;
        case 'C':
            if ((c = src.read()) != '-') {
                throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, lexer.getPosition(),
                        src.getCurrentLine(), "Invalid escape character syntax");
            }
            buffer.append(new byte[] { '\\', 'C', '-' });
            escaped(lexer, src, buffer);
            break;
        case 'c':
            buffer.append(new byte[] { '\\', 'c' });
            escaped(lexer, src, buffer);
            break;
        case RipperLexer.EOF:
            throw new SyntaxException(PID.INVALID_ESCAPE_SYNTAX, lexer.getPosition(),
                    src.getCurrentLine(), "Invalid escape character syntax");
        default:
            if (c != '\\' || c != end) {
                buffer.append('\\');
            }
            buffer.append(c);
        }
    }
}