/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.dispatch;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;

public abstract class CachedDispatchNode extends DispatchNode {

    private final Object cachedName;
    private final RubySymbol cachedNameAsSymbol;

    @Child protected DispatchNode next;

    private final BranchProfile moreThanReferenceCompare = BranchProfile.create();

    public CachedDispatchNode(RubyContext context, Object cachedName, DispatchNode next) {
        super(context);

        assert (cachedName instanceof String) || (cachedName instanceof RubySymbol) || (cachedName instanceof RubyString);
        this.cachedName = cachedName;

        if (cachedName instanceof RubySymbol) {
            cachedNameAsSymbol = (RubySymbol) cachedName;
        } else if (cachedName instanceof RubyString) {
            cachedNameAsSymbol = context.newSymbol(((RubyString) cachedName).getBytes());
        } else if (cachedName instanceof String) {
            cachedNameAsSymbol = context.newSymbol((String) cachedName);
        } else {
            throw new UnsupportedOperationException();
        }

        this.next = next;
    }

    public CachedDispatchNode(CachedDispatchNode prev) {
        super(prev.getContext());
        cachedName = prev.cachedName;
        cachedNameAsSymbol = prev.cachedNameAsSymbol;
        next = prev.next;
    }

    protected final boolean guardName(
            Object methodReceiverObject,
            LexicalScope lexicalScope,
            Object receiverObject,
            Object methodName,
            Object blockObject,
            Object argumentsObjects) {
        if (cachedName == methodName) {
            return true;
        }

        moreThanReferenceCompare.enter();

        if (cachedName instanceof String) {
            return cachedName.equals(methodName);
        } else if (cachedName instanceof RubySymbol) {
            return cachedName == methodName;
        } else if (cachedName instanceof RubyString) {
            return (methodName instanceof RubyString) && ((RubyString) cachedName).getBytes().equals(((RubyString) methodName).getBytes());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected RubySymbol getCachedNameAsSymbol() {
        return cachedNameAsSymbol;
    }

}
