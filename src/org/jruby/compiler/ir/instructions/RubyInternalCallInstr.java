package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.interpreter.InterpreterContext;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.javasupport.util.RuntimeHelpers;

// Rather than building a zillion instructions that capture calls to ruby implementation internals,
// we are building one that will serve as a placeholder for internals-specific call optimizations.
public class RubyInternalCallInstr extends CallInstr {
    public enum RubyInternalsMethod {
       // SSS FIXME: I dont fully understand this method (in the context of multiple assignment).
       // It calls regular to_ary on the object.  But, how does it succeed if it encounters a method_missing?
       // Ex: http://gist.github.com/163551
       TO_ARY("to_ary"),
       DEFINE_ALIAS("defineAlias"),
       GVAR_ALIAS("aliasGlobalVariable"),
       SUPER("super"),
       ZSUPER("super"),
       FOR_EACH("each"),
       //CHECK_ARITY("checkArity"),
       UNDEF_METHOD("undefMethod");

       public MethAddr methAddr;
       RubyInternalsMethod(String methodName) {
           this.methAddr = new MethAddr(methodName);
       }

       public MethAddr getMethAddr() { 
           return this.methAddr; 
       }
    }

    public RubyInternalsMethod implMethod;

    public RubyInternalCallInstr(Variable result, RubyInternalsMethod m, Operand receiver, Operand[] args) {
        super(Operation.RUBY_INTERNALS, result, m.getMethAddr(), receiver, args, null);
        this.implMethod = m;
    }

    public RubyInternalCallInstr(Variable result, RubyInternalsMethod m, Operand receiver, Operand[] args, Operand closure) {
        super(result, m.getMethAddr(), receiver, args, closure);
        this.implMethod = m;
    }

    @Override
    public boolean isRubyInternalsCall() {
        return true;
    }

    @Override
    public boolean isStaticCallTarget() {
        return true;
    }

/***
    // SSS FIXME: Dont optimize these yet!
    @Override
    public IRMethod getTargetMethodWithReceiver(Operand receiver) {
        return null;
    }
***/

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new RubyInternalCallInstr(ii.getRenamedVariable(result),
                this.implMethod, getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), closure == null ? null : closure.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object   receiver;
        Ruby     runtime = context.getRuntime();
        Object   rVal = null;
        switch (this.implMethod) {
            case DEFINE_ALIAS:
            {
                Operand[] args = getCallArgs(); // Guaranteed 2 args by parser
                IRubyObject object = (IRubyObject)getReceiver().retrieve(interp, context, self);
                
                if (object == null || object instanceof RubyFixnum || object instanceof RubySymbol){
                    throw runtime.newTypeError("no class to make alias");
                }

                String newName = args[0].retrieve(interp, context, self).toString();
                String oldName = args[1].retrieve(interp, context, self).toString();

                // FIXME: should be classFor instruction in temp var, but it broke a few cases
                RubyModule module;
                if (object instanceof RubyModule) {
                    module = (RubyModule) object;
                } else {
                    module = object.getMetaClass();
                }

                module.defineAlias(newName, oldName);
                break;
            }
            case GVAR_ALIAS:
                throw new RuntimeException("GVAR_ALIAS: Not implemented yet!");
            case SUPER:
            case ZSUPER:
            {
                IRubyObject   object = (IRubyObject)getReceiver().retrieve(interp, context, self);
                IRubyObject[] args = prepareArguments(interp, context, self, getCallArgs());
                // SSS FIXME: Do we need to set up the frame in some way so this doesn't bomb?
                rVal = RuntimeHelpers.invokeSuper(context, object, args, prepareBlock(interp, context, self));
                break;
            }
            case UNDEF_METHOD:
                rVal = RuntimeHelpers.undefMethod(context, getReceiver().retrieve(interp, context, self));
                break;
            case TO_ARY:
                receiver = getReceiver().retrieve(interp, context, self);
                Operand[] args = getCallArgs();
                // Don't call to_ary if we we have an array already and we are asked not to run to_ary on arrays
                if ((args.length > 0) && ((BooleanLiteral)args[0]).isFalse() && (receiver instanceof RubyArray))
                    rVal = receiver;
                else
                    rVal = RuntimeHelpers.aryToAry((IRubyObject) receiver);
                break;
            case FOR_EACH:
                super.interpret(interp, context, self); // SSS FIXME: Correct?
                break;
            default:
                super.interpret(interp, context, self);
        }

        // Store the result
        if (rVal != null) getResult().store(interp, context, self, rVal);

        return null;
    }

    // SSS FIXME: Copied from runtime/callsite/SuperCallSite!  Probably worth putting in a library
    protected static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule frameClass, String frameName) {
        if (frameClass == null) {
            if (frameName != null) {
                throw context.getRuntime().newNameError("superclass method '" + frameName + "' disabled", frameName);
            } else {
                throw context.getRuntime().newNoMethodError("super called outside of method", null, context.getRuntime().getNil());
            }
        }
    }
}
