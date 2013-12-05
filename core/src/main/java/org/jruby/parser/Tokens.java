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
package org.jruby.parser;

public interface Tokens {
    int yyErrorCode = Ruby20Parser.yyErrorCode;
    int kCLASS      = Ruby20Parser.kCLASS;
    int kMODULE     = Ruby20Parser.kMODULE;
    int kDEF        = Ruby20Parser.kDEF;
    int kUNDEF      = Ruby20Parser.kUNDEF;
    int kBEGIN      = Ruby20Parser.kBEGIN;
    int kRESCUE     = Ruby20Parser.kRESCUE;
    int kENSURE     = Ruby20Parser.kENSURE;
    int kEND        = Ruby20Parser.kEND;
    int kIF         = Ruby20Parser.kIF;
    int kUNLESS     = Ruby20Parser.kUNLESS;
    int kTHEN       = Ruby20Parser.kTHEN;
    int kELSIF      = Ruby20Parser.kELSIF;
    int kELSE       = Ruby20Parser.kELSE;
    int kCASE       = Ruby20Parser.kCASE;
    int kWHEN       = Ruby20Parser.kWHEN;
    int kWHILE      = Ruby20Parser.kWHILE;
    int kUNTIL      = Ruby20Parser.kUNTIL;
    int kFOR        = Ruby20Parser.kFOR;
    int kBREAK      = Ruby20Parser.kBREAK;
    int kNEXT       = Ruby20Parser.kNEXT;
    int kREDO       = Ruby20Parser.kREDO;
    int kRETRY      = Ruby20Parser.kRETRY;
    int kIN         = Ruby20Parser.kIN;
    int kDO         = Ruby20Parser.kDO;
    int kDO_COND    = Ruby20Parser.kDO_COND;
    int kDO_BLOCK   = Ruby20Parser.kDO_BLOCK;
    int kRETURN     = Ruby20Parser.kRETURN;
    int kYIELD      = Ruby20Parser.kYIELD;
    int kSUPER      = Ruby20Parser.kSUPER;
    int kSELF       = Ruby20Parser.kSELF;
    int kNIL        = Ruby20Parser.kNIL;
    int kTRUE       = Ruby20Parser.kTRUE;
    int kFALSE      = Ruby20Parser.kFALSE;
    int kAND        = Ruby20Parser.kAND;
    int kOR         = Ruby20Parser.kOR;
    int kNOT        = Ruby20Parser.kNOT;
    int kIF_MOD     = Ruby20Parser.kIF_MOD;
    int kUNLESS_MOD = Ruby20Parser.kUNLESS_MOD;
    int kWHILE_MOD  = Ruby20Parser.kWHILE_MOD;
    int kUNTIL_MOD  = Ruby20Parser.kUNTIL_MOD;
    int kRESCUE_MOD = Ruby20Parser.kRESCUE_MOD;
    int kALIAS      = Ruby20Parser.kALIAS;
    int kDEFINED    = Ruby20Parser.kDEFINED;
    int klBEGIN     = Ruby20Parser.klBEGIN;
    int klEND       = Ruby20Parser.klEND;
    int k__LINE__   = Ruby20Parser.k__LINE__;
    int k__FILE__   = Ruby20Parser.k__FILE__;
    int k__ENCODING__ = Ruby20Parser.k__ENCODING__;
    int kDO_LAMBDA = Ruby20Parser.kDO_LAMBDA;

    int tIDENTIFIER = Ruby20Parser.tIDENTIFIER;
    int tFID        = Ruby20Parser.tFID;
    int tGVAR       = Ruby20Parser.tGVAR;
    int tIVAR       = Ruby20Parser.tIVAR;
    int tCONSTANT   = Ruby20Parser.tCONSTANT;
    int tCVAR       = Ruby20Parser.tCVAR;
    int tINTEGER    = Ruby20Parser.tINTEGER;
    int tFLOAT      = Ruby20Parser.tFLOAT;
    int tSTRING_CONTENT     = Ruby20Parser.tSTRING_CONTENT;
    int tSTRING_BEG = Ruby20Parser.tSTRING_BEG;
    int tSTRING_END = Ruby20Parser.tSTRING_END;
    int tSTRING_DBEG= Ruby20Parser.tSTRING_DBEG;
    int tSTRING_DVAR= Ruby20Parser.tSTRING_DVAR;
    int tXSTRING_BEG= Ruby20Parser.tXSTRING_BEG;
    int tREGEXP_BEG = Ruby20Parser.tREGEXP_BEG;
    int tREGEXP_END = Ruby20Parser.tREGEXP_END;
    int tWORDS_BEG      = Ruby20Parser.tWORDS_BEG;
    int tQWORDS_BEG      = Ruby20Parser.tQWORDS_BEG;
    int tBACK_REF   = Ruby20Parser.tBACK_REF;
    int tBACK_REF2  = Ruby20Parser.tBACK_REF2;
    int tNTH_REF    = Ruby20Parser.tNTH_REF;

