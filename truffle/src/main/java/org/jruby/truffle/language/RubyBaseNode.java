/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.provider.MemoryManager;
import org.jcodings.Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.exception.CoreExceptions;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.CoreStrings;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.debug.DebugHelpers;
import org.jruby.truffle.extra.AttachmentsManager;
import org.jruby.truffle.platform.posix.Sockets;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.truffle.stdlib.CoverageManager;
import org.jruby.util.ByteList;

@TypeSystemReference(RubyTypes.class)
@ImportStatic(RubyGuards.class)
public abstract class RubyBaseNode extends Node {

    private static final int FLAG_NEWLINE = 0;
    private static final int FLAG_CALL = 1;

    @CompilationFinal private RubyContext context;
    @CompilationFinal private SourceSection sourceSection;
    @CompilationFinal private int flags;

    public RubyBaseNode() {
    }

    public RubyBaseNode(RubyContext context, SourceSection sourceSection) {
        this.context = context;
        this.sourceSection = sourceSection;
    }

    // Guards which use the context and so can't be static

    protected boolean isNil(Object value) {
        return value == nil();
    }

    protected boolean isRubiniusUndefined(Object value) {
        return value == coreLibrary().getRubiniusUndefined();
    }

    protected DynamicObjectFactory getInstanceFactory(DynamicObject rubyClass) {
        return Layouts.CLASS.getInstanceFactory(rubyClass);
    }

    // Helpers methods for terseness

    protected DynamicObject nil() {
        return coreLibrary().getNilObject();
    }

    protected DynamicObject getSymbol(String name) {
        return getContext().getSymbolTable().getSymbol(name);
    }

    protected DynamicObject getSymbol(Rope name) {
        return getContext().getSymbolTable().getSymbol(name);
    }

    protected DynamicObject createString(ByteList bytes) {
        return StringOperations.createString(getContext(), bytes);
    }

    protected DynamicObject create7BitString(CharSequence value, Encoding encoding) {
        return StringOperations.createString(getContext(), StringOperations.encodeRope(value, encoding, CodeRange.CR_7BIT));
    }

    protected DynamicObject createString(Rope rope) {
        return StringOperations.createString(getContext(), rope);
    }

    protected CoreStrings coreStrings() {
        return getContext().getCoreStrings();
    }

    protected CoreLibrary coreLibrary() {
        return getContext().getCoreLibrary();
    }

    protected CoreExceptions coreExceptions() {
        return getContext().getCoreExceptions();
    }

    protected TrufflePosix posix() {
        return getContext().getNativePlatform().getPosix();
    }

    protected Sockets nativeSockets() {
        return getContext().getNativePlatform().getSockets();
    }

    protected MemoryManager memoryManager() {
        return getContext().getNativePlatform().getMemoryManager();
    }

    // Accessors

    public RubyContext getContext() {
        if (context == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            Node parent = getParent();

            while (true) {
                if (parent == null) {
                    throw new UnsupportedOperationException("can't get the RubyContext because the parent is null");
                }

                if (parent instanceof RubyBaseNode) {
                    context = ((RubyBaseNode) parent).getContext();
                    break;
                }

                if (parent instanceof RubyRootNode) {
                    context = ((RubyRootNode) parent).getContext();
                    break;
                }

                parent = parent.getParent();
            }

        }

        return context;
    }

    // Source section

    @Override
    public SourceSection getSourceSection() {
        return sourceSection;
    }

    // Tags

    public void unsafeSetIsNewLine() {
        flags |= 1 << FLAG_NEWLINE;
    }

    public void unsafeSetIsCall() {
        flags |= 1 << FLAG_CALL;
    }

    private boolean isNewLine() {
        return ((flags >> FLAG_NEWLINE) & 1) == 1;
    }

    private boolean isCall() {
        return ((flags >> FLAG_CALL) & 1) == 1;
    }

    private boolean isRoot() {
        return getParent() instanceof RubyRootNode;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == TraceManager.CallTag.class || tag == StandardTags.CallTag.class) {
            return isCall();
        }

        if (tag == AttachmentsManager.LineTag.class
                || tag == TraceManager.LineTag.class
                || tag == CoverageManager.LineTag.class
                || tag == StandardTags.StatementTag.class) {
            return isNewLine();
        }

        if (tag == StandardTags.RootTag.class) {
            return isRoot();
        }

        return false;
    }

}
