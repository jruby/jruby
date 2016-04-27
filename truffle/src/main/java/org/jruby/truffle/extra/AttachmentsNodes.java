/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.extra;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventBinding;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.posix.SpawnFileAction;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.YieldingCoreMethodNode;
import org.jruby.truffle.core.array.ArrayOperations;
import org.jruby.truffle.core.array.ArrayStrategy;
import org.jruby.truffle.core.binding.BindingNodes;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeBuffer;
import org.jruby.truffle.core.rope.RopeNodes;
import org.jruby.truffle.core.rope.RopeNodesFactory;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.parser.ParserContext;
import org.jruby.truffle.platform.UnsafeGroup;
import org.jruby.truffle.tools.simpleshell.SimpleShell;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;
import org.jruby.util.unsafe.UnsafeHolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CoreClass(name = "Truffle::Attachments")
public abstract class AttachmentsNodes {

    @CoreMethod(names = "attach_internal", onSingleton = true, required = 2, needsBlock = true)
    public abstract static class AttachNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isRubyString(file)")
        public DynamicObject attach(DynamicObject file, int line, DynamicObject block) {
            return Layouts.HANDLE.createHandle(coreLibrary().getHandleFactory(), getContext().getAttachmentsManager().attach(file.toString(), line, block));
        }

    }

    @CoreMethod(names = "detach_internal", onSingleton = true, required = 1)
    public abstract static class DetachNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isHandle(handle)")
        public DynamicObject detach(DynamicObject handle) {
            final EventBinding<?> binding = (EventBinding<?>) Layouts.HANDLE.getObject(handle);
            binding.dispose();
            return getContext().getCoreLibrary().getNilObject();
        }

    }

}
