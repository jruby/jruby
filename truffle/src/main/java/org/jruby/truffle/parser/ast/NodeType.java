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
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby.truffle.parser.ast;

public enum NodeType {
    ALIASNODE, ANDNODE, ARGSCATNODE, ARGSNODE, ARGUMENTNODE, ARRAYNODE, ASSIGNABLENODE,
    BACKREFNODE, BEGINNODE, BIGNUMNODE, BINARYOPERATORNODE, BLOCKARGNODE, BLOCKNODE,
    BLOCKPASSNODE, BREAKNODE, CALLNODE, CASENODE, CLASSNODE, CLASSVARASGNNODE, CLASSVARDECLNODE,
    CLASSVARNODE, COLON2NODE, COLON3NODE, CONSTDECLNODE, CONSTNODE, DASGNNODE, DEFINEDNODE,
    DEFNNODE, DEFSNODE, DOTNODE, DREGEXPNODE, DSTRNODE, DSYMBOLNODE, DVARNODE, DXSTRNODE,
    ENSURENODE, EVSTRNODE, FALSENODE, FCALLNODE, FIXNUMNODE, FLIPNODE, FLOATNODE, FORNODE,
    GLOBALASGNNODE, GLOBALVARNODE, HASHNODE, IFNODE, INSTASGNNODE, INSTVARNODE, ISCOPINGNODE,
    ITERNODE, LISTNODE, LOCALASGNNODE, LOCALVARNODE, MATCH2NODE, MATCH3NODE, MATCHNODE,MODULENODE,
    NEWLINENODE, NEXTNODE, NILNODE, NOTNODE, NTHREFNODE, OPASGNANDNODE,
    OPASGNNODE, OPASGNORNODE, OPELEMENTASGNNODE, ORNODE, PREEXENODE, POSTEXENODE, REDONODE, 
    REGEXPNODE, RESCUEBODYNODE, RESCUENODE, RETRYNODE, RETURNNODE, SCLASSNODE, SCOPENODE,
    SELFNODE, SPLATNODE, STARNODE, STRNODE, SUPERNODE, SVALUENODE, SYMBOLNODE,
    TRUENODE, UNDEFNODE, UNTILNODE, VALIASNODE, VCALLNODE, WHENNODE, WHILENODE, XSTRNODE, YIELDNODE,
    ZARRAYNODE, ZEROARGNODE, ZSUPERNODE, COMMENTNODE, ROOTNODE, ATTRASSIGNNODE, ARGSPUSHNODE,
    OPTARGNODE, ARGAUXILIARYNODE, LAMBDANODE, MULTIPLEASGNNODE, RESTARG, ENCODINGNODE,
    LITERALNODE, BLOCKARG18NODE, KEYWORDARGNODE, KEYWORDRESTARGNODE, RATIONALNODE, COMPLEXNODE,
    REQUIRED_KEYWORD_ARGUMENT_VALUE, OPASGNCONSTDECLNODE, JAVASCRIPT;


    /**
     * Whether this node type would always evaluate as being true.
     *
     * @return whether this node type would always evaluate as being true
     */
    public boolean alwaysTrue() {
        switch (this) {
        case TRUENODE:
        case FIXNUMNODE:
        case FLOATNODE:
        case REGEXPNODE:
        case STRNODE:
        case DOTNODE:
        case SYMBOLNODE:
        case BIGNUMNODE:
        case ARRAYNODE:
        case HASHNODE:
            return true;
        default:
            return false;
        }
    }

    /**
     * Whether this node type would always evaluate as being false.
     * 
     * @return whether this node type would always evaluate as being false
     */
    public boolean alwaysFalse() {
        switch (this) {
        case NILNODE:
        case FALSENODE:
            return true;
        default:
            return false;
        }
    }

    public boolean isImmediate() {
        switch (this) {
        case FIXNUMNODE:
        case STRNODE:
        case FLOATNODE:
        case BIGNUMNODE:
        case SELFNODE:
        case TRUENODE:
        case FALSENODE:
        case NILNODE:
            return true;
        default:
            return false;
        }
    }

    private final String simpleName = name().substring(0, name().length() - 4).toLowerCase();

    public String simpleName() {
        return simpleName;
    }
}