    int tUPLUS      = Ruby20Parser.tUPLUS;
    int tUMINUS     = Ruby20Parser.tUMINUS;
    int tUMINUS_NUM     = Ruby20Parser.tUMINUS_NUM;
    int tPOW        = Ruby20Parser.tPOW;
    int tCMP        = Ruby20Parser.tCMP;
    int tEQ         = Ruby20Parser.tEQ;
    int tEQQ        = Ruby20Parser.tEQQ;
    int tNEQ        = Ruby20Parser.tNEQ;
    int tGEQ        = Ruby20Parser.tGEQ;
    int tLEQ        = Ruby20Parser.tLEQ;
    int tANDOP      = Ruby20Parser.tANDOP;
    int tOROP       = Ruby20Parser.tOROP;
    int tMATCH      = Ruby20Parser.tMATCH;
    int tNMATCH     = Ruby20Parser.tNMATCH;
    int tDOT        = Ruby20Parser.tDOT;
    int tDOT2       = Ruby20Parser.tDOT2;
    int tDOT3       = Ruby20Parser.tDOT3;
    int tAREF       = Ruby20Parser.tAREF;
    int tASET       = Ruby20Parser.tASET;
    int tLSHFT      = Ruby20Parser.tLSHFT;
    int tRSHFT      = Ruby20Parser.tRSHFT;
    int tCOLON2     = Ruby20Parser.tCOLON2;

    int tCOLON3     = Ruby20Parser.tCOLON3;
    int tOP_ASGN    = Ruby20Parser.tOP_ASGN;
    int tASSOC      = Ruby20Parser.tASSOC;
    int tLPAREN     = Ruby20Parser.tLPAREN;
    int tLPAREN2     = Ruby20Parser.tLPAREN2;
    int tRPAREN     = Ruby20Parser.tRPAREN;
    int tLPAREN_ARG = Ruby20Parser.tLPAREN_ARG;
    int tLBRACK     = Ruby20Parser.tLBRACK;
    int tRBRACK     = Ruby20Parser.tRBRACK;
    int tLBRACE     = Ruby20Parser.tLBRACE;
    int tLBRACE_ARG     = Ruby20Parser.tLBRACE_ARG;
    int tSTAR       = Ruby20Parser.tSTAR;
    int tSTAR2      = Ruby20Parser.tSTAR2;
    int tAMPER      = Ruby20Parser.tAMPER;
    int tAMPER2     = Ruby20Parser.tAMPER2;
    int tSYMBEG     = Ruby20Parser.tSYMBEG;
    int tTILDE      = Ruby20Parser.tTILDE;
    int tPERCENT    = Ruby20Parser.tPERCENT;
    int tDIVIDE     = Ruby20Parser.tDIVIDE;
    int tPLUS       = Ruby20Parser.tPLUS;
    int tMINUS       = Ruby20Parser.tMINUS;
    int tLT         = Ruby20Parser.tLT;
    int tGT         = Ruby20Parser.tGT;
    int tCARET      = Ruby20Parser.tCARET;
    int tBANG       = Ruby20Parser.tBANG;
    int tLCURLY     = Ruby20Parser.tLCURLY;
    int tRCURLY     = Ruby20Parser.tRCURLY;
    int tPIPE       = Ruby20Parser.tPIPE;
    int tLAMBDA     = Ruby20Parser.tLAMBDA;
    int tLAMBEG     = Ruby20Parser.tLAMBEG;
    int tLABEL      = Ruby20Parser.tLABEL;
    int tSYMBOLS_BEG = Ruby20Parser.tSYMBOLS_BEG;
    int tQSYMBOLS_BEG = Ruby20Parser.tQSYMBOLS_BEG;
    int tDSTAR = Ruby20Parser.tDSTAR;
    
    String[] operators = {"+@", "-@", "**", "<=>", "==", "===", "!=", ">=", "<=", "&&",
                          "||", "=~", "!~", "..", "...", "[]", "[]=", "<<", ">>", "::"};
}
