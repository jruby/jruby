/*
 * node_type.java - No description
 * Created on 10. September 2001, 17:53
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

package org.jruby.original;

public interface node_type {
    final int NODE_METHOD =0;
    final int NODE_FBODY =1;
    final int NODE_CFUNC =2;
    final int NODE_SCOPE =3;
    final int NODE_BLOCK =4;
    final int NODE_IF =5;
    final int NODE_CASE =6;
    final int NODE_WHEN =7;
    final int NODE_OPT_N =8;
    final int NODE_WHILE =9;
    final int NODE_UNTIL =10;
    final int NODE_ITER =11;
    final int NODE_FOR =12;
    final int NODE_BREAK =13;
    final int NODE_NEXT =14;
    final int NODE_REDO =15;
    final int NODE_RETRY =16;
    final int NODE_BEGIN =17;
    final int NODE_RESCUE =18;
    final int NODE_RESBODY =19;
    final int NODE_ENSURE =20;
    final int NODE_AND =21;
    final int NODE_OR =22;
    final int NODE_NOT =23;
    final int NODE_MASGN =24;
    final int NODE_LASGN =25;
    final int NODE_DASGN =26;
    final int NODE_DASGN_CURR =27;
    final int NODE_GASGN =28;
    final int NODE_IASGN =29;
    final int NODE_CDECL =30;
    final int NODE_CVASGN =31;
    final int NODE_CVDECL =32;
    final int NODE_OP_ASGN1 =33;
    final int NODE_OP_ASGN2 =34;
    final int NODE_OP_ASGN_AND =35;
    final int NODE_OP_ASGN_OR =36;
    final int NODE_CALL =37;
    final int NODE_FCALL =38;
    final int NODE_VCALL =39;
    final int NODE_SUPER =40;
    final int NODE_ZSUPER =41;
    final int NODE_ARRAY =42;
    final int NODE_ZARRAY =43;
    final int NODE_HASH =44;
    final int NODE_RETURN =45;
    final int NODE_YIELD =46;
    final int NODE_LVAR =47;
    final int NODE_DVAR =48;
    final int NODE_GVAR =49;
    final int NODE_IVAR =50;
    final int NODE_CONST =51;
    final int NODE_CVAR =52;
    final int NODE_CVAR2 =53;
    final int NODE_NTH_REF =54;
    final int NODE_BACK_REF =55;
    final int NODE_MATCH =56;
    final int NODE_MATCH2 =57;
    final int NODE_MATCH3 =58;
    final int NODE_LIT =59;
    final int NODE_STR =60;
    final int NODE_DSTR =61;
    final int NODE_XSTR =62;
    final int NODE_DXSTR =63;
    final int NODE_EVSTR =64;
    final int NODE_DREGX =65;
    final int NODE_DREGX_ONCE =66;
    final int NODE_ARGS =67;
    final int NODE_ARGSCAT =68;
    final int NODE_ARGSPUSH =69;
    final int NODE_RESTARGS =70;
    final int NODE_RESTARY =71;
    final int NODE_REXPAND =72;
    final int NODE_BLOCK_ARG =73;
    final int NODE_BLOCK_PASS =74;
    final int NODE_DEFN =75;
    final int NODE_DEFS =76;
    final int NODE_ALIAS =77;
    final int NODE_VALIAS =78;
    final int NODE_UNDEF =79;
    final int NODE_CLASS =80;
    final int NODE_MODULE =81;
    final int NODE_SCLASS =82;
    final int NODE_COLON2 =83;
    final int NODE_COLON3 =84;
    final int NODE_CREF =85;
    final int NODE_DOT2 =86;
    final int NODE_DOT3 =87;
    final int NODE_FLIP2 =88;
    final int NODE_FLIP3 =89;
    final int NODE_ATTRSET =90;
    final int NODE_SELF =91;
    final int NODE_NIL =92;
    final int NODE_TRUE =93;
    final int NODE_FALSE =94;
    final int NODE_DEFINED =95;
    final int NODE_NEWLINE =96;
    final int NODE_POSTEXE =97;
    final int NODE_ALLOCA =98;
    final int NODE_DMETHOD =99;
    final int NODE_BMETHOD =100;
    final int NODE_MEMO =101;
    final int NODE_IFUNC =102;
    final int NODE_LAST =103;
}