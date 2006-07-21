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
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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
package org.jruby.ast.executable;

/**
 * This is based on the YARV instruction list available at http://www.atdot.net/yarv/insnstbl.html
 */
public interface YARVInstructions {
    public static final int NOP = 0;
    public static final int GETLOCAL = 1;
    public static final int SETLOCAL = 2;
    public static final int GETSPECIAL = 3;
    public static final int SETSPECIAL = 4;
    public static final int GETDYAMIC = 5;
    public static final int SETDYNAMIC = 6;
    public static final int GETINSTANCEVARIABLE = 7;
    public static final int SETINSTANCEVARIABLE = 8;
    public static final int GETCLASSVARIABLE = 9;
    public static final int SETCLASSVARIABLE = 10;
    public static final int GETCONSTANT = 11;
    public static final int SETCONSTANT = 12;
    public static final int GETGLOBAL = 13;
    public static final int SETGLOBAL = 14;
    public static final int PUTNIL = 15;
    public static final int PUTSELF = 16;
    public static final int PUTUNDEF = 17;
    public static final int PUTOBJECT = 18;
    public static final int PUTSTRING = 19;
    public static final int CONCATSTRINGS = 20;
    public static final int TOSTRING = 21;
    public static final int TOREGEXP = 22;
    public static final int NEWARRAY = 23;
    public static final int DUPARRAY = 24;
    public static final int EXPANDARRAY = 25;
    public static final int CONCATARRAY = 26;
    public static final int SPLATARRAY = 27;
    public static final int CHECKINCLUDEARRAY = 28;
    public static final int NEWHASH = 29;
    public static final int NEWRANGE = 30;
    public static final int PUTNOT = 31;
    public static final int POP = 32;
    public static final int DUP = 33;
    public static final int DUPN = 34;
    public static final int SWAP = 35;
    public static final int REPUT = 36;
    public static final int TOPN = 37;
    public static final int EMPTSTACK = 38;
    public static final int DEFINEMETHOD = 39;
    public static final int ALIAS = 40;
    public static final int UNDEF = 41;
    public static final int DEFINED = 42;
    public static final int POSTEXE = 43;
    public static final int TRACE = 44;
    public static final int DEFINECLASS = 45;
    public static final int SEND = 46;
    public static final int INVOKESUPER = 47;
    public static final int INVOKEBLOCK = 48;
    public static final int LEAVE = 49;
    public static final int FINISH = 50;
    public static final int THROW = 51;
    public static final int JUMP = 52;
    public static final int BRANCHIF = 53;
    public static final int BRANCHUNLESS = 54;
    public static final int GETINLINECACHE = 55;
    public static final int ONCEINLINECACHE = 56;
    public static final int SETINLINECACHE = 57;
    public static final int OPT_CASE_DISPATCH = 58;
    public static final int OPT_CHECKENV = 59;
    public static final int OPT_PLUS = 60;
    public static final int OPT_MINUS = 61;
    public static final int OPT_MULT = 62;
    public static final int OPT_DIV = 63;
    public static final int OPT_MOD = 64;
    public static final int OPT_EQ = 65;
    public static final int OPT_LT = 66;
    public static final int OPT_LE = 67;
    public static final int OPT_LTLT = 68;
    public static final int OPT_AREF = 69;
    public static final int OPT_ASET = 70;
    public static final int OPT_LENGTH = 71;
    public static final int OPT_SUCC = 72;
    public static final int OPT_REGEXPMATCH1 = 73;
    public static final int OPT_REGEXPMATCH2 = 74;
    public static final int OPT_CALL_NATIVE_COMPILED = 75;
    public static final int BITBLT = 76;
    public static final int ANSWER = 77;
    public static final int GETLOCAL_OP_2 = 78;
    public static final int GETLOCAL_OP_3 = 79;
    public static final int GETLOCAL_OP_4 = 80;
    public static final int SETLOCAL_OP_2 = 81;
    public static final int SETLOCAL_OP_3 = 82;
    public static final int SETLOCAL_OP_4 = 83;
    public static final int GETDYNAMIC_OP__WC__0 = 84;
    public static final int GETDYNAMIC_OP_1_0 = 85;
    public static final int GETDYNAMIC_OP_2_0 = 86;
    public static final int GETDYNAMIC_OP_3_0 = 87;
    public static final int GETDYNAMIC_OP_4_0 = 88;
    public static final int SETDYNAMIC_OP__WC__0 = 89;
    public static final int SETDYNAMIC_OP_1_0 = 90;
    public static final int SETDYNAMIC_OP_2_0 = 91;
    public static final int SETDYNAMIC_OP_3_0 = 92;
    public static final int SETDYNAMIC_OP_4_0 = 93;
    public static final int PUTOBJECT_OP_INT2FIX_0_0_C_ = 94;
    public static final int PUTOBJECT_OP_INT2FIX_0_1_C_ = 95;
    public static final int PUTOBJECT_OP_QTRUE = 96;
    public static final int PUTOBJECT_OP_QFALSE = 97;
    public static final int SEND_OP__WC___WC__QFALSE_0__WC_ = 98;
    public static final int SEND_OP__WC__0_QFALSE_0__WC_ = 99;
    public static final int SEND_OP__WC__1_QFALSE_0__WC_ = 100;
    public static final int SEND_OP__WC__2_QFALSE_0__WC_ = 101;
    public static final int SEND_OP__WC__3_QFALSE_0__WC_ = 102;
    public static final int SEND_OP__WC___WC__QFALSE_0x04__WC_ = 103;
    public static final int SEND_OP__WC__0_QFALSE_0x04__WC_ = 104;
    public static final int SEND_OP__WC__1_QFALSE_0x04__WC_ = 105;
    public static final int SEND_OP__WC__2_QFALSE_0x04__WC_ = 106;
    public static final int SEND_OP__WC__3_QFALSE_0x04__WC_ = 107;
    public static final int SEND_OP__WC__0_QFALSE_0x0c__WC_ = 108;
    public static final int UNIFIED_PUTOBJECT_PUTOBJECT = 109;
    public static final int UNIFIED_PUTOBJECT_PUTSTRING = 110;
    public static final int UNIFIED_PUTOBJECT_SETLOCAL = 111;
    public static final int UNIFIED_PUTOBJECT_SETDYNAMIC = 112;
    public static final int UNIFIED_PUTSTRING_PUTSTRING = 113;
    public static final int UNIFIED_PUTSTRING_PUTOBJECT = 114;
    public static final int UNIFIED_PUTSTRING_SETLOCAL = 115;
    public static final int UNIFIED_PUTSTRING_SETDYNAMIC = 116;
    public static final int UNIFIED_DUP_SETLOCAL = 117;
    public static final int UNIFIED_GETLOCAL_GETLOCAL = 118;
    public static final int UNIFIED_GETLOCAL_PUTOBJECT = 119;
}
