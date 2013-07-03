package org.jruby.lexer.yacc;

import org.jruby.lexer.yacc.RubyYaccLexer.Keyword;

public class KeywordMatch {
    public static Keyword match(byte[] words) {
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

    public static Keyword match2(byte[] words) {
        switch(words[0]) {
        case 'd': 
            if (words[1] == 'o') return Keyword.DO;
            break;
        case 'i': 
            switch(words[1]) {
            case 'f': return Keyword.IF; 
            case 'n': return Keyword.IN;
            }
            break;
        case 'o': 
            if (words[1] == 'r') return Keyword.OR;
            break;
        }

        return null;
    }

    public static Keyword match3(byte[] words) {
        switch(words[0]) {
        case 'a': 
            if (words[1] == 'n' && words[2] == 'd') return Keyword.AND;
            break;
        case 'd': 
            if (words[1] == 'e' && words[2] == 'f') return Keyword.DEF;
            break;
        case 'e': 
            if (words[1] == 'n' && words[2] == 'd') return Keyword.END;
            break;
        case 'f': 
            if (words[1] == 'o' && words[2] == 'r') return Keyword.FOR;
            break;
        case 'n': 
            switch (words[1]) {
            case 'i':
                if (words[2] == 'l') return Keyword.NIL;
                break;
            case 'o':
                if (words[2] == 't') return Keyword.NOT;
                break;
            }
            break;
        case 'E': 
            if (words[1] == 'N' && words[2] == 'D') return Keyword.LEND;
            break;
        }
        return null;
    }

    public static Keyword match4(byte[] words) {
        switch(words[0]) {
        case 'c': 
            if (words[1] == 'a' && words[2] == 's' && words[3] == 'e') return Keyword.CASE;
            break;
        case 'e': 
            if (words[1] == 'l' && words[2] == 's' && words[3] == 'e') return Keyword.ELSE;
            break;
        case 'n': 
            if (words[1] == 'e' && words[2] == 'x' && words[3] == 't') return Keyword.NEXT;
            break;
        case 'r': 
            if (words[1] == 'e' && words[2] == 'd' && words[3] == 'o') return Keyword.REDO;
            break;
        case 's': 
            if (words[1] == 'e' && words[2] == 'l' && words[3] == 'f') return Keyword.SELF;
            break;
        case 't': 
            switch (words[1]) {
            case 'h':
                if (words[2] == 'e' && words[3] == 'n') return Keyword.THEN;
                break;
            case 'r':
                if (words[2] == 'u' && words[3] == 'e') return Keyword.TRUE;
                break;
            }
            break;
        case 'w': 
            if (words[1] == 'h' && words[2] == 'e' && words[3] == 'n') return Keyword.WHEN;
            break;
        }
        return null;
    }

    public static Keyword match5(byte[] words) {
        switch(words[0]) {
        case 'a': 
            if (words[1] == 'l' && words[2] == 'i' && words[3] == 'a' && words[4] == 's') return Keyword.ALIAS;
            break;
        case 'b': 
            switch (words[1]) {
            case 'e':
                if (words[2] == 'g' && words[3] == 'i' && words[4] == 'n') return Keyword.BEGIN;
                break;
            case 'r':
                if (words[2] == 'e' && words[3] == 'a' && words[4] == 'k') return Keyword.BREAK;
                break;
            }
            break;
        case 'c': 
            if (words[1] == 'l' && words[2] == 'a' && words[3] == 's' && words[4] == 's') return Keyword.CLASS;
            break;
        case 'e': 
            if (words[1] == 'l' && words[2] == 's' && words[3] == 'i' && words[4] == 'f') return Keyword.ELSIF;
            break;
        case 'f': 
            if (words[1] == 'a' && words[2] == 'l' && words[3] == 's' && words[4] == 'e') return Keyword.FALSE;
            break;
        case 'r': 
            if (words[1] == 'e' && words[2] == 't' && words[3] == 'r' && words[4] == 'y') return Keyword.RETRY;
            break;
        case 's': 
            if (words[1] == 'u' && words[2] == 'p' && words[3] == 'e' && words[4] == 'r') return Keyword.SUPER;
            break;
        case 'u': 
            if (words[1] == 'n') {
                switch (words[2]) {
                case 'd':
                    if (words[3] == 'e' && words[4] == 'f') return Keyword.UNDEF;
                    break;
                case 't': 
                    if (words[3] == 'i' && words[4] == 'l') return Keyword.UNTIL;
                    break;
                }
            }
            break;
        case 'y': 
            if (words[1] == 'i' && words[2] == 'e' && words[3] == 'l' && words[4] == 'd') return Keyword.YIELD;
            break;
        case 'w': 
            if (words[1] == 'h' && words[2] == 'i' && words[3] == 'l' && words[4] == 'e') return Keyword.WHILE;
            break;
        case 'B': 
            if (words[1] == 'E' && words[2] == 'G' && words[3] == 'I' && words[4] == 'N') return Keyword.LBEGIN;
            break;
        }
        return null;
    }

    public static Keyword match6(byte[] words) {
        switch(words[0]) {
        case 'e': 
            if (words[1] == 'n' && words[2] == 's' && words[3] == 'u' && words[4] == 'r' && words[5] == 'e') return Keyword.ENSURE;
            break;
        case 'm': 
            if (words[1] == 'o' && words[2] == 'd' && words[3] == 'u' && words[4] == 'l' && words[5] == 'e') return Keyword.MODULE;
            break;
        case 'r': 
            if (words[1] == 'e') {
                switch (words[2]) {
                case 't':
                    if (words[3] == 'u' && words[4] == 'r'&& words[5] == 'n') return Keyword.RETURN;
                    break;
                    
                case 's':
                    if (words[3] == 'c' && words[4] == 'u'&& words[5] == 'e') return Keyword.RESCUE;
                    break;
                }
            }
            break;
        case 'u': 
            if (words[1] == 'n' && words[2] == 'l' && words[3] == 'e' && words[4] == 's'&& words[5] == 's') return Keyword.UNLESS;
        }
        return null;
    }

    public static Keyword match8(byte[] words) {
        switch(words[0]) {
        case 'd': 
            if (words[1] == 'e' && words[2] == 'f' && words[3] == 'i' && words[4] == 'n'&& words[5] == 'e' && words[6] == 'd' || words[7] == '?') return Keyword.DEFINED_P;
            break;
        case '_': 
            if (words[1] == '_') {
                switch (words[2]) {
                case 'L':
                    if (words[3] == 'I' && words[4] == 'N'&& words[5] == 'E' && words[6] == '_' && words[7] == '_') return Keyword.__LINE__;
                case 'F':
                    if (words[3] == 'I' && words[4] == 'L'&& words[5] == 'E' && words[6] == '_' && words[7] == '_') return Keyword.__FILE__;
                }
            }
            break;
        }
        return null;
    }
    public static Keyword match12(byte[] words) {
        if (words[0] == '_' && words[1] == '_' && words[2] == 'e' && words[3] == 'n'&& words[4] == 'c' && words[5] == 'o' && words[6] == 'd' && words[7] == 'i' && words[8] == 'n' &&  words[9] == 'g' && words[10] == '_' && words[11] == '_') return Keyword.__ENCODING__;
        return null;
    }
}
