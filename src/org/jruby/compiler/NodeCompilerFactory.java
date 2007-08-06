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

import java.util.HashSet;
import java.util.Set;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;

/**
 *
 * @author headius
 */
public class NodeCompilerFactory {
    public static final boolean SAFE = System.getProperty("jruby.jit.safe", "true").equals("true");
    public static final Set UNSAFE_CALLS;
    
    static {
        UNSAFE_CALLS = new HashSet();
    }
    
    public static YARVNodesCompiler getYARVCompiler() {
        return new YARVNodesCompiler();
    }
    public static NodeCompiler getCompiler(Node node) {
        switch (node.nodeId) {
        case NodeTypes.ALIASNODE:
            // safe
            return new AliasNodeCompiler();
        case NodeTypes.ANDNODE:
            // safe
            return new AndNodeCompiler();
        case NodeTypes.ARRAYNODE:
            // safe
            return new ArrayNodeCompiler();
        case NodeTypes.ATTRASSIGNNODE:
            // safe, I think :)
            return new AttrAssignNodeCompiler();
        case NodeTypes.BEGINNODE:
            // safe
            return new BeginNodeCompiler();
        case NodeTypes.BIGNUMNODE:
            // safe
            return new BignumNodeCompiler();
        case NodeTypes.BLOCKNODE:
            // safe
            return new BlockNodeCompiler();
        case NodeTypes.BREAKNODE:
            // Not safe yet; something weird with break-handling try/catch and calls like "foo bar {}"
            if (SAFE) throw new NotCompilableException("Can't compile node safely: " + node);
            return new BreakNodeCompiler();
        case NodeTypes.CALLNODE:
            // safe; yield or block nodes that aren't should raise
            return new CallNodeCompiler();
        case NodeTypes.CLASSNODE:
            if (SAFE) throw new NotCompilableException("Can't compile class definitions safely: " + node);
            return new ClassNodeCompiler();
        case NodeTypes.CLASSVARNODE:
            return new ClassVarNodeCompiler();
        case NodeTypes.CLASSVARASGNNODE:
            return new ClassVarAsgnNodeCompiler();
        case NodeTypes.CONSTDECLNODE:
            // this should be safe as well with TC doing the right thing
            return new ConstDeclNodeCompiler();
        case NodeTypes.COLON2NODE:
            return new Colon2NodeCompiler();
        case NodeTypes.CONSTNODE:
            // I think this is safe now that cref and class are being pushed on TC
            return new ConstNodeCompiler();
        case NodeTypes.DASGNNODE:
            // safe
            return new DAsgnNodeCompiler();
        case NodeTypes.DEFNNODE:
            // safe; it's primarily odd arg types that are problems, and defn compiler will catch those
            return new DefnNodeCompiler();
        case NodeTypes.DOTNODE:
            // safe
            return new DotNodeCompiler();
        case NodeTypes.DSTRNODE:
            // safe
            return new DStrNodeCompiler();
        case NodeTypes.DVARNODE:
            // safe
            return new DVarNodeCompiler();
        case NodeTypes.EVSTRNODE:
            // safe
            return new EvStrNodeCompiler();
        case NodeTypes.FALSENODE:
            // safe
            return new FalseNodeCompiler();
        case NodeTypes.FCALLNODE:
            // safe
            return new FCallNodeCompiler();
        case NodeTypes.FIXNUMNODE:
            // safe
            return new FixnumNodeCompiler();
        case NodeTypes.FLOATNODE:
            // safe
            return new FloatNodeCompiler();
        case NodeTypes.GLOBALASGNNODE:
            // safe
            return new GlobalAsgnNodeCompiler();
        case NodeTypes.GLOBALVARNODE:
            // safe
            return new GlobalVarNodeCompiler();
        case NodeTypes.HASHNODE:
            // safe
            return new HashNodeCompiler();
        case NodeTypes.IFNODE:
            // safe
            return new IfNodeCompiler();
        case NodeTypes.INSTASGNNODE:
            // safe
            return new InstAsgnNodeCompiler();
        case NodeTypes.INSTVARNODE:
            // safe
            return new InstVarNodeCompiler();
        case NodeTypes.ITERNODE:
            return new IterNodeCompiler();
        case NodeTypes.LOCALASGNNODE:
            // safe
            return new LocalAsgnNodeCompiler();
        case NodeTypes.LOCALVARNODE:
            // safe
            return new LocalVarNodeCompiler();
        case NodeTypes.MATCHNODE:
            // safe
            return new MatchNodeCompiler();
        case NodeTypes.MATCH2NODE:
            // safe
            return new Match2NodeCompiler();
        case NodeTypes.MATCH3NODE:
            // safe
            return new Match3NodeCompiler();
        case NodeTypes.MODULENODE:
            if (SAFE) throw new NotCompilableException("Can't compile module definitions safely: " + node);
            return new ModuleNodeCompiler();
        case NodeTypes.NEWLINENODE:
            // safe
            return new NewlineNodeCompiler();
        case NodeTypes.NTHREFNODE:
            // safe
            return new NthRefNodeCompiler();
        case NodeTypes.NILNODE:
            // safe
            return new NilNodeCompiler();
        case NodeTypes.NOTNODE:
            // safe
            return new NotNodeCompiler();
        case NodeTypes.OPASGNNODE:
            // safe
            return new OpAsgnNodeCompiler();
        case NodeTypes.ORNODE:
            // safe
            return new OrNodeCompiler();
        case NodeTypes.REGEXPNODE:
            // safe
            return new RegexpNodeCompiler();
        case NodeTypes.RETURNNODE:
            // safe; throws error if non-local
            return new ReturnNodeCompiler();
        case NodeTypes.ROOTNODE:
            // safe
            return new RootNodeCompiler();
        case NodeTypes.SELFNODE:
            // safe
            return new SelfNodeCompiler();
        case NodeTypes.SPLATNODE:
            if (SAFE) throw new NotCompilableException("Can't compile node safely: " + node);
            return new SplatNodeCompiler();
        case NodeTypes.STRNODE:
            // safe
            return new StringNodeCompiler();
        case NodeTypes.SVALUENODE:
            if (SAFE) throw new NotCompilableException("Can't compile node safely: " + node);
            return new SValueNodeCompiler();
        case NodeTypes.SYMBOLNODE:
            // safe
            return new SymbolNodeCompiler();
        case NodeTypes.TRUENODE:
            // safe
            return new TrueNodeCompiler();
        case NodeTypes.VCALLNODE:
            // safe
            return new VCallNodeCompiler();
        case NodeTypes.WHILENODE:
            // safe; things like next and closures that aren't complete yet will fail to compile
            return new WhileNodeCompiler();
        case NodeTypes.YIELDNODE:
            // safe; arg types that can't be handled will fail to compile, but yield logic is correct
            return new YieldNodeCompiler();
        case NodeTypes.ZARRAYNODE:
            // safe
            return new ZArrayNodeCompiler();
        }
        
        throw new NotCompilableException("Can't compile node: " + node);
    }
    
