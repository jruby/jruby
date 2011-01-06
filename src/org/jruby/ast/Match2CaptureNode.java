package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyMatchData;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Match2CaptureNode extends Match2Node {
    // Allocated locals that the regexp will assign after performing a match
    private int[] scopeOffsets;

    public Match2CaptureNode(ISourcePosition position, Node receiverNode, Node valueNode,
            int[] scopeOffsets) {
        super(position, receiverNode, valueNode);

        this.scopeOffsets = scopeOffsets;
    }

    public int[] getScopeOffsets() {
        return scopeOffsets;
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        IRubyObject result = super.interpret(runtime, context, self, aBlock);
        DynamicScope scope = context.getCurrentScope();

        if (result.isNil()) { // match2 directly calls match so we know we can count on result
            IRubyObject nil = runtime.getNil();
            
            for (int i = 0; i < scopeOffsets.length; i++) {
                scope.setValue(nil, scopeOffsets[i], 0);
            }
        } else {
            RubyMatchData matchData = (RubyMatchData) context.getCurrentScope().getBackRef(runtime);
            // FIXME: Mass assignment is possible since we know they are all locals in the same
            //   scope that are also contiguous
            IRubyObject[] namedValues = matchData.getNamedBackrefValues(runtime);

            for (int i = 0; i < scopeOffsets.length; i++) {
                scope.setValue(namedValues[i], scopeOffsets[i] & 0xffff, scopeOffsets[i] >> 16);
            }
        }

        return result;
    }
}
