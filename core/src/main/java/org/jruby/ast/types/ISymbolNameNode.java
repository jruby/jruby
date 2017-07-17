package org.jruby.ast.types;

import org.jruby.RubySymbol;

/**
 * Symbols are live Ruby objects and not all node types can
 * stand up a symbol on demand (no runtime access) so we
 * differentiate between these special name nodes.
 */
public interface ISymbolNameNode extends INameNode {
    public RubySymbol getSymbolName();
}
