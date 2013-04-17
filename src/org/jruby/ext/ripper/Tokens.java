/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ext.ripper;

/*
 ***** BEGIN LICENSE BLOCK *****
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

public interface Tokens {
    int yyErrorCode = Ripper19Parser.yyErrorCode;
    int kCLASS      = Ripper19Parser.kCLASS;
    int kMODULE     = Ripper19Parser.kMODULE;
    int kDEF        = Ripper19Parser.kDEF;
    int kUNDEF      = Ripper19Parser.kUNDEF;
    int kBEGIN      = Ripper19Parser.kBEGIN;
    int kRESCUE     = Ripper19Parser.kRESCUE;
    int kENSURE     = Ripper19Parser.kENSURE;
    int kEND        = Ripper19Parser.kEND;
    int kIF         = Ripper19Parser.kIF;
    int kUNLESS     = Ripper19Parser.kUNLESS;
    int kTHEN       = Ripper19Parser.kTHEN;
    int kELSIF      = Ripper19Parser.kELSIF;
    int kELSE       = Ripper19Parser.kELSE;
    int kCASE       = Ripper19Parser.kCASE;
    int kWHEN       = Ripper19Parser.kWHEN;
    int kWHILE      = Ripper19Parser.kWHILE;
    int kUNTIL      = Ripper19Parser.kUNTIL;
    int kFOR        = Ripper19Parser.kFOR;
    int kBREAK      = Ripper19Parser.kBREAK;
    int kNEXT       = Ripper19Parser.kNEXT;
    int kREDO       = Ripper19Parser.kREDO;
    int kRETRY      = Ripper19Parser.kRETRY;
    int kIN         = Ripper19Parser.kIN;
    int kDO         = Ripper19Parser.kDO;
    int kDO_COND    = Ripper19Parser.kDO_COND;
    int kDO_BLOCK   = Ripper19Parser.kDO_BLOCK;
    int kRETURN     = Ripper19Parser.kRETURN;
    int kYIELD      = Ripper19Parser.kYIELD;
    int kSUPER      = Ripper19Parser.kSUPER;
    int kSELF       = Ripper19Parser.kSELF;
    int kNIL        = Ripper19Parser.kNIL;
    int kTRUE       = Ripper19Parser.kTRUE;
    int kFALSE      = Ripper19Parser.kFALSE;
    int kAND        = Ripper19Parser.kAND;
    int kOR         = Ripper19Parser.kOR;
    int kNOT        = Ripper19Parser.kNOT;
    int kIF_MOD     = Ripper19Parser.kIF_MOD;
    int kUNLESS_MOD = Ripper19Parser.kUNLESS_MOD;
    int kWHILE_MOD  = Ripper19Parser.kWHILE_MOD;
    int kUNTIL_MOD  = Ripper19Parser.kUNTIL_MOD;
    int kRESCUE_MOD = Ripper19Parser.kRESCUE_MOD;
    int kALIAS      = Ripper19Parser.kALIAS;
    int kDEFINED    = Ripper19Parser.kDEFINED;
    int klBEGIN     = Ripper19Parser.klBEGIN;
    int klEND       = Ripper19Parser.klEND;
    int k__LINE__   = Ripper19Parser.k__LINE__;
    int k__FILE__   = Ripper19Parser.k__FILE__;
    int k__ENCODING__ = Ripper19Parser.k__ENCODING__;
    int kDO_LAMBDA = Ripper19Parser.kDO_LAMBDA;

    int tIDENTIFIER = Ripper19Parser.tIDENTIFIER;
    int tFID        = Ripper19Parser.tFID;
    int tGVAR       = Ripper19Parser.tGVAR;
    int tIVAR       = Ripper19Parser.tIVAR;
    int tCONSTANT   = Ripper19Parser.tCONSTANT;
    int tCVAR       = Ripper19Parser.tCVAR;
    int tINTEGER    = Ripper19Parser.tINTEGER;
    int tFLOAT      = Ripper19Parser.tFLOAT;
    int tSTRING_CONTENT     = Ripper19Parser.tSTRING_CONTENT;
    int tSTRING_BEG = Ripper19Parser.tSTRING_BEG;
    int tSTRING_END = Ripper19Parser.tSTRING_END;
    int tSTRING_DBEG= Ripper19Parser.tSTRING_DBEG;
    int tSTRING_DVAR= Ripper19Parser.tSTRING_DVAR;
    int tXSTRING_BEG= Ripper19Parser.tXSTRING_BEG;
    int tREGEXP_BEG = Ripper19Parser.tREGEXP_BEG;
    int tREGEXP_END = Ripper19Parser.tREGEXP_END;
    int tWORDS_BEG      = Ripper19Parser.tWORDS_BEG;
    int tQWORDS_BEG      = Ripper19Parser.tQWORDS_BEG;
    int tBACK_REF   = Ripper19Parser.tBACK_REF;
    int tBACK_REF2  = Ripper19Parser.tBACK_REF2;
    int tNTH_REF    = Ripper19Parser.tNTH_REF;

    int tUPLUS      = Ripper19Parser.tUPLUS;
    int tUMINUS     = Ripper19Parser.tUMINUS;
    int tUMINUS_NUM     = Ripper19Parser.tUMINUS_NUM;
    int tPOW        = Ripper19Parser.tPOW;
    int tCMP        = Ripper19Parser.tCMP;
    int tEQ         = Ripper19Parser.tEQ;
    int tEQQ        = Ripper19Parser.tEQQ;
    int tNEQ        = Ripper19Parser.tNEQ;
    int tGEQ        = Ripper19Parser.tGEQ;
    int tLEQ        = Ripper19Parser.tLEQ;
    int tANDOP      = Ripper19Parser.tANDOP;
    int tOROP       = Ripper19Parser.tOROP;
    int tMATCH      = Ripper19Parser.tMATCH;
    int tNMATCH     = Ripper19Parser.tNMATCH;
    int tDOT        = Ripper19Parser.tDOT;
    int tDOT2       = Ripper19Parser.tDOT2;
    int tDOT3       = Ripper19Parser.tDOT3;
    int tAREF       = Ripper19Parser.tAREF;
    int tASET       = Ripper19Parser.tASET;
    int tLSHFT      = Ripper19Parser.tLSHFT;
    int tRSHFT      = Ripper19Parser.tRSHFT;
    int tCOLON2     = Ripper19Parser.tCOLON2;

    int tCOLON3     = Ripper19Parser.tCOLON3;
    int tOP_ASGN    = Ripper19Parser.tOP_ASGN;
    int tASSOC      = Ripper19Parser.tASSOC;
    int tLPAREN     = Ripper19Parser.tLPAREN;
    int tLPAREN2     = Ripper19Parser.tLPAREN2;
    int tRPAREN     = Ripper19Parser.tRPAREN;
    int tLPAREN_ARG = Ripper19Parser.tLPAREN_ARG;
    int tLBRACK     = Ripper19Parser.tLBRACK;
    int tRBRACK     = Ripper19Parser.tRBRACK;
    int tLBRACE     = Ripper19Parser.tLBRACE;
    int tLBRACE_ARG     = Ripper19Parser.tLBRACE_ARG;
    int tSTAR       = Ripper19Parser.tSTAR;
    int tSTAR2      = Ripper19Parser.tSTAR2;
    int tAMPER      = Ripper19Parser.tAMPER;
    int tAMPER2     = Ripper19Parser.tAMPER2;
    int tSYMBEG     = Ripper19Parser.tSYMBEG;
    int tTILDE      = Ripper19Parser.tTILDE;
    int tPERCENT    = Ripper19Parser.tPERCENT;
    int tDIVIDE     = Ripper19Parser.tDIVIDE;
    int tPLUS       = Ripper19Parser.tPLUS;
    int tMINUS       = Ripper19Parser.tMINUS;
    int tLT         = Ripper19Parser.tLT;
    int tGT         = Ripper19Parser.tGT;
    int tCARET      = Ripper19Parser.tCARET;
    int tBANG       = Ripper19Parser.tBANG;
    int tLCURLY     = Ripper19Parser.tLCURLY;
    int tRCURLY     = Ripper19Parser.tRCURLY;
    int tPIPE       = Ripper19Parser.tPIPE;
    int tLAMBDA     = Ripper19Parser.tLAMBDA;
    int tLAMBEG     = Ripper19Parser.tLAMBEG;
    int tLABEL      = Ripper19Parser.tLABEL;
    int tCHAR       = Ripper19Parser.tCHAR;
    
    int tIGNORED_NL = Ripper19Parser.tIGNORED_NL;
    int tCOMMENT = Ripper19Parser.tCOMMENT;
    int tEMBDOC_BEG = Ripper19Parser.tEMBDOC_BEG;
    int tEMBDOC = Ripper19Parser.tEMBDOC;
    int tEMBDOC_END = Ripper19Parser.tEMBDOC_END;
    int tSP = Ripper19Parser.tSP;
    int tHEREDOC_BEG = Ripper19Parser.tHEREDOC_BEG;
    int tHEREDOC_END = Ripper19Parser.tHEREDOC_END;
    int k__END__   = Ripper19Parser.k__END__;

    String[] operators = {"+@", "-@", "**", "<=>", "==", "===", "!=", ">=", "<=", "&&",
                          "||", "=~", "!~", "..", "...", "[]", "[]=", "<<", ">>", "::"};
}
