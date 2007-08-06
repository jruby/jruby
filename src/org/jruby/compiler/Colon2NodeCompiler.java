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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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

import org.jruby.ast.Colon2Node;
import org.jruby.ast.Node;

import org.jruby.runtime.CallType;

/**
 *
 * @author Ola Bini
 */
public class Colon2NodeCompiler implements NodeCompiler{
    
    /** Creates a new instance of Colon2NodeCompiler */
    public Colon2NodeCompiler() {
    }
    
    public void compile(Node node, Compiler context) {
        context.lineNumber(node.getPosition());
        Colon2Node iVisited = (Colon2Node) node;
        Node leftNode = iVisited.getLeftNode();
        final String name = iVisited.getName();

        if(leftNode == null) {
            context.loadObject();
            context.retrieveConstantFromModule(name);
        } else {
            NodeCompilerFactory.getCompiler(iVisited.getLeftNode()).compile(iVisited.getLeftNode(), context);
            
            BranchCallback moduleCallback = new BranchCallback() {
                    public void branch(Compiler context) {
                        context.retrieveConstantFromModule(name);
                    }
                };

            BranchCallback notModuleCallback = new BranchCallback() {
                    public void branch(Compiler context) {
                        context.invokeDynamic(name, true, false, CallType.FUNCTIONAL, null, false);
                    }
                };

            context.branchIfModule(moduleCallback, notModuleCallback);
        }
    }
}
