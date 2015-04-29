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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyString;

public abstract class ModulePrimitiveNodes {

    @RubiniusPrimitive(name = "module_mirror")
    public abstract static class ModuleMirrorPrimitiveNode extends RubiniusPrimitiveNode {

        @CompilerDirectives.CompilationFinal RubyModule stringMirror;
        
        public ModuleMirrorPrimitiveNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyModule moduleMirror(RubyString string) {
            if (stringMirror == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                final RubyModule rubinius = (RubyModule) getContext().getCoreLibrary().getObjectClass().getConstants().get("Rubinius").getValue();
                final RubyModule mirror = (RubyModule) rubinius.getConstants().get("Mirror").getValue();
                stringMirror = (RubyModule) mirror.getConstants().get("String").getValue();
            }
            
            return stringMirror;
        }

    }

}
