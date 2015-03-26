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
    int yyErrorCode = RipperParser.yyErrorCode;
    int kCLASS      = RipperParser.kCLASS;
    int kMODULE     = RipperParser.kMODULE;
    int kDEF        = RipperParser.kDEF;
    int kUNDEF      = RipperParser.kUNDEF;
    int kBEGIN      = RipperParser.kBEGIN;
    int kRESCUE     = RipperParser.kRESCUE;
    int kENSURE     = RipperParser.kENSURE;
    int kEND        = RipperParser.kEND;
    int kIF         = RipperParser.kIF;
    int kUNLESS     = RipperParser.kUNLESS;
    int kTHEN       = RipperParser.kTHEN;
    int kELSIF      = RipperParser.kELSIF;
    int kELSE       = RipperParser.kELSE;
    int kCASE       = RipperParser.kCASE;
    int kWHEN       = RipperParser.kWHEN;
    int kWHILE      = RipperParser.kWHILE;
    int kUNTIL      = RipperParser.kUNTIL;
    int kFOR        = RipperParser.kFOR;
    int kBREAK      = RipperParser.kBREAK;
    int kNEXT       = RipperParser.kNEXT;
    int kREDO       = RipperParser.kREDO;
    int kRETRY      = RipperParser.kRETRY;
    int kIN         = RipperParser.kIN;
    int kDO         = RipperParser.kDO;
    int kDO_COND    = RipperParser.kDO_COND;
    int kDO_BLOCK   = RipperParser.kDO_BLOCK;
    int kRETURN     = RipperParser.kRETURN;
    int kYIELD      = RipperParser.kYIELD;
    int kSUPER      = RipperParser.kSUPER;
    int kSELF       = RipperParser.kSELF;
    int kNIL        = RipperParser.kNIL;
    int kTRUE       = RipperParser.kTRUE;
    int kFALSE      = RipperParser.kFALSE;
    int kAND        = RipperParser.kAND;
    int kOR         = RipperParser.kOR;
    int kNOT        = RipperParser.kNOT;
    int kIF_MOD     = RipperParser.kIF_MOD;
    int kUNLESS_MOD = RipperParser.kUNLESS_MOD;
    int kWHILE_MOD  = RipperParser.kWHILE_MOD;
    int kUNTIL_MOD  = RipperParser.kUNTIL_MOD;
    int kRESCUE_MOD = RipperParser.kRESCUE_MOD;
    int kALIAS      = RipperParser.kALIAS;
    int kDEFINED    = RipperParser.kDEFINED;
    int klBEGIN     = RipperParser.klBEGIN;
    int klEND       = RipperParser.klEND;
    int k__LINE__   = RipperParser.k__LINE__;
    int k__FILE__   = RipperParser.k__FILE__;
    int k__ENCODING__ = RipperParser.k__ENCODING__;
    int kDO_LAMBDA = RipperParser.kDO_LAMBDA;

    int tIDENTIFIER = RipperParser.tIDENTIFIER;
    int tFID        = RipperParser.tFID;
    int tGVAR       = RipperParser.tGVAR;
    int tIVAR       = RipperParser.tIVAR;
    int tCONSTANT   = RipperParser.tCONSTANT;
    int tCVAR       = RipperParser.tCVAR;
    int tIMAGINARY  = RipperParser.tIMAGINARY;
    int tINTEGER    = RipperParser.tINTEGER;
    int tFLOAT      = RipperParser.tFLOAT;
    int tRATIONAL   = RipperParser.tRATIONAL;
    int tSTRING_CONTENT     = RipperParser.tSTRING_CONTENT;
    int tSTRING_BEG = RipperParser.tSTRING_BEG;
    int tSTRING_END = RipperParser.tSTRING_END;
    int tSTRING_DBEG= RipperParser.tSTRING_DBEG;
    int tSTRING_DVAR= RipperParser.tSTRING_DVAR;
    int tXSTRING_BEG= RipperParser.tXSTRING_BEG;
    int tREGEXP_BEG = RipperParser.tREGEXP_BEG;
    int tREGEXP_END = RipperParser.tREGEXP_END;
    int tWORDS_BEG      = RipperParser.tWORDS_BEG;
    int tQWORDS_BEG      = RipperParser.tQWORDS_BEG;
    int tBACK_REF   = RipperParser.tBACK_REF;
    int tBACK_REF2  = RipperParser.tBACK_REF2;
    int tNTH_REF    = RipperParser.tNTH_REF;

    int tUPLUS      = RipperParser.tUPLUS;
    int tUMINUS     = RipperParser.tUMINUS;
    int tUMINUS_NUM     = RipperParser.tUMINUS_NUM;
    int tPOW        = RipperParser.tPOW;
    int tCMP        = RipperParser.tCMP;
    int tEQ         = RipperParser.tEQ;
    int tEQQ        = RipperParser.tEQQ;
    int tNEQ        = RipperParser.tNEQ;
    int tGEQ        = RipperParser.tGEQ;
    int tLEQ        = RipperParser.tLEQ;
    int tANDOP      = RipperParser.tANDOP;
    int tOROP       = RipperParser.tOROP;
    int tMATCH      = RipperParser.tMATCH;
    int tNMATCH     = RipperParser.tNMATCH;
    int tDOT        = RipperParser.tDOT;
    int tDOT2       = RipperParser.tDOT2;
    int tDOT3       = RipperParser.tDOT3;
    int tAREF       = RipperParser.tAREF;
    int tASET       = RipperParser.tASET;
    int tLSHFT      = RipperParser.tLSHFT;
    int tRSHFT      = RipperParser.tRSHFT;
    int tCOLON2     = RipperParser.tCOLON2;

    int tCOLON3     = RipperParser.tCOLON3;
    int tOP_ASGN    = RipperParser.tOP_ASGN;
    int tASSOC      = RipperParser.tASSOC;
    int tLPAREN     = RipperParser.tLPAREN;
    int tLPAREN2     = RipperParser.tLPAREN2;
    int tRPAREN     = RipperParser.tRPAREN;
    int tLPAREN_ARG = RipperParser.tLPAREN_ARG;
    int tLBRACK     = RipperParser.tLBRACK;
    int tRBRACK     = RipperParser.tRBRACK;
    int tLBRACE     = RipperParser.tLBRACE;
    int tLBRACE_ARG     = RipperParser.tLBRACE_ARG;
    int tSTAR       = RipperParser.tSTAR;
    int tSTAR2      = RipperParser.tSTAR2;
    int tAMPER      = RipperParser.tAMPER;
    int tAMPER2     = RipperParser.tAMPER2;
    int tSYMBEG     = RipperParser.tSYMBEG;
    int tTILDE      = RipperParser.tTILDE;
    int tPERCENT    = RipperParser.tPERCENT;
    int tDIVIDE     = RipperParser.tDIVIDE;
    int tPLUS       = RipperParser.tPLUS;
    int tMINUS       = RipperParser.tMINUS;
    int tLT         = RipperParser.tLT;
    int tGT         = RipperParser.tGT;
    int tCARET      = RipperParser.tCARET;
    int tBANG       = RipperParser.tBANG;
    int tLCURLY     = RipperParser.tLCURLY;
    int tRCURLY     = RipperParser.tRCURLY;
    int tPIPE       = RipperParser.tPIPE;
    int tLAMBDA     = RipperParser.tLAMBDA;
    int tLAMBEG     = RipperParser.tLAMBEG;
    int tLABEL      = RipperParser.tLABEL;
    int tSYMBOLS_BEG = RipperParser.tSYMBOLS_BEG;
    int tQSYMBOLS_BEG = RipperParser.tQSYMBOLS_BEG;
    int tDSTAR = RipperParser.tDSTAR;
    int tLABEL_END = RipperParser.tLABEL_END;
    int tSTRING_DEND = RipperParser.tSTRING_DEND;
    int tCHAR       = RipperParser.tCHAR;
    
    int tIGNORED_NL = RipperParser.tIGNORED_NL;
    int tCOMMENT = RipperParser.tCOMMENT;
    int tEMBDOC_BEG = RipperParser.tEMBDOC_BEG;
    int tEMBDOC = RipperParser.tEMBDOC;
    int tEMBDOC_END = RipperParser.tEMBDOC_END;
    int tSP = RipperParser.tSP;
    int tHEREDOC_BEG = RipperParser.tHEREDOC_BEG;
    int tHEREDOC_END = RipperParser.tHEREDOC_END;
    int k__END__   = RipperParser.k__END__;

    String[] operators = {"+@", "-@", "**", "<=>", "==", "===", "!=", ">=", "<=", "&&",
                          "||", "=~", "!~", "..", "...", "[]", "[]=", "<<", ">>", "::"};
}
