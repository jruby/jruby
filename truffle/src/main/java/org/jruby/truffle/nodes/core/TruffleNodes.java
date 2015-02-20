/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import java.util.Locale;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyString;

@CoreClass(name = "Truffle")
public abstract class TruffleNodes {

    @CoreMethod(names = "graal?", onSingleton = true)
    public abstract static class GraalNode extends CoreMethodNode {

        public GraalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GraalNode(GraalNode prev) {
            super(prev);
        }

        @Specialization
        public boolean graal() {
            return Truffle.getRuntime().getName().toLowerCase(Locale.ENGLISH).contains("graal");
        }

    }

    @CoreMethod(names = "substrate?", onSingleton = true)
    public abstract static class SubstrateNode extends CoreMethodNode {

        public SubstrateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubstrateNode(SubstrateNode prev) {
            super(prev);
        }

        @Specialization
        public boolean substrate() {
            return getContext().getRuntime().isSubstrateVM();
        }

    }

    @CoreMethod(names = "version", onSingleton = true)
    public abstract static class VersionNode extends CoreMethodNode {

        public VersionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public VersionNode(VersionNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString version() {
            return getContext().makeString(System.getProperty("graal.version", "unknown"));
        }

    }

}
