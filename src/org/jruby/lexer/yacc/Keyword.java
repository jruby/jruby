/*
 * kwtable.java - No description
 * Created on 10. September 2001, 17:51
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo 
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.lexer.yacc;

import org.jruby.parser.Token;

public class Keyword implements Token {
	public String name;
    public int id0, id1;
    public LexState state;

    private Keyword() {
        this("", 0, 0, LexState.EXPR_BEG);
    }

    private Keyword(String name, int id0, int id1, LexState state) {
        this.name = name;
        this.id0 = id0;
        this.id1 = id1;
        this.state = state;
    }

    private static final int TOTAL_KEYWORDS = 40;
    private static final int MIN_WORD_LENGTH = 2;
    private static final int MAX_WORD_LENGTH = 8;
    private static final int MIN_HASH_VALUE = 6;
    private static final int MAX_HASH_VALUE = 55;

    private static final byte[] asso_values = {
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 11, 56, 56, 36, 56,  1, 37,
        31,  1, 56, 56, 56, 56, 29, 56,  1, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56,  1, 56, 32,  1,  2,
        1,  1,  4, 23, 56, 17, 56, 20,  9,  2,
        9, 26, 14, 56,  5,  1,  1, 16, 56, 21,
        20,  9, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56, 56, 56, 56, 56,
        56, 56, 56, 56, 56, 56
    };

    private static int hash(String str, int len) {
        int hval = len;
        switch (hval) {
        default:
        case 3:
            hval += asso_values[str.charAt(2) & 255];
        case 2:
        case 1:
            hval += asso_values[str.charAt(0) & 255];
            break;
        }
        return hval + asso_values[str.charAt(len - 1) & 255];
    }

    private static final Keyword[] wordlist = {
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword("end", kEND, kEND, LexState.EXPR_END),
        new Keyword("else", kELSE, kELSE, LexState.EXPR_BEG),
        new Keyword("case", kCASE, kCASE, LexState.EXPR_BEG),
        new Keyword("ensure", kENSURE, kENSURE, LexState.EXPR_BEG),
        new Keyword("module", kMODULE, kMODULE, LexState.EXPR_BEG),
        new Keyword("elsif", kELSIF, kELSIF, LexState.EXPR_BEG),
        new Keyword("def", kDEF, kDEF, LexState.EXPR_FNAME),
        new Keyword("rescue", kRESCUE, kRESCUE_MOD, LexState.EXPR_MID),
        new Keyword("not", kNOT, kNOT, LexState.EXPR_BEG),
        new Keyword("then", kTHEN, kTHEN, LexState.EXPR_BEG),
        new Keyword("yield", kYIELD, kYIELD, LexState.EXPR_ARG),
        new Keyword("for", kFOR, kFOR, LexState.EXPR_BEG),
        new Keyword("self", kSELF, kSELF, LexState.EXPR_END),
        new Keyword("false", kFALSE, kFALSE, LexState.EXPR_END),
        new Keyword("retry", kRETRY, kRETRY, LexState.EXPR_END),
        new Keyword("return", kRETURN, kRETURN, LexState.EXPR_MID),
        new Keyword("true", kTRUE, kTRUE, LexState.EXPR_END),
        new Keyword("if", kIF, kIF_MOD, LexState.EXPR_BEG),
        new Keyword("defined?", kDEFINED, kDEFINED, LexState.EXPR_ARG),
        new Keyword("super", kSUPER, kSUPER, LexState.EXPR_ARG),
        new Keyword("undef", kUNDEF, kUNDEF, LexState.EXPR_FNAME),
        new Keyword("break", kBREAK, kBREAK, LexState.EXPR_MID),
        new Keyword("in", kIN, kIN, LexState.EXPR_BEG),
        new Keyword("do", kDO, kDO, LexState.EXPR_BEG),
        new Keyword("nil", kNIL, kNIL, LexState.EXPR_END),
        new Keyword("until", kUNTIL, kUNTIL_MOD, LexState.EXPR_BEG),
        new Keyword("unless", kUNLESS, kUNLESS_MOD, LexState.EXPR_BEG),
        new Keyword("or", kOR, kOR, LexState.EXPR_BEG),
        new Keyword("next", kNEXT, kNEXT, LexState.EXPR_MID),
        new Keyword("when", kWHEN, kWHEN, LexState.EXPR_BEG),
        new Keyword("redo", kREDO, kREDO, LexState.EXPR_END),
        new Keyword("and", kAND, kAND, LexState.EXPR_BEG),
        new Keyword("begin", kBEGIN, kBEGIN, LexState.EXPR_BEG),
        new Keyword("__LINE__", k__LINE__, k__LINE__, LexState.EXPR_END),
        new Keyword("class", kCLASS, kCLASS, LexState.EXPR_CLASS),
        new Keyword("__FILE__", k__FILE__, k__FILE__, LexState.EXPR_END),
        new Keyword("END", klEND, klEND, LexState.EXPR_END),
        new Keyword("BEGIN", klBEGIN, klBEGIN, LexState.EXPR_END),
        new Keyword("while", kWHILE, kWHILE_MOD, LexState.EXPR_BEG),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword(),
        new Keyword("alias", kALIAS, kALIAS, LexState.EXPR_FNAME)
    };

    public static Keyword getKeyword(String str, int len) {
        if (len <= MAX_WORD_LENGTH && len >= MIN_WORD_LENGTH) {
            int key = hash(str, len);
            if (key <= MAX_HASH_VALUE && key >= MIN_HASH_VALUE) {
                if (str.equals(wordlist[key].name)) {
					return wordlist[key];
				}
            }
        }
        return null;
    }
}
