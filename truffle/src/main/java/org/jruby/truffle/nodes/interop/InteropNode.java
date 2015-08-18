/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.MethodNodes;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.objects.ReadInstanceVariableNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.InternalMethod;

import java.util.List;

public abstract class InteropNode extends RubyNode {

    public InteropNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static InteropNode createRead(RubyContext context, SourceSection sourceSection) {
        return new UnresolvedInteropReadNode(context, sourceSection);
    }

    public static InteropNode createWrite(RubyContext context, SourceSection sourceSection) {
        return new UnresolvedInteropWriteNode(context, sourceSection);
    }

    public static InteropNode createExecuteAfterRead(RubyContext context, SourceSection sourceSection, int arity) {
        return new UnresolvedInteropExecuteAfterReadNode(context, sourceSection, arity);
    }

    public static InteropNode createIsExecutable(final RubyContext context, final SourceSection sourceSection) {
        return new InteropIsExecutable(context, sourceSection);
    }
    
    public static InteropNode createExecute(final RubyContext context, final SourceSection sourceSection) {
        return new InteropExecute(context, sourceSection);
    }

    public static InteropNode createIsBoxedPrimitive(final RubyContext context, final SourceSection sourceSection) {
        return new InteropIsBoxedPrimitive(context, sourceSection);
    }

    public static InteropNode createIsNull(final RubyContext context, final SourceSection sourceSection) {
        return new InteropIsNull(context, sourceSection);
    }

    public static InteropNode createHasSizePropertyFalse(final RubyContext context, final SourceSection sourceSection) {
        return new InteropHasSizePropertyFalse(context, sourceSection);
    }

    public static InteropNode createHasSizePropertyTrue(final RubyContext context, final SourceSection sourceSection) {
        return new InteropHasSizePropertyTrue(context, sourceSection);
    }

    public static RubyNode createGetSize(RubyContext context, final SourceSection sourceSection) {
        return new InteropGetSizeProperty(context, sourceSection);
    }

    public static RubyNode createStringIsBoxed(RubyContext context, final SourceSection sourceSection) {
        return new InteropStringIsBoxed(context, sourceSection);
    }

    public static RubyNode createStringRead(RubyContext context, final SourceSection sourceSection) {
        return new UnresolvedInteropStringReadNode(context, sourceSection);
    }

    public static RubyNode createStringUnbox(RubyContext context, final SourceSection sourceSection) {
        return new InteropStringUnboxNode(context, sourceSection);
    }
    
    private static class InteropExecute extends InteropNode {
        @Child private ExecuteMethodNode execute;
    	
    	public InteropExecute(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.execute = InteropNodeFactory.ExecuteMethodNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
        	Object result = execute.executeWithTarget(frame, ForeignAccess.getReceiver(frame));
            return result;
        }
    }
    
    protected static abstract class AbstractExecuteMethodNode extends InteropNode {
    	public AbstractExecuteMethodNode(RubyContext context,
				SourceSection sourceSection) {
			super(context, sourceSection);
		}

		public abstract Object executeWithTarget(VirtualFrame frame, Object method);
    }
    
    @NodeChild(value="method", type = InteropNode.class)
    protected static abstract class ExecuteMethodNode extends AbstractExecuteMethodNode {
    	@Child private IndirectCallNode callNode;
    	public ExecuteMethodNode(RubyContext context,
				SourceSection sourceSection) {
			super(context, sourceSection);
			callNode = Truffle.getRuntime().createIndirectCallNode();
		}
        
		@Specialization(guards = {"isRubyMethod(method)", "method == cachedMethod"})
    	protected Object doCall(VirtualFrame frame, DynamicObject method,
                                @Cached("method") DynamicObject cachedMethod,
                                @Cached("getMethod(cachedMethod)") InternalMethod internalMethod,
                                @Cached("create(getMethod(cachedMethod).getCallTarget())") DirectCallNode callNode) {
                        final List<Object> faArgs = ForeignAccess.getArguments(frame);
    		// skip first argument; it's the receiver but a RubyMethod knows its receiver
			Object[] args = faArgs.subList(1, faArgs.size()).toArray();
			return callNode.call(frame, RubyArguments.pack(internalMethod, internalMethod.getDeclarationFrame(), Layouts.METHOD.getReceiver(cachedMethod), null, args));
    	}
		
