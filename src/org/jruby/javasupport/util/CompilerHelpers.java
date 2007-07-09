package org.jruby.javasupport.util;

import jregex.Pattern;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.evaluator.EvaluationState;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlock;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code. 
 *
 */
public class CompilerHelpers {
    private final static org.jruby.RegexpTranslator TRANS = new org.jruby.RegexpTranslator();

    public static CompiledBlock createBlock(ThreadContext context, IRubyObject self, int arity, 
            String[] staticScopeNames, CompiledBlockCallback callback) {
        StaticScope staticScope = 
            new BlockStaticScope(context.getCurrentScope().getStaticScope(), staticScopeNames);
        staticScope.determineModule();
        
        return new CompiledBlock(context, self, Arity.createArity(arity), 
                new DynamicScope(staticScope, context.getCurrentScope()), callback);
    }
    
    public static IRubyObject def(ThreadContext context, Visibility visibility, IRubyObject self, Class compiledClass, String name, String javaName, String[] scopeNames, int arity) {
        Ruby runtime = context.getRuntime();
        
        RubyModule containingClass = context.getRubyClass();
        
        if (containingClass == null) {
            throw runtime.newTypeError("No class to add method.");
        }
        
        if (containingClass == runtime.getObject() && name == "initialize") {
            runtime.getWarnings().warn("redefining Object#initialize may cause infinite loop");
        }
        
        StaticScope scope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), scopeNames);
        scope.determineModule();
        
        MethodFactory factory = MethodFactory.createFactory(compiledClass.getClassLoader());
        DynamicMethod method;
        
        if (name == "initialize" || visibility.isModuleFunction()) {
            method = factory.getCompiledMethod(containingClass, compiledClass, javaName, 
                    Arity.createArity(arity), Visibility.PRIVATE, scope);
        } else {
            method = factory.getCompiledMethod(containingClass, compiledClass, javaName, 
                    Arity.createArity(arity), visibility, scope);
        }
        
        containingClass.addMethod(name, method);
        
        if (visibility.isModuleFunction()) {
            containingClass.getSingletonClass().addMethod(name,
                    new WrapperMethod(containingClass.getSingletonClass(), method,
                    Visibility.PUBLIC));
            containingClass.callMethod(context, "singleton_method_added", runtime.newSymbol(name));
        }
        
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            ((MetaClass) containingClass).getAttachedObject().callMethod(
                    context, "singleton_method_added", runtime.newSymbol(name));
        } else {
            containingClass.callMethod(context, "method_added", runtime.newSymbol(name));
        }
        
        return runtime.getNil();
    }
    
    public static IRubyObject doAttrAssign(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, String name, IRubyObject caller, CallType callType, Block block) {
        if (receiver == caller) callType = CallType.VARIABLE;
        
        try {
            return receiver.compilerCallMethod(context, name, args, caller, callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static IRubyObject doAttrAssignIndexed(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, byte methodIndex, String name, IRubyObject caller, 
            CallType callType, Block block) {
        if (receiver == caller) callType = CallType.VARIABLE;
        
        try {
            return receiver.compilerCallMethodWithIndex(context, methodIndex, name, args, caller, 
                    callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static IRubyObject doInvokeDynamic(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, String name, IRubyObject caller, CallType callType, Block block) {
        try {
            return receiver.compilerCallMethod(context, name, args, caller, callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static IRubyObject doInvokeDynamicIndexed(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, byte methodIndex, String name, IRubyObject caller, 
            CallType callType, Block block) {
        try {
            return receiver.compilerCallMethodWithIndex(context, methodIndex, name, args, caller, 
                    callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }

    public static RubyArray ensureRubyArray(IRubyObject value) {
        if (!(value instanceof RubyArray)) {
            value = RubyArray.newArray(value.getRuntime(), value);
        }
        return (RubyArray) value;
    }
    
    public static IRubyObject fetchClassVariable(ThreadContext context, Ruby runtime, 
            IRubyObject self, String name) {
        RubyModule rubyClass = EvaluationState.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        return rubyClass.getClassVar(name);
    }
   
    // needs to be rewritten to support new jump exceptions
//    public static IRubyObject handleJumpException(JumpException je, Block block) {
//        // JRUBY-530, Kernel#loop case:
//        if (je.isBreakInKernelLoop()) {
//            // consume and rethrow or just keep rethrowing?
//            if (block == je.getTarget()) je.setBreakInKernelLoop(false);
//
//            throw je;
//        }
//
//        return (IRubyObject) je.getValue();
//    }
    
    public static IRubyObject nullToNil(IRubyObject value, Ruby runtime) {
        return value != null ? value : runtime.getNil();
    }
    
    public static RubyClass prepareSuperClass(Ruby runtime, IRubyObject rubyClass) {
        if (rubyClass != null) {
            if (!(rubyClass instanceof RubyClass)) {
                throw runtime.newTypeError("superclass must be a Class (" + 
                        RubyObject.trueFalseNil(rubyClass) + ") given");
            }
            return (RubyClass)rubyClass;
        }
        return (RubyClass)null;
    }
    
    public static RubyModule prepareClassNamespace(ThreadContext context, IRubyObject rubyModule) {
        if (rubyModule == null || rubyModule.isNil()) {
            rubyModule = context.getCurrentScope().getStaticScope().getModule();
            
            if (rubyModule == null) {
                throw context.getRuntime().newTypeError("no outer class/module");
            }
        }
        
        return (RubyModule)rubyModule;
    }
    
    public static int regexpLiteralFlags(int options) {
        return TRANS.flagsFor(options,0);
    }

    public static Pattern regexpLiteral(Ruby runtime, String ptr, int options) {
        IRubyObject noCaseGlobal = runtime.getGlobalVariables().get("$=");

        int extraOptions = noCaseGlobal.isTrue() ? ReOptions.RE_OPTION_IGNORECASE : 0;

        try {
            return TRANS.translate(ptr, options | extraOptions, 0);
        } catch(jregex.PatternSyntaxException e) {
            throw runtime.newRegexpError(e.getMessage());
        }
    }

    public static IRubyObject setClassVariable(ThreadContext context, Ruby runtime, 
            IRubyObject self, String name, IRubyObject value) {
        RubyModule rubyClass = EvaluationState.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        rubyClass.setClassVar(name, value);
   
        return value;
    }
    
    public static void raiseArgumentError(Ruby runtime, int given, int required, int opt, int rest) {
        if (opt == 0) {
            if (rest < 0) {
                // no opt, no rest, exact match
                if (given != required) {
                    throw runtime.newArgumentError("wrong # of arguments(" + given + " for " + required + ")");
                }
            } else {
                // only rest, must be at least required
                if (given < required) {
                    throw runtime.newArgumentError("wrong # of arguments(" + given + " for " + required + ")");
                }
            }
        } else {
            if (rest < 0) {
                // opt but no rest, must be at least required and no more than required + opt
                if (given < required) {
                    throw runtime.newArgumentError("wrong # of arguments(" + given + " for " + required + ")");
                } else if (given > (required + opt)) {
                    throw runtime.newArgumentError("wrong # of arguments(" + given + " for " + (required + opt) + ")");
                }
            } else {
                // opt and rest, must be at least required
                if (given < required) {
                    throw runtime.newArgumentError("wrong # of arguments(" + given + " for " + required + ")");
                }
            }
        }
    }
    
    public static void processBlockArgument(Ruby runtime, ThreadContext context, Block block, int index) {
        if (!block.isGiven()) return;
        
        RubyProc blockArg;
        
        if (block.getProcObject() != null) {
            blockArg = (RubyProc) block.getProcObject();
        } else {
            blockArg = runtime.newProc(false, block);
            blockArg.getBlock().isLambda = block.isLambda;
        }
        // We pass depth zero since we know this only applies to newly created local scope
        context.getCurrentScope().setValue(index, blockArg, 0);
    }
}
