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
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.truffle.parser.parser;

public interface Tokens {
    int yyErrorCode = RubyParser.yyErrorCode;
    int kCLASS      = RubyParser.kCLASS;
    int kMODULE     = RubyParser.kMODULE;
    int kDEF        = RubyParser.kDEF;
    int kUNDEF      = RubyParser.kUNDEF;
    int kBEGIN      = RubyParser.kBEGIN;
    int kRESCUE     = RubyParser.kRESCUE;
    int kENSURE     = RubyParser.kENSURE;
    int kEND        = RubyParser.kEND;
    int kIF         = RubyParser.kIF;
    int kUNLESS     = RubyParser.kUNLESS;
    int kTHEN       = RubyParser.kTHEN;
    int kELSIF      = RubyParser.kELSIF;
    int kELSE       = RubyParser.kELSE;
    int kCASE       = RubyParser.kCASE;
    int kWHEN       = RubyParser.kWHEN;
    int kWHILE      = RubyParser.kWHILE;
    int kUNTIL      = RubyParser.kUNTIL;
    int kFOR        = RubyParser.kFOR;
    int kBREAK      = RubyParser.kBREAK;
    int kNEXT       = RubyParser.kNEXT;
    int kREDO       = RubyParser.kREDO;
    int kRETRY      = RubyParser.kRETRY;
    int kIN         = RubyParser.kIN;
    int kDO         = RubyParser.kDO;
    int kDO_COND    = RubyParser.kDO_COND;
    int kDO_BLOCK   = RubyParser.kDO_BLOCK;
    int kRETURN     = RubyParser.kRETURN;
    int kYIELD      = RubyParser.kYIELD;
    int kSUPER      = RubyParser.kSUPER;
    int kSELF       = RubyParser.kSELF;
    int kNIL        = RubyParser.kNIL;
    int kTRUE       = RubyParser.kTRUE;
    int kFALSE      = RubyParser.kFALSE;
    int kAND        = RubyParser.kAND;
    int kOR         = RubyParser.kOR;
    int kNOT        = RubyParser.kNOT;
    int kIF_MOD     = RubyParser.kIF_MOD;
    int kUNLESS_MOD = RubyParser.kUNLESS_MOD;
    int kWHILE_MOD  = RubyParser.kWHILE_MOD;
    int kUNTIL_MOD  = RubyParser.kUNTIL_MOD;
    int kRESCUE_MOD = RubyParser.kRESCUE_MOD;
    int kALIAS      = RubyParser.kALIAS;
    int kDEFINED    = RubyParser.kDEFINED;
    int klBEGIN     = RubyParser.klBEGIN;
    int klEND       = RubyParser.klEND;
    int k__LINE__   = RubyParser.k__LINE__;
    int k__FILE__   = RubyParser.k__FILE__;
    int k__ENCODING__ = RubyParser.k__ENCODING__;
    int kDO_LAMBDA = RubyParser.kDO_LAMBDA;

    int tIDENTIFIER = RubyParser.tIDENTIFIER;
    int tFID        = RubyParser.tFID;
    int tGVAR       = RubyParser.tGVAR;
    int tIVAR       = RubyParser.tIVAR;
    int tCONSTANT   = RubyParser.tCONSTANT;
    int tCVAR       = RubyParser.tCVAR;
    int tINTEGER    = RubyParser.tINTEGER;
    int tFLOAT      = RubyParser.tFLOAT;
    int tRATIONAL   = RubyParser.tRATIONAL;
    int tSTRING_CONTENT     = RubyParser.tSTRING_CONTENT;
    int tSTRING_BEG = RubyParser.tSTRING_BEG;
    int tSTRING_END = RubyParser.tSTRING_END;
    int tSTRING_DBEG= RubyParser.tSTRING_DBEG;
    int tSTRING_DVAR= RubyParser.tSTRING_DVAR;
    int tXSTRING_BEG= RubyParser.tXSTRING_BEG;
    int tREGEXP_BEG = RubyParser.tREGEXP_BEG;
    int tREGEXP_END = RubyParser.tREGEXP_END;
    int tWORDS_BEG      = RubyParser.tWORDS_BEG;
    int tQWORDS_BEG      = RubyParser.tQWORDS_BEG;
    int tBACK_REF   = RubyParser.tBACK_REF;
    int tBACK_REF2  = RubyParser.tBACK_REF2;
    int tNTH_REF    = RubyParser.tNTH_REF;

