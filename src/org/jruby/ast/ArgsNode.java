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
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.lexer.yacc.SourcePosition;

/**
 * arguments for a function.
 *  this is used both in the function definition
 * and in actual function calls                                     
 * <ul>
 * <li>
 * u1 ==&gt; optNode (BlockNode) Optional argument description
 * </li>
 * <li>
 * u2 ==&gt; rest (int) index of the rest argument (the array arg with a * in front 
 * </li>
 * <li>
 * u3 ==&gt; count (int) number of arguments
 * </li>
 * </ul>
 *
 * @author  jpetersen
 */
public class ArgsNode extends Node {
    static final long serialVersionUID = 3709437716296564785L;

    private final int argsCount;
    private final ListNode optArgs;
    private final int restArg;
    private final BlockArgNode blockArgNode;

    /**
     * 
     * @param optArgs  Node describing the optional arguments
     * 				This Block will contain assignments to locals (LAsgnNode)
     * @param restArg  index of the rest argument in the local table
     * 				(the array argument prefixed by a * which collects 
     * 				all additional params)
     * 				or -1 if there is none.
     * @param argsCount number of regular arguments
     * @param blockArgNode An optional block argument (&amp;arg).
     **/
    public ArgsNode(SourcePosition position, int argsCount, ListNode optArgs, int restArg, BlockArgNode blockArgNode) {
        super(position);

        this.argsCount = argsCount;
        this.optArgs = optArgs;
        this.restArg = restArg;
        this.blockArgNode = blockArgNode;
    }

    /**
     * Accept for the visitor pattern.
     * @param iVisitor the visitor
     **/
    public void accept(NodeVisitor iVisitor) {
        iVisitor.visitArgsNode(this);
    }

    /**
     * Gets the argsCount.
     * @return Returns a int
     */
    public int getArgsCount() {
        return argsCount;
    }

    /**
     * Gets the optArgs.
     * @return Returns a ListNode
     */
    public ListNode getOptArgs() {
        return optArgs;
    }

    /**
     * Gets the restArg.
     * @return Returns a int
     */
    public int getRestArg() {
        return restArg;
    }

    /**
     * Gets the blockArgNode.
     * @return Returns a BlockArgNode
     */
    public BlockArgNode getBlockArgNode() {
        return blockArgNode;
    }
}
