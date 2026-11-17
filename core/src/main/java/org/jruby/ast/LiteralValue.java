package org.jruby.ast;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This represents the same function as NODE_LIT in MRI.  All of these values are capable of
 * being printed in error messages without neccesarily being defined in our Runtime (although
 * SymbolNode does eagerly make RubySymbols.
 *
 * This is used in remove_duplicate_keys to look for repeated keys and also by flip logic.
 */
public interface LiteralValue {
    IRubyObject literalValue(Ruby runtime);
}
