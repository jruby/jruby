package org.jruby.javasupport.util;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyLocalJumpError;
import org.jruby.RubyMatchData;
import org.jruby.RubyModule;
import org.jruby.RubyProc;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.ReOptions;
import org.jruby.parser.StaticScope;
import org.jruby.regexp.PatternSyntaxException;
import org.jruby.regexp.RegexpFactory;
import org.jruby.regexp.RegexpPattern;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.CompiledBlock;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.CompiledSharedScopeBlock;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.MethodFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code. 
 *
 */
public class RuntimeHelpers {
    public static CompiledBlock createBlock(ThreadContext context, IRubyObject self, int arity, 
            String[] staticScopeNames, CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argsNodeType, boolean light) {
        StaticScope staticScope = 
            new BlockStaticScope(context.getCurrentScope().getStaticScope(), staticScopeNames);
        staticScope.determineModule();
        
        CompiledBlock block = new CompiledBlock(
                    context,
                    self,
                    Arity.createArity(arity),
                    new DynamicScope(staticScope, context.getCurrentScope()),
                    callback,
                    hasMultipleArgsHead,
                    argsNodeType);
        block.setLight(light);
        
        return block;
    }
    
    public static IRubyObject runBeginBlock(ThreadContext context, IRubyObject self, String[] staticScopeNames, CompiledBlockCallback callback) {
        StaticScope staticScope = 
            new BlockStaticScope(context.getCurrentScope().getStaticScope(), staticScopeNames);
        staticScope.determineModule();
        
        context.preScopedBody(new DynamicScope(staticScope, context.getCurrentScope()));
        
        Block block = new CompiledBlock(context, self, Arity.createArity(0), 
                context.getCurrentScope(), callback, false, Block.ZERO_ARGS);
        
        block.yield(context, null);
        
        context.postScopedBody();
        
        return context.getRuntime().getNil();
    }
    
    public static CompiledSharedScopeBlock createSharedScopeBlock(ThreadContext context, IRubyObject self, int arity, 
            CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argsNodeType) {
        
        return new CompiledSharedScopeBlock(context, self, Arity.createArity(arity), 
                context.getCurrentScope(), callback, hasMultipleArgsHead, argsNodeType);
    }
    
