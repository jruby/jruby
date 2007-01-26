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
package org.jruby.compiler.yarv;

import java.util.List;
import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.RootNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.executable.YARVInstructions;
import org.jruby.ast.executable.YARVMachine;
import org.jruby.compiler.Compiler;
import org.jruby.compiler.NodeCompiler;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class StandardYARVCompiler implements NodeCompiler {
    private YARVMachine.InstructionSequence iseq;
    private List until = new ArrayList();
    private Ruby runtime;

    public StandardYARVCompiler(Ruby runtime) {
        this.runtime = runtime;
    }

    private void debugs(String s) {
        System.err.println(s);
    }

    public void compile(Node node, Compiler context) {
        debugs("[compile step 1 (traverse each node)]");
        int line = -1;
        compileLoop: while(true) {
            switch(node.nodeId) {
            case NodeTypes.NEWLINENODE:
                line = node.getPosition().getEndLine();
                node = ((NewlineNode)node).getNextNode();
                continue compileLoop;
            case NodeTypes.ROOTNODE:
                node = ((RootNode)node).getBodyNode();
                continue compileLoop;
            case NodeTypes.STRNODE:
                ADD_INSN1(line, YARVInstructions.PUTSTRING, ((StrNode)node).getValue());
                return;
            case NodeTypes.FCALLNODE:
                FCallNode iNode = (FCallNode) node;
                // ADD_CALL_RECEIVER(line)
                ADD_INSN(line, YARVInstructions.PUTNIL);
                int[] argc_flags = setupArg(iNode.getArgsNode());
                int flags = argc_flags[1];
                flags |= YARVInstructions.FCALL_FLAG;
                ADD_SEND_R(line, iNode.getName(), argc_flags[0], null, flags);
                return;
            default:
                debugs(" ... doesn't handle node: " + node);
                break compileLoop;
            }
        }
    }

    private int[] setupArg(Node node) {
        int[] n = new int[] {0,0};
        if(node == null) {
            return n;
        }
        debugs("setupArg(" + node + ")");
        return n;
    }

    private void ADD_INSN(int line, int insn) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        debugs("ADD_INSN(" + line + ", " + insn + ")");
        until.add(i);
    }

    private void ADD_SEND_R(int line, String name, int argc, Object block, int flags) {

    }

    private void ADD_INSN1(int line, int insn, IRubyObject obj) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        i.o_op0 = obj;
        debugs("ADD_INSN1(" + line + ", " + insn + ", " + obj + ")");
        until.add(i);
    }

    private void ADD_INSN1(int line, int insn, String obj) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        i.s_op0 = obj;
        debugs("ADD_INSN1(" + line + ", " + insn + ", " + obj + ")");
        until.add(i);
    }

    public YARVMachine.InstructionSequence getInstructionSequence() {
        iseq = new YARVMachine.InstructionSequence(runtime,"<main>","<unknown>","toplevel");
        iseq.body = (YARVMachine.Instruction[])until.toArray(new YARVMachine.Instruction[until.size()]);
        return iseq;
    }
}// StandardYARVCompiler
