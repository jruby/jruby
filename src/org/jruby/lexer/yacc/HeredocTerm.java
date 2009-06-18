/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
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
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.parser.Tokens;
import org.jruby.util.ByteList;


/**
 * A lexing unit for scanning a heredoc element.
 * Example:
 * <pre>
 * foo(<<EOS, bar)
 * This is heredoc country!
 * EOF
 * 
 * Where:
 * EOS = marker
 * ',bar)\n' = lastLine
 * </pre>
 *  
 */
public class HeredocTerm extends StrTerm {
    // Marker delimiting heredoc boundary
    private final ByteList marker;

    // Expand variables, Indentation of final marker
    private final int flags;

    // Portion of line right after beginning marker
    private final ByteList lastLine;
    
    public HeredocTerm(ByteList marker, int func, ByteList lastLine) {
        this.marker = marker;
        this.flags = func;
        this.lastLine = lastLine;
    }
    
    public int parseString(RubyYaccLexer lexer, LexerSource src) throws java.io.IOException {
        boolean indent = (flags & RubyYaccLexer.STR_FUNC_INDENT) != 0;

        if (src.peek(RubyYaccLexer.EOF)) syntaxError(src);

        // Found end marker for this heredoc
        if (src.lastWasBeginOfLine() && src.matchMarker(marker, indent, true)) {
            ISourcePosition position = lexer.getPosition();
            
            // Put back lastLine for any elements past start of heredoc marker
            src.unreadMany(lastLine);
            
            lexer.yaccValue = new Token(marker, position);
            return Tokens.tSTRING_END;
        }

        ByteList str = new ByteList();
        ISourcePosition position;
        
        if ((flags & RubyYaccLexer.STR_FUNC_EXPAND) == 0) {
            do {
                str.append(src.readLineBytes());
                str.append('\n');
                if (src.peek(RubyYaccLexer.EOF)) syntaxError(src);
                position = lexer.getPosition();
            } while (!src.matchMarker(marker, indent, true));
        } else {
            int c = src.read();
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
                str.append('#');
            }

            src.unread(c);

            // MRI has extra pointer which makes our code look a little bit
            // more strange in
            // comparison
            do {
                if ((c = new StringTerm(flags, '\0', '\n').parseStringIntoBuffer(lexer, src, str)) == RubyYaccLexer.EOF) {
                    syntaxError(src);
                }
                if (c != '\n') {
                    lexer.yaccValue = new StrNode(lexer.getPosition(), str);
                    return Tokens.tSTRING_CONTENT;
                }
                str.append(src.read());
                
                if (src.peek(RubyYaccLexer.EOF)) syntaxError(src);
                position = lexer.getPosition();
            } while (!src.matchMarker(marker, indent, true));
        }

        src.unreadMany(lastLine);
        lexer.setStrTerm(new StringTerm(-1, '\0', '\0'));
        lexer.yaccValue = new StrNode(position, str);
        return Tokens.tSTRING_CONTENT;
    }
    
    private void syntaxError(LexerSource src) {
        throw new SyntaxException(PID.STRING_MARKER_MISSING, src.getPosition(), src.getCurrentLine(), 
                "can't find string \"" + marker + "\" anywhere before EOF", marker);
    }
}
