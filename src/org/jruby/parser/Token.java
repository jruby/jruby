/*
 * Token.java - No description
 * Created on 05. Oktober 2001, 17:52
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
package org.jruby.parser;

public interface Token {
    int yyErrorCode = DefaultRubyParser.yyErrorCode;
    int kCLASS      = DefaultRubyParser.kCLASS;
    int kMODULE     = DefaultRubyParser.kMODULE;
    int kDEF        = DefaultRubyParser.kDEF;
    int kUNDEF      = DefaultRubyParser.kUNDEF;
    int kBEGIN      = DefaultRubyParser.kBEGIN;
    int kRESCUE     = DefaultRubyParser.kRESCUE;
    int kENSURE     = DefaultRubyParser.kENSURE;
    int kEND        = DefaultRubyParser.kEND;
    int kIF         = DefaultRubyParser.kIF;
    int kUNLESS     = DefaultRubyParser.kUNLESS;
    int kTHEN       = DefaultRubyParser.kTHEN;
    int kELSIF      = DefaultRubyParser.kELSIF;
    int kELSE       = DefaultRubyParser.kELSE;
    int kCASE       = DefaultRubyParser.kCASE;
    int kWHEN       = DefaultRubyParser.kWHEN;
    int kWHILE      = DefaultRubyParser.kWHILE;
    int kUNTIL      = DefaultRubyParser.kUNTIL;
    int kFOR        = DefaultRubyParser.kFOR;
    int kBREAK      = DefaultRubyParser.kBREAK;
    int kNEXT       = DefaultRubyParser.kNEXT;
    int kREDO       = DefaultRubyParser.kREDO;
    int kRETRY      = DefaultRubyParser.kRETRY;
    int kIN         = DefaultRubyParser.kIN;
    int kDO         = DefaultRubyParser.kDO;
    int kDO_COND    = DefaultRubyParser.kDO_COND;
    int kDO_BLOCK   = DefaultRubyParser.kDO_BLOCK;
    int kRETURN     = DefaultRubyParser.kRETURN;
    int kYIELD      = DefaultRubyParser.kYIELD;
    int kSUPER      = DefaultRubyParser.kSUPER;
    int kSELF       = DefaultRubyParser.kSELF;
    int kNIL        = DefaultRubyParser.kNIL;
    int kTRUE       = DefaultRubyParser.kTRUE;
    int kFALSE      = DefaultRubyParser.kFALSE;
    int kAND        = DefaultRubyParser.kAND;
    int kOR         = DefaultRubyParser.kOR;
    int kNOT        = DefaultRubyParser.kNOT;
    int kIF_MOD     = DefaultRubyParser.kIF_MOD;
    int kUNLESS_MOD = DefaultRubyParser.kUNLESS_MOD;
    int kWHILE_MOD  = DefaultRubyParser.kWHILE_MOD;
    int kUNTIL_MOD  = DefaultRubyParser.kUNTIL_MOD;
    int kRESCUE_MOD = DefaultRubyParser.kRESCUE_MOD;
    int kALIAS      = DefaultRubyParser.kALIAS;
    int kDEFINED    = DefaultRubyParser.kDEFINED;
    int klBEGIN     = DefaultRubyParser.klBEGIN;
    int klEND       = DefaultRubyParser.klEND;
    int k__LINE__   = DefaultRubyParser.k__LINE__;
    int k__FILE__   = DefaultRubyParser.k__FILE__;

    int tIDENTIFIER = DefaultRubyParser.tIDENTIFIER;
    int tFID        = DefaultRubyParser.tFID;
    int tGVAR       = DefaultRubyParser.tGVAR;
    int tIVAR       = DefaultRubyParser.tIVAR;
    int tCONSTANT   = DefaultRubyParser.tCONSTANT;
    int tCVAR       = DefaultRubyParser.tCVAR;
    int tINTEGER    = DefaultRubyParser.tINTEGER;
    int tFLOAT      = DefaultRubyParser.tFLOAT;
    int tSTRING     = DefaultRubyParser.tSTRING;
    int tXSTRING    = DefaultRubyParser.tXSTRING;
    int tREGEXP     = DefaultRubyParser.tREGEXP;
    int tDXSTRING   = DefaultRubyParser.tDXSTRING;
    int tDREGEXP    = DefaultRubyParser.tDREGEXP;
    int tDSTRING    = DefaultRubyParser.tDSTRING;
    int tBACK_REF   = DefaultRubyParser.tBACK_REF;
    int tNTH_REF    = DefaultRubyParser.tNTH_REF;
    
    int tARRAY      = DefaultRubyParser.tARRAY;

    int tUPLUS      = DefaultRubyParser.tUPLUS;
    int tUMINUS     = DefaultRubyParser.tUMINUS;
    int tPOW        = DefaultRubyParser.tPOW;
    int tCMP        = DefaultRubyParser.tCMP;
    int tEQ         = DefaultRubyParser.tEQ;
    int tEQQ        = DefaultRubyParser.tEQQ;
    int tNEQ        = DefaultRubyParser.tNEQ;
    int tGEQ        = DefaultRubyParser.tGEQ;
    int tLEQ        = DefaultRubyParser.tLEQ;
    int tANDOP      = DefaultRubyParser.tANDOP;
    int tOROP       = DefaultRubyParser.tOROP;
    int tMATCH      = DefaultRubyParser.tMATCH;
    int tNMATCH     = DefaultRubyParser.tNMATCH;
    int tDOT2       = DefaultRubyParser.tDOT2;
    int tDOT3       = DefaultRubyParser.tDOT3;
    int tAREF       = DefaultRubyParser.tAREF;
    int tASET       = DefaultRubyParser.tASET;
    int tLSHFT      = DefaultRubyParser.tLSHFT;
    int tRSHFT      = DefaultRubyParser.tRSHFT;
    int tCOLON2     = DefaultRubyParser.tCOLON2;

    int tCOLON3     = DefaultRubyParser.tCOLON3;
    int tOP_ASGN    = DefaultRubyParser.tOP_ASGN;
    int tASSOC      = DefaultRubyParser.tASSOC;
    int tLPAREN     = DefaultRubyParser.tLPAREN;
    int tLBRACK     = DefaultRubyParser.tLBRACK;
    int tLBRACE     = DefaultRubyParser.tLBRACE;
    int tSTAR       = DefaultRubyParser.tSTAR;
    int tAMPER      = DefaultRubyParser.tAMPER;
    int tSYMBEG     = DefaultRubyParser.tSYMBEG;
    int LAST_TOKEN  = DefaultRubyParser.LAST_TOKEN;

    String[] operators = {"+@", "-@", "**", "<=>", "==", "===", "!=", ">=", "<=", "&&",
                          "||", "=~", "!~", "..", "...", "[]", "[]=", "<<", ">>", "::"};
}