		@Specialization(guards = "isRubyMethod(method)")
    	protected Object doCall(VirtualFrame frame, DynamicObject method) {
			final InternalMethod internalMethod = Layouts.METHOD.getMethod(method);
                        final List<Object> faArgs = ForeignAccess.getArguments(frame);
    		// skip first argument; it's the receiver but a RubyMethod knows its receiver
			Object[] args = faArgs.subList(1, faArgs.size()).toArray();
            return callNode.call(frame, internalMethod.getCallTarget(), RubyArguments.pack(
                    internalMethod,
                    internalMethod.getDeclarationFrame(),
                    Layouts.METHOD.getReceiver(method),
                    null,
                    args));
		}

        protected InternalMethod getMethod(DynamicObject method) {
            return Layouts.METHOD.getMethod(method);
        }

    }

    private static class InteropIsExecutable extends InteropNode {
        public InteropIsExecutable(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return RubyGuards.isRubyMethod(ForeignAccess.getReceiver(frame));
        }

    }

    private static class InteropIsBoxedPrimitive extends InteropNode {
        public InteropIsBoxedPrimitive(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return false;
        }

    }

    private static class InteropIsNull extends InteropNode {
        public InteropIsNull(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ForeignAccess.getReceiver(frame) == nil();
        }
    }

    private static class InteropHasSizePropertyFalse extends InteropNode {
        public InteropHasSizePropertyFalse(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return false;
        }
    }

    private static class InteropHasSizePropertyTrue extends InteropNode {
        public InteropHasSizePropertyTrue(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return true;
        }
    }

    private static class InteropGetSizeProperty extends InteropNode {

        @Child private DispatchHeadNode head;
        public InteropGetSizeProperty(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return head.dispatch(frame, ForeignAccess.getReceiver(frame), "size", null, new Object[] {});
        }
    }

    private static class InteropStringIsBoxed extends InteropNode {

        public InteropStringIsBoxed(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object o = ForeignAccess.getReceiver(frame);
            return RubyGuards.isRubyString(o) && Layouts.STRING.getByteList(((DynamicObject) o)).length() == 1;
        }
    }

    private static class InteropStringUnboxNode extends RubyNode {

