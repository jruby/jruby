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
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import jnr.ffi.provider.MemoryManager;
import org.jcodings.Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.array.ArrayHelpers;
import org.jruby.truffle.core.exception.CoreExceptions;
import org.jruby.truffle.core.format.FormatRootNode;
import org.jruby.truffle.core.kernel.TraceManager;
import org.jruby.truffle.core.numeric.BignumOperations;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.CoreStrings;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.platform.posix.Sockets;
import org.jruby.truffle.platform.posix.TrufflePosix;
import org.jruby.truffle.stdlib.CoverageManager;
import org.jruby.util.ByteList;

import java.math.BigInteger;

@TypeSystemReference(RubyTypes.class)
@ImportStatic(RubyGuards.class)
public abstract class RubyBaseNode extends Node {

    private static final int FLAG_NEWLINE = 0;
    private static final int FLAG_CALL = 1;
    private static final int FLAG_ROOT = 2;

    @CompilationFinal private RubyContext context;

    private int sourceStartLine;
    private int sourceEndLine;

    private int flags;

    public RubyBaseNode() {
    }

    public RubyBaseNode(RubyContext context) {
        this.context = context;
    }

    public RubyBaseNode(SourceSection sourceSection) {
        if (sourceSection != null) {
            unsafeSetSourceSection(new RubySourceSection(sourceSection));
        }
    }

    public RubyBaseNode(RubySourceSection sourceSection) {
        if (sourceSection != null) {
            unsafeSetSourceSection(sourceSection);
        }
    }

    public RubyBaseNode(RubyContext context, SourceSection sourceSection) {
        this.context = context;

        if (sourceSection != null) {
            unsafeSetSourceSection(new RubySourceSection(sourceSection));
        }
    }

    public RubyBaseNode(RubyContext context, RubySourceSection sourceSection) {
        this.context = context;

        if (sourceSection != null) {
            unsafeSetSourceSection(sourceSection);
        }
    }

    // Guards which use the context and so can't be static

    protected boolean isNil(Object value) {
        return value == nil();
    }

    protected boolean isRubiniusUndefined(Object value) {
        return value == coreLibrary().getRubiniusUndefined();
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

    protected DynamicObject createString(byte[] bytes, Encoding encoding) {
        return StringOperations.createString(getContext(), RopeOperations.create(bytes, encoding, CodeRange.CR_7BIT));
    }

    protected DynamicObject create7BitString(CharSequence value, Encoding encoding) {
        return StringOperations.createString(getContext(), StringOperations.encodeRope(value, encoding, CodeRange.CR_7BIT));
    }

    protected DynamicObject createString(Rope rope) {
        return StringOperations.createString(getContext(), rope);
    }

    protected DynamicObject createArray(Object store, int size) {
        return ArrayHelpers.createArray(getContext(), store, size);
    }

    protected DynamicObject createBignum(BigInteger value) {
        return BignumOperations.createBignum(getContext(), value);
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

    protected DynamicObject handle(Object object) {
        return Layouts.HANDLE.createHandle(coreLibrary().getHandleFactory(), object);
    }

    // Accessors

    public RubyContext getContext() {
        if (context == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();

            Node parent = getParent();

            while (true) {
                if (parent == null) {
                    context = RubyContext.getInstance();
                    break;
                }

                if (parent instanceof RubyBaseNode) {
                    context = ((RubyBaseNode) parent).getContext();
                    break;
                }

                if (parent instanceof RubyRootNode) {
                    context = ((RubyRootNode) parent).getContext();
                    break;
                }

                if (parent instanceof FormatRootNode) {
                    context = ((FormatRootNode) parent).getContext();
                    break;
                }

                parent = parent.getParent();
            }

        }

        return context;
    }

    // Source section

    public void unsafeSetSourceSection(RubySourceSection sourceSection) {
        assert sourceStartLine == 0;
        sourceStartLine = sourceSection.getStartLine();
        sourceEndLine = sourceSection.getEndLine();
    }

    public RubySourceSection getRubySourceSection() {
        if (sourceStartLine == 0) {
            return null;
        } else {
            return new RubySourceSection(sourceStartLine, sourceEndLine);
        }
    }

    public RubySourceSection getEncapsulatingRubySourceSection() {
        Node node = this;

        while (node != null) {
            if (node instanceof RubyBaseNode && ((RubyBaseNode) node).sourceStartLine != 0) {
                return ((RubyBaseNode) node).getRubySourceSection();
            }

            if (node instanceof RootNode) {
                return new RubySourceSection(node.getSourceSection());
            }

            node = node.getParent();
        }

        return null;
    }

    @Override
    public SourceSection getSourceSection() {
        if (sourceStartLine == 0) {
            return null;
        } else {
            final RootNode rootNode = getRootNode();

            if (rootNode == null) {
                return null;
            }

            return getRubySourceSection().toSourceSection(rootNode.getSourceSection().getSource());
        }
    }

    // Tags

    public void unsafeSetIsNewLine() {
        flags |= 1 << FLAG_NEWLINE;
    }

    public void unsafeSetIsCall() {
        flags |= 1 << FLAG_CALL;
    }

    public void unsafeSetIsRoot() {
        flags |= 1 << FLAG_ROOT;
    }

    private boolean isNewLine() {
        return ((flags >> FLAG_NEWLINE) & 1) == 1;
    }

    private boolean isCall() {
        return ((flags >> FLAG_CALL) & 1) == 1;
    }

    private boolean isRoot() {
        return ((flags >> FLAG_ROOT) & 1) == 1;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (tag == TraceManager.CallTag.class || tag == StandardTags.CallTag.class) {
            return isCall();
        }

        if (tag == TraceManager.LineTag.class
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
