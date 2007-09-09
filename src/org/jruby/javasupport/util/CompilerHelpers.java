package org.jruby.javasupport.util;

import org.jruby.regexp.RegexpFactory;
import org.jruby.regexp.RegexpPattern;
import org.jruby.regexp.PatternSyntaxException;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyMatchData;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.RubyRegexp;
import org.jruby.ast.NodeType;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.evaluator.EvaluationState;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.CallConfiguration;
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
import org.jruby.runtime.CompiledSharedScopeBlock;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code. 
 *
 */
public class CompilerHelpers {
    public static CompiledBlock createBlock(ThreadContext context, IRubyObject self, int arity, 
            String[] staticScopeNames, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argsNodeType) {
        StaticScope staticScope = 
            new BlockStaticScope(context.getCurrentScope().getStaticScope(), staticScopeNames);
        staticScope.determineModule();
        
        return new CompiledBlock(context, self, Arity.createArity(arity), 
                new DynamicScope(staticScope, context.getCurrentScope()), callback, hasMultipleArgsHead, argsNodeType);
    }
    
    public static CompiledSharedScopeBlock createSharedScopeBlock(ThreadContext context, IRubyObject self, int arity, 
            CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argsNodeType) {
        
        return new CompiledSharedScopeBlock(context, self, Arity.createArity(arity), 
                context.getCurrentScope(), callback, hasMultipleArgsHead, argsNodeType);
    }
    
    public static IRubyObject def(ThreadContext context, IRubyObject self, Object scriptObject, String name, String javaName, String[] scopeNames,
            int arity, CallConfiguration callConfig) {
        Class compiledClass = scriptObject.getClass();
        Ruby runtime = context.getRuntime();
        
        RubyModule containingClass = context.getRubyClass();
        Visibility visibility = context.getCurrentVisibility();
        
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
            method = factory.getCompiledMethod(containingClass, javaName, 
                    Arity.createArity(arity), Visibility.PRIVATE, scope, scriptObject);
        } else {
            method = factory.getCompiledMethod(containingClass, javaName, 
                    Arity.createArity(arity), visibility, scope, scriptObject);
        }
        
        method.setCallConfig(callConfig);
        
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
    
    public static IRubyObject defs(ThreadContext context, IRubyObject self, IRubyObject receiver, Object scriptObject, String name, String javaName, String[] scopeNames,
            int arity, CallConfiguration callConfig) {
        Class compiledClass = scriptObject.getClass();
        Ruby runtime = context.getRuntime();
        
        RubyClass rubyClass;
        if (receiver.isNil()) {
            rubyClass = runtime.getNilClass();
        } else if (receiver == runtime.getTrue()) {
            rubyClass = runtime.getClass("TrueClass");
        } else if (receiver == runtime.getFalse()) {
            rubyClass = runtime.getClass("FalseClass");
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure; can't define singleton method.");
            }
            if (receiver.isFrozen()) {
                throw runtime.newFrozenError("object");
            }
            if (receiver.getMetaClass() == runtime.getFixnum() || receiver.getMetaClass() == runtime.getClass("Symbol")) {
                throw runtime.newTypeError("can't define singleton method \"" + name
                                           + "\" for " + receiver.getType());
            }
   
            rubyClass = receiver.getSingletonClass();
        }
   
        if (runtime.getSafeLevel() >= 4) {
            Object method = rubyClass.getMethods().get(name);
            if (method != null) {
                throw runtime.newSecurityError("Redefining method prohibited.");
            }
        }
        