        public InteropStringUnboxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return Layouts.STRING.getByteList(((DynamicObject) ForeignAccess.getReceiver(frame))).get(0);
        }
    }

    private static class UnresolvedInteropReadNode extends InteropNode {

        private final int labelIndex;

        public UnresolvedInteropReadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccess.getArguments(frame).get(labelIndex);
            if (label instanceof  String || RubyGuards.isRubySymbol(label) || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableReadNode(getContext(), getSourceSection(), name, labelIndex)).execute(frame);
                    }
                }
                DynamicObject receiver = (DynamicObject) ForeignAccess.getReceiver(frame);
                InternalMethod labelMethod = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), label.toString());
                InternalMethod indexedSetter = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), "[]=");
                if (labelMethod == null && indexedSetter != null) {
                    return this.replace(new ResolvedInteropIndexedReadNode(getContext(), getSourceSection(), labelIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropReadNode(getContext(), getSourceSection(), (String) label, labelIndex)).execute(frame);
                } else if (RubyGuards.isRubySymbol(label)) {
                    return this.replace(new ResolvedInteropReadFromSymbolNode(getContext(), getSourceSection(), (DynamicObject) label, labelIndex)).execute(frame);
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(label + " not allowed as name");
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(label + " not allowed as name");
            }
        }
    }

    private static class UnresolvedInteropStringReadNode extends InteropNode {

        private final int labelIndex;

        public UnresolvedInteropStringReadNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccess.getArguments(frame).get(labelIndex);
            if (label instanceof  String || RubyGuards.isRubySymbol(label) || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableReadNode(getContext(), getSourceSection(), name, labelIndex)).execute(frame);
                    }
                }
                if (label instanceof Integer || label instanceof  Long) {
                    return this.replace(new InteropReadStringByteNode(getContext(), getSourceSection(), labelIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropReadNode(getContext(), getSourceSection(), (String) label, labelIndex)).execute(frame);
                } else if (RubyGuards.isRubySymbol(label)) {
                    return this.replace(new ResolvedInteropReadFromSymbolNode(getContext(), getSourceSection(), (DynamicObject) label, labelIndex)).execute(frame);
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(label + " not allowed as name");
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(label + " not allowed as name");
            }
        }
    }


    static class InteropReadStringByteNode extends RubyNode {

        private final int labelIndex;

        public InteropReadStringByteNode(RubyContext context, SourceSection sourceSection, int labelIndex) {
            super(context, sourceSection);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (RubyGuards.isRubyString(ForeignAccess.getReceiver(frame))) {
                final DynamicObject string = (DynamicObject) ForeignAccess.getReceiver(frame);
                final int index = (int) ForeignAccess.getArguments(frame).get(labelIndex);
                if (index >= Layouts.STRING.getByteList(string).length()) {
                    return 0;
                } else {
                    return (byte) Layouts.STRING.getByteList(string).get(index);
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Not implemented");
            }
        }
    }

    private static class ResolvedInteropIndexedReadNode extends RubyNode {

        private final String name;
        @Child private DispatchHeadNode head;
        @Child private IndexLabelToRubyNode toRubyIndex;
        private final int indexIndex;

        public ResolvedInteropIndexedReadNode(RubyContext context, SourceSection sourceSection, int indexIndex) {
            super(context, sourceSection);
            this.name = "[]";
            this.indexIndex = indexIndex;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.toRubyIndex = IndexLabelToRubyNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object index = toRubyIndex.executeWithTarget(frame, ForeignAccess.getArguments(frame).get(indexIndex));
            return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[] {index});
        }
    }

    private static class InteropInstanceVariableReadNode extends InteropNode {

        @Child private ReadInstanceVariableNode read;
        private final String name;
        private final int labelIndex;

        public InteropInstanceVariableReadNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.read = new ReadInstanceVariableNode(context, sourceSection, name, new RubyInteropReceiverNode(context, sourceSection), false);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals((String) ForeignAccess.getArguments(frame).get(labelIndex))) {
                return read.execute(frame);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Not implemented");
            }
        }
    }

    private static class InteropInstanceVariableWriteNode extends RubyNode {

        @Child private WriteInstanceVariableNode write;
        private final String name;
        private final int labelIndex;

        public InteropInstanceVariableWriteNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.labelIndex = labelIndex;
            this.write = new WriteInstanceVariableNode(context, sourceSection, name, new RubyInteropReceiverNode(context, sourceSection), new RubyInteropArgumentNode(context, sourceSection, valueIndex), false);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals((String) ForeignAccess.getArguments(frame).get(labelIndex))) {
                return write.execute(frame);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Not implemented");
            }
        }
    }

    private static class RubyInteropReceiverNode extends RubyNode {
        public RubyInteropReceiverNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ForeignAccess.getReceiver(frame);
        }
    }

    private static class RubyInteropArgumentNode extends RubyNode {

        private final int index;

        public RubyInteropArgumentNode(RubyContext context, SourceSection sourceSection, int index) {
            super(context, sourceSection);
            this.index = index;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ForeignAccess.getArguments(frame).get(index);
        }
    }

    private static class ResolvedInteropReadNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final String name;
        private final int labelIndex;

        public ResolvedInteropReadNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[]{});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class ResolvedInteropReadFromSymbolNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final DynamicObject name;
        private final int labelIndex;

        public ResolvedInteropReadFromSymbolNode(RubyContext context, SourceSection sourceSection, DynamicObject name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[]{});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class UnresolvedInteropWriteNode extends InteropNode {

        private final int labelIndex;
        private final int valueIndex;

        public UnresolvedInteropWriteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.labelIndex = 0;
            this.valueIndex = 1;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccess.getArguments(frame).get(labelIndex);
            if (label instanceof  String || RubyGuards.isRubySymbol(label) || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableWriteNode(getContext(), getSourceSection(), name, labelIndex, valueIndex)).execute(frame);
                    }
                }
                DynamicObject receiver = (DynamicObject) ForeignAccess.getReceiver(frame);
                InternalMethod labelMethod = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), label.toString());
                InternalMethod indexedSetter = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), "[]=");
                if (labelMethod == null && indexedSetter != null) {
                    return this.replace(new ResolvedInteropIndexedWriteNode(getContext(), getSourceSection(), labelIndex, valueIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropWriteNode(getContext(), getSourceSection(), (String) label, labelIndex, valueIndex)).execute(frame);
                } else if (RubyGuards.isRubySymbol(label)) {
                    return this.replace(new ResolvedInteropWriteToSymbolNode(getContext(), getSourceSection(), (DynamicObject) label, labelIndex, valueIndex)).execute(frame);
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new IllegalStateException(label + " not allowed as name");
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(label + " not allowed as name");
            }
        }
    }

    private static class ResolvedInteropIndexedWriteNode extends RubyNode {

        private final String name;
        @Child private DispatchHeadNode head;
        @Child private IndexLabelToRubyNode toRubyIndex;
        private final int indexIndex;
        private final int valueIndex;

        public ResolvedInteropIndexedWriteNode(RubyContext context, SourceSection sourceSection, int indexIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = "[]=";
            this.indexIndex = indexIndex;
            this.valueIndex = valueIndex;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.toRubyIndex = IndexLabelToRubyNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object index = toRubyIndex.executeWithTarget(frame, ForeignAccess.getArguments(frame).get(indexIndex));
            Object value = ForeignAccess.getArguments(frame).get(valueIndex);
            return head.dispatch(frame, ForeignAccess.getReceiver(frame), name, null, new Object[] {index, value});
        }
    }

    private static class ResolvedInteropWriteNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final String name;
        private final String accessName;
        private final int labelIndex;
        private final int valueIndex;

        public ResolvedInteropWriteNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.accessName = name + "=";
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
            this.valueIndex = valueIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                Object value = ForeignAccess.getArguments(frame).get(valueIndex);
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), accessName, null, new Object[]{value});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class ResolvedInteropWriteToSymbolNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final DynamicObject name;
        private final DynamicObject  accessName;
        private final int labelIndex;
        private final int valueIndex;

        public ResolvedInteropWriteToSymbolNode(RubyContext context, SourceSection sourceSection, DynamicObject name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.accessName = context.getSymbol(Layouts.SYMBOL.getString(name) + "=");
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
            this.valueIndex = valueIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                Object value = ForeignAccess.getArguments(frame).get(valueIndex);
                return head.dispatch(frame, ForeignAccess.getReceiver(frame), accessName, null, new Object[]{value});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class UnresolvedInteropExecuteAfterReadNode extends InteropNode {

        private final int arity;
        private final int labelIndex;

        public UnresolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, int arity){
            super(context, sourceSection);
            this.arity = arity;
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (ForeignAccess.getArguments(frame).get(labelIndex) instanceof  String) {
                return this.replace(new ResolvedInteropExecuteAfterReadNode(getContext(), getSourceSection(), (String) ForeignAccess.getArguments(frame).get(labelIndex), arity)).execute(frame);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(ForeignAccess.getArguments(frame).get(0) + " not allowed as name");
            }
        }
    }

    private static class ResolvedInteropExecuteAfterReadNode extends InteropNode {

        @Child private DispatchHeadNode head;
        @Child private InteropArgumentsNode arguments;
        private final String name;
        private final int labelIndex;
        private final int receiverIndex;

        public ResolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, String name, int arity) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, DispatchAction.CALL_METHOD);
            this.arguments = new InteropArgumentsNode(context, sourceSection, arity); // [0] is label, [1] is the receiver
            this.labelIndex = 0;
            this.receiverIndex = 1;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccess.getArguments(frame).get(labelIndex))) {
                Object[] args = new Object[arguments.getCount(frame)];
                arguments.executeFillObjectArray(frame, args);
                return head.dispatch(frame, ForeignAccess.getArguments(frame).get(receiverIndex), ForeignAccess.getArguments(frame).get(labelIndex), null, args);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class InteropArgumentNode extends RubyNode {
        private final int index;

        public InteropArgumentNode(RubyContext context, SourceSection sourceSection, int index) {
            super(context, sourceSection);
            this.index = index;
        }

        public Object execute(VirtualFrame frame) {
            return ForeignAccess.getArguments(frame).get(index);
        }

    }

    private static class InteropArgumentsNode extends RubyNode {

        @Children private final InteropArgumentNode[] arguments;

        public InteropArgumentsNode(RubyContext context, SourceSection sourceSection, int arity) {
            super(context, sourceSection);
            this.arguments = new InteropArgumentNode[arity - 1]; // exclude the receiver
            // Execute(Read(receiver, label), a0 (which is the receiver), a1, a2)
            // the arguments array looks like:
            // label, a0 (which is the receiver), a1, a2, ...
            for (int i = 2; i < 2 + arity - 1; i++) {
                arguments[i - 2] = new InteropArgumentNode(context, sourceSection, i);
            }
        }

        public int getCount(VirtualFrame frame) {
            return arguments.length;
        }

        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException();
        }

        @ExplodeLoop
        public void executeFillObjectArray(VirtualFrame frame, Object[] args) {
            for (int i = 0; i < arguments.length; i++) {
                args[i] = arguments[i].execute(frame);
            }
        }
    }
}