    public static NodeCompiler getArgumentsCompiler(Node node) {
        switch (node.nodeId) {
        case NodeTypes.ARRAYNODE:
            return new ArrayNodeArgsCompiler();
        }
        
        throw new NotCompilableException("Can't compile argument node: " + node);
    }
    
    public static NodeCompiler getAssignmentCompiler(Node node) {
        switch (node.nodeId) {
            // disabled for now; incomplete
        //case NodeTypes.MULTIPLEASGNNODE:
        //    return new MultipleAsgnNodeAsgnCompiler();
        }
        
        throw new NotCompilableException("Can't compile assignment node: " + node);
    }
    
    /**
     * Check whether the target node can safely be compiled.
     * 
     * @param node 
     */
    public static void confirmNodeIsSafe(Node node) {
        switch (node.nodeId) {
        case NodeTypes.ARGSNODE:
            ArgsNode argsNode = (ArgsNode)node;
            // FIXME: We can't compile cases like def(a=(b=1)) because the variables
            // in the arg list get ordered differently than you might expect (b comes first)
            // So the code below searches through all opt args, ensuring none of them skip
            // indicies. A skipped index means there's a hidden local var/arg like b above
            // and so we shouldn't try to compile.
            if (argsNode.getOptArgs() != null && argsNode.getOptArgs().size() > 0) {
                int index = argsNode.getArgsCount() - 1;
                
                for (int i = 0; i < argsNode.getOptArgs().size(); i++) {
                    int newIndex = ((LocalAsgnNode)argsNode.getOptArgs().get(i)).getIndex() - 2;
                    
                    if (newIndex - index != 1) {
                        throw new NotCompilableException("Can't compile def with optional args that assign other variables at: " + node.getPosition());
                    }
                    index = newIndex;
                }
            }
            
            // Also do not compile anything with a block argument or "rest" argument
            if (argsNode.getBlockArgNode() != null) throw new NotCompilableException("Can't compile def with block arg at: " + node.getPosition());
            if (argsNode.getRestArg() != -1) throw new NotCompilableException("Can't compile def with rest arg at: " + node.getPosition());
            break;
        }
    }
}
