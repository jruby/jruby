/*
 * Constants.java - No description
 * Created on 02. November 2001, 01:25
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.runtime;

public final class Constants {
    public static final int SCOPE_PUBLIC        = 0;
    public static final int SCOPE_PRIVATE       = 1;
    public static final int SCOPE_PROTECTED     = 2;
    public static final int SCOPE_MODFUNC       = 5;
    public static final int SCOPE_MASK          = 7;
    
    public static final int NOEX_PUBLIC         = 0;
    public static final int NOEX_UNDEF          = 1;
    public static final int NOEX_CFUNC          = 1;
    public static final int NOEX_PRIVATE        = 2;
    public static final int NOEX_PROTECTED      = 4;
                                                
    public static final int NODE_METHOD         = 0;
    public static final int NODE_FBODY          = 1;
    public static final int NODE_CFUNC          = 2;
    public static final int NODE_SCOPE          = 3;
    public static final int NODE_BLOCK          = 4;
    public static final int NODE_IF             = 5;
    public static final int NODE_CASE           = 6;
    public static final int NODE_WHEN           = 7;
    public static final int NODE_OPT_N          = 8;
    public static final int NODE_WHILE          = 9;
    public static final int NODE_UNTIL          = 10;
    public static final int NODE_ITER           = 11;
    public static final int NODE_FOR            = 12;
    public static final int NODE_BREAK          = 13;
    public static final int NODE_NEXT           = 14;
    public static final int NODE_REDO           = 15;
    public static final int NODE_RETRY          = 16;
    public static final int NODE_BEGIN          = 17;
    public static final int NODE_RESCUE         = 18;
    public static final int NODE_RESBODY        = 19;
    public static final int NODE_ENSURE         = 20;
    public static final int NODE_AND            = 21;
    public static final int NODE_OR             = 22;
    public static final int NODE_NOT            = 23;
    public static final int NODE_MASGN          = 24;
    public static final int NODE_LASGN          = 25;
    public static final int NODE_DASGN          = 26;
    public static final int NODE_DASGN_CURR     = 27;
    public static final int NODE_GASGN          = 28;
    public static final int NODE_IASGN          = 29;
    public static final int NODE_CDECL          = 30;
    public static final int NODE_CVASGN         = 31;
    public static final int NODE_CVDECL         = 32;
    public static final int NODE_OP_ASGN1       = 33;
    public static final int NODE_OP_ASGN2       = 34;
    public static final int NODE_OP_ASGN_AND    = 35;
    public static final int NODE_OP_ASGN_OR     = 36;
    public static final int NODE_CALL           = 37;
    public static final int NODE_FCALL          = 38;
    public static final int NODE_VCALL          = 39;
    public static final int NODE_SUPER          = 40;
    public static final int NODE_ZSUPER         = 41;
    public static final int NODE_ARRAY          = 42;
    public static final int NODE_ZARRAY         = 43;
    public static final int NODE_HASH           = 44;
    public static final int NODE_RETURN         = 45;
    public static final int NODE_YIELD          = 46;
    public static final int NODE_LVAR           = 47;
    public static final int NODE_DVAR           = 48;
    public static final int NODE_GVAR           = 49;
    public static final int NODE_IVAR           = 50;
    public static final int NODE_CONST          = 51;
    public static final int NODE_CVAR           = 52;
    public static final int NODE_CVAR2          = 53;
    public static final int NODE_NTH_REF        = 54;
    public static final int NODE_BACK_REF       = 55;
    public static final int NODE_MATCH          = 56;
    public static final int NODE_MATCH2         = 57;
    public static final int NODE_MATCH3         = 58;
    public static final int NODE_LIT            = 59;
    public static final int NODE_STR            = 60;
    public static final int NODE_DSTR           = 61;
    public static final int NODE_XSTR           = 62;
    public static final int NODE_DXSTR          = 63;
    public static final int NODE_EVSTR          = 64;
    public static final int NODE_DREGX          = 65;
    public static final int NODE_DREGX_ONCE     = 66;
    public static final int NODE_ARGS           = 67;
    public static final int NODE_ARGSCAT        = 68;
    public static final int NODE_ARGSPUSH       = 69;
    public static final int NODE_RESTARGS       = 70;
    public static final int NODE_RESTARY        = 71;
    public static final int NODE_REXPAND        = 72;
    public static final int NODE_BLOCK_ARG      = 73;
    public static final int NODE_BLOCK_PASS     = 74;
    public static final int NODE_DEFN           = 75;
    public static final int NODE_DEFS           = 76;
    public static final int NODE_ALIAS          = 77;
    public static final int NODE_VALIAS         = 78;
    public static final int NODE_UNDEF          = 79;
    public static final int NODE_CLASS          = 80;
    public static final int NODE_MODULE         = 81;
    public static final int NODE_SCLASS         = 82;
    public static final int NODE_COLON2         = 83;
    public static final int NODE_COLON3         = 84;
    public static final int NODE_CREF           = 85;
    public static final int NODE_DOT2           = 86;
    public static final int NODE_DOT3           = 87;
    public static final int NODE_FLIP2          = 88;
    public static final int NODE_FLIP3          = 89;
    public static final int NODE_ATTRSET        = 90;
    public static final int NODE_SELF           = 91;
    public static final int NODE_NIL            = 92;
    public static final int NODE_TRUE           = 93;
    public static final int NODE_FALSE          = 94;
    public static final int NODE_DEFINED        = 95;
    public static final int NODE_NEWLINE        = 96;
    public static final int NODE_POSTEXE        = 97;
    public static final int NODE_ALLOCA         = 98;
    public static final int NODE_DMETHOD        = 99;
    public static final int NODE_BMETHOD        = 100;
    public static final int NODE_MEMO           = 101;
    public static final int NODE_IFUNC          = 102;
    public static final int NODE_LAST           = 103;
}
