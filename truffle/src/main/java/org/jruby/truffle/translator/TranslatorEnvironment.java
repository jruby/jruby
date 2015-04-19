/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.methods.locals.ReadLevelVariableNodeFactory;
import org.jruby.truffle.nodes.methods.locals.ReadLocalVariableNodeFactory;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.core.RubyProc;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TranslatorEnvironment {

    private final RubyContext context;

    private final ParseEnvironment parseEnvironment;

    private final FrameDescriptor frameDescriptor;

    private final List<FrameSlot> flipFlopStates = new ArrayList<>();

    private final long returnID;
    private final boolean isBlock;

    private final boolean ownScopeForAssignments;
    private final boolean neverAssignInParentScope;

    protected final TranslatorEnvironment parent;
    private boolean needsDeclarationFrame = true; // We keep the logic as we might do it differently one day.
    private final SharedMethodInfo sharedMethodInfo;

    private final String namedMethodName;

    // TODO(CS): overflow?
    private static AtomicInteger tempIndex = new AtomicInteger();

    public boolean hasRestParameter = false;

    public TranslatorEnvironment(RubyContext context, TranslatorEnvironment parent, FrameDescriptor frameDescriptor, ParseEnvironment parseEnvironment, long returnID, boolean ownScopeForAssignments,
                    boolean neverAssignInParentScope, SharedMethodInfo sharedMethodInfo, String namedMethodName, boolean isBlock) {
        this.context = context;
        this.parent = parent;
        this.frameDescriptor = frameDescriptor;
        this.parseEnvironment = parseEnvironment;
        this.returnID = returnID;
        this.ownScopeForAssignments = ownScopeForAssignments;
        this.neverAssignInParentScope = neverAssignInParentScope;
        this.sharedMethodInfo = sharedMethodInfo;
        this.namedMethodName = namedMethodName;
        this.isBlock = isBlock;
    }

    public TranslatorEnvironment(RubyContext context, TranslatorEnvironment parent, ParseEnvironment parseEnvironment, long returnID, boolean ownScopeForAssignments, boolean neverAssignInParentScope,
                    SharedMethodInfo methodIdentifier, String namedMethodName, boolean isBlock) {
        this(context, parent, new FrameDescriptor(context.getCoreLibrary().getNilObject()), parseEnvironment, returnID, ownScopeForAssignments, neverAssignInParentScope, methodIdentifier,
                namedMethodName, isBlock);
    }

    public static TranslatorEnvironment newRootEnvironment(RubyContext context, FrameDescriptor frameDescriptor, ParseEnvironment parseEnvironment, long returnID, boolean ownScopeForAssignments,
            boolean neverAssignInParentScope, SharedMethodInfo sharedMethodInfo, String namedMethodName, boolean isBlock) {
        return new TranslatorEnvironment(context, null, frameDescriptor, parseEnvironment, returnID, ownScopeForAssignments, neverAssignInParentScope, sharedMethodInfo, namedMethodName, isBlock);
    }

    public LexicalScope getLexicalScope() {
        return parseEnvironment.getLexicalScope();
    }

    public LexicalScope pushLexicalScope() {
        return parseEnvironment.pushLexicalScope();
    }

    public void popLexicalScope() {
        parseEnvironment.popLexicalScope();
    }

    public TranslatorEnvironment getParent() {
        return parent;
    }

    public TranslatorEnvironment getParent(int level) {
        assert level >= 0;
        if (level == 0) {
            return this;
        } else {
            return parent.getParent(level - 1);
        }
    }

    public FrameSlot declareVar(String name) {
        return getFrameDescriptor().findOrAddFrameSlot(name);
    }

    public FrameSlot declareVarWhereAllowed(String name) {
        if (isBlock) {
            return parent.declareVarWhereAllowed(name);
        } else {
            return declareVar(name);
        }
    }

    public SharedMethodInfo findMethodForLocalVar(String name) {
        TranslatorEnvironment current = this;
        do {
            FrameSlot slot = current.getFrameDescriptor().findFrameSlot(name);
            if (slot != null) {
                return current.sharedMethodInfo;
            }

            current = current.parent;
        } while (current != null);

        return null;
    }

    public RubyNode findOrAddLocalVarNodeDangerous(String name, SourceSection sourceSection) {
        RubyNode localVar = findLocalVarNode(name, sourceSection);

        if (localVar == null) {
            declareVar(name);
            localVar = findLocalVarNode(name, sourceSection);
        }

        return localVar;
    }

    public RubyNode findLocalVarNode(String name, SourceSection sourceSection) {
        TranslatorEnvironment current = this;
        int level = -1;
        try {
            do {
                level++;
                FrameSlot slot = current.getFrameDescriptor().findFrameSlot(name);
                if (slot != null) {
                    if (level == 0) {
                        return ReadLocalVariableNodeFactory.create(context, sourceSection, slot);
                    } else {
                        return ReadLevelVariableNodeFactory.create(context, sourceSection, slot, level);
                    }
                }

                current = current.parent;
            } while (current != null);
        } finally {
            if (current != null) {
                current = this;
                while (level-- > 0) {
                    current.needsDeclarationFrame = true;
                    current = current.parent;
                }
            }
        }

        return null;
    }

    public void setNeedsDeclarationFrame() {
        needsDeclarationFrame = true;
    }

    public boolean needsDeclarationFrame() {
        return needsDeclarationFrame;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public String allocateLocalTemp(String indicator) {
        final String name = "rubytruffle_temp_" + indicator + "_" + tempIndex.getAndIncrement();
        declareVar(name);
        return name;
    }

    public long getReturnID() {
        return returnID;
    }

    public ParseEnvironment getParseEnvironment() {
        return parseEnvironment;
    }

    public boolean hasOwnScopeForAssignments() {
        return ownScopeForAssignments;
    }

    public boolean getNeverAssignInParentScope() {
        return neverAssignInParentScope;
    }

    public SharedMethodInfo getSharedMethodInfo() {
        return sharedMethodInfo;
    }

    public List<FrameSlot> getFlipFlopStates() {
        return flipFlopStates;
    }

    public String getNamedMethodName() {
        return namedMethodName;
    }

    public boolean isBlock() {
        return isBlock;
    }
}