    int tUPLUS      = RubyParser.tUPLUS;
    int tUMINUS     = RubyParser.tUMINUS;
    int tUMINUS_NUM     = RubyParser.tUMINUS_NUM;
    int tPOW        = RubyParser.tPOW;
    int tCMP        = RubyParser.tCMP;
    int tEQ         = RubyParser.tEQ;
    int tEQQ        = RubyParser.tEQQ;
    int tNEQ        = RubyParser.tNEQ;
    int tGEQ        = RubyParser.tGEQ;
    int tLEQ        = RubyParser.tLEQ;
    int tANDOP      = RubyParser.tANDOP;
    int tOROP       = RubyParser.tOROP;
    int tMATCH      = RubyParser.tMATCH;
    int tNMATCH     = RubyParser.tNMATCH;
    int tDOT        = RubyParser.tDOT;
    int tDOT2       = RubyParser.tDOT2;
    int tDOT3       = RubyParser.tDOT3;
    int tAREF       = RubyParser.tAREF;
    int tASET       = RubyParser.tASET;
    int tLSHFT      = RubyParser.tLSHFT;
    int tRSHFT      = RubyParser.tRSHFT;
    int tCOLON2     = RubyParser.tCOLON2;

    int tCOLON3     = RubyParser.tCOLON3;
    int tOP_ASGN    = RubyParser.tOP_ASGN;
    int tASSOC      = RubyParser.tASSOC;
    int tLPAREN     = RubyParser.tLPAREN;
    int tLPAREN2     = RubyParser.tLPAREN2;
    int tRPAREN     = RubyParser.tRPAREN;
    int tLPAREN_ARG = RubyParser.tLPAREN_ARG;
    int tLBRACK     = RubyParser.tLBRACK;
    int tRBRACK     = RubyParser.tRBRACK;
    int tLBRACE     = RubyParser.tLBRACE;
    int tLBRACE_ARG     = RubyParser.tLBRACE_ARG;
    int tSTAR       = RubyParser.tSTAR;
    int tSTAR2      = RubyParser.tSTAR2;
    int tAMPER      = RubyParser.tAMPER;
    int tAMPER2     = RubyParser.tAMPER2;
    int tSYMBEG     = RubyParser.tSYMBEG;
    int tTILDE      = RubyParser.tTILDE;
    int tPERCENT    = RubyParser.tPERCENT;
    int tDIVIDE     = RubyParser.tDIVIDE;
    int tPLUS       = RubyParser.tPLUS;
    int tMINUS       = RubyParser.tMINUS;
    int tLT         = RubyParser.tLT;
    int tGT         = RubyParser.tGT;
    int tCARET      = RubyParser.tCARET;
    int tBANG       = RubyParser.tBANG;
    int tLCURLY     = RubyParser.tLCURLY;
    int tRCURLY     = RubyParser.tRCURLY;
    int tPIPE       = RubyParser.tPIPE;
    int tLAMBDA     = RubyParser.tLAMBDA;
    int tLAMBEG     = RubyParser.tLAMBEG;
    int tLABEL      = RubyParser.tLABEL;
    int tSYMBOLS_BEG = RubyParser.tSYMBOLS_BEG;
    int tQSYMBOLS_BEG = RubyParser.tQSYMBOLS_BEG;
    int tDSTAR = RubyParser.tDSTAR;
    int tLABEL_END = RubyParser.tLABEL_END;
    int tSTRING_DEND = RubyParser.tSTRING_DEND;
    int tCHAR = RubyParser.tCHAR;
    int tANDDOT = RubyParser.tANDDOT;

    int tJAVASCRIPT = RubyParser.tJAVASCRIPT;
    
    String[] operators = {"+@", "-@", "**", "<=>", "==", "===", "!=", ">=", "<=", "&&",
                          "||", "=~", "!~", "..", "...", "[]", "[]=", "<<", ">>", "::"};
}
