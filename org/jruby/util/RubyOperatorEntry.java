/*
 * RubyOperatorEntry.java - No description
 * Created on 09. Oktober 2001, 00:48
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.util;

import org.jruby.*;
import org.jruby.parser.*;

/**
 *
 * @author  jpetersen
 * @version 
 */
public class RubyOperatorEntry {
    public RubyId token;
    public String name;

    /** Creates new RubyOperatorEntry */
    public RubyOperatorEntry(Ruby ruby, int token, String name) {
        this.token = RubyId.newId(ruby, token);
        this.name = name;
    }

    public static void initOperatorTable(Ruby ruby) {
        ruby.setOperatorTable(new RubyOperatorEntry[] {
            new RubyOperatorEntry(ruby, Token.tDOT2,    ".."),
            new RubyOperatorEntry(ruby, Token.tDOT3,    "..."),
            new RubyOperatorEntry(ruby, '+',            "+"),
            new RubyOperatorEntry(ruby, '-',            "-"),
            new RubyOperatorEntry(ruby, '+',            "+(binary)"),
            new RubyOperatorEntry(ruby, '-',            "-(binary)"),
            new RubyOperatorEntry(ruby, '*',            "*"),
            new RubyOperatorEntry(ruby, '/',            "/"),
            new RubyOperatorEntry(ruby, '%',            "%"),
            new RubyOperatorEntry(ruby, Token.tPOW,     "**"),
            new RubyOperatorEntry(ruby, Token.tUPLUS,   "+@"),
            new RubyOperatorEntry(ruby, Token.tUMINUS,  "-@"),
            new RubyOperatorEntry(ruby, Token.tUPLUS,	"+(unary)"),
            new RubyOperatorEntry(ruby, Token.tUMINUS,  "-(unary)"),
            new RubyOperatorEntry(ruby, '|',            "|"),
            new RubyOperatorEntry(ruby, '^',            "^"),
            new RubyOperatorEntry(ruby, '&',            "&"),
            new RubyOperatorEntry(ruby, Token.tCMP,     "<=>"),
            new RubyOperatorEntry(ruby, '>',            ">"),
            new RubyOperatorEntry(ruby, Token.tGEQ,     ">="),
            new RubyOperatorEntry(ruby, '<',            "<"),
            new RubyOperatorEntry(ruby, Token.tLEQ,     "<="),
            new RubyOperatorEntry(ruby, Token.tEQ,      "=="),
            new RubyOperatorEntry(ruby, Token.tEQQ,     "==="),
            new RubyOperatorEntry(ruby, Token.tNEQ,     "!="),
            new RubyOperatorEntry(ruby, Token.tMATCH,   "=~"),
            new RubyOperatorEntry(ruby, Token.tNMATCH,  "!~"),
            new RubyOperatorEntry(ruby, '!',            "!"),
            new RubyOperatorEntry(ruby, '~',            "~"),
            new RubyOperatorEntry(ruby, '!',            "!(unary)"),
            new RubyOperatorEntry(ruby, '~',            "~(unary)"),
            new RubyOperatorEntry(ruby, '!',            "!@"),
            new RubyOperatorEntry(ruby, '~',            "~@"),
            new RubyOperatorEntry(ruby, Token.tAREF,    "[]"),
            new RubyOperatorEntry(ruby, Token.tASET,    "[]="),
            new RubyOperatorEntry(ruby, Token.tLSHFT,   "<<"),
            new RubyOperatorEntry(ruby, Token.tRSHFT,   ">>"),
            new RubyOperatorEntry(ruby, Token.tCOLON2,  "::"),
            new RubyOperatorEntry(ruby, '`',            "`"),
            // new RubyOperatorEntry(this, 0,              null),
        });
    }
}