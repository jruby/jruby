/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.methods;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyModule;

@NodeChild(value="module", type=RubyNode.class)
public abstract class AliasNode extends RubyNode {

    @Child private RubyNode module;
    final String newName;
    final String oldName;

    public AliasNode(RubyContext context, SourceSection sourceSection, String newName, String oldName) {
        super(context, sourceSection);
        this.newName = newName;
        this.oldName = oldName;
    }

    public AliasNode(AliasNode prev) {
        super(prev);
        newName = prev.newName;
        oldName = prev.oldName;
    }

    public Object noClass() {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(getContext().getCoreLibrary().typeErrorNoClassToMakeAlias(this));
    }

    @Specialization
    public Object alias(boolean value) {
        return noClass();
    }

    @Specialization
    public Object alias(int value) {
        return noClass();
    }

    @Specialization
    public Object alias(long value) {
        return noClass();
    }

    @Specialization
    public Object alias(double value) {
        return noClass();
    }

    @Specialization
    public Object alias(RubyBignum value) {
        return noClass();
    }

    @Specialization
    public Object alias(RubyModule module) {
        notDesignedForCompilation();

        module.alias(this, newName, oldName);
        return null;
    }

    @Specialization(guards = {"!isRubyModule", "!isRubyBignum"})
    public Object alias(RubyBasicObject object) {
        notDesignedForCompilation();

        object.getSingletonClass(this).alias(this, newName, oldName);
        return null;
    }

}
