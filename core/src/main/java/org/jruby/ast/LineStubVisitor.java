package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.ast.visitor.AbstractNodeVisitor;

public class LineStubVisitor extends AbstractNodeVisitor {
    private Ruby runtime;
    private RubyArray lines;

    public LineStubVisitor(Ruby runtime, RubyArray lines) {
        this.runtime = runtime;
        this.lines = lines;
    }
    @Override
    protected Object defaultVisit(Node node) {
        if (node.isNewline()) lines.set(node.getLine() + 1, RubyFixnum.newFixnum(runtime, 0));

        for (Node child: node.childNodes()) {
            if (child != null) defaultVisit(child);
        }

        return null;
    }
}
