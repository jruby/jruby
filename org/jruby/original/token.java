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
    int kCLASS      = 257;
    int kMODULE     = 258;
    int kDEF        = 259;
    int kUNDEF      = 260;
    int kBEGIN      = 261;
    int kRESCUE     = 262;
    int kENSURE     = 263;
    int kEND        = 264;
    int kIF         = 265;
    int kUNLESS     = 266;
    int kTHEN       = 267;
    int kELSIF      = 268;
    int kELSE       = 269;
    int kCASE       = 270;
    int kWHEN       = 271;
    int kWHILE      = 272;
    int kUNTIL      = 273;
    int kFOR        = 274;
    int kBREAK      = 275;
    int kNEXT       = 276;
    int kREDO       = 277;
    int kRETRY      = 278;
    int kIN         = 279;
    int kDO         = 280;
    int kDO_COND    = 281;
    int kDO_BLOCK   = 282;
    int kRETURN     = 283;
    int kYIELD      = 284;
    int kSUPER      = 285;
    int kSELF       = 286;
    int kNIL        = 287;
    int kTRUE       = 288;
    int kFALSE      = 289;
    int kAND        = 290;
    int kOR         = 291;
    int kNOT        = 292;
    int kIF_MOD     = 293;
    int kUNLESS_MOD = 294;
    int kWHILE_MOD  = 295;
    int kUNTIL_MOD  = 296;
    int kRESCUE_MOD = 297;
    int kALIAS      = 298;
    int kDEFINED    = 299;
    int klBEGIN     = 300;
    int klEND       = 301;
    int k__LINE__   = 302;
    int k__FILE__   = 303;
    int tIDENTIFIER = 304;
    int tFID        = 305;
    int tGVAR       = 306;
    int tIVAR       = 307;
    int tCONSTANT   = 308;
    int tCVAR       = 309;
    int tINTEGER    = 310;
    int tFLOAT      = 311;
    int tSTRING     = 312;
    int tXSTRING    = 313;
    int tREGEXP     = 314;
    int tDSTRING    = 315;
    int tDXSTRING   = 316;
    int tDREGEXP    = 317;
    int tNTH_REF    = 318;
    int tBACK_REF   = 319;
    int tUPLUS      = 320;
    int tUMINUS     = 321;
    int tPOW        = 322;
    int tCMP        = 323;
    int tEQ         = 324;
    int tEQQ        = 325;
    int tNEQ        = 326;
    int tGEQ        = 327;
    int tLEQ        = 328;
    int tANDOP      = 329;
    int tOROP       = 330;
    int tMATCH      = 331;
    int tNMATCH     = 332;
    int tDOT2       = 333;
    int tDOT3       = 334;
    int tAREF       = 335;
    int tASET       = 336;
    int tLSHFT      = 337;
    int tRSHFT      = 338;
    int tCOLON2     = 339;
    int tCOLON3     = 340;
    int tOP_ASGN    = 341;
    int tASSOC      = 342;
    int tLPAREN     = 343;
    int tLPAREN_ARG = 344;
    int tRPAREN     = 345;
    int tLBRACK     = 346;
    int tLBRACE     = 347;
    int tLBRACE_ARG = 348;
    int tSTAR       = 349;
    int tAMPER      = 350;
    int tSYMBEG     = 351;
    int LAST_TOKEN  = 352;
    int yyErrorCode = 256;
}
