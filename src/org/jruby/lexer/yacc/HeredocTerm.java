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

import org.jruby.parser.Token;


public class HeredocTerm extends StrTerm {
	private final String eos;
	private final int func;
	private final String lastLine;
    
    public HeredocTerm(SourcePosition position, String eos, int func, String lastLine) {
        this.eos = eos;
        this.func = func;
        this.lastLine = lastLine;
    }
    
    public int parseString(RubyYaccLexer lexer, LexerSource src) {
        char c;
        boolean indent = (func & RubyYaccLexer.STR_FUNC_INDENT) != 0;
        StringBuffer str = new StringBuffer();

        if ((c = src.read()) == RubyYaccLexer.EOF) {
            throw new SyntaxException(src.getPosition(), "can't find string \"" + eos + "\" anywhere before EOF");
        }
        if (src.wasBeginOfLine() && src.matchString(eos + '\n', indent)) {
            src.unreadMany(lastLine);
            return Token.tSTRING_END;
        }

        if ((func & RubyYaccLexer.STR_FUNC_EXPAND) == 0) {
            /*
             * if (c == '\n') { support.unread(c); }
             */

            // Something missing here...
            /*
             * int lastLineLength = here.getLastLineLength();
             * 
             * if (lastLineLength > 0) { // It looks like I needed to append
             * last line as well...
             * support.unreadMany(here.getLastLineLength());
             * str.append(support.readLine()); str.append("\n"); }
             */

            /*
             * c was read above and should be unread before we start
             * to fill the str buffer
             */
            src.unread(c);
            do {
                str.append(src.readLine());
                str.append("\n");

                if (src.peek('\0')) {
                    throw new SyntaxException(src.getPosition(), "can't find string \"" + eos + "\" anywhere before EOF");
                }
            } while (!src.matchString(eos + '\n', indent));
        } else {
            StringBuffer buffer = new StringBuffer(100);
            if (c == '#') {
                switch (c = src.read()) {
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

            do {
                if ((c = new StringTerm(src.getPosition(), func, '\n', '\0').parseStringIntoBuffer(src, buffer)) == RubyYaccLexer.EOF) {
                    throw new SyntaxException(src.getPosition(), "can't find string \"" + eos + "\" anywhere before EOF");
                }
                if (c != '\n') {
                    lexer.yaccValue = buffer.toString();
                    return Token.tSTRING_CONTENT;
                }
                buffer.append(src.read());
                if ((c = src.read()) == RubyYaccLexer.EOF) {
                    throw new SyntaxException(src.getPosition(), "can't find string \"" + eos + "\" anywhere before EOF");
                }
                // We need to pushback so when whole match looks it did not
                // lose a char during last EOF
                src.unread(c);
            } while (!src.matchString(eos + '\n', indent));
            str = new StringBuffer(buffer.toString());
        }

        src.unreadMany(lastLine);
        lexer.setStrTerm(new StringTerm(src.getPosition(), -1, '\0', '\0'));
        lexer.yaccValue = str.toString();
        return Token.tSTRING_CONTENT;
    }
}