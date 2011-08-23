package org.jruby.ast;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

/**
 * Base class for all D (e.g. Dynamic) node types like DStrNode, DSymbolNode, etc...
 */
public abstract class DNode extends ListNode {
    protected Encoding encoding; // If encoding is set then we should obey 1.9 semantics.

    public DNode(ISourcePosition position) {
        this(position, null);
    }

    public DNode(ISourcePosition position, Encoding encoding) {
        super(position);

        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public IRubyObject interpret(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        return buildDynamicString(runtime, context, self, aBlock);
    }

    // Enebo: Without massive rethink of AST system I think we are stuck with an if check
    public boolean is19() {
        return encoding != null;
    }

    public boolean isSameEncoding(StrNode strNode) {
        return strNode.getValue().getEncoding() == encoding;
    }

    protected RubyString allocateString(Ruby runtime) {
        ByteList bytes = new ByteList();
        
        if (is19()) bytes.setEncoding(encoding);

        return RubyString.newStringShared(runtime, bytes, StringSupport.CR_7BIT);
    }

    public void appendToString(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock, RubyString string, Node node) {
        if (node instanceof StrNode && (!is19() || isSameEncoding((StrNode) node))) {
            string.getByteList().append(((StrNode) node).getValue());
        } else if (is19()) {
            string.append19(node.interpret(runtime, context, self, aBlock));
        } else {
            string.append(node.interpret(runtime, context, self, aBlock));
        }
    }

    public RubyString buildDynamicString(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyString string = allocateString(runtime);

        int size = size();
        for (int i = 0; i < size; i++) {
            appendToString(runtime, context, self, aBlock, string, get(i));
        }

        return string;
    }

    @Override
    public ByteList definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        ByteList definition = super.definition(runtime, context, self, aBlock);
        return is19() && definition == null ? EXPRESSION_BYTELIST : definition;
    }
}