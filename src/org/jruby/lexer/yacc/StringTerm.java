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
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.lexer.yacc;

import org.jruby.ast.RegexpNode;
import org.jruby.ast.StrNode;
import org.jruby.parser.ReOptions;
import org.jruby.parser.Tokens;

public class StringTerm extends StrTerm {
    /* bit flags to indicate the string type */
	private int func;

    private final char term;

    private final char paren;

    /* nested string level */
    private int nest;

    public StringTerm(int func, char term, char paren) {
        this.func = func;
        this.term = term;
        this.paren = paren;
        this.nest = 0;
    }

    public int parseString(final RubyYaccLexer lexer, LexerSource src) {
        char c;
        int space = 0;

        if (func == -1) {
            lexer.setValue(new Token("\"", lexer.getPosition()));
            return Tokens.tSTRING_END;
        }

        c = src.read();
        if ((func & RubyYaccLexer.STR_FUNC_QWORDS) != 0
                && Character.isWhitespace(c)) {
            do {
                c = src.read();
            } while (Character.isWhitespace(c));
            space = 1;
        }

        if (c == term && nest == 0) {
            if ((func & RubyYaccLexer.STR_FUNC_QWORDS) != 0) {
                func = -1;
                lexer.getPosition();
                return ' ';
            }
            if ((func & RubyYaccLexer.STR_FUNC_REGEXP) != 0) {
                lexer.setValue(new RegexpNode(src.getPosition(), "", parseRegexpFlags(src)));
                return Tokens.tREGEXP_END;
            }
            lexer.setValue(new Token("\"", lexer.getPosition()));
            return Tokens.tSTRING_END;
        }
        if (space != 0) {
            src.unread(c);
            lexer.getPosition();
            return ' ';
        }
        StringBuffer buffer = lexer.getTokenBuffer();
        buffer.setLength(0);
        if ((func & RubyYaccLexer.STR_FUNC_EXPAND) != 0 && c == '#') {
            c = src.read();
            switch (c) {
            case '$':
            case '@':
                src.unread(c);
                lexer.setValue(new Token("#" + c, lexer.getPosition()));
                return Tokens.tSTRING_DVAR;
            case '{':
                lexer.setValue(new Token("#" + c, lexer.getPosition())); 
                return Tokens.tSTRING_DBEG;
            }
            buffer.append('#');
        }
        src.unread(c);
        if (parseStringIntoBuffer(src, buffer) == 0) {
            throw new SyntaxException(src.getPosition(), "unterminated string meets end of file");
        }

        lexer.setValue(new StrNode(lexer.getPosition(), buffer.toString())); 
        return Tokens.tSTRING_CONTENT;
    }

    private int parseRegexpFlags(final LexerSource src) {
        char kcode = 0;
        int options = 0;
        char c;
        StringBuffer unknownFlags = new StringBuffer(10);

        for (c = src.read(); c != RubyYaccLexer.EOF
                && Character.isLetter(c); c = src.read()) {
            switch (c) {
            case 'i':
                options |= ReOptions.RE_OPTION_IGNORECASE;
                break;
            case 'x':
                options |= ReOptions.RE_OPTION_EXTENDED;
                break;
            case 'm':
                options |= ReOptions.RE_OPTION_MULTILINE;
                break;
            case 'o':
                options |= ReOptions.RE_OPTION_ONCE;
                break;
            case 'n':
                kcode = 16;
                break;
            case 'e':
                kcode = 32;
                break;
            case 's':
                kcode = 48;
                break;
            case 'u':
                kcode = 64;
                break;
            default:
                unknownFlags.append(c);
                break;
            }
        }
        src.unread(c);
        if (unknownFlags.length() != 0) {
            throw new SyntaxException(src.getPosition(), "unknown regexp option"
                    + (unknownFlags.length() > 1 ? "s" : "") + " - "
                    + unknownFlags.toString());
        }
        return options | kcode;
    }

