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
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.ast;

public final class NodeTypes {
    public static final int ALIASNODE = 0;
    public static final int ANDNODE = 1;
    public static final int ARGSCATNODE = 2;
    public static final int ARGSNODE = 3;
    public static final int ARGUMENTNODE = 4;
    public static final int ARRAYNODE = 5;
    public static final int ASSIGNABLENODE = 6;
    public static final int BACKREFNODE = 7;
    public static final int BEGINNODE = 8;
    public static final int BIGNUMNODE = 9;
    public static final int BINARYOPERATORNODE = 10;
    public static final int BLOCKARGNODE = 11;
    public static final int BLOCKNODE = 12;
    public static final int BLOCKPASSNODE = 13;
    public static final int BREAKNODE = 14;
    public static final int CALLNODE = 15;
    public static final int CASENODE = 16;
    public static final int CLASSNODE = 17;
    public static final int CLASSVARASGNNODE = 18;
    public static final int CLASSVARDECLNODE = 19;
    public static final int CLASSVARNODE = 20;
    public static final int COLON2NODE = 21;
    public static final int COLON3NODE = 22;
    public static final int CONSTDECLNODE = 23;
    public static final int CONSTNODE = 24;
    public static final int DASGNNODE = 25;
    public static final int DEFINEDNODE = 26;
    public static final int DEFNNODE = 27;
    public static final int DEFSNODE = 28;
    public static final int DOTNODE = 29;
    public static final int DREGEXPNODE = 30;
    public static final int DSTRNODE = 31;
    public static final int DSYMBOLNODE = 32;
    public static final int DVARNODE = 33;
    public static final int DXSTRNODE = 34;
    public static final int ENSURENODE = 35;
    public static final int EVSTRNODE = 36;
    public static final int FALSENODE = 37;
    public static final int FCALLNODE = 38;
    public static final int FIXNUMNODE = 39;
    public static final int FLIPNODE = 40;
    public static final int FLOATNODE = 41;
    public static final int FORNODE = 42;
    public static final int GLOBALASGNNODE = 43;
    public static final int GLOBALVARNODE = 44;
    public static final int HASHNODE = 45;
    public static final int IFNODE = 46;
    public static final int INSTASGNNODE = 47;
    public static final int INSTVARNODE = 48;
    public static final int ISCOPINGNODE = 49;
    public static final int ITERNODE = 50;
    public static final int LISTNODE = 51;
    public static final int LOCALASGNNODE = 52;
    public static final int LOCALVARNODE = 53;
    public static final int MATCH2NODE = 54;
    public static final int MATCH3NODE = 55;
    public static final int MATCHNODE = 56;
    public static final int MODULENODE = 57;
    public static final int MULTIPLEASGNNODE = 58;
    public static final int NEWLINENODE = 59;
    public static final int NEXTNODE = 60;
    public static final int NILNODE = 61;
    public static final int NODETYPES = 62;
    public static final int NOTNODE = 63;
    public static final int NTHREFNODE = 64;
    public static final int OPASGNANDNODE = 65;
    public static final int OPASGNNODE = 66;
    public static final int OPASGNORNODE = 67;
    public static final int OPELEMENTASGNNODE = 68;
    public static final int OPTNNODE = 69;
    public static final int ORNODE = 70;
    public static final int POSTEXENODE = 71;
    public static final int REDONODE = 72;
    public static final int REGEXPNODE = 73;
    public static final int RESCUEBODYNODE = 74;
    public static final int RESCUENODE = 75;
    public static final int RETRYNODE = 76;
    public static final int RETURNNODE = 77;
    public static final int SCLASSNODE = 78;
    public static final int SCOPENODE = 79;
    public static final int SELFNODE = 80;
    public static final int SPLATNODE = 81;
    public static final int STARNODE = 82;
    public static final int STRNODE = 83;
    public static final int SUPERNODE = 84;
    public static final int SVALUENODE = 85;
    public static final int SYMBOLNODE = 86;
    public static final int TOARYNODE = 87;
    public static final int TRUENODE = 88;
    public static final int UNDEFNODE = 89;
    public static final int UNTILNODE = 90;
    public static final int VALIASNODE = 91;
    public static final int VCALLNODE = 92;
    public static final int WHENNODE = 93;
    public static final int WHILENODE = 94;
    public static final int XSTRNODE = 95;
    public static final int YIELDNODE = 96;
    public static final int ZARRAYNODE = 97;
    public static final int ZEROARGNODE = 98;
    public static final int ZSUPERNODE = 99;
    public static final int COMMENTNODE = 100;
    public static final int ROOTNODE = 101; 
    
    private NodeTypes() {}
}
