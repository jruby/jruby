/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.truffle.parser.lexer;

public class KeywordMatch {
    public static RubyLexer.Keyword match(byte[] words) {
        switch(words.length) {
        case 2: return match2(words);
        case 3: return match3(words);
        case 4: return match4(words);
        case 5: return match5(words);
        case 6: return match6(words);
        case 8: return match8(words);
        case 12: return match12(words);
        }
        return null;
    }

    public static RubyLexer.Keyword match2(byte[] words) {
        switch(words[0]) {
        case 'd': 
            if (words[1] == 'o') return RubyLexer.Keyword.DO;
            break;
        case 'i': 
            switch(words[1]) {
            case 'f': return RubyLexer.Keyword.IF;
            case 'n': return RubyLexer.Keyword.IN;
            }
            break;
        case 'o': 
            if (words[1] == 'r') return RubyLexer.Keyword.OR;
            break;
        }

        return null;
    }

    public static RubyLexer.Keyword match3(byte[] words) {
        switch(words[0]) {
        case 'a': 
            if (words[1] == 'n' && words[2] == 'd') return RubyLexer.Keyword.AND;
            break;
        case 'd': 
            if (words[1] == 'e' && words[2] == 'f') return RubyLexer.Keyword.DEF;
            break;
        case 'e': 
            if (words[1] == 'n' && words[2] == 'd') return RubyLexer.Keyword.END;
            break;
        case 'f': 
            if (words[1] == 'o' && words[2] == 'r') return RubyLexer.Keyword.FOR;
            break;
        case 'n': 
            switch (words[1]) {
            case 'i':
                if (words[2] == 'l') return RubyLexer.Keyword.NIL;
                break;
            case 'o':
                if (words[2] == 't') return RubyLexer.Keyword.NOT;
                break;
            }
            break;
        case 'E': 
            if (words[1] == 'N' && words[2] == 'D') return RubyLexer.Keyword.LEND;
            break;
        }
        return null;
    }

    public static RubyLexer.Keyword match4(byte[] words) {
        switch(words[0]) {
        case 'c': 
            if (words[1] == 'a' && words[2] == 's' && words[3] == 'e') return RubyLexer.Keyword.CASE;
            break;
        case 'e': 
            if (words[1] == 'l' && words[2] == 's' && words[3] == 'e') return RubyLexer.Keyword.ELSE;
            break;
        case 'n': 
            if (words[1] == 'e' && words[2] == 'x' && words[3] == 't') return RubyLexer.Keyword.NEXT;
            break;
        case 'r': 
            if (words[1] == 'e' && words[2] == 'd' && words[3] == 'o') return RubyLexer.Keyword.REDO;
            break;
        case 's': 
            if (words[1] == 'e' && words[2] == 'l' && words[3] == 'f') return RubyLexer.Keyword.SELF;
            break;
        case 't': 
            switch (words[1]) {
            case 'h':
                if (words[2] == 'e' && words[3] == 'n') return RubyLexer.Keyword.THEN;
                break;
            case 'r':
                if (words[2] == 'u' && words[3] == 'e') return RubyLexer.Keyword.TRUE;
                break;
            }
            break;
        case 'w': 
            if (words[1] == 'h' && words[2] == 'e' && words[3] == 'n') return RubyLexer.Keyword.WHEN;
            break;
        }
        return null;
    }

