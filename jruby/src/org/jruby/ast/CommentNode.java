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

import java.util.List;

import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.evaluator.Instruction;
import org.jruby.lexer.yacc.ISourcePosition;

/**
 * Representation of a comment.  Note that comments are not part of evaluation so you do
 * not get the ability to visit this node as part of evaluation.  In theory we could add
 * this if we envisioned some wacky annotation system, but we have no crazy ideas yet.
 *   
 */
public class CommentNode extends Node {
    private static final long serialVersionUID = -8304070370230933044L;
   
    // Textual data (this is missing newline as a single line comment and =begin..=end
    // comments do contain the intermediary newlines.
    String content;

    public CommentNode(ISourcePosition position, String content) {
        super(position, NodeTypes.COMMENTNODE);

        this.content = content;
    }

    public Instruction accept(NodeVisitor visitor) {
        return null;
    }
    
    public String getContent() {
        return content;
    }

    public List childNodes() {
        return EMPTY_LIST;
    }

}
