package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.CFG;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.interpreter.Jump;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class InterpretedIRMethod extends DynamicMethod {
    private IRMethod method;
    private final int temporaryVariableSize;

    // We can probably use IRMethod callArgs for something (at least arity)
    public InterpretedIRMethod(IRMethod method, RubyModule implementationClass) {
        super(implementationClass, Visibility.PRIVATE, CallConfiguration.FrameNoneScopeNone);

        method.allocateStaticScope(null);

        this.temporaryVariableSize = method.getTemporaryVariableSize();
        this.method = method;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        InterpreterContext interp = new IRInterpreterContext(context, self, args, method.getStaticScope());

        System.out.println("ARGS IN IRMETHOD: " + java.util.Arrays.toString(args));
        try {
            CFG cfg = method.getCFG();
            BasicBlock basicBlock = cfg.getEntryBB();
            BasicBlock jumpBlock = basicBlock;


            while (basicBlock != null) {
//            System.out.println("BB:" + basicBlock);
                for (Instr instruction : basicBlock.getInstrs()) {
                    try {
                        System.out.println("EXEC'ing: " + instruction);
                        instruction.interpret(interp, self);
                    } catch (Jump jump) {
                        jumpBlock = cfg.getTargetBB(jump.getTarget());
                        break;
                    }
                }

                if (jumpBlock != basicBlock) {
                    basicBlock = jumpBlock;
                } else {
                    basicBlock = cfg.getFallThroughBB(basicBlock);
                    jumpBlock = basicBlock;
                }

            }

            return (IRubyObject) interp.getReturnValue();
        } finally {
            if (interp.getFrame() != null) {
                context.popFrame();
                interp.setFrame(null);
            }
            context.postMethodScopeOnly();
        }
    }

    @Override
    public DynamicMethod dup() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public class IRInterpreterContext implements InterpreterContext {
        private final ThreadContext context;
        protected Object returnValue;
        protected Object self;
        protected IRubyObject[] parameters;
        protected Object[] temporaryVariables;
        protected DynamicScope localVariables;
        protected Frame frame;

        public IRInterpreterContext(ThreadContext context, IRubyObject self, IRubyObject[] parameters, StaticScope staticScope) {
            context.preMethodScopeOnly(self.getMetaClass(), staticScope);

            this.context = context;
            this.self = self;
            this.parameters = parameters;
//            System.out.println("TVS: " + temporaryVariableSize);
            this.temporaryVariables = new Object[temporaryVariableSize];
            this.localVariables = context.getCurrentScope();
        }

        public Object getReturnValue() {
            // FIXME: Maybe returnValue is a sure thing and we don't need this check.  Should be this way.
            return returnValue == null ? context.getRuntime().getNil() : returnValue;
        }

        public void setReturnValue(Object returnValue) {
            this.returnValue = returnValue;
        }

        public Object getTemporaryVariable(int offset) {
            return temporaryVariables[offset];
        }

        public Object setTemporaryVariable(int offset, Object value) {
            Object oldValue = temporaryVariables[offset];

            temporaryVariables[offset] = value;

            return oldValue;
        }

        public Object getLocalVariable(int location) {
            int depth = location >> 16;
            int offset = location & 0xffff;

            return localVariables.getValue(offset, depth);
        }

        public ThreadContext getContext() {
            return context;
        }

        public Object getParameter(int offset) {
            return parameters[offset - 1];
        }

        public Object setLocalVariable(int location, Object value) {
            int depth = location >> 16;
            int offset = location & 0xffff;
            
            localVariables.setValue((IRubyObject) value, offset, depth);

            return value;
        }

        public Object getSelf() {
            return self;
        }

        public Frame getFrame() {
            return frame;
        }

        public void setFrame(Frame frame) {
            this.frame = frame;
        }
    }
}
