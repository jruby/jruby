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
package org.jruby.truffle.parser.ast.visitor;

import org.jruby.truffle.parser.ast.ClassParseNode;
import org.jruby.truffle.parser.ast.InstAsgnParseNode;
import org.jruby.truffle.parser.ast.InstVarParseNode;
import org.jruby.truffle.parser.ast.ModuleParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.PostExeParseNode;
import org.jruby.truffle.parser.ast.PreExeParseNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Visitor to search AST nodes for instance variables. Certain nodes are
 * ignored during walking since they always create a new context with a new
 * self.
 * 
 * Example usage:
 * 
 * <code>
 * ParseNode node = getNodeFromSomewhere();
 * InstanceVariableFinder finder = new InstanceVariableFinder();
 * node.accept(finder);
 * System.out.println("found: " + finder.getFoundVariables);
 * </code>
 */
public class InstanceVariableFinder extends AbstractNodeVisitor<Void> {

    @Override
    protected Void defaultVisit(ParseNode iVisited) {
        visitChildren(iVisited);
        return null;
    }

    /** The set of instance variables found during walking. */
    private final Set<String> foundVariables = new HashSet<String>();
    
    /**
     * Walk a node and its children looking for instance variables using a new
     * InstanceVariableFinder. Return an array of the variable names found.
     * 
     * @param node the node to walk
     * @return an array of instance variable names found
     */
    public static Set<String> findVariables(ParseNode node) {
        InstanceVariableFinder ivf = new InstanceVariableFinder();
        node.accept(ivf);
        return ivf.getFoundVariables();
    }
    
    /**
     * Return the Set of all instance variables found during walking.
     * 
     * @return a Set of all instance variable names found
     */
    public Set<String> getFoundVariables() {
        return foundVariables;
    }
    
    /**
     * ClassParseNode creates a new scope and self, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitClassNode(ClassParseNode iVisited) {
        return null;
    }

    /**
     * Add the name of the instance variable being assigned to our set of
     * instance variable names and continue to walk child nodes.
     * 
     * @return null
     */
    @Override
    public Void visitInstAsgnNode(InstAsgnParseNode iVisited) {
        foundVariables.add(iVisited.getName());
        List<ParseNode> nodes = iVisited.childNodes();
        for (int i = 0; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            node.accept(this);
        }
        return null;
    }

    /**
     * Add the name of the instance variable being retrieved to our set of
     * instance variable names and continue to walk child nodes.
     * 
     * @return null
     */
    @Override
    public Void visitInstVarNode(InstVarParseNode iVisited) {
        foundVariables.add(iVisited.getName());
        List<ParseNode> nodes = iVisited.childNodes();
        for (int i = 0; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
        }
        return null;
    }

    /**
     * ModuleParseNode creates a new scope and self, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitModuleNode(ModuleParseNode iVisited) {
        return null;
    }

    /**
     * PreExeParseNode can't appear in methods, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitPreExeNode(PreExeParseNode iVisited) {
        return null;
    }

    /**
     * PostExeParseNode can't appear in methods, so do not search for ivars.
     * 
     * @return null
     */
    @Override
    public Void visitPostExeNode(PostExeParseNode iVisited) {
        return null;
    }
}