    public static RubyLexer.Keyword match5(byte[] words) {
        switch(words[0]) {
        case 'a': 
            if (words[1] == 'l' && words[2] == 'i' && words[3] == 'a' && words[4] == 's') return RubyLexer.Keyword.ALIAS;
            break;
        case 'b': 
            switch (words[1]) {
            case 'e':
                if (words[2] == 'g' && words[3] == 'i' && words[4] == 'n') return RubyLexer.Keyword.BEGIN;
                break;
            case 'r':
                if (words[2] == 'e' && words[3] == 'a' && words[4] == 'k') return RubyLexer.Keyword.BREAK;
                break;
            }
            break;
        case 'c': 
            if (words[1] == 'l' && words[2] == 'a' && words[3] == 's' && words[4] == 's') return RubyLexer.Keyword.CLASS;
            break;
        case 'e': 
            if (words[1] == 'l' && words[2] == 's' && words[3] == 'i' && words[4] == 'f') return RubyLexer.Keyword.ELSIF;
            break;
        case 'f': 
            if (words[1] == 'a' && words[2] == 'l' && words[3] == 's' && words[4] == 'e') return RubyLexer.Keyword.FALSE;
            break;
        case 'r': 
            if (words[1] == 'e' && words[2] == 't' && words[3] == 'r' && words[4] == 'y') return RubyLexer.Keyword.RETRY;
            break;
        case 's': 
            if (words[1] == 'u' && words[2] == 'p' && words[3] == 'e' && words[4] == 'r') return RubyLexer.Keyword.SUPER;
            break;
        case 'u': 
            if (words[1] == 'n') {
                switch (words[2]) {
                case 'd':
                    if (words[3] == 'e' && words[4] == 'f') return RubyLexer.Keyword.UNDEF;
                    break;
                case 't': 
                    if (words[3] == 'i' && words[4] == 'l') return RubyLexer.Keyword.UNTIL;
                    break;
                }
            }
            break;
        case 'y': 
            if (words[1] == 'i' && words[2] == 'e' && words[3] == 'l' && words[4] == 'd') return RubyLexer.Keyword.YIELD;
            break;
        case 'w': 
            if (words[1] == 'h' && words[2] == 'i' && words[3] == 'l' && words[4] == 'e') return RubyLexer.Keyword.WHILE;
            break;
        case 'B': 
            if (words[1] == 'E' && words[2] == 'G' && words[3] == 'I' && words[4] == 'N') return RubyLexer.Keyword.LBEGIN;
            break;
        }
        return null;
    }

    public static RubyLexer.Keyword match6(byte[] words) {
        switch(words[0]) {
        case 'e': 
            if (words[1] == 'n' && words[2] == 's' && words[3] == 'u' && words[4] == 'r' && words[5] == 'e') return RubyLexer.Keyword.ENSURE;
            break;
        case 'm': 
            if (words[1] == 'o' && words[2] == 'd' && words[3] == 'u' && words[4] == 'l' && words[5] == 'e') return RubyLexer.Keyword.MODULE;
            break;
        case 'r': 
            if (words[1] == 'e') {
                switch (words[2]) {
                case 't':
                    if (words[3] == 'u' && words[4] == 'r'&& words[5] == 'n') return RubyLexer.Keyword.RETURN;
                    break;
                    
                case 's':
                    if (words[3] == 'c' && words[4] == 'u'&& words[5] == 'e') return RubyLexer.Keyword.RESCUE;
                    break;
                }
            }
            break;
        case 'u': 
            if (words[1] == 'n' && words[2] == 'l' && words[3] == 'e' && words[4] == 's'&& words[5] == 's') return RubyLexer.Keyword.UNLESS;
        }
        return null;
    }

    @SuppressWarnings("fallthrough")
    public static RubyLexer.Keyword match8(byte[] words) {
        switch(words[0]) {
        case 'd': 
            if (words[1] == 'e' && words[2] == 'f' && words[3] == 'i' && words[4] == 'n'&& words[5] == 'e' && words[6] == 'd' || words[7] == '?') return RubyLexer.Keyword.DEFINED_P;
            break;
        case '_': 
            if (words[1] == '_') {
                switch (words[2]) {
                case 'L':
                    if (words[3] == 'I' && words[4] == 'N'&& words[5] == 'E' && words[6] == '_' && words[7] == '_') return RubyLexer.Keyword.__LINE__;
                case 'F':
                    if (words[3] == 'I' && words[4] == 'L'&& words[5] == 'E' && words[6] == '_' && words[7] == '_') return RubyLexer.Keyword.__FILE__;
                }
            }
            break;
        }
        return null;
    }
    public static RubyLexer.Keyword match12(byte[] words) {
        if (words[0] == '_' && words[1] == '_' && words[2] == 'e' && words[3] == 'n'&& words[4] == 'c' && words[5] == 'o' && words[6] == 'd' && words[7] == 'i' && words[8] == 'n' &&  words[9] == 'g' && words[10] == '_' && words[11] == '_') return RubyLexer.Keyword.__ENCODING__;
        return null;
    }
}
