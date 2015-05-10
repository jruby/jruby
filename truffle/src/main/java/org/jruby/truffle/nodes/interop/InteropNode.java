/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.interop;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.nodes.interop.InteropNodeFactory.ExecuteMethodNodeGen;
import org.jruby.truffle.nodes.objects.ReadInstanceVariableNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyMethod;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.core.RubySymbol;
import org.jruby.truffle.runtime.methods.InternalMethod;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.interop.ForeignAccessArguments;
import com.oracle.truffle.interop.messages.Execute;
import com.oracle.truffle.interop.messages.Read;
import com.oracle.truffle.interop.messages.Write;



public abstract class InteropNode extends RubyNode {
    public InteropNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public static InteropNode createRead(RubyContext context, SourceSection sourceSection, Read read) {
        return new UnresolvedInteropReadNode(context, sourceSection, read);
    }

    public static InteropNode createWrite(RubyContext context, SourceSection sourceSection, Write write) {
        return new UnresolvedInteropWriteNode(context, sourceSection, write);
    }

    public static InteropNode createExecuteAfterRead(RubyContext context, SourceSection sourceSection, Execute execute) {
        return new UnresolvedInteropExecuteAfterReadNode(context, sourceSection, execute);
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

    public static RubyNode createStringRead(RubyContext context, final SourceSection sourceSection, Read read) {
        return new UnresolvedInteropStringReadNode(context, sourceSection, read);
    }

    public static RubyNode createStringUnbox(RubyContext context, final SourceSection sourceSection) {
        return new InteropStringUnboxNode(context, sourceSection);
    }
    
    private static class InteropExecute extends InteropNode {
        @Child private ExecuteMethodNode execute;
    	
    	public InteropExecute(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            this.execute = ExecuteMethodNodeGen.create(context, sourceSection, null);
        }


        
        @Override
        public Object execute(VirtualFrame frame) {
        	Object result = execute.executeWithTarget(frame, ForeignAccessArguments.getReceiver(frame.getArguments()));
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

    	
		@Specialization(guards = {"method == cachedMethod"})
    	protected Object doCall(VirtualFrame frame, RubyMethod method, @Cached("method") RubyMethod cachedMethod, @Cached("cachedMethod.getMethod()") InternalMethod internalMethod,  @Cached("create(cachedMethod.getMethod().getCallTarget())") DirectCallNode callNode) {
    		// skip first argument; it's the receiver but a RubyMethod knows its receiver
			Object[] args = ForeignAccessArguments.extractUserArguments(1, frame.getArguments());
			return callNode.call(frame, RubyArguments.pack(internalMethod, internalMethod.getDeclarationFrame(), cachedMethod.getReceiver(), null, args));
    	}
		
		@Specialization
    	protected Object doCall(VirtualFrame frame, RubyMethod method) {
			final InternalMethod internalMethod = method.getMethod();
			// skip first argument; it's the receiver but a RubyMethod knows its receiver
			Object[] args = ForeignAccessArguments.extractUserArguments(1, frame.getArguments());
            return callNode.call(frame, method.getMethod().getCallTarget(), RubyArguments.pack(
                    internalMethod,
                    internalMethod.getDeclarationFrame(),
                    method.getReceiver(),
                    null,
                    args));
		}
    }


    private static class InteropIsExecutable extends InteropNode {
        public InteropIsExecutable(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ForeignAccessArguments.getReceiver(frame.getArguments()) instanceof RubyMethod;
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
            return ForeignAccessArguments.getReceiver(frame.getArguments()) == nil();
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
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), "size", null, new Object[] {});
        }
    }

    private static class InteropStringIsBoxed extends InteropNode {

        public InteropStringIsBoxed(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object o = ForeignAccessArguments.getReceiver(frame.getArguments());
            return o instanceof RubyString && ((RubyString) o).getByteList().length() == 1;
        }
    }


    private static class InteropStringUnboxNode extends RubyNode {