        StaticScope scope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), scopeNames);
        scope.determineModule();
        
        MethodFactory factory = MethodFactory.createFactory(compiledClass.getClassLoader());
        DynamicMethod method;
        
        method = factory.getCompiledMethod(rubyClass, javaName, 
                Arity.createArity(arity), Visibility.PUBLIC, scope, scriptObject);
        
        method.setCallConfig(callConfig);
        
        rubyClass.addMethod(name, method);
        receiver.callMethod(context, "singleton_method_added", runtime.newSymbol(name));
        
        return runtime.getNil();
    }
    
    public static RubyClass getSingletonClass(Ruby runtime, IRubyObject receiver) {
        RubyClass singletonClass;

        if (receiver.isNil()) {
            singletonClass = runtime.getNilClass();
        } else if (receiver == runtime.getTrue()) {
            singletonClass = runtime.getClass("TrueClass");
        } else if (receiver == runtime.getFalse()) {
            singletonClass = runtime.getClass("FalseClass");
        } else if (receiver.getMetaClass() == runtime.getFixnum() || receiver.getMetaClass() == runtime.getClass("Symbol")) {
            throw runtime.newTypeError("no virtual class for " + receiver.getMetaClass().getBaseName());
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
            }

            singletonClass = receiver.getSingletonClass();
        }
        
        return singletonClass;
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

    public static RubyArray ensureMultipleAssignableRubyArray(Ruby runtime, IRubyObject value, boolean masgnHasHead) {
        if (!(value instanceof RubyArray)) {
            value = ArgsUtil.convertToRubyArray(runtime, value, masgnHasHead);
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
        if (!(rubyClass instanceof RubyClass)) {
            throw runtime.newTypeError("superclass must be a Class (" + 
                    RubyObject.trueFalseNil(rubyClass) + ") given");
        }
        return (RubyClass)rubyClass;
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
    
    public static RegexpPattern regexpLiteral(Ruby runtime, String ptr, int options) {
        IRubyObject noCaseGlobal = runtime.getGlobalVariables().get("$=");

        int extraOptions = noCaseGlobal.isTrue() ? ReOptions.RE_OPTION_IGNORECASE : 0;

        try {
            if((options & 256) == 256 ) {
                return RegexpFactory.getFactory("java").createPattern(ByteList.create(ptr), (options & ~256) | extraOptions, 0);
            } else {
                return runtime.getRegexpFactory().createPattern(ByteList.create(ptr), options | extraOptions, 0);
            }
        } catch(PatternSyntaxException e) {
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
    
    public static String getLocalJumpTypeOrRethrow(RaiseException re) {
        RubyException exception = re.getException();
        Ruby runtime = exception.getRuntime();
        if (exception.isKindOf(runtime.getClass("LocalJumpError"))) {
            RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();

            IRubyObject reason = jumpError.reason();

            return reason.asSymbol();
        }

        throw re;
    }
    
    public static void processBlockArgument(Ruby runtime, ThreadContext context, Block block, int index) {
        if (!block.isGiven()) {
            context.getCurrentScope().setValue(index, runtime.getNil(), 0);
            return;
        }
        
        RubyProc blockArg;
        
        if (block.getProcObject() != null) {
            blockArg = block.getProcObject();
        } else {
            blockArg = runtime.newProc(false, block);
            blockArg.getBlock().isLambda = block.isLambda;
        }
        // We pass depth zero since we know this only applies to newly created local scope
        context.getCurrentScope().setValue(index, blockArg, 0);
    }
        
    public static void processRestArg(Ruby runtime, IRubyObject[] scope, int restArg, IRubyObject[] args, int start) {
        if (args.length <= start) {
            scope[restArg] = RubyArray.newArray(runtime, 0);
        } else {
            scope[restArg] = RubyArray.newArrayNoCopy(runtime, args, start);
        }
    }
    
    public static IRubyObject[] createObjectArray(IRubyObject arg1) {
        return new IRubyObject[] {arg1};
    }
    
    public static IRubyObject[] createObjectArray(IRubyObject arg1, IRubyObject arg2) {
        return new IRubyObject[] {arg1, arg2};
    }
    
    public static IRubyObject[] createObjectArray(IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
        return new IRubyObject[] {arg1, arg2, arg3};
    }
    
    public static IRubyObject[] createObjectArray(IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4) {
        return new IRubyObject[] {arg1, arg2, arg3, arg4};
    }
    
    public static IRubyObject[] createObjectArray(IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject arg4, IRubyObject arg5) {
        return new IRubyObject[] {arg1, arg2, arg3, arg4, arg5};
    }
    
    public static Block getBlockFromBlockPassBody(IRubyObject proc, Block currentBlock) {
        Ruby runtime = proc.getRuntime();

        // No block from a nil proc
        if (proc.isNil()) return Block.NULL_BLOCK;

        // If not already a proc then we should try and make it one.
        if (!(proc instanceof RubyProc)) {
            proc = proc.convertToType(runtime.getClass("Proc"), 0, "to_proc", false);

            if (!(proc instanceof RubyProc)) {
                throw runtime.newTypeError("wrong argument type "
                        + proc.getMetaClass().getName() + " (expected Proc)");
            }
        }

        // TODO: Add safety check for taintedness
        if (currentBlock != null && currentBlock.isGiven()) {
            RubyProc procObject = currentBlock.getProcObject();
            // The current block is already associated with proc.  No need to create a new one
            if (procObject != null && procObject == proc) return currentBlock;
        }

        return ((RubyProc) proc).getBlock();
    }
    
    public static IRubyObject backref(ThreadContext context) {
        IRubyObject backref = context.getCurrentFrame().getBackRef();
        
        if(backref instanceof RubyMatchData) {
            ((RubyMatchData)backref).use();
        }
        return backref;
    }
    
    public static IRubyObject backrefLastMatch(ThreadContext context) {
        IRubyObject backref = context.getCurrentFrame().getBackRef();
        
        return RubyRegexp.last_match(backref);
    }
    
    public static IRubyObject backrefMatchPre(ThreadContext context) {
        IRubyObject backref = context.getCurrentFrame().getBackRef();
        
        return RubyRegexp.match_pre(backref);
    }
    
    public static IRubyObject backrefMatchPost(ThreadContext context) {
        IRubyObject backref = context.getCurrentFrame().getBackRef();
        
        return RubyRegexp.match_post(backref);
    }
    
    public static IRubyObject backrefMatchLast(ThreadContext context) {
        IRubyObject backref = context.getCurrentFrame().getBackRef();
        
        return RubyRegexp.match_last(backref);
    }
    
    public static IRubyObject callZSuper(Ruby runtime, ThreadContext context, Block block, IRubyObject self) {
        if (context.getFrameKlazz() == null) {
            String name = context.getFrameName();
            throw runtime.newNameError("superclass method '" + name
                    + "' disabled", name);
        }
        
        // Has the method that is calling super received a block argument
        if (!block.isGiven()) block = context.getCurrentFrame().getBlock(); 
        
        context.getCurrentScope().getArgValues(context.getFrameArgs(),context.getCurrentFrame().getRequiredArgCount());
        return self.callSuper(context, context.getFrameArgs(), block);
    }
    
    public static IRubyObject[] appendToObjectArray(IRubyObject[] array, IRubyObject add) {
        IRubyObject[] newArray = new IRubyObject[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = add;
        return newArray;
    }
}
