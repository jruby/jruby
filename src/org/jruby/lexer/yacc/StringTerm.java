/*
 * Copyright (C) 2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.lexer.yacc;

import org.jruby.ast.RegexpNode;
import org.jruby.parser.ReOptions;
import org.jruby.parser.Token;

public class StringTerm extends StrTerm {
    /* bit flags to indicate the string type */
	private int func;

    private final char term;

    private final char paren;

    /* nested string level */
    private int nest;

    public StringTerm(SourcePosition position, int func, char term, char paren) {
        this.func = func;
        this.term = term;
        this.paren = paren;
        this.nest = 0;
    }

    public int parseString(final RubyYaccLexer lexer, LexerSource src) {
        char c;
        int space = 0;
        StringBuffer buffer = new StringBuffer(100);

        if (func == -1)
            return Token.tSTRING_END;

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
                return ' ';
            }
            if ((func & RubyYaccLexer.STR_FUNC_REGEXP) != 0) {
                lexer.yaccValue = new RegexpNode(src.getPosition(),
                        buffer.toString(), parseRegexpFlags(src));
                return Token.tREGEXP_END;
            }
            return Token.tSTRING_END;
        }
        if (space != 0) {
            src.unread(c);
            return ' ';
        }
        if ((func & RubyYaccLexer.STR_FUNC_EXPAND) != 0 && c == '#') {
            c = src.read();
            switch (c) {
            case '$':
            case '@':
                src.unread(c);
                return Token.tSTRING_DVAR;
            case '{':
                return Token.tSTRING_DBEG;
            }
            buffer.append('#');
        }
        src.unread(c);
        if (parseStringIntoBuffer(src, buffer) == 0) {
            throw new SyntaxException(src.getPosition(), "unterminated string meets end of file");
        }

        lexer.yaccValue = buffer.toString();
        return Token.tSTRING_CONTENT;
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
            } else if ((func & RubyYaccLexer.STR_FUNC_EXPAND) != 0 && c == '#'
                    && !src.peek('\n')) {
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
            throw new SyntaxException(src.getPosition(),
                    "Invalid escape character syntax");
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
                    throw new SyntaxException(src.getPosition(),
                            "Invalid escape character syntax");
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
                throw new SyntaxException(src.getPosition(),
                        "Invalid escape character syntax");
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
                throw new SyntaxException(src.getPosition(),
                        "Invalid escape character syntax");
            }
            buffer.append("\\M-");
            escaped(src, buffer);
            break;
        case 'C':
            if ((c = src.read()) != '-') {
                throw new SyntaxException(src.getPosition(),
                        "Invalid escape character syntax");
            }
            buffer.append("\\C-");
            escaped(src, buffer);
            break;
        case 'c':
            buffer.append("\\c");
            escaped(src, buffer);
            break;
        case 0:
            throw new SyntaxException(src.getPosition(),
                    "Invalid escape character syntax");
        default:
            if (c != '\\' || c != term) {
                buffer.append('\\');
            }
            buffer.append(c);
        }
    }
}