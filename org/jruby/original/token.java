/*
 * token.java - No description
 * Created on 10. September 2001, 17:53
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
package org.jruby.original;

public interface token {
    
    public static final int kCLASS =257;
    
    public static final int kMODULE =258;
    
    public static final int kDEF =259;
    
    public static final int kUNDEF =260;
    
    public static final int kBEGIN =261;
    
    public static final int kRESCUE =262;
    
    public static final int kENSURE =263;
    
    public static final int kEND =264;
    
    public static final int kIF =265;
    
    public static final int kUNLESS =266;
    
    public static final int kTHEN =267;
    
    public static final int kELSIF =268;
    
    public static final int kELSE =269;
    
    public static final int kCASE =270;
    
    public static final int kWHEN =271;
    
    public static final int kWHILE =272;
    
    public static final int kUNTIL =273;
    
    public static final int kFOR =274;
    
    public static final int kBREAK =275;
    
    public static final int kNEXT =276;
    
    public static final int kREDO =277;
    
    public static final int kRETRY =278;
    
    public static final int kIN =279;
    
    public static final int kDO =280;
    
    public static final int kDO_COND =281;
    
    public static final int kDO_BLOCK =282;
    
    public static final int kRETURN =283;
    
    public static final int kYIELD =284;
    
    public static final int kSUPER =285;
    
    public static final int kSELF =286;
    
    public static final int kNIL =287;
    
    public static final int kTRUE =288;
    
    public static final int kFALSE =289;
    
    public static final int kAND =290;
    
    public static final int kOR =291;
    
    public static final int kNOT =292;
    
    public static final int kIF_MOD =293;
    
    public static final int kUNLESS_MOD =294;
    
    public static final int kWHILE_MOD =295;
    
    public static final int kUNTIL_MOD =296;
    
    public static final int kRESCUE_MOD =297;
    
    public static final int kALIAS =298;
    
    public static final int kDEFINED =299;
    
    public static final int klBEGIN =300;
    
    public static final int klEND =301;
    
    public static final int k__LINE__ =302;
    
    public static final int k__FILE__ =303;
    
    public static final int tIDENTIFIER =304;
    
    public static final int tFID =305;
    
    public static final int tGVAR =306;
    
    public static final int tIVAR =307;
    
    public static final int tCONSTANT =308;
    
    public static final int tCVAR =309;
    
    public static final int tINTEGER =310;
    
    public static final int tFLOAT =311;
    
    public static final int tSTRING =312;
    
    public static final int tXSTRING =313;
    
    public static final int tREGEXP =314;
    
    public static final int tDSTRING =315;
    
    public static final int tDXSTRING =316;
    
    public static final int tDREGEXP =317;
    
    public static final int tNTH_REF =318;
    
    public static final int tBACK_REF =319;
    
    public static final int tUPLUS =320;
    
    public static final int tUMINUS =321;
    
    public static final int tPOW =322;
    
    public static final int tCMP =323;
    
    public static final int tEQ =324;
    
    public static final int tEQQ =325;
    
    public static final int tNEQ =326;
    
    public static final int tGEQ =327;
    
    public static final int tLEQ =328;
    
    public static final int tANDOP =329;
    
    public static final int tOROP =330;
    
    public static final int tMATCH =331;
    
    public static final int tNMATCH =332;
    
    public static final int tDOT2 =333;
    
    public static final int tDOT3 =334;
    
    public static final int tAREF =335;
    
    public static final int tASET =336;
    
    public static final int tLSHFT =337;
    
    public static final int tRSHFT =338;
    
    public static final int tCOLON2 =339;
    
    public static final int tCOLON3 =340;
    
    public static final int tOP_ASGN =341;
    
    public static final int tASSOC =342;
    
    public static final int tLPAREN =343;
    
    public static final int tLPAREN_ARG =344;
    
    public static final int tRPAREN =345;
    
    public static final int tLBRACK =346;
    
    public static final int tLBRACE =347;
    
    public static final int tLBRACE_ARG =348;
    
    public static final int tSTAR =349;
    
    public static final int tAMPER =350;
    
    public static final int tSYMBEG =351;
    
    public static final int LAST_TOKEN =352;
    
    public static final int yyErrorCode =256;
    
}

