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

import org.jruby.ast.StrNode;
import org.jruby.parser.Tokens;


public class HeredocTerm extends StrTerm {
	private final String eos;
	private final int func;
	private final String lastLine;
    
    public HeredocTerm(String eos, int func, String lastLine) {
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
            return Tokens.tSTRING_END;
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
                    lexer.setValue(new Token("#" + c, lexer.getPosition()));
                    return Tokens.tSTRING_DVAR;
                case '{':
                    lexer.setValue(new Token("#" + c, lexer.getPosition()));
                    return Tokens.tSTRING_DBEG;
                }
                buffer.append('#');
            }

            src.unread(c);

            do {
                if ((c = new StringTerm(func, '\n', '\0').parseStringIntoBuffer(src, buffer)) == RubyYaccLexer.EOF) {
                    throw new SyntaxException(src.getPosition(), "can't find string \"" + eos + "\" anywhere before EOF");
                }
                if (c != '\n') {
                    lexer.yaccValue = new StrNode(lexer.getPosition(), buffer.toString());
                    return Tokens.tSTRING_CONTENT;
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
        lexer.setStrTerm(new StringTerm(-1, '\0', '\0'));
        lexer.yaccValue = new StrNode(lexer.getPosition(), str.toString());
        return Tokens.tSTRING_CONTENT;
    }
}