    public static IRubyObject def(ThreadContext context, IRubyObject self, Object scriptObject, String name, String javaName, String[] scopeNames,
            int arity, int required, int optional, int rest, CallConfiguration callConfig) {
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

        if (name == "__id__" || name == "__send__") {
            runtime.getWarnings().warn("redefining `" + name + "' may cause serious problem"); 
        }

        StaticScope scope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), scopeNames);
        scope.determineModule();
        scope.setArities(required, optional, rest);
        
        MethodFactory factory = MethodFactory.createFactory(compiledClass.getClassLoader());
        DynamicMethod method;
        
        if (name == "initialize" || visibility == Visibility.MODULE_FUNCTION) {
            method = factory.getCompiledMethod(containingClass, javaName, 
                    Arity.createArity(arity), Visibility.PRIVATE, scope, scriptObject);
        } else {
            method = factory.getCompiledMethod(containingClass, javaName, 
                    Arity.createArity(arity), visibility, scope, scriptObject);
        }
        
        method.setCallConfig(callConfig);
        
        containingClass.addMethod(name, method);
        
        if (visibility == Visibility.MODULE_FUNCTION) {
            containingClass.getSingletonClass().addMethod(name,
                    new WrapperMethod(containingClass.getSingletonClass(), method,
                    Visibility.PUBLIC));
            containingClass.callMethod(context, "singleton_method_added", runtime.newSymbol(name));
        }
        
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            ((MetaClass) containingClass).getAttached().callMethod(
                    context, "singleton_method_added", runtime.newSymbol(name));
        } else {
            containingClass.callMethod(context, "method_added", runtime.newSymbol(name));
        }
        
        return runtime.getNil();
    }
    
    public static IRubyObject defs(ThreadContext context, IRubyObject self, IRubyObject receiver, Object scriptObject, String name, String javaName, String[] scopeNames,
            int arity, int required, int optional, int rest, CallConfiguration callConfig) {
        Class compiledClass = scriptObject.getClass();
        Ruby runtime = context.getRuntime();
        
        if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
            throw runtime.newSecurityError("Insecure; can't define singleton method.");
        }

        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
          throw runtime.newTypeError("can't define singleton method \"" + name
          + "\" for " + receiver.getMetaClass().getBaseName());
        }

        if (receiver.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass rubyClass = receiver.getSingletonClass();

        if (runtime.getSafeLevel() >= 4 && rubyClass.getMethods().get(name) != null) {
            throw runtime.newSecurityError("redefining method prohibited.");
        }
        
        StaticScope scope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), scopeNames);
        scope.determineModule();
        scope.setArities(required, optional, rest);
        
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
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw runtime.newTypeError("no virtual class for " + receiver.getMetaClass().getBaseName());
        } else {
            if (runtime.getSafeLevel() >= 4 && !receiver.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
            }

            return receiver.getSingletonClass();
        }
    }

    public static IRubyObject doAttrAssign(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, String name, IRubyObject caller, CallType callType, Block block) {
        if (receiver == caller) callType = CallType.VARIABLE;
        
        try {
            return compilerCallMethod(context, receiver, name, args, caller, callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static IRubyObject doAttrAssignIndexed(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, byte methodIndex, String name, IRubyObject caller, 
            CallType callType, Block block) {
        if (receiver == caller) callType = CallType.VARIABLE;
        
        try {
            return compilerCallMethodWithIndex(context, receiver, methodIndex, name, args, caller, 
                    callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static IRubyObject doInvokeDynamic(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, String name, IRubyObject caller, CallType callType, Block block) {
        try {
            return compilerCallMethod(context, receiver, name, args, caller, callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static IRubyObject doInvokeDynamicIndexed(IRubyObject receiver, IRubyObject[] args, 
            ThreadContext context, byte methodIndex, String name, IRubyObject caller, 
            CallType callType, Block block) {
        try {
            return compilerCallMethodWithIndex(context, receiver, methodIndex, name, args, caller, 
                    callType, block);
        } catch (StackOverflowError sfe) {
            throw context.getRuntime().newSystemStackError("stack level too deep");
        }
    }

    /**
     * Used by the compiler to ease calling indexed methods, also to handle visibility.
     * NOTE: THIS IS NOT THE SAME AS THE SWITCHVALUE VERSIONS.
     */
    public static IRubyObject compilerCallMethodWithIndex(ThreadContext context, IRubyObject receiver, int methodIndex, String name, IRubyObject[] args, IRubyObject caller, CallType callType, Block block) {
        RubyModule module = receiver.getMetaClass();
        
        if (module.index != 0) {
            return receiver.callMethod(context, module, methodIndex, name, args, callType, block);
        }
        
        return compilerCallMethod(context, receiver, name, args, caller, callType, block);
    }
    
    /**
     * Used by the compiler to handle visibility
     */
    public static IRubyObject compilerCallMethod(ThreadContext context, IRubyObject receiver, String name,
            IRubyObject[] args, IRubyObject caller, CallType callType, Block block) {
        assert args != null;
        DynamicMethod method = null;
        RubyModule rubyclass = receiver.getMetaClass();
        method = rubyclass.searchMethod(name);
        
        if (method.isUndefined() || (!name.equals("method_missing") && !method.isCallableFrom(caller, callType))) {
            return callMethodMissing(context, receiver, method, name, args, caller, callType, block);
        }

        return method.call(context, receiver, rubyclass, name, args, block);
    }
    
    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, DynamicMethod method, String name, int methodIndex,
                                                IRubyObject[] args, IRubyObject self, CallType callType, Block block) {
        // store call information so method_missing impl can use it            
        context.setLastCallStatus(callType);            
        context.setLastVisibility(method.getVisibility());

        if (methodIndex == MethodIndex.METHOD_MISSING) {
            return RubyKernel.method_missing(self, args, block);
        }

        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = RubySymbol.newSymbol(self.getRuntime(), name);

        return receiver.callMethod(context, "method_missing", newArgs, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, DynamicMethod method, String name, 
                                                IRubyObject[] args, IRubyObject self, CallType callType, Block block) {
        // store call information so method_missing impl can use it            
        context.setLastCallStatus(callType);            
        context.setLastVisibility(method.getVisibility());

        if (name.equals("method_missing")) {
            return RubyKernel.method_missing(self, args, block);
        }

        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = RubySymbol.newSymbol(self.getRuntime(), name);

        return receiver.callMethod(context, "method_missing", newArgs, block);
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
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        return rubyClass.getClassVar(name);
    }
    
    public static IRubyObject fastFetchClassVariable(ThreadContext context, Ruby runtime, 
            IRubyObject self, String internedName) {
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        return rubyClass.fastGetClassVar(internedName);
    }
    
    public static IRubyObject nullToNil(IRubyObject value, Ruby runtime) {
        return value != null ? value : runtime.getNil();
    }
    
    public static RubyClass prepareSuperClass(Ruby runtime, IRubyObject rubyClass) {
        RubyClass.checkInheritable(rubyClass); // use the same logic as in EvaluationState
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
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        rubyClass.setClassVar(name, value);
   
        return value;
    }
    
    public static IRubyObject fastSetClassVariable(ThreadContext context, Ruby runtime, 
            IRubyObject self, String internedName, IRubyObject value) {
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        rubyClass.fastSetClassVar(internedName, value);
   
        return value;
    }
    
    public static IRubyObject declareClassVariable(ThreadContext context, Ruby runtime, IRubyObject self, String name, IRubyObject value) {
        // FIXME: This isn't quite right; it shouldn't evaluate the value if it's going to throw the error
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) throw runtime.newTypeError("no class/module to define class variable");
        
        rubyClass.setClassVar(name, value);
   
        return value;
    }
    
    public static IRubyObject fastDeclareClassVariable(ThreadContext context, Ruby runtime, IRubyObject self, String internedName, IRubyObject value) {
        // FIXME: This isn't quite right; it shouldn't evaluate the value if it's going to throw the error
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(context, runtime);
   
        if (rubyClass == null) throw runtime.newTypeError("no class/module to define class variable");
        
        rubyClass.fastSetClassVar(internedName, value);
   
        return value;
    }
    
    public static void handleArgumentSizes(ThreadContext context, Ruby runtime, int given, int required, int opt, int rest) {
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
        if (exception.isKindOf(runtime.fastGetClass("LocalJumpError"))) {
            RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();

            IRubyObject reason = jumpError.reason();

            return reason.asSymbol();
        }

        throw re;
    }
    
    public static IRubyObject unwrapLocalJumpErrorValue(RaiseException re) {
        return ((RubyLocalJumpError)re.getException()).exit_value();
    }
    
    public static IRubyObject processBlockArgument(Ruby runtime, Block block) {
        if (!block.isGiven()) {
            return runtime.getNil();
        }
        
        RubyProc blockArg;
        
        if (block.getProcObject() != null) {
            blockArg = block.getProcObject();
        } else {
            blockArg = runtime.newProc(Block.Type.PROC, block);
            blockArg.getBlock().type = Block.Type.PROC;
        }
        
        return blockArg;
    }
    
    public static Block getBlockFromBlockPassBody(IRubyObject proc, Block currentBlock) {
        Ruby runtime = proc.getRuntime();

        // No block from a nil proc
        if (proc.isNil()) return Block.NULL_BLOCK;

        // If not already a proc then we should try and make it one.
        if (!(proc instanceof RubyProc)) {
            proc = proc.convertToType(runtime.getProc(), 0, "to_proc", false);

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
        
        return self.callSuper(context, context.getCurrentScope().getArgValues(), block);
    }
    
    public static IRubyObject[] appendToObjectArray(IRubyObject[] array, IRubyObject add) {
        IRubyObject[] newArray = new IRubyObject[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = add;
        return newArray;
    }
    
    public static IRubyObject returnJump(IRubyObject result, ThreadContext context) {
        throw new JumpException.ReturnJump(context.getFrameJumpTarget(), result);
    }
    
    public static IRubyObject breakJumpInWhile(JumpException.BreakJump bj, Block aBlock) {
        // JRUBY-530, while case
        if (bj.getTarget() == aBlock) {
            bj.setTarget(null);
            
            throw bj;
        }

        return (IRubyObject) bj.getValue();
    }
    
    public static IRubyObject breakJump(IRubyObject value) {
        throw new JumpException.BreakJump(null, value);
    }
    
    public static IRubyObject breakLocalJumpError(Ruby runtime, IRubyObject value) {
        throw runtime.newLocalJumpError("break", value, "unexpected break");
    }
    
    public static IRubyObject[] concatObjectArrays(IRubyObject[] array, IRubyObject[] add) {
        IRubyObject[] newArray = new IRubyObject[array.length + add.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(add, 0, newArray, array.length, add.length);
        return newArray;
    }
    
    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject[] exceptions, Ruby runtime, ThreadContext context, IRubyObject self) {
        for (int i = 0; i < exceptions.length; i++) {
            if (!exceptions[i].isKindOf(runtime.getModule())) {
                throw runtime.newTypeError("class or module required for rescue clause");
            }
            IRubyObject result = exceptions[i].callMethod(context, "===", currentException);
            if (result.isTrue()) return result;
        }
        return runtime.getFalse();
    }
    
    public static void checkSuperDisabled(ThreadContext context) {
        RubyModule klazz = context.getFrameKlazz();
        
        if (klazz == null) {
            String name = context.getFrameName();
            throw context.getRuntime().newNameError("Superclass method '" + name
                    + "' disabled.", name);
        }
    }
    
    public static Block ensureSuperBlock(Block given, Block parent) {
        if (!given.isGiven()) {
            return parent;
        }
        return given;
    }
    
    public static RubyModule findImplementerIfNecessary(RubyModule clazz, RubyModule implementationClass) {
        if (implementationClass != null && implementationClass.needsImplementer()) {
            // modules are included with a shim class; we must find that shim to handle super() appropriately
            return clazz.findImplementer(implementationClass);
        } else {
            // classes are directly in the hierarchy, so no special logic is necessary for implementer
            return implementationClass;
        }
    }
    
    public static RubyArray createSubarray(RubyArray input, int start) {
        return (RubyArray)input.subseqLight(start, input.size() - start);
    }
    
    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start) {
        return RubyArray.newArrayNoCopy(runtime, input, start);
    }
    
    public static RubyBoolean isWhenTriggered(IRubyObject expression, IRubyObject expressionsObject, ThreadContext context) {
        RubyArray expressions = ASTInterpreter.splatValue(context.getRuntime(), expressionsObject);
        for (int j = 0,k = expressions.getLength(); j < k; j++) {
            IRubyObject condition = expressions.eltInternal(j);

            if ((expression != null && condition.callMethod(context, MethodIndex.OP_EQQ, "===", expression)
                    .isTrue())
                    || (expression == null && condition.isTrue())) {
                return context.getRuntime().getTrue();
            }
        }
        
        return context.getRuntime().getFalse();
    }
    
    public static IRubyObject setConstantInModule(IRubyObject module, IRubyObject value, String name, ThreadContext context) {
        return context.setConstantInModule(name, (RubyModule)module, value);
    }
    
    public static IRubyObject retryJump() {
        throw JumpException.RETRY_JUMP;
    }
    
    public static IRubyObject redoJump() {
        throw JumpException.REDO_JUMP;
    }
    
    public static IRubyObject redoLocalJumpError(Ruby runtime) {
        throw runtime.newLocalJumpError("redo", runtime.getNil(), "unexpected redo");
    }
    
    public static IRubyObject nextJump(IRubyObject value) {
        throw new JumpException.NextJump(value);
    }
    
    public static IRubyObject nextLocalJumpError(Ruby runtime, IRubyObject value) {
        throw runtime.newLocalJumpError("next", value, "unexpected next");
    }
    
    public static ISourcePosition constructPosition(String file, int line) {
        return new SimpleSourcePosition(file, line);
    }
    
    public static final int MAX_SPECIFIC_ARITY_OBJECT_ARRAY = 5;
    
    public static IRubyObject[] constructObjectArray(IRubyObject one) {
        return new IRubyObject[] {one};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two) {
        return new IRubyObject[] {one, two};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three) {
        return new IRubyObject[] {one, two, three};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four) {
        return new IRubyObject[] {one, two, three, four};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five) {
        return new IRubyObject[] {one, two, three, four, five};
    }
    
    public static final int MAX_SPECIFIC_ARITY_HASH = 3;
    
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.put(key1, value1);
        return hash;
    }
    
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.put(key1, value1);
        hash.put(key2, value2);
        return hash;
    }
    
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2, IRubyObject key3, IRubyObject value3) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.put(key1, value1);
        hash.put(key2, value2);
        hash.put(key3, value3);
        return hash;
    }
    
    public static IRubyObject defineAlias(ThreadContext context, String newName, String oldName) {
        Ruby runtime = context.getRuntime();
        RubyModule module = context.getRubyClass();
   
        if (module == null) throw runtime.newTypeError("no class to make alias");
   
        module.defineAlias(newName, oldName);
        module.callMethod(context, "method_added", runtime.newSymbol(newName));
   
        return runtime.getNil();
    }
    
    public static IRubyObject getInstanceVariable(Ruby runtime, IRubyObject self, String name) {
        IRubyObject result = self.getInstanceVariable(name);
        if (result == null) return runtime.getNil();
        return result;
    }
    
    public static IRubyObject fastGetInstanceVariable(Ruby runtime, IRubyObject self, String internedName) {
        IRubyObject result;
        if ((result = self.fastGetInstanceVariable(internedName)) != null) return result;
        return runtime.getNil();
    }
    
    public static IRubyObject negate(IRubyObject value, Ruby runtime) {
        if (value.isTrue()) return runtime.getFalse();
        return runtime.getTrue();
    }
    
    public static IRubyObject stringOrNil(String value, Ruby runtime, IRubyObject nil) {
        if (value == null) return nil;
        return RubyString.newString(runtime, value);
    }
    
    public static void preLoad(ThreadContext context, String[] varNames) {
        StaticScope staticScope = new LocalStaticScope(context.getCurrentScope().getStaticScope(), varNames);
        staticScope.setModule(context.getRuntime().getObject());
        DynamicScope scope = new DynamicScope(staticScope);
        
        // Each root node has a top-level scope that we need to push
        context.preScopedBody(scope);
    }
    
    public static void postLoad(ThreadContext context) {
        context.postScopedBody();
    }
    
    public static void registerEndBlock(CompiledSharedScopeBlock block, Ruby runtime) {
        runtime.pushExitBlock(runtime.newProc(Block.Type.LAMBDA, block));
    }
    
    public static IRubyObject match3(RubyRegexp regexp, IRubyObject value, ThreadContext context) {
        if (value instanceof RubyString) {
            return regexp.op_match(value);
        } else {
            return value.callMethod(context, "=~", regexp);
        }
    }
    
    public static IRubyObject getErrorInfo(Ruby runtime) {
        return runtime.getGlobalVariables().get("$!");
    }
    
    public static void setErrorInfo(Ruby runtime, IRubyObject error) {
        runtime.getGlobalVariables().set("$!", error);
    }
}