        public InteropStringUnboxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return ((RubyString) ForeignAccessArguments.getReceiver(frame.getArguments())).getByteList().get(0);
        }
    }


    private static class UnresolvedInteropReadNode extends InteropNode {

        private final int labelIndex;

        public UnresolvedInteropReadNode(RubyContext context, SourceSection sourceSection, Read read) {
            super(context, sourceSection);
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex);
            if (label instanceof  String || label instanceof  RubySymbol || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableReadNode(getContext(), getSourceSection(), name, labelIndex)).execute(frame);
                    }
                }
                RubyBasicObject receiver = (RubyBasicObject) ForeignAccessArguments.getReceiver(frame.getArguments());
                InternalMethod labelMethod = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), label.toString());
                InternalMethod indexedSetter = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), "[]=");
                if (labelMethod == null && indexedSetter != null) {
                    return this.replace(new ResolvedInteropIndexedReadNode(getContext(), getSourceSection(), labelIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropReadNode(getContext(), getSourceSection(), (String) label, labelIndex)).execute(frame);
                } else if (label instanceof  RubySymbol) {
                    return this.replace(new ResolvedInteropReadFromSymbolNode(getContext(), getSourceSection(), (RubySymbol) label, labelIndex)).execute(frame);
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

        public UnresolvedInteropStringReadNode(RubyContext context, SourceSection sourceSection, Read read) {
            super(context, sourceSection);
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex);
            if (label instanceof  String || label instanceof  RubySymbol || label instanceof Integer) {
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
                } else if (label instanceof  RubySymbol) {
                    return this.replace(new ResolvedInteropReadFromSymbolNode(getContext(), getSourceSection(), (RubySymbol) label, labelIndex)).execute(frame);
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
            if (ForeignAccessArguments.getReceiver(frame.getArguments()) instanceof RubyString) {
                final RubyString string = (RubyString) ForeignAccessArguments.getReceiver(frame.getArguments());
                final int index = (int) ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex);
                if (index >= string.getByteList().length()) {
                    return 0;
                } else {
                    return (byte) string.getByteList().get(index);
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
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.toRubyIndex = IndexLabelToRubyNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object index = toRubyIndex.executeWithTarget(frame, ForeignAccessArguments.getArgument(frame.getArguments(), indexIndex));
            return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), name, null, new Object[] {index});
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
            if (name.equals((String) ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
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
            if (name.equals((String) ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
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
            return ForeignAccessArguments.getReceiver(frame.getArguments());
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
            return ForeignAccessArguments.getArgument(frame.getArguments(), index);
        }
    }

    private static class ResolvedInteropReadNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final String name;
        private final int labelIndex;

        public ResolvedInteropReadNode(RubyContext context, SourceSection sourceSection, String name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
                return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), name, null, new Object[]{});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class ResolvedInteropReadFromSymbolNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final RubySymbol name;
        private final int labelIndex;

        public ResolvedInteropReadFromSymbolNode(RubyContext context, SourceSection sourceSection, RubySymbol name, int labelIndex) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
                return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), name, null, new Object[]{});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class UnresolvedInteropWriteNode extends InteropNode {

        private final int labelIndex;
        private final int valueIndex;

        public UnresolvedInteropWriteNode(RubyContext context, SourceSection sourceSection, Write write) {
            super(context, sourceSection);
            this.labelIndex = 0;
            this.valueIndex = 1;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object label = ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex);
            if (label instanceof  String || label instanceof  RubySymbol || label instanceof Integer) {
                if (label instanceof  String) {
                    String name = (String) label;
                    if (name.startsWith("@")) {
                        return this.replace(new InteropInstanceVariableWriteNode(getContext(), getSourceSection(), name, labelIndex, valueIndex)).execute(frame);
                    }
                }
                RubyBasicObject receiver = (RubyBasicObject) ForeignAccessArguments.getReceiver(frame.getArguments());
                InternalMethod labelMethod = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), label.toString());
                InternalMethod indexedSetter = ModuleOperations.lookupMethod(getContext().getCoreLibrary().getMetaClass(receiver), "[]=");
                if (labelMethod == null && indexedSetter != null) {
                    return this.replace(new ResolvedInteropIndexedWriteNode(getContext(), getSourceSection(), labelIndex, valueIndex)).execute(frame);
                } else if (label instanceof  String) {
                    return this.replace(new ResolvedInteropWriteNode(getContext(), getSourceSection(), (String) label, labelIndex, valueIndex)).execute(frame);
                } else if (label instanceof  RubySymbol) {
                    return this.replace(new ResolvedInteropWriteToSymbolNode(getContext(), getSourceSection(), (RubySymbol) label, labelIndex, valueIndex)).execute(frame);
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
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.toRubyIndex = IndexLabelToRubyNodeGen.create(context, sourceSection, null);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object index = toRubyIndex.executeWithTarget(frame, ForeignAccessArguments.getArgument(frame.getArguments(), indexIndex));
            Object value = ForeignAccessArguments.getArgument(frame.getArguments(), valueIndex);
            return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), name, null, new Object[] {index, value});
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
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
            this.valueIndex = valueIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
                Object value = ForeignAccessArguments.getArgument(frame.getArguments(), valueIndex);
                return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), accessName, null, new Object[]{value});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }


    private static class ResolvedInteropWriteToSymbolNode extends InteropNode {

        @Child private DispatchHeadNode head;
        private final RubySymbol name;
        private final RubySymbol accessName;
        private final int labelIndex;
        private final int valueIndex;

        public ResolvedInteropWriteToSymbolNode(RubyContext context, SourceSection sourceSection, RubySymbol name, int labelIndex, int valueIndex) {
            super(context, sourceSection);
            this.name = name;
            this.accessName = context.getSymbol(name.toString() + "=");
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.labelIndex = labelIndex;
            this.valueIndex = valueIndex;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
                Object value = ForeignAccessArguments.getArgument(frame.getArguments(), valueIndex);
                return head.dispatch(frame, ForeignAccessArguments.getReceiver(frame.getArguments()), accessName, null, new Object[]{value});
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException("Name changed");
            }
        }
    }

    private static class UnresolvedInteropExecuteAfterReadNode extends InteropNode {

        private final Execute execute;
        private final int labelIndex;

        public UnresolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, Execute execute) {
            super(context, sourceSection);
            this.execute = execute;
            this.labelIndex = 0;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex) instanceof  String) {
                return this.replace(new ResolvedInteropExecuteAfterReadNode(getContext(), getSourceSection(), (String) ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex), execute)).execute(frame);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new IllegalStateException(ForeignAccessArguments.getArgument(frame.getArguments(), 0) + " not allowed as name");
            }
        }
    }

    private static class ResolvedInteropExecuteAfterReadNode extends InteropNode {

        @Child private DispatchHeadNode head;
        @Child private InteropArgumentsNode arguments;
        private final String name;
        private final int labelIndex;
        private final int receiverIndex;

        public ResolvedInteropExecuteAfterReadNode(RubyContext context, SourceSection sourceSection, String name, Execute message) {
            super(context, sourceSection);
            this.name = name;
            this.head = new DispatchHeadNode(context, true, false, MissingBehavior.CALL_METHOD_MISSING, null, DispatchAction.CALL_METHOD);
            this.arguments = new InteropArgumentsNode(context, sourceSection, message); // [0] is label, [1] is the receiver
            this.labelIndex = 0;
            this.receiverIndex = 1;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (name.equals(ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex))) {
                Object[] args = new Object[arguments.getCount(frame)];
                arguments.executeFillObjectArray(frame, args);
                return head.dispatch(frame, ForeignAccessArguments.getArgument(frame.getArguments(), receiverIndex), ForeignAccessArguments.getArgument(frame.getArguments(), labelIndex), null, args);
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
            return ForeignAccessArguments.extractUserArguments(frame.getArguments())[index];
        }

    }

    private static class InteropArgumentsNode extends RubyNode {

        @Children private final InteropArgumentNode[] arguments;

        public InteropArgumentsNode(RubyContext context, SourceSection sourceSection, Execute message) {
            super(context, sourceSection);
            this.arguments = new InteropArgumentNode[message.getArity() - 1]; // exclude the receiver
            // Execute(Read(receiver, label), a0 (which is the receiver), a1, a2)
            // the arguments array looks like:
            // label, a0 (which is the receiver), a1, a2, ...
            for (int i = 2; i < 2 + message.getArity() - 1; i++) {
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
