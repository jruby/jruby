package org.jruby.ir;

import org.jruby.ast.ArgsNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.KeywordRestArgNode;
import org.jruby.ast.Node;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.instructions.ruby20.ReceiveKeywordArgInstr;
import org.jruby.ir.instructions.ruby20.ReceiveKeywordRestArgInstr;

public class IRBuilder20 extends IRBuilder19 {
    public IRBuilder20(IRManager manager) {
        super(manager);
    }

    @Override
    public boolean is1_9() {
        return false;
    }

    @Override
    public boolean is2_0() {
        return true;
    }

    @Override
    public void receiveArgs(final ArgsNode argsNode, IRScope s) {
        // 1.9 pre, opt, rest, post args
        receiveNonBlockArgs(argsNode, s);

        // 2.0 keyword args
        ListNode keywords = argsNode.getKeywords();
        int required = argsNode.getRequiredArgsCount();
        if (keywords != null) {
            for (Node knode : keywords.childNodes()) {
                KeywordArgNode kwarg = (KeywordArgNode)knode;
                AssignableNode kasgn = (AssignableNode)kwarg.getAssignable();
                String argName = ((INameNode) kasgn).getName();
                Variable av = s.getNewLocalVariable(argName, 0);
                Label l = s.getNewLabel();
                // FIXME: add right arg decriptor
                if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("kwarg", argName);
                s.addInstr(new ReceiveKeywordArgInstr(av, required));
                s.addInstr(BNEInstr.create(av, UndefinedValue.UNDEFINED, l)); // if 'av' is not undefined, we are done
                build(kasgn, s);
                s.addInstr(new LabelInstr(l));
            }
        }

        // 2.0 keyword rest arg
        KeywordRestArgNode keyRest = argsNode.getKeyRest();
        if (keyRest != null) {
            String argName = keyRest.getName();
            Variable av = s.getNewLocalVariable(argName, 0);
            // FIXME: add right arg decriptor
            if (s instanceof IRMethod) ((IRMethod)s).addArgDesc("kwrestarg", argName);
            s.addInstr(new ReceiveKeywordRestArgInstr(av, required));
        }

        // Block arg
        receiveBlockArg(argsNode, s);
    }
}
