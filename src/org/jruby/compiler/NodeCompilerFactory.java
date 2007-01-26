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

package org.jruby.compiler;

import org.jruby.Ruby;

import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;

import org.jruby.compiler.yarv.StandardYARVCompiler;

/**
 *
 * @author headius
 */
public class NodeCompilerFactory {
    public static YARVNodesCompiler getYARVCompiler() {
        return new YARVNodesCompiler();
    }
    public static NodeCompiler getCompiler(Node node) {
        switch (node.nodeId) {
            case NodeTypes.ALIASNODE:
                return new AliasNodeCompiler();
            case NodeTypes.ANDNODE:
                return new AndNodeCompiler();
            case NodeTypes.ARRAYNODE:
                return new ArrayNodeCompiler();
            case NodeTypes.BEGINNODE:
                return new BeginNodeCompiler();
            case NodeTypes.BIGNUMNODE:
                return new BignumNodeCompiler();
            case NodeTypes.BLOCKNODE:
                return new BlockNodeCompiler();
            case NodeTypes.CALLNODE:
                return new CallNodeCompiler();
            case NodeTypes.CONSTNODE:
                return new ConstNodeCompiler();
            case NodeTypes.DEFNNODE:
                return new DefnNodeCompiler();
            case NodeTypes.FALSENODE:
                return new FalseNodeCompiler();
            case NodeTypes.FCALLNODE:
                return new FCallNodeCompiler();
            case NodeTypes.FIXNUMNODE:
                return new FixnumNodeCompiler();
            case NodeTypes.GLOBALASGNNODE:
                return new GlobalAsgnNodeCompiler();
            case NodeTypes.GLOBALVARNODE:
                return new GlobalVarNodeCompiler();
            case NodeTypes.IFNODE:
                return new IfNodeCompiler();
            case NodeTypes.INSTASGNNODE:
                return new InstAsgnNodeCompiler();
            case NodeTypes.INSTVARNODE:
                return new InstVarNodeCompiler();
            //case NodeTypes.ITERNODE:
            //    return new IterNodeCompiler();
            case NodeTypes.LOCALASGNNODE:
                return new LocalAsgnNodeCompiler();
            case NodeTypes.LOCALVARNODE:
                return new LocalVarNodeCompiler();
            case NodeTypes.NEWLINENODE:
                return new NewlineNodeCompiler();
            case NodeTypes.NILNODE:
                return new NilNodeCompiler();
            case NodeTypes.NOTNODE:
                return new NotNodeCompiler();
            case NodeTypes.ORNODE:
                return new OrNodeCompiler();
            case NodeTypes.ROOTNODE:
                return new RootNodeCompiler();
            case NodeTypes.SELFNODE:
                return new SelfNodeCompiler();
            case NodeTypes.STRNODE:
                return new StringNodeCompiler();
            case NodeTypes.SYMBOLNODE:
                return new SymbolNodeCompiler();
            case NodeTypes.TRUENODE:
                return new TrueNodeCompiler();
            case NodeTypes.VCALLNODE:
                return new VCallNodeCompiler();
            case NodeTypes.WHILENODE:
                return new WhileNodeCompiler();
        }
        
        throw new NotCompilableException("Can't compile node: " + node);
    }
    
    public static NodeCompiler getArgumentsCompiler(Node node) {
        switch (node.nodeId) {
            case NodeTypes.ARRAYNODE:
                return new ArrayNodeArgsCompiler();
        }
        
        throw new NotCompilableException("Can't compile node: " + node);
    }
}
