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
import java.util.Iterator;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.IArgumentNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.Node;
import org.jruby.ast.NodeTypes;
import org.jruby.ast.RootNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.executable.YARVInstructions;
import org.jruby.ast.executable.YARVMachine;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.compiler.Compiler;
import org.jruby.compiler.NodeCompiler;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class StandardYARVCompiler implements NodeCompiler {
    private YARVMachine.InstructionSequence iseq;
    private Ruby runtime;
    private int last_line = -1;

    private LinkAnchor current_iseq;

    private String[] locals = new String[0];

    private static final int COMPILE_OK=1;
    private static final int COMPILE_NG=0;

    private static abstract class LinkElement {
        public LinkElement next;
        public LinkElement prev;
    }

    private static class LinkAnchor extends LinkElement {
        LinkElement last;
    }

    private static class Label extends LinkElement {
    }

    private static class Insn extends LinkElement {
        YARVMachine.Instruction i;
    }

    private static class EnsureRange {
        Label begin;
        Label end;
        EnsureRange next;
    }

    private static void verify_list(String info, LinkAnchor anchor) {
        int flag = 0;
        LinkElement list = anchor.next;
        LinkElement plist = anchor;
        while(list != null) {
            if(plist != list.prev) {
                flag++;
            }
            plist = list;
            list = list.next;
        }

        if(anchor.last != plist && anchor.last != null) {
            flag |= 0x70000;
        }

        if(flag != 0) {
            throw new RuntimeException("list verify error: " + Integer.toString(flag, 16) + " (" + info + ")");
        }
    }

    private static void ADD_ELEM(LinkAnchor anchor, LinkElement elem) {
        elem.prev = anchor.last;
        anchor.last.next = elem;
        anchor.last = elem;
        verify_list("add", anchor);
    }

    private static void INSERT_ELEM_PREV(LinkElement elem1, LinkElement elem2) {
        elem2.prev = elem1.prev;
        elem2.next = elem1;
        elem1.prev = elem2;
        if(elem2.prev != null) {
            elem2.prev.next = elem2;
        }
    }

    private static void REPLACE_ELEM(LinkElement elem1, LinkElement elem2) {
        elem2.prev = elem1.prev;
        elem2.next = elem1.next;
        if(elem1.prev != null) {
            elem1.prev.next = elem2;
        }
        if(elem1.next != null) {
            elem1.next.prev = elem2;
        }
    }

    private static void REMOVE_ELEM(LinkElement elem) {
        elem.prev.next = elem.next;
        if(elem.next != null) {
            elem.next.prev = elem.prev;
        }
    }

    private static LinkElement FIRST_ELEMENT(LinkAnchor anchor) {
        return anchor.next;
    }

    private static LinkElement POP_ELEMENT(LinkAnchor anchor) {
        LinkElement elem = anchor.last;
        anchor.last = anchor.last.prev;
        anchor.last.next = null;
        verify_list("pop", anchor);
        return elem;
    }

    private static LinkElement SHIFT_ELEMENT(LinkAnchor anchor) {
        LinkElement elem = anchor.next;
        if(null != elem) {
            anchor.next = elem.next;
        }
        return elem;
    }

    private static int LIST_SIZE(LinkAnchor anchor) {
        LinkElement elem = anchor.next;
        int size = 0;
        while(elem != null) {
            size++;
            elem = elem.next;
        }
        return size;
    }

    private static boolean LIST_SIZE_ZERO(LinkAnchor anchor) {
        return anchor.next == null;
    }

    private static void APPEND_LIST(LinkAnchor anc1, LinkAnchor anc2) {
        if(anc2.next != null) {
            anc1.last.next = anc2.next;
            anc2.next.prev = anc1.last;
            anc1.last = anc2.last;
        }
        verify_list("append", anc1);
    }

    private static void INSERT_LIST(LinkAnchor anc1, LinkAnchor anc2) {
        if(anc2.next != null) {
            LinkElement first = anc1.next;
            anc1.next = anc2.next;
            anc1.next.prev = anc1;
            anc2.last.next = first;
            if(first != null) {
                first.prev = anc2.last;
            } else {
                anc1.last = anc2.last;
            }
        }
        verify_list("append", anc1);
    }

    private static void ADD_SEQ(LinkAnchor seq1, LinkAnchor seq2) {
        APPEND_LIST(seq1,seq2);
    }

    private int debug_compile(String msg, int v) {
        debugs(msg);
        return v;
    }

    private int COMPILE(LinkAnchor anchor, String desc, Node node) {
        return debug_compile("== " + desc, iseq_compile_each(anchor, node, false));
    }

    private int COMPILE(LinkAnchor anchor, String desc, Node node, boolean poped) {
        return debug_compile("== " + desc, iseq_compile_each(anchor, node, poped));
    }

    private int COMPILE_POPED(LinkAnchor anchor, String desc, Node node) {
        return debug_compile("== " + desc, iseq_compile_each(anchor, node, true));
    }

    private LinkAnchor DECL_ANCHOR() {
        LinkAnchor l = new LinkAnchor();
        l.last = l;
        return l;
    }

    public StandardYARVCompiler(Ruby runtime) {
        this.runtime = runtime;
    }

    private void debugs(String s) {
        System.err.println(s);
    }

    public void compile(int line, Node node) {
    }

    public void compile(Node node) {
        iseq_compile(null,node);
    }

    public void compile(Node node, Compiler context) {
        compile(node);
    }

    public void iseq_compile(IRubyObject self, Node narg) {
        LinkAnchor list_anchor = DECL_ANCHOR();
        Node node = narg;
        debugs("[compile step 1 (traverse each node)]");
        COMPILE(list_anchor, "top level node", node);
        ADD_INSN(list_anchor, last_line, YARVInstructions.LEAVE);
        current_iseq = list_anchor;
    }

    private int nd_line(Node node) {
        if(node.getPosition() != null) {
            return node.getPosition().getEndLine();
        }
        return last_line;
    }

    private int iseq_compile_each(LinkAnchor ret, Node node, boolean poped) {
        if(node == null) {
            if(!poped) {
                debugs("NODE_NIL(implicit)");
                ADD_INSN(ret, 0, YARVInstructions.PUTNIL);
                return COMPILE_OK;
            }
            return COMPILE_OK;
        }
        last_line = nd_line(node);

        LinkAnchor recv = null;
        LinkAnchor args = null;

        compileLoop: while(true) {
            switch(node.nodeId) {
            case NodeTypes.BLOCKNODE:
                List l = ((BlockNode)node).childNodes();
                int sz = l.size();
                for(int i=0;i<sz;i++) {
                    boolean p = !(i+1 == sz && !poped);
                    COMPILE(ret, "BLOCK body", (Node)l.get(i),p);
                }
                break compileLoop;
            case NodeTypes.NEWLINENODE:
                node = ((NewlineNode)node).getNextNode();
                continue compileLoop;
            case NodeTypes.ROOTNODE:
                locals = ((RootNode)node).getStaticScope().getAllNamesInScope();
                node = ((RootNode)node).getBodyNode();
                continue compileLoop;
            case NodeTypes.STRNODE:
                if(!poped) {
                    ADD_INSN1(ret, nd_line(node), YARVInstructions.PUTSTRING, ((StrNode)node).getValue());
                }
                break compileLoop;
            case NodeTypes.CONSTNODE:
                // Check for inline const cache here
                ADD_INSN(ret, nd_line(node), YARVInstructions.PUTNIL);
                ADD_INSN1(ret, nd_line(node), YARVInstructions.GETCONSTANT, ((ConstNode)node).getName());
                if(poped) {
                    ADD_INSN(ret, nd_line(node), YARVInstructions.POP);
                }
                break compileLoop;
            case NodeTypes.LOCALASGNNODE:
                int idx = ((LocalAsgnNode)node).getIndex()-2;
                debugs("lvar: " + idx);
                COMPILE(ret, "lvalue", ((LocalAsgnNode)node).getValueNode());
                if(!poped) {
                    ADD_INSN(ret, nd_line(node), YARVInstructions.DUP);
                }
                ADD_INSN1(ret, nd_line(node), YARVInstructions.SETLOCAL, idx);
                break compileLoop;
            case NodeTypes.LOCALVARNODE:
                if(!poped) {
                    int idx2 = ((LocalVarNode)node).getIndex()-2;
                    debugs("idx: "+idx2);
                    ADD_INSN1(ret, nd_line(node), YARVInstructions.GETLOCAL, idx2);
                }
                break compileLoop;
            case NodeTypes.CALLNODE:
            case NodeTypes.FCALLNODE:
            case NodeTypes.VCALLNODE:
                recv = DECL_ANCHOR();
                args = DECL_ANCHOR();
                if(node instanceof CallNode) {
                    COMPILE(recv, "recv", ((CallNode)node).getReceiverNode());
                } else {
                    ADD_CALL_RECEIVER(recv, nd_line(node));
                }
                int argc = 0;
                int flags = 0;
                if(!(node instanceof VCallNode)) {
                    int[] argc_flags = setup_arg(args, (IArgumentNode)node);
                    argc = argc_flags[0];
                    flags = argc_flags[1];
                } else {
                    argc = 0;
                }

                ADD_SEQ(ret, recv);
                ADD_SEQ(ret, args);

                switch(node.nodeId) {
                case NodeTypes.VCALLNODE:
                    flags |= YARVInstructions.VCALL_FLAG;
                    /* VCALL is funcall, so fall through */
                case NodeTypes.FCALLNODE:
                    flags |= YARVInstructions.FCALL_FLAG;
                }

                ADD_SEND_R(ret, nd_line(node), ((INameNode)node).getName(), argc, null, flags);
                if(poped) {
                    ADD_INSN(ret, nd_line(node), YARVInstructions.POP);
                }
                break compileLoop;
            case NodeTypes.ARRAYNODE:
                compile_array(ret, node, true);
                if(poped) {
                    ADD_INSN(ret, nd_line(node), YARVInstructions.POP);
                }
                break compileLoop;
            case NodeTypes.ZARRAYNODE:
                if(!poped) {
                    ADD_INSN1(ret, nd_line(node), YARVInstructions.NEWARRAY, 0);
                }
                break compileLoop;
            case NodeTypes.HASHNODE:
                LinkAnchor list = DECL_ANCHOR();
                long size = 0;
                Node lnode = ((HashNode)node).getListNode();
                if(lnode.childNodes().size()>0) {
                    compile_array(list, lnode, false);
                    size = ((Insn)POP_ELEMENT(list)).i.l_op0;
                    ADD_SEQ(ret, list);
                }

                ADD_INSN1(ret, nd_line(node), YARVInstructions.NEWHASH, size);

                if(poped) {
                    ADD_INSN(ret, nd_line(node), YARVInstructions.POP);
                }
                break compileLoop;
            case NodeTypes.FIXNUMNODE:
                FixnumNode iVisited = (FixnumNode) node;
                if(!poped) {
                    ADD_INSN1(ret, nd_line(node), YARVInstructions.PUTOBJECT, iVisited.getFixnum(runtime));
                }
                break compileLoop;
            default:
                debugs(" ... doesn't handle node: " + node);
                break compileLoop;
            }
        }

        return COMPILE_OK;
    }

    private int compile_array(LinkAnchor ret, Node node_root, boolean opt_p) {
        Node node = node_root;
        int len = ((ArrayNode)node).size();
        int line = nd_line(node);
        int i =0;
        LinkAnchor anchor = DECL_ANCHOR();
        List c = node.childNodes();
        for(Iterator iter = c.iterator(); iter.hasNext();) {
            node = (Node)iter.next();
            if(opt_p && !(node instanceof ILiteralNode)) {
                opt_p = false;
            }
            COMPILE(anchor, "array element", node);
        }

        if(opt_p) {
            List l = new ArrayList();
            for(Iterator iter = c.iterator(); iter.hasNext();) {
                node = (Node)iter.next();
                switch(node.nodeId) {
                case NodeTypes.FIXNUMNODE:
                    l.add(((FixnumNode)node).getFixnum(runtime));
                    break;
                default:
                    debugs(" ... doesn't handle array literal node: " + node);
                    break;
                }
            }
            ADD_INSN1(ret, nd_line(node_root), YARVInstructions.DUPARRAY, runtime.newArray(l));
        } else {
            ADD_INSN1(anchor, line, YARVInstructions.NEWARRAY, len);
            APPEND_LIST(ret, anchor);
        }

        return len;
    }

    private int[] setup_arg(LinkAnchor args, IArgumentNode node) {
        int[] n = new int[] {0,0};
        Node argn = node.getArgsNode();
        LinkAnchor arg_block = DECL_ANCHOR();
        LinkAnchor args_push = DECL_ANCHOR();

        if(argn != null) {
            switch(argn.nodeId) {
            case NodeTypes.SPLATNODE:
                break;
            case NodeTypes.ARGSCATNODE:
                break;
            case NodeTypes.ARGSPUSHNODE:
                break;
            default:
                n[0] = compile_array(args,argn,false);
                POP_ELEMENT(args);
                break;
            }
        }
        
        if (!LIST_SIZE_ZERO(args_push)) {
            ADD_SEQ(args, args_push);
        }

        return n;
    }

    private Insn new_insn(YARVMachine.Instruction i) {
        Insn n = new Insn();
        n.i = i;
        n.next = null;
        return n;
    }

    private void ADD_CALL_RECEIVER(LinkAnchor seq, int line) {
        ADD_INSN(seq, line, YARVInstructions.PUTNIL);
    }

    private void ADD_INSN(LinkAnchor seq, int line, int insn) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        debugs("ADD_INSN(" + line + ", " + YARVInstructions.name(insn) + ")");
        ADD_ELEM(seq, new_insn(i));
    }

    private void ADD_SEND_R(LinkAnchor seq, int line, String name, int argc, Object block, int flags) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(YARVInstructions.SEND);
        i.line_no = line;
        i.s_op0 = name;
        i.i_op1 = argc;
        i.i_op3 = flags;
        debugs("ADD_SEND_R(" + line + ", " + YARVInstructions.name(YARVInstructions.SEND) + ", " + name + ", " + argc + ", " + flags + ")");
        ADD_ELEM(seq, new_insn(i));
    }

    private void ADD_INSN1(LinkAnchor seq, int line, int insn, IRubyObject obj) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        i.o_op0 = obj;
        debugs("ADD_INSN1(" + line + ", " + YARVInstructions.name(insn) + ", " + obj + ")");
        ADD_ELEM(seq, new_insn(i));
    }

    private void ADD_INSN1(LinkAnchor seq, int line, int insn, long op) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        i.l_op0 = op;
        debugs("ADD_INSN1(" + line + ", " + YARVInstructions.name(insn) + ", " + op + ")");
        ADD_ELEM(seq, new_insn(i));
    }

    private void ADD_INSN1(LinkAnchor seq, int line, int insn, String obj) {
        YARVMachine.Instruction i = new YARVMachine.Instruction(insn);
        i.line_no = line;
        i.s_op0 = obj;
        debugs("ADD_INSN1(" + line + ", " + YARVInstructions.name(insn) + ", " + obj + ")");
        ADD_ELEM(seq, new_insn(i));
    }

    public YARVMachine.InstructionSequence getInstructionSequence(String name, String filename, String level) {
        iseq = new YARVMachine.InstructionSequence(runtime, name, filename, level);
        List l = new ArrayList();
        LinkElement elm = current_iseq;
        while(elm != null) {
            if(elm instanceof Insn) {
                l.add(((Insn)elm).i);
            }
            elm = elm.next;
        }
        debugs("instructions: " + l);
        iseq.body = (YARVMachine.Instruction[])l.toArray(new YARVMachine.Instruction[l.size()]);
        iseq.locals = locals;
        return iseq;
    }
}// StandardYARVCompiler
