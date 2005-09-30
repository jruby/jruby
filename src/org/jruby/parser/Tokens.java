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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.parser;

public interface Tokens {
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
    int tSTRING_CONTENT     = DefaultRubyParser.tSTRING_CONTENT;
    int tSTRING_BEG = DefaultRubyParser.tSTRING_BEG;
    int tSTRING_END = DefaultRubyParser.tSTRING_END;
    int tSTRING_DBEG= DefaultRubyParser.tSTRING_DBEG;
    int tSTRING_DVAR= DefaultRubyParser.tSTRING_DVAR;
    int tXSTRING_BEG= DefaultRubyParser.tXSTRING_BEG;
    int tREGEXP_BEG = DefaultRubyParser.tREGEXP_BEG;
    int tREGEXP_END = DefaultRubyParser.tREGEXP_END;
    int tWORDS_BEG      = DefaultRubyParser.tWORDS_BEG;
    int tQWORDS_BEG      = DefaultRubyParser.tQWORDS_BEG;
    int tBACK_REF   = DefaultRubyParser.tBACK_REF;
    int tBACK_REF2  = DefaultRubyParser.tBACK_REF2;
    int tNTH_REF    = DefaultRubyParser.tNTH_REF;

    int tUPLUS      = DefaultRubyParser.tUPLUS;
    int tUMINUS     = DefaultRubyParser.tUMINUS;
    int tUMINUS_NUM     = DefaultRubyParser.tUMINUS_NUM;
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
    int tDOT        = DefaultRubyParser.tDOT;
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
    int tLPAREN2     = DefaultRubyParser.tLPAREN2;
    int tLPAREN_ARG = DefaultRubyParser.tLPAREN_ARG;
    int tLBRACK     = DefaultRubyParser.tLBRACK;
    int tLBRACE     = DefaultRubyParser.tLBRACE;
    int tLBRACE_ARG     = DefaultRubyParser.tLBRACE_ARG;
    int tSTAR       = DefaultRubyParser.tSTAR;
    int tSTAR2      = DefaultRubyParser.tSTAR2;
    int tAMPER      = DefaultRubyParser.tAMPER;
    int tAMPER2     = DefaultRubyParser.tAMPER2;
    int tSYMBEG     = DefaultRubyParser.tSYMBEG;
    int tTILDE      = DefaultRubyParser.tTILDE;
    int tPERCENT    = DefaultRubyParser.tPERCENT;
    int tDIVIDE     = DefaultRubyParser.tDIVIDE;
    int tPLUS       = DefaultRubyParser.tPLUS;
    int tMINUS       = DefaultRubyParser.tMINUS;
    int tLT         = DefaultRubyParser.tLT;
    int tGT         = DefaultRubyParser.tGT;
    int tCARET      = DefaultRubyParser.tCARET;
    int tBANG       = DefaultRubyParser.tBANG;
    int tLCURLY       = DefaultRubyParser.tLCURLY;
    int tPIPE       = DefaultRubyParser.tPIPE;

    String[] operators = {"+@", "-@", "**", "<=>", "==", "===", "!=", ">=", "<=", "&&",
                          "||", "=~", "!~", "..", "...", "[]", "[]=", "<<", ">>", "::"};
}
