/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public abstract class CachedDispatchNode extends DispatchNode {

    private final Object cachedName;
    private final RubySymbol cachedNameAsSymbol;
    private final boolean indirect;

    @Child protected DispatchNode next;

    private final BranchProfile moreThanReferenceCompare = BranchProfile.create();

    public CachedDispatchNode(
            RubyContext context,
            Object cachedName,
            DispatchNode next,
            boolean indirect,
            DispatchAction dispatchAction) {
        super(context, dispatchAction);

        assert (cachedName instanceof String) || (cachedName instanceof RubySymbol) || (cachedName instanceof RubyString);
        this.cachedName = cachedName;

        if (cachedName instanceof RubySymbol) {
            cachedNameAsSymbol = (RubySymbol) cachedName;
        } else if (cachedName instanceof RubyString) {
            cachedNameAsSymbol = context.getSymbol(((RubyString) cachedName).getByteList());
        } else if (cachedName instanceof String) {
            cachedNameAsSymbol = context.getSymbol((String) cachedName);
        } else {
            throw new UnsupportedOperationException();
        }

        this.indirect = indirect;

        this.next = next;
    }

    @Override
    protected DispatchNode getNext() {
        return next;
    }

    protected final boolean guardName(Object methodName) {
        if (cachedName == methodName) {
            return true;
        }

        moreThanReferenceCompare.enter();

        if (cachedName instanceof String) {
            return cachedName.equals(methodName);
        } else if (cachedName instanceof RubySymbol) {
            // TODO(CS, 11-Jan-15) this just repeats the above guard...
            return cachedName == methodName;
        } else if (cachedName instanceof RubyString) {
            return (methodName instanceof RubyString) && ((RubyString) cachedName).getByteList().equal(((RubyString) methodName).getByteList());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected RubySymbol getCachedNameAsSymbol() {
        return cachedNameAsSymbol;
    }

    public boolean isIndirect() {
        return indirect;
    }
}
