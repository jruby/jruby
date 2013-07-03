package org.jruby.ast;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.Helpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;

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
        RubyString string = RubyString.newString(runtime, new ByteList());

        if (is19()) string.setEncoding(encoding);

        return string;
    }

    public void appendToString(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock, RubyString string, Node node) {
        if (node instanceof StrNode) {
            StrNode strNode = (StrNode)node;
            if (!is19() || isSameEncoding(strNode)) {
                string.getByteList().append(strNode.getValue());
            } else {
                string.cat19(strNode.getValue(), strNode.getCodeRange());
            }
        } else if (node instanceof EvStrNode) {
            EvStrNode evStrNode = (EvStrNode)node;

            Node bodyNode = evStrNode.getBody();
            if (bodyNode == null) return;

            IRubyObject body = bodyNode.interpret(runtime, context, self, aBlock);
            if (is19()) {
                Helpers.shortcutAppend(string, body);
            } else {
                Helpers.shortcutAppend18(string, body);
            }
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
    public RubyString definition(Ruby runtime, ThreadContext context, IRubyObject self, Block aBlock) {
        RubyString definition = super.definition(runtime, context, self, aBlock);
        return is19() && definition == null ? runtime.getDefinedMessage(DefinedMessage.EXPRESSION) : definition;
    }
}