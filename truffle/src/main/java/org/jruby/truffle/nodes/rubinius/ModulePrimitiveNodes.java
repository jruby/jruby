/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.rubinius;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;


public abstract class ModulePrimitiveNodes {

    @RubiniusPrimitive(name = "module_mirror")
    public abstract static class ModuleMirrorPrimitiveNode extends RubiniusPrimitiveNode {

        public ModuleMirrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "logicalClass == object.getLogicalClass()")
        public Object moduleMirrorCached(RubyBasicObject object,
                                             @Cached("object.getLogicalClass()") RubyClass logicalClass,
                                             @Cached("lookupMirror(object)") Object mirror) {
            return mirror;
        }

        @Specialization
        public Object moduleMirrorUncached(RubyBasicObject object) {
            return lookupMirror(object);
        }

        @TruffleBoundary
        protected Object lookupMirror(RubyBasicObject object) {
            final RubyModule rubinius = (RubyModule) getContext().getCoreLibrary().getObjectClass().getConstants().get("Rubinius").getValue();
            final RubyModule mirror = (RubyModule) rubinius.getConstants().get("Mirror").getValue();
            final RubyConstant objectMirrorConstant = mirror.getConstants().get(object.getLogicalClass().getName());

            if (objectMirrorConstant == null) {
                return nil();
            }

            return objectMirrorConstant.getValue();
        }

    }

}
