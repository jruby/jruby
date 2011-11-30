/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler;

import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.RubySymbol;

/**
 *
 * @author headius
 */
public enum FastSwitchType {
    FIXNUM(RubyFixnum.class),
    SINGLE_CHAR_STRING(RubyString.class),
    SINGLE_CHAR_SYMBOL(RubySymbol.class);

    private final Class associatedClass;

    FastSwitchType(Class associatedClass) {
        this.associatedClass = associatedClass;
    }

    public Class getAssociatedClass() {
        return associatedClass;
    }
}