    public char parseStringIntoBuffer(LexerSource src, StringBuffer buffer) {
        char c;

        while ((c = src.read()) != RubyYaccLexer.EOF) {
            if (paren != '\0' && c == paren) {
                nest++;
            } else if (c == term) {
                if (nest == 0) {
                    src.unread(c);
                    break;
                }
                nest--;
            } else if ((func & RubyYaccLexer.STR_FUNC_EXPAND) != 0 && c == '#' && !src.peek('\n')) {
                char c2 = src.read();

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
                    if ((func & RubyYaccLexer.STR_FUNC_QWORDS) != 0) {
                        break;
                    }
                    if ((func & RubyYaccLexer.STR_FUNC_EXPAND) != 0) {
                        continue;
                    }
                    buffer.append('\\');
                    break;

                case '\\':
                    if ((func & RubyYaccLexer.STR_FUNC_ESCAPE) != 0) {
                        buffer.append(c);
                    }
                    break;

                default:
                    if ((func & RubyYaccLexer.STR_FUNC_REGEXP) != 0) {
                        src.unread(c);
                        parseEscapeIntoBuffer(src, buffer);
                        continue;
                    } else if ((func & RubyYaccLexer.STR_FUNC_EXPAND) != 0) {
                        src.unread(c);
                        if ((func & RubyYaccLexer.STR_FUNC_ESCAPE) != 0) {
                            buffer.append('\\');
                        }
                        c = src.readEscape();
                    } else if ((func & RubyYaccLexer.STR_FUNC_QWORDS) != 0
                            && Character.isWhitespace(c)) {
                        /* ignore backslashed spaces in %w */
                    } else if (c != term && !(paren != '\0' && c == paren)) {
                        buffer.append('\\');
                    }
                }
            } else if ((func & RubyYaccLexer.STR_FUNC_QWORDS) != 0
                    && Character.isWhitespace(c)) {
                src.unread(c);
                break;
            }
            if (c == '\0' && (func & RubyYaccLexer.STR_FUNC_SYMBOL) != 0) {
                throw new SyntaxException(src.getPosition(), "symbol cannot contain '\\0'");
            }
            buffer.append(c);
        }
        return c;
    }

    // Was a goto in original ruby lexer
    private void escaped(LexerSource src, StringBuffer buffer) {
        char c;

        switch (c = src.read()) {
        case '\\':
            parseEscapeIntoBuffer(src, buffer);
            break;
        case RubyYaccLexer.EOF:
            throw new SyntaxException(src.getPosition(), "Invalid escape character syntax");
        default:
            buffer.append(c);
        }
    }

    private void parseEscapeIntoBuffer(LexerSource src, StringBuffer buffer) {
        char c;

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
                if (c == RubyYaccLexer.EOF) {
                    throw new SyntaxException(src.getPosition(), "Invalid escape character syntax");
                }
                if (!RubyYaccLexer.isOctChar(c)) {
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
            if (!RubyYaccLexer.isHexChar(c)) {
                throw new SyntaxException(src.getPosition(), "Invalid escape character syntax");
            }
            buffer.append(c);
            c = src.read();
            if (RubyYaccLexer.isHexChar(c)) {
                buffer.append(c);
            } else {
                src.unread(c);
            }
            break;
        case 'M':
            if ((c = src.read()) != '-') {
                throw new SyntaxException(src.getPosition(), "Invalid escape character syntax");
            }
            buffer.append("\\M-");
            escaped(src, buffer);
            break;
        case 'C':
            if ((c = src.read()) != '-') {
                throw new SyntaxException(src.getPosition(), "Invalid escape character syntax");
            }
            buffer.append("\\C-");
            escaped(src, buffer);
            break;
        case 'c':
            buffer.append("\\c");
            escaped(src, buffer);
            break;
        case 0:
            throw new SyntaxException(src.getPosition(), "Invalid escape character syntax");
        default:
            if (c != '\\' || c != term) {
                buffer.append('\\');
            }
            buffer.append(c);
        }
    }
}
