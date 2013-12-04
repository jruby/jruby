package org.jruby.runtime;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.jruby.*;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.MultipleAsgn19Node;
import org.jruby.ast.Node;
import org.jruby.ast.NodeType;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.compiler.ASTInspector;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.internal.runtime.methods.CallConfiguration;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.UndefinedMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.javasupport.JavaClass;
import org.jruby.javasupport.JavaUtil;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.util.ByteList;
import org.jruby.util.DefinedMessage;
import org.jruby.util.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.unicode.UnicodeEncoding;

import static org.jruby.runtime.invokedynamic.MethodNames.EQL;
import static org.jruby.runtime.invokedynamic.MethodNames.OP_EQUAL;
import org.jruby.util.JavaNameMangler;

/**
 * Helper methods which are called by the compiler.  Note: These will show no consumers, but
 * generated code does call these so don't remove them thinking they are dead code. 
 *
 */
public class Helpers {
    public static CallSite selectAttrAsgnCallSite(IRubyObject receiver, IRubyObject self, CallSite normalSite, CallSite variableSite) {
        if (receiver == self) return variableSite;
        return normalSite;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject arg0, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, arg0, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject arg0, IRubyObject arg1, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, arg0, arg1, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, IRubyObject value, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, arg0, arg1, arg2, value);
        return value;
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject[] args, ThreadContext context, IRubyObject caller) {
        callSite.call(context, caller, receiver, args);
        return args[args.length - 1];
    }
    public static IRubyObject doAttrAsgn(IRubyObject receiver, CallSite callSite, IRubyObject[] args, IRubyObject value, ThreadContext context, IRubyObject caller) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = value;
        callSite.call(context, caller, receiver, newArgs);
        return value;
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject[] receivers) {
        for (int i = 0; i < receivers.length; i++) {
            IRubyObject receiver = receivers[i];
            if (invokeEqqForCaseWhen(callSite, context, caller, arg, receiver)) return true;
        }
        return false;
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject receiver) {
        IRubyObject result = callSite.call(context, caller, receiver, arg);
        if (result.isTrue()) return true;
        return false;
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject receiver0, IRubyObject receiver1) {
        IRubyObject result = callSite.call(context, caller, receiver0, arg);
        if (result.isTrue()) return true;
        return invokeEqqForCaseWhen(callSite, context, caller, arg, receiver1);
    }

    public static boolean invokeEqqForCaseWhen(CallSite callSite, ThreadContext context, IRubyObject caller, IRubyObject arg, IRubyObject receiver0, IRubyObject receiver1, IRubyObject receiver2) {
        IRubyObject result = callSite.call(context, caller, receiver0, arg);
        if (result.isTrue()) return true;
        return invokeEqqForCaseWhen(callSite, context, caller, arg, receiver1, receiver2);
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject[] receivers) {
        for (int i = 0; i < receivers.length; i++) {
            if (receivers[i].isTrue()) return true;
        }
        return false;
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject receiver) {
        return receiver.isTrue();
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject receiver0, IRubyObject receiver1) {
        return receiver0.isTrue() || receiver1.isTrue();
    }

    public static boolean invokeEqqForCaselessWhen(IRubyObject receiver0, IRubyObject receiver1, IRubyObject receiver2) {
        return receiver0.isTrue() || receiver1.isTrue() || receiver2.isTrue();
    }
    
    public static CompiledBlockCallback createBlockCallback(Object scriptObject, String closureMethod, String file, int line) {
        Class scriptClass = scriptObject.getClass();
        ClassLoader scriptClassLoader = scriptClass.getClassLoader();
        MethodFactory factory = MethodFactory.createFactory(scriptClassLoader);
        
        return factory.getBlockCallback(closureMethod, file, line, scriptObject);
    }

    public static CompiledBlockCallback19 createBlockCallback19(Object scriptObject, String closureMethod, String file, int line) {
        Class scriptClass = scriptObject.getClass();
        ClassLoader scriptClassLoader = scriptClass.getClassLoader();
        MethodFactory factory = MethodFactory.createFactory(scriptClassLoader);

        return factory.getBlockCallback19(closureMethod, file, line, scriptObject);
    }

    public static byte[] createBlockCallbackOffline(String classPath, String closureMethod, String file, int line) {
        MethodFactory factory = MethodFactory.createFactory(Helpers.class.getClassLoader());

        return factory.getBlockCallbackOffline(closureMethod, file, line, classPath);
    }

    public static byte[] createBlockCallback19Offline(String classPath, String closureMethod, String file, int line) {
        MethodFactory factory = MethodFactory.createFactory(Helpers.class.getClassLoader());

        return factory.getBlockCallback19Offline(closureMethod, file, line, classPath);
    }

    public static String buildBlockDescriptor19(
            String closureMethod,
            int arity,
            String file,
            int line,
            boolean hasMultipleArgsHead,
            NodeType argsNodeId,
            String parameterList,
            ASTInspector inspector) {
        return buildBlockDescriptor(closureMethod, arity, file, line, hasMultipleArgsHead, argsNodeId, inspector) +
                ":" + parameterList;
    }

    public static String buildBlockDescriptor(
            String closureMethod,
            int arity,
            String file,
            int line,
            boolean hasMultipleArgsHead,
            NodeType argsNodeId,
            ASTInspector inspector) {

        // We use : to delimit since mangleMethodName mangles it (i.e. it will
        // never appear in the resulting mangled string)
        String descriptor =
                JavaNameMangler.mangleMethodName(closureMethod) + ':' +
                arity + ':' +
                hasMultipleArgsHead + ':' +
                BlockBody.asArgumentType(argsNodeId) + ':' +
                JavaNameMangler.mangleMethodName(file) + ':' +
                line + ':' +
                !(inspector.hasClosure() || inspector.hasScopeAwareMethods());

        return descriptor;
    }
    
    public static String[] parseBlockDescriptor(String descriptor) {
        String[] mangled = descriptor.split(":");
        mangled[0] = JavaNameMangler.demangleMethodName(mangled[0]);
        mangled[4] = JavaNameMangler.demangleMethodName(mangled[4]);
        return mangled;
    }

    public static BlockBody createCompiledBlockBody(ThreadContext context, Object scriptObject, StaticScope scope, String descriptor) {
        String[] firstSplit = parseBlockDescriptor(descriptor);
        return createCompiledBlockBody(context, scriptObject, firstSplit[0], Integer.parseInt(firstSplit[1]), scope, Boolean.valueOf(firstSplit[2]), Integer.parseInt(firstSplit[3]), firstSplit[4], Integer.parseInt(firstSplit[5]), Boolean.valueOf(firstSplit[6]));
    }
    
    public static BlockBody createCompiledBlockBody(ThreadContext context, Object scriptObject, String closureMethod, int arity,
            StaticScope staticScope, boolean hasMultipleArgsHead, int argsNodeType, String file, int line, boolean light) {
        staticScope.determineModule();
        
        if (light) {
            return CompiledBlockLight.newCompiledBlockLight(
                    Arity.createArity(arity), staticScope,
                    createBlockCallback(scriptObject, closureMethod, file, line),
                    hasMultipleArgsHead, argsNodeType);
        } else {
            return CompiledBlock.newCompiledBlock(
                    Arity.createArity(arity), staticScope,
                    createBlockCallback(scriptObject, closureMethod, file, line),
                    hasMultipleArgsHead, argsNodeType);
        }
    }

    public static BlockBody createCompiledBlockBody19(ThreadContext context, Object scriptObject, StaticScope scope, String descriptor) {
        String[] firstSplit = parseBlockDescriptor(descriptor);
        return createCompiledBlockBody19(context, scriptObject, firstSplit[0], Integer.parseInt(firstSplit[1]), scope, Boolean.valueOf(firstSplit[2]), Integer.parseInt(firstSplit[3]), firstSplit[4], Integer.parseInt(firstSplit[5]), Boolean.valueOf(firstSplit[6]), firstSplit[7]);
    }

    public static BlockBody createCompiledBlockBody19(ThreadContext context, Object scriptObject, String closureMethod, int arity,
            StaticScope staticScope, boolean hasMultipleArgsHead, int argsNodeType, String file, int line, boolean light, String parameterList) {
        staticScope.determineModule();

        if (light) {
            return CompiledBlockLight19.newCompiledBlockLight(
                    Arity.createArity(arity), staticScope,
                    createBlockCallback19(scriptObject, closureMethod, file, line),
                    hasMultipleArgsHead, argsNodeType, parameterList.split(";"));
        } else {
            return CompiledBlock19.newCompiledBlock(
                    Arity.createArity(arity), staticScope,
                    createBlockCallback19(scriptObject, closureMethod, file, line),
                    hasMultipleArgsHead, argsNodeType, parameterList.split(";"));
        }
    }
    
    public static Block createBlock(ThreadContext context, IRubyObject self, BlockBody body) {
        return CompiledBlock.newCompiledClosure(
                context,
                self,
                body);
    }

    public static Block createBlock19(ThreadContext context, IRubyObject self, BlockBody body) {
        return CompiledBlock19.newCompiledClosure(
                context,
                self,
                body);
    }
    
    public static IRubyObject runBeginBlock(ThreadContext context, IRubyObject self, String scopeString, CompiledBlockCallback callback) {
        StaticScope staticScope = decodeScope(context, context.getCurrentStaticScope(), scopeString);
        
        context.preScopedBody(DynamicScope.newDynamicScope(staticScope, context.getCurrentScope()));
        
        Block block = CompiledBlock.newCompiledClosure(context, self, Arity.createArity(0), staticScope, callback, false, BlockBody.ZERO_ARGS);
        
        try {
            block.yield(context, null);
        } finally {
            context.postScopedBody();
        }

        return context.runtime.getNil();
    }
    
    public static Block createSharedScopeBlock(ThreadContext context, IRubyObject self, int arity, 
            CompiledBlockCallback callback, boolean hasMultipleArgsHead, int argsNodeType) {
        
        return CompiledSharedScopeBlock.newCompiledSharedScopeClosure(context, self, Arity.createArity(arity), 
                context.getCurrentScope(), callback, hasMultipleArgsHead, argsNodeType);
    }
    
    public static IRubyObject def(ThreadContext context, IRubyObject self, Object scriptObject, String rubyName, String javaName, StaticScope scope,
            int arity, String filename, int line, CallConfiguration callConfig, String parameterDesc) {
        Class compiledClass = scriptObject.getClass();
        Ruby runtime = context.runtime;
        
        RubyModule containingClass = context.getRubyClass();
        Visibility visibility = context.getCurrentVisibility();
        
        performNormalMethodChecks(containingClass, runtime, rubyName);
        
        MethodFactory factory = MethodFactory.createFactory(compiledClass.getClassLoader());
        DynamicMethod method = constructNormalMethod(
                factory, javaName,
                rubyName, containingClass, new SimpleSourcePosition(filename, line), arity, scope, visibility, scriptObject,
                callConfig,
                parameterDesc);
        
        addInstanceMethod(containingClass, rubyName, method, visibility,context, runtime);
        
        return runtime.newSymbol(rubyName);
    }
    
    public static IRubyObject defs(ThreadContext context, IRubyObject self, IRubyObject receiver, Object scriptObject, String rubyName, String javaName, StaticScope scope,
            int arity, String filename, int line, CallConfiguration callConfig, String parameterDesc) {
        Class compiledClass = scriptObject.getClass();
        Ruby runtime = context.runtime;

        RubyClass rubyClass = performSingletonMethodChecks(runtime, receiver, rubyName);
        
        MethodFactory factory = MethodFactory.createFactory(compiledClass.getClassLoader());
        DynamicMethod method = constructSingletonMethod(
                factory, rubyName, javaName, rubyClass,
                new SimpleSourcePosition(filename, line), arity, scope,
                scriptObject, callConfig, parameterDesc);
        
        rubyClass.addMethod(rubyName, method);
        
        callSingletonMethodHook(receiver,context, runtime.fastNewSymbol(rubyName));
        
        return runtime.newSymbol(rubyName);
    }

    public static byte[] defOffline(String rubyName, String javaName, String classPath, String invokerName, Arity arity, StaticScope scope, CallConfiguration callConfig, String filename, int line) {
        MethodFactory factory = MethodFactory.createFactory(Helpers.class.getClassLoader());
        byte[] methodBytes = factory.getCompiledMethodOffline(rubyName, javaName, classPath, invokerName, arity, scope, callConfig, filename, line);

        return methodBytes;
    }
    
    public static RubyClass getSingletonClass(Ruby runtime, IRubyObject receiver) {
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton");
        } else {
            return receiver.getSingletonClass();
        }
    }

    // TODO: Only used by interface implementation; eliminate it
    public static IRubyObject invokeMethodMissing(IRubyObject receiver, String name, IRubyObject[] args) {
        ThreadContext context = receiver.getRuntime().getCurrentContext();

        // store call information so method_missing impl can use it
        context.setLastCallStatusAndVisibility(CallType.FUNCTIONAL, Visibility.PUBLIC);

        if (name.equals("method_missing")) {
            return RubyKernel.method_missing(context, receiver, args, Block.NULL_BLOCK);
        }

        IRubyObject[] newArgs = prepareMethodMissingArgs(args, context, name);

        return invoke(context, receiver, "method_missing", newArgs, Block.NULL_BLOCK);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject[] args, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, args, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, arg0, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, arg0, arg1, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, arg0, arg1, arg2, block);
    }

    public static IRubyObject callMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType, Block block) {
        return selectMethodMissing(context, receiver, visibility, name, callType).call(context, receiver, receiver.getMetaClass(), name, block);
    }

    public static DynamicMethod selectMethodMissing(ThreadContext context, IRubyObject receiver, Visibility visibility, String name, CallType callType) {
        Ruby runtime = context.runtime;

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        DynamicMethod methodMissing = receiver.getMetaClass().searchMethod("method_missing");
        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingMethod(methodMissing, callType);
    }

    public static DynamicMethod selectMethodMissing(ThreadContext context, RubyClass selfClass, Visibility visibility, String name, CallType callType) {
        Ruby runtime = context.runtime;

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        DynamicMethod methodMissing = selfClass.searchMethod("method_missing");
        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingMethod(methodMissing, callType);
    }

    public static DynamicMethod selectMethodMissing(RubyClass selfClass, Visibility visibility, String name, CallType callType) {
        Ruby runtime = selfClass.getClassRuntime();

        if (name.equals("method_missing")) {
            return selectInternalMM(runtime, visibility, callType);
        }

        DynamicMethod methodMissing = selfClass.searchMethod("method_missing");
        if (methodMissing.isUndefined() || methodMissing.equals(runtime.getDefaultMethodMissing())) {
            return selectInternalMM(runtime, visibility, callType);
        }
        return new MethodMissingMethod(methodMissing, callType);
    }

    public static final Map<String, String> map(String... keyValues) {
        HashMap<String, String> map = new HashMap<String, String>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length;) {
            map.put(keyValues[i++], keyValues[i++]);
        }
        return map;
    }

    private static class MethodMissingMethod extends DynamicMethod {
        private final DynamicMethod delegate;
        private final CallType lastCallStatus;

        public MethodMissingMethod(DynamicMethod delegate, CallType lastCallStatus) {
            this.delegate = delegate;
            this.lastCallStatus = lastCallStatus;
        }

        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
            context.setLastCallStatus(lastCallStatus);
            return this.delegate.call(context, self, clazz, "method_missing", prepareMethodMissingArgs(args, context, name), block);
        }

        @Override
        public DynamicMethod dup() {
            return this;
        }
    }

    private static DynamicMethod selectInternalMM(Ruby runtime, Visibility visibility, CallType callType) {
        if (visibility == Visibility.PRIVATE) {
            return runtime.getPrivateMethodMissing();
        } else if (visibility == Visibility.PROTECTED) {
            return runtime.getProtectedMethodMissing();
        } else if (callType == CallType.VARIABLE) {
            return runtime.getVariableMethodMissing();
        } else if (callType == CallType.SUPER) {
            return runtime.getSuperMethodMissing();
        } else {
            return runtime.getNormalMethodMissing();
        }
    }

    private static IRubyObject[] prepareMethodMissingArgs(IRubyObject[] args, ThreadContext context, String name) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 1, args.length);
        newArgs[0] = context.runtime.newSymbol(name);

        return newArgs;
    }
    
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, Block block) {
        return self.getMetaClass().finvoke(context, self, name, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, arg2, block);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return self.getMetaClass().finvoke(context, self, name, args, block);
    }
    
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name) {
        return self.getMetaClass().finvoke(context, self, name);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0) {
        return self.getMetaClass().finvoke(context, self, name, arg0);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return self.getMetaClass().finvoke(context, self, name, arg0, arg1, arg2);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject... args) {
        return self.getMetaClass().finvoke(context, self, name, args);
    }
    
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, CallType callType) {
        return Helpers.invoke(context, self, name, IRubyObject.NULL_ARRAY, callType, Block.NULL_BLOCK);
    }
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject[] args, CallType callType, Block block) {
        return self.getMetaClass().invoke(context, self, name, args, callType, block);
    }
    
    public static IRubyObject invoke(ThreadContext context, IRubyObject self, String name, IRubyObject arg, CallType callType, Block block) {
        return self.getMetaClass().invoke(context, self, name, arg, callType, block);
    }
    
    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject[] args, Block block) {
        return asClass.finvoke(context, self, name, args, block);
    }
    
    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, Block block) {
        return asClass.finvoke(context, self, name, block);
    }
    
    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, Block block) {
        return asClass.finvoke(context, self, name, arg0, block);
    }
    
    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        return asClass.finvoke(context, self, name, arg0, arg1, block);
    }
    
    public static IRubyObject invokeAs(ThreadContext context, RubyClass asClass, IRubyObject self, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        return asClass.finvoke(context, self, name, arg0, arg1, arg2, block);
    }

    public static IRubyObject invokeChecked(ThreadContext context, IRubyObject self, String name) {
        return self.getMetaClass().finvokeChecked(context, self, name);
    }

    /**
     * The protocol for super method invocation is a bit complicated
     * in Ruby. In real terms it involves first finding the real
     * implementation class (the super class), getting the name of the
     * method to call from the frame, and then invoke that on the
     * super class with the current self as the actual object
     * invoking.
     */
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return invokeSuper(context, self, context.getFrameKlazz(), context.getFrameName(), args, block);
    }

    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, RubyModule klass, String name, IRubyObject[] args, Block block) {
        checkSuperDisabledOrOutOfMethod(context, klass, name);

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klass).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, args, block);
        }
        return method.call(context, self, superClass, name, args, block);
    }
    
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, block);
        }
        return method.call(context, self, superClass, name, block);
    }
    
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, arg0, block);
        }
        return method.call(context, self, superClass, name, arg0, block);
    }
    
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, arg0, arg1, block);
        }
        return method.call(context, self, superClass, name, arg0, arg1, block);
    }
    
    public static IRubyObject invokeSuper(ThreadContext context, IRubyObject self, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        checkSuperDisabledOrOutOfMethod(context);
        RubyModule klazz = context.getFrameKlazz();
        String name = context.getFrameName();

        RubyClass superClass = findImplementerIfNecessary(self.getMetaClass(), klazz).getSuperClass();
        DynamicMethod method = superClass != null ? superClass.searchMethod(name) : UndefinedMethod.INSTANCE;

        if (method.isUndefined()) {
            return callMethodMissing(context, self, method.getVisibility(), name, CallType.SUPER, arg0, arg1, arg2, block);
        }
        return method.call(context, self, superClass, name, arg0, arg1, arg2, block);
    }

    public static RubyArray ensureRubyArray(IRubyObject value) {
        return ensureRubyArray(value.getRuntime(), value);
    }

    public static RubyArray ensureRubyArray(Ruby runtime, IRubyObject value) {
        return value instanceof RubyArray ? (RubyArray)value : RubyArray.newArray(runtime, value);
    }

    public static RubyArray ensureMultipleAssignableRubyArray(IRubyObject value, Ruby runtime, boolean masgnHasHead) {
        if (!(value instanceof RubyArray)) {
            value = ArgsUtil.convertToRubyArray19(runtime, value, masgnHasHead);
        }
        return (RubyArray) value;
    }
    
    public static IRubyObject fetchClassVariable(Ruby runtime, StaticScope scope,
            IRubyObject self, String name) {
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(runtime, scope);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        return rubyClass.getClassVar(name);
    }
    
    public static IRubyObject getConstant(ThreadContext context, String internedName) {
        Ruby runtime = context.runtime;

        return context.getCurrentScope().getStaticScope().getConstantWithConstMissing(internedName);
    }

    public static IRubyObject nullToNil(IRubyObject value, ThreadContext context) {
        return value != null ? value : context.nil;
    }
    
    public static IRubyObject nullToNil(IRubyObject value, Ruby runtime) {
        return value != null ? value : runtime.getNil();
    }
    
    public static IRubyObject nullToNil(IRubyObject value, IRubyObject nil) {
        return value != null ? value : nil;
    }
    
    public static RubyClass prepareSuperClass(Ruby runtime, IRubyObject rubyClass) {
        RubyClass.checkInheritable(rubyClass); // use the same logic as in EvaluationState
        return (RubyClass)rubyClass;
    }
    
    public static RubyModule prepareClassNamespace(ThreadContext context, StaticScope scope, IRubyObject rubyModule) {
        if (rubyModule == null || rubyModule.isNil()) {
            rubyModule = scope.getModule();

            if (rubyModule == null) {
                throw context.runtime.newTypeError("no outer class/module");
            }
        }

        if (rubyModule instanceof RubyModule) {
            return (RubyModule)rubyModule;
        } else {
            throw context.runtime.newTypeError(rubyModule + " is not a class/module");
        }
    }
    
    public static IRubyObject setClassVariable(Ruby runtime, StaticScope scope,
            IRubyObject self, String name, IRubyObject value) {
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(runtime, scope);
   
        if (rubyClass == null) rubyClass = self.getMetaClass();

        rubyClass.setClassVar(name, value);
   
        return value;
    }
    
    public static IRubyObject declareClassVariable(Ruby runtime, StaticScope scope, IRubyObject self, String name, IRubyObject value) {
        // FIXME: This isn't quite right; it shouldn't evaluate the value if it's going to throw the error
        RubyModule rubyClass = ASTInterpreter.getClassVariableBase(runtime, scope);
   
        if (rubyClass == null) throw runtime.newTypeError("no class/module to define class variable");
        
        rubyClass.setClassVar(name, value);
   
        return value;
    }
    
    public static void handleArgumentSizes(ThreadContext context, Ruby runtime, int given, int required, int opt, int rest) {
        if (opt == 0) {
            if (rest < 0) {
                // no opt, no rest, exact match
                if (given != required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                }
            } else {
                // only rest, must be at least required
                if (given < required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                }
            }
        } else {
            if (rest < 0) {
                // opt but no rest, must be at least required and no more than required + opt
                if (given < required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                } else if (given > (required + opt)) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + (required + opt) + ")");
                }
            } else {
                // opt and rest, must be at least required
                if (given < required) {
                    throw runtime.newArgumentError("wrong number of arguments (" + given + " for " + required + ")");
                }
            }
        }
    }
    
    /**
     * If it's Redo, Next, or Break, rethrow it as a normal exception for while to handle
     * @param re
     * @param context
     */
    public static Throwable unwrapRedoNextBreakOrJustLocalJump(RaiseException re, ThreadContext context) {
        RubyException exception = re.getException();
        if (context.runtime.getLocalJumpError().isInstance(exception)) {
            RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();

            switch (jumpError.getReason()) {
            case REDO:
                return JumpException.REDO_JUMP;
            case NEXT:
                return new JumpException.NextJump(jumpError.exit_value());
            case BREAK:
                return new JumpException.BreakJump(context.getFrameJumpTarget(), jumpError.exit_value());
            }
        }
        return re;
    }
    
    public static String getLocalJumpTypeOrRethrow(RaiseException re) {
        RubyException exception = re.getException();
        Ruby runtime = exception.getRuntime();
        if (runtime.getLocalJumpError().isInstance(exception)) {
            RubyLocalJumpError jumpError = (RubyLocalJumpError)re.getException();

            IRubyObject reason = jumpError.reason();

            return reason.asJavaString();
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
        
        return processGivenBlock(block, runtime);
    }

    private static IRubyObject processGivenBlock(Block block, Ruby runtime) {
        RubyProc blockArg = block.getProcObject();

        if (blockArg == null) {
            blockArg = runtime.newBlockPassProc(Block.Type.PROC, block);
            blockArg.getBlock().type = Block.Type.PROC;
        }

        return blockArg;
    }
    
    public static Block getBlockFromBlockPassBody(Ruby runtime, IRubyObject proc, Block currentBlock) {
        // No block from a nil proc
        if (proc.isNil()) return Block.NULL_BLOCK;

        // If not already a proc then we should try and make it one.
        if (!(proc instanceof RubyProc)) {
            proc = coerceProc(proc, runtime);
        }

        return getBlockFromProc(currentBlock, proc);
    }

    private static IRubyObject coerceProc(IRubyObject proc, Ruby runtime) throws RaiseException {
        proc = TypeConverter.convertToType(proc, runtime.getProc(), "to_proc", false);

        if (!(proc instanceof RubyProc)) {
            throw runtime.newTypeError("wrong argument type " + proc.getMetaClass().getName() + " (expected Proc)");
        }
        return proc;
    }

    private static Block getBlockFromProc(Block currentBlock, IRubyObject proc) {
        // TODO: Add safety check for taintedness
        if (currentBlock != null && currentBlock.isGiven()) {
            RubyProc procObject = currentBlock.getProcObject();
            // The current block is already associated with proc.  No need to create a new one
            if (procObject != null && procObject == proc) {
                return currentBlock;
            }
        }

        return ((RubyProc) proc).getBlock();       
    }
    
    public static Block getBlockFromBlockPassBody(IRubyObject proc, Block currentBlock) {
        return getBlockFromBlockPassBody(proc.getRuntime(), proc, currentBlock);

    }
    
    public static IRubyObject backref(ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        
        if(backref instanceof RubyMatchData) {
            ((RubyMatchData)backref).use();
        }
        return backref;
    }
    
    public static IRubyObject backrefLastMatch(ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        
        return RubyRegexp.last_match(backref);
    }
    
    public static IRubyObject backrefMatchPre(ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        
        return RubyRegexp.match_pre(backref);
    }
    
    public static IRubyObject backrefMatchPost(ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        
        return RubyRegexp.match_post(backref);
    }
    
    public static IRubyObject backrefMatchLast(ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        
        return RubyRegexp.match_last(backref);
    }

    public static IRubyObject[] getArgValues(ThreadContext context) {
        return context.getCurrentScope().getArgValues();
    }
    
    public static IRubyObject callZSuper(Ruby runtime, ThreadContext context, Block block, IRubyObject self) {
        // Has the method that is calling super received a block argument
        if (!block.isGiven()) block = context.getCurrentFrame().getBlock(); 
        
        return Helpers.invokeSuper(context, self, context.getCurrentScope().getArgValues(), block);
    }
    
    public static IRubyObject[] appendToObjectArray(IRubyObject[] array, IRubyObject add) {
        IRubyObject[] newArray = new IRubyObject[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = add;
        return newArray;
    }
    
    public static JumpException.ReturnJump returnJump(IRubyObject result, ThreadContext context) {
        return context.returnJump(result);
    }
    
    public static IRubyObject throwReturnJump(IRubyObject result, ThreadContext context) {
        throw context.returnJump(result);
    }
    
    public static IRubyObject breakJumpInWhile(JumpException.BreakJump bj, ThreadContext context) {
        // JRUBY-530, while case
        if (bj.getTarget() == context.getFrameJumpTarget()) {
            return (IRubyObject) bj.getValue();
        }

        throw bj;
    }
    
    public static IRubyObject breakJump(ThreadContext context, IRubyObject value) {
        throw new JumpException.BreakJump(context.getFrameJumpTarget(), value);
    }
    
    public static IRubyObject breakLocalJumpError(Ruby runtime, IRubyObject value) {
        throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, value, "unexpected break");
    }
    
    public static IRubyObject[] concatObjectArrays(IRubyObject[] array, IRubyObject[] add) {
        return toArray(array, add);
    }
    
    public static IRubyObject[] toArray(IRubyObject[] array, IRubyObject... add) {
        IRubyObject[] newArray = new IRubyObject[array.length + add.length];
        System.arraycopy(array, 0, newArray, 0, array.length);
        System.arraycopy(add, 0, newArray, array.length, add.length);
        return newArray;
    }
    
    public static IRubyObject[] toArray(IRubyObject obj, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[rest.length + 1];
        newArray[0] = obj;
        System.arraycopy(rest, 0, newArray, 1, rest.length);
        return newArray;
    }
    
    public static IRubyObject[] toArray(IRubyObject obj0, IRubyObject obj1, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[rest.length + 2];
        newArray[0] = obj0;
        newArray[1] = obj1;
        System.arraycopy(rest, 0, newArray, 2, rest.length);
        return newArray;
    }
    
    public static IRubyObject[] toArray(IRubyObject obj0, IRubyObject obj1, IRubyObject obj2, IRubyObject... rest) {
        IRubyObject[] newArray = new IRubyObject[rest.length + 3];
        newArray[0] = obj0;
        newArray[1] = obj1;
        newArray[2] = obj2;
        System.arraycopy(rest, 0, newArray, 3, rest.length);
        return newArray;
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject[] exceptions, ThreadContext context) {
        for (int i = 0; i < exceptions.length; i++) {
            IRubyObject result = isExceptionHandled(currentException, exceptions[i], context);
            if (result.isTrue()) return result;
        }
        return context.runtime.getFalse();
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception, ThreadContext context) {
        return isExceptionHandled((IRubyObject)currentException, exception, context);
    }

    public static IRubyObject isExceptionHandled(IRubyObject currentException, IRubyObject exception, ThreadContext context) {
        Ruby runtime = context.runtime;
        if (!runtime.getModule().isInstance(exception)) {
            throw runtime.newTypeError("class or module required for rescue clause");
        }
        IRubyObject result = invoke(context, exception, "===", currentException);
        if (result.isTrue()) return result;
        return runtime.getFalse();
    }
    
    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception0, IRubyObject exception1, ThreadContext context) {
        IRubyObject result = isExceptionHandled(currentException, exception0, context);
        if (result.isTrue()) return result;
        return isExceptionHandled(currentException, exception1, context);
    }

    public static IRubyObject isExceptionHandled(RubyException currentException, IRubyObject exception0, IRubyObject exception1, IRubyObject exception2, ThreadContext context) {
        IRubyObject result = isExceptionHandled(currentException, exception0, context);
        if (result.isTrue()) return result;
        return isExceptionHandled(currentException, exception1, exception2, context);
    }

    private static boolean checkJavaException(Throwable throwable, IRubyObject catchable, ThreadContext context) {
        Ruby runtime = context.runtime;
        if (
                // rescue exception needs to catch Java exceptions
                runtime.getException() == catchable ||

                // rescue Object needs to catch Java exceptions
                runtime.getObject() == catchable ||

                // rescue StandardError needs t= catch Java exceptions
                runtime.getStandardError() == catchable) {

            if (throwable instanceof RaiseException) {
                return isExceptionHandled(((RaiseException)throwable).getException(), catchable, context).isTrue();
            }

            // let Ruby exceptions decide if they handle it
            return isExceptionHandled(JavaUtil.convertJavaToUsableRubyObject(runtime, throwable), catchable, context).isTrue();

        } else if (runtime.getNativeException() == catchable) {
            // NativeException catches Java exceptions, lazily creating the wrapper
            return true;

        } else if (catchable instanceof RubyClass && catchable.getInstanceVariables().hasInstanceVariable("@java_class")) {
            RubyClass rubyClass = (RubyClass)catchable;
            JavaClass javaClass = (JavaClass)rubyClass.getInstanceVariable("@java_class");
            if (javaClass != null) {
                Class cls = javaClass.javaClass();
                if (cls.isInstance(throwable)) {
                    return true;
                }
            }
        }

        return false;
    }
    
    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject[] throwables, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwables, context);
        } else {
            if (throwables.length == 0) {
                // no rescue means StandardError, which rescues Java exceptions
                return context.runtime.getTrue();
            } else {
                for (int i = 0; i < throwables.length; i++) {
                    if (checkJavaException(currentThrowable, throwables[i], context)) {
                        return context.runtime.getTrue();
                    }
                }
            }

            return context.runtime.getFalse();
        }
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable, context);
        } else {
            if (checkJavaException(currentThrowable, throwable, context)) {
                return context.runtime.getTrue();
            }

            return context.runtime.getFalse();
        }
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable0, IRubyObject throwable1, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }

        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable0, throwable1, context);
        } else {
            if (checkJavaException(currentThrowable, throwable0, context)) {
                return context.runtime.getTrue();
            }
            if (checkJavaException(currentThrowable, throwable1, context)) {
                return context.runtime.getTrue();
            }

            return context.runtime.getFalse();
        }
    }

    public static IRubyObject isJavaExceptionHandled(Throwable currentThrowable, IRubyObject throwable0, IRubyObject throwable1, IRubyObject throwable2, ThreadContext context) {
        if (currentThrowable instanceof Unrescuable) {
            throwException(currentThrowable);
        }
        
        if (currentThrowable instanceof RaiseException) {
            return isExceptionHandled(((RaiseException)currentThrowable).getException(), throwable0, throwable1, throwable2, context);
        } else {
            if (checkJavaException(currentThrowable, throwable0, context)) {
                return context.runtime.getTrue();
            }
            if (checkJavaException(currentThrowable, throwable1, context)) {
                return context.runtime.getTrue();
            }
            if (checkJavaException(currentThrowable, throwable2, context)) {
                return context.runtime.getTrue();
            }

            return context.runtime.getFalse();
        }
    }

    public static void storeExceptionInErrorInfo(Throwable currentThrowable, ThreadContext context) {
        IRubyObject exception = null;
        if (currentThrowable instanceof RaiseException) {
            exception = ((RaiseException)currentThrowable).getException();
        } else {
            exception = JavaUtil.convertJavaToUsableRubyObject(context.runtime, currentThrowable);
        }
        context.setErrorInfo(exception);
    }

    public static void storeNativeExceptionInErrorInfo(Throwable currentThrowable, ThreadContext context) {
        IRubyObject exception = null;
        if (currentThrowable instanceof RaiseException) {
            exception = ((RaiseException)currentThrowable).getException();
        } else {
            Ruby runtime = context.runtime;

            // wrap Throwable in a NativeException object
            exception = new NativeException(runtime, runtime.getNativeException(), currentThrowable);
            ((NativeException)exception).prepareIntegratedBacktrace(context, currentThrowable.getStackTrace());
        }
        context.setErrorInfo(exception);
    }

    public static void clearErrorInfo(ThreadContext context) {
        context.setErrorInfo(context.runtime.getNil());
    }
    
    public static void checkSuperDisabledOrOutOfMethod(ThreadContext context) {
        checkSuperDisabledOrOutOfMethod(context, context.getFrameKlazz(), context.getFrameName());
    }

    public static void checkSuperDisabledOrOutOfMethod(ThreadContext context, RubyModule klass, String name) {
        if (klass == null) {
            if (name != null) {
                throw context.runtime.newNameError("superclass method '" + name + "' disabled", name);
            } else {
                throw context.runtime.newNoMethodError("super called outside of method", null, context.nil);
            }
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

    public static RubyArray createSubarray(RubyArray input, int start, int post) {
        return (RubyArray)input.subseqLight(start, input.size() - post - start);
    }
    
    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start) {
        if (start >= input.length) {
            return RubyArray.newEmptyArray(runtime);
        } else {
            return RubyArray.newArrayNoCopy(runtime, input, start);
        }
    }

    public static RubyArray createSubarray(IRubyObject[] input, Ruby runtime, int start, int exclude) {
        int length = input.length - exclude - start;
        if (length <= 0) {
            return RubyArray.newEmptyArray(runtime);
        } else {
            return RubyArray.newArrayNoCopy(runtime, input, start, length);
        }
    }

    public static IRubyObject elementOrNull(IRubyObject[] input, int element) {
        if (element >= input.length) {
            return null;
        } else {
            return input[element];
        }
    }

    public static IRubyObject optElementOrNull(IRubyObject[] input, int element, int postCount) {
        if (element + postCount >= input.length) {
            return null;
        } else {
            return input[element];
        }
    }

    public static IRubyObject elementOrNil(IRubyObject[] input, int element, IRubyObject nil) {
        if (element >= input.length) {
            return nil;
        } else {
            return input[element];
        }
    }

    public static IRubyObject postElementOrNil(IRubyObject[] input, int postCount, int postIndex, IRubyObject nil) {
        int aryIndex = input.length - postCount + postIndex;
        if (aryIndex >= input.length || aryIndex < 0) {
            return nil;
        } else {
            return input[aryIndex];
        }
    }
    
    public static RubyBoolean isWhenTriggered(IRubyObject expression, IRubyObject expressionsObject, ThreadContext context) {
        RubyArray expressions = Helpers.splatValue(expressionsObject);
        for (int j = 0,k = expressions.getLength(); j < k; j++) {
            IRubyObject condition = expressions.eltInternal(j);

            if ((expression != null && condition.callMethod(context, "===", expression).isTrue()) ||
                    (expression == null && condition.isTrue())) {
                return context.runtime.getTrue();
            }
        }

        return context.runtime.getFalse();
    }
    
    public static IRubyObject setConstantInModule(ThreadContext context, String name, IRubyObject value, IRubyObject module) {
        if (!(module instanceof RubyModule)) {
            throw context.runtime.newTypeError(module.toString() + " is not a class/module");
        }
        ((RubyModule)module).setConstant(name, value);

        return value;
    }

    public static IRubyObject setConstantInCurrent(IRubyObject value, ThreadContext context, String name) {
        return context.getCurrentStaticScope().setConstant(name, value);
    }

    public static IRubyObject retryJump() {
        throw JumpException.RETRY_JUMP;
    }

    public static IRubyObject redoJump() {
        throw JumpException.REDO_JUMP;
    }

    public static IRubyObject redoLocalJumpError(Ruby runtime) {
        throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.REDO, runtime.getNil(), "unexpected redo");
    }

    public static IRubyObject nextJump(IRubyObject value) {
        throw new JumpException.NextJump(value);
    }
    
    public static IRubyObject nextLocalJumpError(Ruby runtime, IRubyObject value) {
        throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.NEXT, value, "unexpected next");
    }
    
    public static final int MAX_SPECIFIC_ARITY_OBJECT_ARRAY = 10;
    
    public static IRubyObject[] anewarrayIRubyObjects(int size) {
        return new IRubyObject[size];
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, int start) {
        ary[start] = one;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, int start) {
        ary[start] = one;
        ary[start+1] = two;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        ary[start+8] = nine;
        return ary;
    }
    
    public static IRubyObject[] aastoreIRubyObjects(IRubyObject[] ary, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten, int start) {
        ary[start] = one;
        ary[start+1] = two;
        ary[start+2] = three;
        ary[start+3] = four;
        ary[start+4] = five;
        ary[start+5] = six;
        ary[start+6] = seven;
        ary[start+7] = eight;
        ary[start+8] = nine;
        ary[start+9] = ten;
        return ary;
    }
    
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
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six) {
        return new IRubyObject[] {one, two, three, four, five, six};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven) {
        return new IRubyObject[] {one, two, three, four, five, six, seven};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight, nine};
    }
    
    public static IRubyObject[] constructObjectArray(IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten) {
        return new IRubyObject[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one) {
        return RubyArray.newArrayLight(runtime, one);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two) {
        return RubyArray.newArrayLight(runtime, one, two);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three) {
        return RubyArray.newArrayLight(runtime, one, two, three);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four) {
        return RubyArray.newArrayLight(runtime, one, two, three, four);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight, nine);
    }

    public static RubyArray constructRubyArray(Ruby runtime, IRubyObject one, IRubyObject two, IRubyObject three, IRubyObject four, IRubyObject five, IRubyObject six, IRubyObject seven, IRubyObject eight, IRubyObject nine, IRubyObject ten) {
        return RubyArray.newArrayLight(runtime, one, two, three, four, five, six, seven, eight, nine, ten);
    }
    
    public static String[] constructStringArray(String one) {
        return new String[] {one};
    }
    
    public static String[] constructStringArray(String one, String two) {
        return new String[] {one, two};
    }
    
    public static String[] constructStringArray(String one, String two, String three) {
        return new String[] {one, two, three};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four) {
        return new String[] {one, two, three, four};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four, String five) {
        return new String[] {one, two, three, four, five};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six) {
        return new String[] {one, two, three, four, five, six};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven) {
        return new String[] {one, two, three, four, five, six, seven};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight) {
        return new String[] {one, two, three, four, five, six, seven, eight};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight, String nine) {
        return new String[] {one, two, three, four, five, six, seven, eight, nine};
    }
    
    public static String[] constructStringArray(String one, String two, String three, String four, String five, String six, String seven, String eight, String nine, String ten) {
        return new String[] {one, two, three, four, five, six, seven, eight, nine, ten};
    }
    
    public static final int MAX_SPECIFIC_ARITY_HASH = 3;
    
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetCheckString(runtime, key1, value1);
        return hash;
    }
    
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetCheckString(runtime, key1, value1);
        hash.fastASetCheckString(runtime, key2, value2);
        return hash;
    }
    
    public static RubyHash constructHash(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2, IRubyObject key3, IRubyObject value3) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetCheckString(runtime, key1, value1);
        hash.fastASetCheckString(runtime, key2, value2);
        hash.fastASetCheckString(runtime, key3, value3);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime, IRubyObject key1, IRubyObject value1) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        return hash;
    }

    public static RubyHash constructSmallHash(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2, IRubyObject key3, IRubyObject value3) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        hash.fastASetSmallCheckString(runtime, key3, value3);
        return hash;
    }
    
    public static RubyHash constructHash19(Ruby runtime, IRubyObject key1, IRubyObject value1) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetCheckString(runtime, key1, value1);
        return hash;
    }
    
    public static RubyHash constructHash19(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetCheckString(runtime, key1, value1);
        hash.fastASetCheckString(runtime, key2, value2);
        return hash;
    }
    
    public static RubyHash constructHash19(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2, IRubyObject key3, IRubyObject value3) {
        RubyHash hash = RubyHash.newHash(runtime);
        hash.fastASetCheckString(runtime, key1, value1);
        hash.fastASetCheckString(runtime, key2, value2);
        hash.fastASetCheckString(runtime, key3, value3);
        return hash;
    }

    public static RubyHash constructSmallHash19(Ruby runtime, IRubyObject key1, IRubyObject value1) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        return hash;
    }

    public static RubyHash constructSmallHash19(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        return hash;
    }

    public static RubyHash constructSmallHash19(Ruby runtime, IRubyObject key1, IRubyObject value1, IRubyObject key2, IRubyObject value2, IRubyObject key3, IRubyObject value3) {
        RubyHash hash = RubyHash.newSmallHash(runtime);
        hash.fastASetSmallCheckString(runtime, key1, value1);
        hash.fastASetSmallCheckString(runtime, key2, value2);
        hash.fastASetSmallCheckString(runtime, key3, value3);
        return hash;
    }

    public static IRubyObject undefMethod(ThreadContext context, Object nameArg) {
        RubyModule module = context.getRubyClass();

        String name = (nameArg instanceof String) ?
            (String) nameArg : nameArg.toString();

        if (module == null) {
            throw context.runtime.newTypeError("No class to undef method '" + name + "'.");
        }

        module.undef(context, name);

        return context.runtime.getNil();
    }
    
    public static IRubyObject defineAlias(ThreadContext context, IRubyObject self, Object newNameArg, Object oldNameArg) {
        Ruby runtime = context.runtime;
        RubyModule module = context.getRubyClass();
   
        if (module == null || self instanceof RubyFixnum || self instanceof RubySymbol){
            throw runtime.newTypeError("no class to make alias");
        }

        String newName = (newNameArg instanceof String) ?
            (String) newNameArg : newNameArg.toString();
        String oldName = (oldNameArg instanceof String) ? 
            (String) oldNameArg : oldNameArg.toString();

        module.defineAlias(newName, oldName);
        module.callMethod(context, "method_added", runtime.newSymbol(newName));
   
        return runtime.getNil();
    }
    
    public static IRubyObject negate(IRubyObject value, Ruby runtime) {
        if (value.isTrue()) return runtime.getFalse();
        return runtime.getTrue();
    }
    
    public static IRubyObject stringOrNil(ByteList value, ThreadContext context) {
        if (value == null) return context.nil;
        return RubyString.newStringShared(context.runtime, value);
    }
    
    public static StaticScope preLoad(ThreadContext context, String[] varNames) {
        StaticScope staticScope = context.runtime.getStaticScopeFactory().newLocalScope(null, varNames);
        preLoadCommon(context, staticScope, false);

        return staticScope;
    }

    public static StaticScope preLoad(ThreadContext context, String scopeString, boolean wrap) {
        StaticScope staticScope = decodeScope(context, null, scopeString);
        preLoadCommon(context, staticScope, wrap);

        return staticScope;
    }

    public static void preLoadCommon(ThreadContext context, StaticScope staticScope, boolean wrap) {
        RubyClass objectClass = context.runtime.getObject();
        IRubyObject topLevel = context.runtime.getTopSelf();
        if (wrap) {
            staticScope.setModule(RubyModule.newModule(context.runtime));
        } else {
            staticScope.setModule(objectClass);
        }
        DynamicScope scope = DynamicScope.newDynamicScope(staticScope);

        // Each root node has a top-level scope that we need to push
        context.preScopedBody(scope);
        context.preNodeEval(objectClass, topLevel);
    }
    
    public static void postLoad(ThreadContext context) {
        context.postNodeEval();
        context.postScopedBody();
    }
    
    public static void registerEndBlock(Block block, Ruby runtime) {
        runtime.pushExitBlock(runtime.newProc(Block.Type.LAMBDA, block));
    }
    
    public static IRubyObject match3(RubyRegexp regexp, IRubyObject value, ThreadContext context) {
        if (value instanceof RubyString) {
            return regexp.op_match(context, value);
        } else {
            return value.callMethod(context, "=~", regexp);
        }
    }

    public static IRubyObject match3_19(RubyRegexp regexp, IRubyObject value, ThreadContext context) {
        if (value instanceof RubyString) {
            return regexp.op_match19(context, value);
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

    public static IRubyObject setLastLine(Ruby runtime, ThreadContext context, IRubyObject value) {
        return context.setLastLine(value);
    }

    public static IRubyObject getLastLine(Ruby runtime, ThreadContext context) {
        return context.getLastLine();
    }

    public static IRubyObject setBackref(Ruby runtime, ThreadContext context, IRubyObject value) {
        if (!value.isNil() && !(value instanceof RubyMatchData)) throw runtime.newTypeError(value, runtime.getMatchData());
        return context.setBackRef(value);
    }

    public static IRubyObject getBackref(Ruby runtime, ThreadContext context) {
        IRubyObject backref = context.getBackRef();
        if (backref instanceof RubyMatchData) ((RubyMatchData)backref).use();
        return backref;
    }
    
    public static IRubyObject preOpAsgnWithOrAnd(IRubyObject receiver, ThreadContext context, IRubyObject self, CallSite varSite) {
        return varSite.call(context, self, receiver);
    }
    
    public static IRubyObject postOpAsgnWithOrAnd(IRubyObject receiver, IRubyObject value, ThreadContext context, IRubyObject self, CallSite varAsgnSite) {
        varAsgnSite.call(context, self, receiver, value);
        return value;
    }
    
    public static IRubyObject opAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg, CallSite varSite, CallSite opSite, CallSite opAsgnSite) {
        IRubyObject var = varSite.call(context, self, receiver);
        IRubyObject result = opSite.call(context, self, var, arg);
        opAsgnSite.call(context, self, receiver, result);

        return result;
    }
    
    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, result);

        return result;
    }
    
    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver, arg);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, arg, result);

        return result;
    }
    
    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg1, IRubyObject arg2, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver, arg1, arg2);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, arg1, arg2, result);

        return result;
    }
    
    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver, arg1, arg2, arg3);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, new IRubyObject[] {arg1, arg2, arg3, result});

        return result;
    }
    
    public static IRubyObject opElementAsgnWithMethod(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite elementSite, CallSite opSite, CallSite elementAsgnSite) {
        IRubyObject var = elementSite.call(context, self, receiver);
        IRubyObject result = opSite.call(context, self, var, value);
        elementAsgnSite.call(context, self, receiver, appendToObjectArray(args, result));

        return result;
    }

    
    public static IRubyObject opElementAsgnWithOrPartTwoOneArg(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject arg, IRubyObject value, CallSite asetSite) {
        asetSite.call(context, self, receiver, arg, value);
        return value;
    }
    
    public static IRubyObject opElementAsgnWithOrPartTwoTwoArgs(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite asetSite) {
        asetSite.call(context, self, receiver, args[0], args[1], value);
        return value;
    }
    
    public static IRubyObject opElementAsgnWithOrPartTwoThreeArgs(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite asetSite) {
        asetSite.call(context, self, receiver, new IRubyObject[] {args[0], args[1], args[2], value});
        return value;
    }
    
    public static IRubyObject opElementAsgnWithOrPartTwoNArgs(ThreadContext context, IRubyObject self, IRubyObject receiver, IRubyObject[] args, IRubyObject value, CallSite asetSite) {
        IRubyObject[] newArgs = new IRubyObject[args.length + 1];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[args.length] = value;
        asetSite.call(context, self, receiver, newArgs);
        return value;
    }

    public static RubyArray arrayValue(IRubyObject value) {
        Ruby runtime = value.getRuntime();
        return arrayValue(runtime.getCurrentContext(), runtime, value);
    }
    
    public static RubyArray arrayValue(ThreadContext context, Ruby runtime, IRubyObject value) {
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
            // remove this hack too.

            if (value.respondsTo("to_a") && value.getMetaClass().searchMethod("to_a").getImplementationClass() != runtime.getKernel()) {
                IRubyObject avalue = value.callMethod(context, "to_a");
                if (!(avalue instanceof RubyArray)) {
                    if (avalue.isNil()) {
                        return runtime.newArray(value);
                    } else {
                        throw runtime.newTypeError("`to_a' did not return Array");
                    }
                }
                return (RubyArray)avalue;
            } else {
                return runtime.newArray(value);
            }
        }
        RubyArray arr = (RubyArray) tmp;
        
        return arr.aryDup19();
    }

    public static RubyArray asArray18(ThreadContext context, IRubyObject value) {
        Ruby runtime = context.runtime;
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
            // remove this hack too.

            if (value.respondsTo("to_a") && value.getMetaClass().searchMethod("to_a").getImplementationClass() != runtime.getKernel()) {
                IRubyObject avalue = value.callMethod(context, "to_a");
                if (!(avalue instanceof RubyArray)) {
                    if (avalue.isNil()) {
                        return runtime.newArray(value);
                    } else {
                        throw runtime.newTypeError("`to_a' did not return Array");
                    }
                }
                return (RubyArray)avalue;
            } else {
                return runtime.newArray(value);
            }
        }
        
        return (RubyArray) tmp;
    }
    
    // mri: rb_Array
    // FIXME: Replace arrayValue/asArray18 with this on 9k (currently dead -- respond_to? logic broken further down the line -- fix that first)
    public static RubyArray asArray(ThreadContext context, IRubyObject value) {
        RubyClass array = context.runtime.getArray();
        IRubyObject tmp = TypeConverter.convertToTypeWithCheck19(value, array, "to_ary");
        
        if (tmp.isNil()) {
            tmp = TypeConverter.convertToTypeWithCheck19(value, array, "to_a");

            if (tmp.isNil()) return context.runtime.newEmptyArray();
        }
        
        return (RubyArray) tmp; // converters will guarantee it is RubyArray or raise.
    }
    
    public static IRubyObject aryToAry(IRubyObject value) {
        if (value instanceof RubyArray) return value;

        if (value.respondsTo("to_ary")) {
            return TypeConverter.convertToType(value, value.getRuntime().getArray(), "to_ary", false);
        }

        return value.getRuntime().newArray(value);
    }

    public static IRubyObject aValueSplat(IRubyObject value) {
        if (!(value instanceof RubyArray) || ((RubyArray) value).length().getLongValue() == 0) {
            return value.getRuntime().getNil();
        }

        RubyArray array = (RubyArray) value;

        return array.getLength() == 1 ? array.first() : array;
    }

    public static IRubyObject aValueSplat19(IRubyObject value) {
        if (!(value instanceof RubyArray)) {
            return value.getRuntime().getNil();
        }

        return (RubyArray) value;
    }

    public static RubyArray splatValue(IRubyObject value) {
        if (value.isNil()) {
            return value.getRuntime().newArray(value);
        }

        return arrayValue(value);
    }

    public static RubyArray splatValue19(IRubyObject value) {
        if (value.isNil()) {
            return value.getRuntime().newEmptyArray();
        }

        return arrayValue(value);
    }
    
    public static IRubyObject unsplatValue19(IRubyObject argsResult) {
        if (argsResult instanceof RubyArray) {
            RubyArray array = (RubyArray) argsResult;
                    
            if (array.size() == 1) {
                IRubyObject newResult = array.eltInternal(0);
                if (!((newResult instanceof RubyArray) && ((RubyArray) newResult).size() == 0)) {
                    argsResult = newResult;
                }
            }
        }        
        return argsResult;
    }

    public static IRubyObject unsplatValue19IfArityOne(IRubyObject argsResult, Block block) {
        if (block.isGiven() && block.arity().getValue() > 1) argsResult = Helpers.unsplatValue19(argsResult);
        return argsResult;
    }
        
    public static IRubyObject[] splatToArguments(IRubyObject value) {
        Ruby runtime = value.getRuntime();
        
        if (value.isNil()) {
            return runtime.getSingleNilArray();
        }
        
        return splatToArgumentsCommon(runtime, value);
    }
    
    public static IRubyObject[] splatToArguments19(IRubyObject value) {
        Ruby runtime = value.getRuntime();
        
        if (value.isNil()) {
            return IRubyObject.NULL_ARRAY;
        }
        
        return splatToArgumentsCommon(runtime, value);
    }
    
    private static IRubyObject[] splatToArgumentsCommon(Ruby runtime, IRubyObject value) {
        
        if (value.isNil()) {
            return runtime.getSingleNilArray();
        }
        
        IRubyObject tmp = value.checkArrayType();

        if (tmp.isNil()) {
            return convertSplatToJavaArray(runtime, value);
        }
        return ((RubyArray)tmp).toJavaArrayMaybeUnsafe();
    }
    
    private static IRubyObject[] convertSplatToJavaArray(Ruby runtime, IRubyObject value) {
        // Object#to_a is obsolete.  We match Ruby's hack until to_a goes away.  Then we can
        // remove this hack too.

        RubyClass metaClass = value.getMetaClass();
        DynamicMethod method = metaClass.searchMethod("to_a");
        if (method.isUndefined() || method.getImplementationClass() == runtime.getKernel()) {
            return new IRubyObject[] {value};
        }

        IRubyObject avalue = method.call(runtime.getCurrentContext(), value, metaClass, "to_a");
        if (!(avalue instanceof RubyArray)) {
            if (avalue.isNil()) {
                return new IRubyObject[] {value};
            } else {
                throw runtime.newTypeError("`to_a' did not return Array");
            }
        }
        return ((RubyArray)avalue).toJavaArray();
    }
    
    public static IRubyObject[] argsCatToArguments(IRubyObject[] args, IRubyObject cat) {
        IRubyObject[] ary = splatToArguments(cat);
        return argsCatToArgumentsCommon(args, ary);
    }
    
    public static IRubyObject[] argsCatToArguments19(IRubyObject[] args, IRubyObject cat) {
        IRubyObject[] ary = splatToArguments19(cat);
        return argsCatToArgumentsCommon(args, ary);
    }
    
    private static IRubyObject[] argsCatToArgumentsCommon(IRubyObject[] args, IRubyObject[] ary) {
        if (ary.length > 0) {
            IRubyObject[] newArgs = new IRubyObject[args.length + ary.length];
            System.arraycopy(args, 0, newArgs, 0, args.length);
            System.arraycopy(ary, 0, newArgs, args.length, ary.length);
            args = newArgs;
        }
        
        return args;
    }

    public static void addInstanceMethod(RubyModule containingClass, String name, DynamicMethod method, Visibility visibility, ThreadContext context, Ruby runtime) {
        containingClass.addMethod(name, method);

        RubySymbol sym = runtime.fastNewSymbol(name);
        if (visibility == Visibility.MODULE_FUNCTION) {
            addModuleMethod(containingClass, name, method, context, sym);
        }

        callNormalMethodHook(containingClass, context, sym);
    }

    private static void addModuleMethod(RubyModule containingClass, String name, DynamicMethod method, ThreadContext context, RubySymbol sym) {
        containingClass.getSingletonClass().addMethod(name, new WrapperMethod(containingClass.getSingletonClass(), method, Visibility.PUBLIC));
        containingClass.callMethod(context, "singleton_method_added", sym);
    }

    private static void callNormalMethodHook(RubyModule containingClass, ThreadContext context, RubySymbol name) {
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (containingClass.isSingleton()) {
            callSingletonMethodHook(((MetaClass) containingClass).getAttached(), context, name);
        } else {
            containingClass.callMethod(context, "method_added", name);
        }
    }

    private static void callSingletonMethodHook(IRubyObject receiver, ThreadContext context, RubySymbol name) {
        receiver.callMethod(context, "singleton_method_added", name);
    }

    private static DynamicMethod constructNormalMethod(
            MethodFactory factory,
            String javaName,
            String name,
            RubyModule containingClass,
            ISourcePosition position,
            int arity,
            StaticScope scope,
            Visibility visibility,
            Object scriptObject,
            CallConfiguration callConfig,
            String parameterDesc) {
        
        DynamicMethod method;

        if (name.equals("initialize") || name.equals("initialize_copy") || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }
        
        if (RubyInstanceConfig.LAZYHANDLES_COMPILE) {
            method = factory.getCompiledMethodLazily(
                    containingClass,
                    name,
                    javaName,
                    Arity.createArity(arity),
                    visibility,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc);
        } else {
            method = factory.getCompiledMethod(
                    containingClass,
                    name,
                    javaName,
                    Arity.createArity(arity),
                    visibility,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc);
        }

        return method;
    }

    private static DynamicMethod constructSingletonMethod(
            MethodFactory factory,
            String rubyName,
            String javaName,
            RubyClass rubyClass,
            ISourcePosition position,
            int arity,
            StaticScope scope,
            Object scriptObject,
            CallConfiguration callConfig,
            String parameterDesc) {
        
        if (RubyInstanceConfig.LAZYHANDLES_COMPILE) {
            return factory.getCompiledMethodLazily(
                    rubyClass,
                    rubyName,
                    javaName,
                    Arity.createArity(arity),
                    Visibility.PUBLIC,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc);
        } else {
            return factory.getCompiledMethod(
                    rubyClass,
                    rubyName,
                    javaName,
                    Arity.createArity(arity),
                    Visibility.PUBLIC,
                    scope,
                    scriptObject,
                    callConfig,
                    position,
                    parameterDesc);
        }
    }

    public static String encodeScope(StaticScope scope) {
        StringBuilder namesBuilder = new StringBuilder(scope.getType().name());

        namesBuilder.append(',');

        boolean first = true;
        for (String name : scope.getVariables()) {
            if (!first) namesBuilder.append(';');
            first = false;
            namesBuilder.append(name);
        }
        namesBuilder
                .append(',')
                .append(scope.getRequiredArgs())
                .append(',')
                .append(scope.getOptionalArgs())
                .append(',')
                .append(scope.getRestArg());

        return namesBuilder.toString();
    }

    @Deprecated
    public static StaticScope decodeRootScope(ThreadContext context, String scopeString) {
        return decodeScope(context, null, scopeString);
    }

    @Deprecated
    public static StaticScope decodeLocalScope(ThreadContext context, String scopeString) {
        return decodeScope(context, context.getCurrentStaticScope(), scopeString);
    }

    @Deprecated
    public static StaticScope decodeLocalScope(ThreadContext context, StaticScope parent, String scopeString) {
        return decodeScope(context, parent, scopeString);
    }

    @Deprecated
    public static StaticScope decodeBlockScope(ThreadContext context, String scopeString) {
        return decodeScope(context, context.getCurrentStaticScope(), scopeString);
    }

    public static StaticScope decodeScope(ThreadContext context, StaticScope parent, String scopeString) {
        String[][] decodedScope = decodeScopeDescriptor(scopeString);
        StaticScope scope = null;
        switch (StaticScope.Type.valueOf(decodedScope[0][0])) {
            case BLOCK:
                scope = context.runtime.getStaticScopeFactory().newBlockScope(parent, decodedScope[1]);
                break;
            case EVAL:
                scope = context.runtime.getStaticScopeFactory().newEvalScope(parent, decodedScope[1]);
                break;
            case LOCAL:
                scope = context.runtime.getStaticScopeFactory().newLocalScope(parent, decodedScope[1]);
                break;
        }
        setAritiesFromDecodedScope(scope, decodedScope[0]);
        return scope;
    }

    private static String[][] decodeScopeDescriptor(String scopeString) {
        String[] scopeElements = scopeString.split(",");
        String[] scopeNames = scopeElements[1].length() == 0 ? new String[0] : getScopeNames(scopeElements[1]);
        return new String[][] {scopeElements, scopeNames};
    }

    private static void setAritiesFromDecodedScope(StaticScope scope, String[] scopeElements) {
        scope.setArities(Integer.parseInt(scopeElements[2]), Integer.parseInt(scopeElements[3]), Integer.parseInt(scopeElements[4]));
    }

    public static StaticScope decodeScopeAndDetermineModule(ThreadContext context, StaticScope parent, String scopeString) {
        StaticScope scope = decodeScope(context, parent, scopeString);
        scope.determineModule();

        return scope;
    }

    private static void performNormalMethodChecks(RubyModule containingClass, Ruby runtime, String name) throws RaiseException {

        if (containingClass == runtime.getDummy()) {
            throw runtime.newTypeError("no class/module to add method");
        }

        if (containingClass == runtime.getObject() && name.equals("initialize")) {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining Object#initialize may cause infinite loop");
        }

        if (name.equals("__id__") || name.equals("__send__")) {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining `" + name + "' may cause serious problem");
        }
    }

    public static RubyClass performSingletonMethodChecks(Ruby runtime, IRubyObject receiver, String name) throws RaiseException {
        if (receiver instanceof RubyFixnum || receiver instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + name + "\" for " + receiver.getMetaClass().getBaseName());
        }

        if (receiver.isFrozen()) {
            throw runtime.newFrozenError("object");
        }
        
        RubyClass rubyClass = receiver.getSingletonClass();
        
        return rubyClass;
    }
    
    public static IRubyObject arrayEntryOrNil(RubyArray array, int index) {
        if (index < array.getLength()) {
            return array.eltInternal(index);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayEntryOrNilZero(RubyArray array) {
        if (0 < array.getLength()) {
            return array.eltInternal(0);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayEntryOrNilOne(RubyArray array) {
        if (1 < array.getLength()) {
            return array.eltInternal(1);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayEntryOrNilTwo(RubyArray array) {
        if (2 < array.getLength()) {
            return array.eltInternal(2);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNil(RubyArray array, int pre, int post, int index) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + index);
        } else if (pre + index < array.getLength()) {
            return array.eltInternal(pre + index);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNilZero(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 0);
        } else if (pre + 0 < array.getLength()) {
            return array.eltInternal(pre + 0);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNilOne(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 1);
        } else if (pre + 1 < array.getLength()) {
            return array.eltInternal(pre + 1);
        } else {
            return array.getRuntime().getNil();
        }
    }

    public static IRubyObject arrayPostOrNilTwo(RubyArray array, int pre, int post) {
        if (pre + post < array.getLength()) {
            return array.eltInternal(array.getLength() - post + 2);
        } else if (pre + 2 < array.getLength()) {
            return array.eltInternal(pre + 2);
        } else {
            return array.getRuntime().getNil();
        }
    }
    
    public static RubyArray subarrayOrEmpty(RubyArray array, Ruby runtime, int index) {
        if (index < array.getLength()) {
            return createSubarray(array, index);
        } else {
            return RubyArray.newEmptyArray(runtime);
        }
    }

    public static RubyArray subarrayOrEmpty(RubyArray array, Ruby runtime, int index, int post) {
        if (index + post < array.getLength()) {
            return createSubarray(array, index, post);
        } else {
            return RubyArray.newEmptyArray(runtime);
        }
    }
    
    public static RubyModule checkIsModule(IRubyObject maybeModule) {
        if (maybeModule instanceof RubyModule) return (RubyModule)maybeModule;
        
        throw maybeModule.getRuntime().newTypeError(maybeModule + " is not a class/module");
    }
    
    public static IRubyObject getGlobalVariable(Ruby runtime, String name) {
        return runtime.getGlobalVariables().get(name);
    }
    
    public static IRubyObject setGlobalVariable(IRubyObject value, Ruby runtime, String name) {
        return runtime.getGlobalVariables().set(name, value);
    }

    public static IRubyObject getInstanceVariable(IRubyObject self, Ruby runtime, String internedName) {
        IRubyObject result = self.getInstanceVariables().getInstanceVariable(internedName);
        if (result != null) return result;
        if (runtime.isVerbose()) warnAboutUninitializedIvar(runtime, internedName);
        return runtime.getNil();
    }

    private static void warnAboutUninitializedIvar(Ruby runtime, String internedName) {
        runtime.getWarnings().warning(ID.IVAR_NOT_INITIALIZED, "instance variable " + internedName + " not initialized");
    }

    public static IRubyObject setInstanceVariable(IRubyObject value, IRubyObject self, String name) {
        return self.getInstanceVariables().setInstanceVariable(name, value);
    }

    public static RubyProc newLiteralLambda(ThreadContext context, Block block, IRubyObject self) {
        return RubyProc.newProc(context.runtime, block, Block.Type.LAMBDA);
    }

    public static void fillNil(IRubyObject[]arr, int from, int to, Ruby runtime) {
        if (arr.length == 0) return;
        IRubyObject nils[] = runtime.getNilPrefilledArray();
        int i;

        for (i = from; i + Ruby.NIL_PREFILLED_ARRAY_SIZE < to; i += Ruby.NIL_PREFILLED_ARRAY_SIZE) {
            System.arraycopy(nils, 0, arr, i, Ruby.NIL_PREFILLED_ARRAY_SIZE);
        }
        System.arraycopy(nils, 0, arr, i, to - i);
    }

    public static void fillNil(IRubyObject[]arr, Ruby runtime) {
        if (arr.length == 0) return;
        fillNil(arr, 0, arr.length, runtime);
    }

    public static boolean isFastSwitchableString(IRubyObject str) {
        return str instanceof RubyString;
    }

    public static boolean isFastSwitchableSingleCharString(IRubyObject str) {
        return str instanceof RubyString && ((RubyString)str).getByteList().length() == 1;
    }

    public static int getFastSwitchString(IRubyObject str) {
        ByteList byteList = ((RubyString)str).getByteList();
        return byteList.hashCode();
    }

    public static int getFastSwitchSingleCharString(IRubyObject str) {
        ByteList byteList = ((RubyString)str).getByteList();
        return byteList.get(0);
    }

    public static boolean isFastSwitchableSymbol(IRubyObject sym) {
        return sym instanceof RubySymbol;
    }

    public static boolean isFastSwitchableSingleCharSymbol(IRubyObject sym) {
        return sym instanceof RubySymbol && ((RubySymbol)sym).asJavaString().length() == 1;
    }

    public static int getFastSwitchSymbol(IRubyObject sym) {
        String str = ((RubySymbol)sym).asJavaString();
        return str.hashCode();
    }

    public static int getFastSwitchSingleCharSymbol(IRubyObject sym) {
        String str = ((RubySymbol)sym).asJavaString();
        return (int)str.charAt(0);
    }

    public static Block getBlock(ThreadContext context, IRubyObject self, Node node) {
        IterNode iter = (IterNode)node;
        iter.getScope().determineModule();

        // Create block for this iter node
        // FIXME: We shouldn't use the current scope if it's not actually from the same hierarchy of static scopes
        if (iter.getBlockBody() instanceof InterpretedBlock) {
            return InterpretedBlock.newInterpretedClosure(context, iter.getBlockBody(), self);
        } else {
            return Interpreted19Block.newInterpretedClosure(context, iter.getBlockBody(), self);
        }
    }

    public static Block getBlock(Ruby runtime, ThreadContext context, IRubyObject self, Node node, Block aBlock) {
        return Helpers.getBlockFromBlockPassBody(runtime, node.interpret(runtime, context, self, aBlock), aBlock);
    }

    /**
     * Equivalent to rb_equal in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEqual(ThreadContext context, IRubyObject a, IRubyObject b) {
        Ruby runtime = context.runtime;
        if (a == b) return runtime.getTrue();
        IRubyObject res = invokedynamic(context, a, OP_EQUAL, b);
        return runtime.newBoolean(res.isTrue());
    }

    /**
     * Equivalent to rb_eql in MRI
     *
     * @param context
     * @param a
     * @param b
     * @return
     */
    public static RubyBoolean rbEql(ThreadContext context, IRubyObject a, IRubyObject b) {
        Ruby runtime = context.runtime;
        if (a == b) return runtime.getTrue();
        IRubyObject res = invokedynamic(context, a, EQL, b);
        return runtime.newBoolean(res.isTrue());
    }

    public static void traceLine(ThreadContext context) {
        String name = context.getFrameName();
        RubyModule type = context.getFrameKlazz();
        context.runtime.callEventHooks(context, RubyEvent.LINE, context.getFile(), context.getLine(), name, type);
    }

    public static void traceClass(ThreadContext context) {
        String name = context.getFrameName();
        RubyModule type = context.getFrameKlazz();
        context.runtime.callEventHooks(context, RubyEvent.CLASS, context.getFile(), context.getLine(), name, type);
    }

    public static void traceEnd(ThreadContext context) {
        String name = context.getFrameName();
        RubyModule type = context.getFrameKlazz();
        context.runtime.callEventHooks(context, RubyEvent.END, context.getFile(), context.getLine(), name, type);
    }

    /**
     * Some of this code looks scary.  All names for an alias or undef is a
     * fitem in 1.8/1.9 grammars.  This means it is guaranteed to be either
     * a LiteralNode of a DSymbolNode.  Nothing else is possible.  Also
     * Interpreting a DSymbolNode will always yield a RubySymbol.
     */
    public static String interpretAliasUndefName(Node nameNode, Ruby runtime,
            ThreadContext context, IRubyObject self, Block aBlock) {
        String name;

        if (nameNode instanceof LiteralNode) {
            name = ((LiteralNode) nameNode).getName();
        } else {
            assert nameNode instanceof DSymbolNode: "Alias or Undef not literal or dsym";
            name = ((RubySymbol) nameNode.interpret(runtime, context, self, aBlock)).asJavaString();
        }

        return name;
    }

    /**
     * Used by the compiler to simplify arg checking in variable-arity paths
     *
     * @param context thread context
     * @param args arguments array
     * @param min minimum required
     * @param max maximum allowed
     */
    public static void checkArgumentCount(ThreadContext context, IRubyObject[] args, int min, int max) {
        checkArgumentCount(context, args.length, min, max);
    }

    /**
     * Used by the compiler to simplify arg checking in variable-arity paths
     *
     * @param context thread context
     * @param args arguments array
     * @param req required number
     */
    public static void checkArgumentCount(ThreadContext context, IRubyObject[] args, int req) {
        checkArgumentCount(context, args.length, req, req);
    }
    
    public static void checkArgumentCount(ThreadContext context, int length, int min, int max) {
        int expected = 0;
        if (length < min) {
            expected = min;
        } else if (max > -1 && length > max) {
            expected = max;
        } else {
            return;
        }
        throw context.runtime.newArgumentError(length, expected);
    }

    public static boolean isModuleAndHasConstant(IRubyObject left, String name) {
        return left instanceof RubyModule && ((RubyModule) left).getConstantFromNoConstMissing(name, false) != null;
    }

    public static RubyString getDefinedConstantOrBoundMethod(IRubyObject left, String name) {
        if (isModuleAndHasConstant(left, name)) return left.getRuntime().getDefinedMessage(DefinedMessage.CONSTANT);
        if (left.getMetaClass().isMethodBound(name, true)) left.getRuntime().getDefinedMessage(DefinedMessage.METHOD);
        return null;
    }

    public static RubyModule getSuperClassForDefined(Ruby runtime, RubyModule klazz) {
        RubyModule superklazz = klazz.getSuperClass();

        if (superklazz == null && klazz.isModule()) superklazz = runtime.getObject();

        return superklazz;
    }

    public static boolean isGenerationEqual(IRubyObject object, int generation) {
        RubyClass metaClass;
        if (object instanceof RubyBasicObject) {
            metaClass = ((RubyBasicObject)object).getMetaClass();
        } else {
            metaClass = object.getMetaClass();
        }
        return metaClass.getGeneration() == generation;
    }

    public static String[] getScopeNames(String scopeNames) {
        StringTokenizer toker = new StringTokenizer(scopeNames, ";");
        ArrayList list = new ArrayList(10);
        while (toker.hasMoreTokens()) {
            list.add(toker.nextToken().intern());
        }
        return (String[])list.toArray(new String[list.size()]);
    }

    public static IRubyObject[] arraySlice1N(IRubyObject arrayish) {
        arrayish = aryToAry(arrayish);
        RubyArray arrayish2 = ensureMultipleAssignableRubyArray(arrayish, arrayish.getRuntime(), true);
        return new IRubyObject[] {arrayEntryOrNilZero(arrayish2), subarrayOrEmpty(arrayish2, arrayish2.getRuntime(), 1)};
    }

    public static IRubyObject arraySlice1(IRubyObject arrayish) {
        arrayish = aryToAry(arrayish);
        RubyArray arrayish2 = ensureMultipleAssignableRubyArray(arrayish, arrayish.getRuntime(), true);
        return arrayEntryOrNilZero(arrayish2);
    }

    public static RubyClass metaclass(IRubyObject object) {
        return object instanceof RubyBasicObject ?
            ((RubyBasicObject)object).getMetaClass() :
            object.getMetaClass();
    }

    public static String rawBytesToString(byte[] bytes) {
        // stuff bytes into chars
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) chars[i] = (char)bytes[i];
        return new String(chars);
    }

    public static byte[] stringToRawBytes(String string) {
        char[] chars = string.toCharArray();
        byte[] bytes = new byte[chars.length];
        for (int i = 0; i < chars.length; i++) bytes[i] = (byte)chars[i];
        return bytes;
    }

    public static String encodeCaptureOffsets(int[] scopeOffsets) {
        char[] encoded = new char[scopeOffsets.length * 2];
        for (int i = 0; i < scopeOffsets.length; i++) {
            int offDepth = scopeOffsets[i];
            char off = (char)(offDepth & 0xFFFF);
            char depth = (char)(offDepth >> 16);
            encoded[2 * i] = off;
            encoded[2 * i + 1] = depth;
        }
        return new String(encoded);
    }

    public static int[] decodeCaptureOffsets(String encoded) {
        char[] chars = encoded.toCharArray();
        int[] scopeOffsets = new int[chars.length / 2];
        for (int i = 0; i < scopeOffsets.length; i++) {
            char off = chars[2 * i];
            char depth = chars[2 * i + 1];
            scopeOffsets[i] = (((int)depth) << 16) | (int)off;
        }
        return scopeOffsets;
    }

    public static IRubyObject match2AndUpdateScope(IRubyObject receiver, ThreadContext context, IRubyObject value, String scopeOffsets) {
        IRubyObject match = ((RubyRegexp)receiver).op_match(context, value);
        updateScopeWithCaptures(context, decodeCaptureOffsets(scopeOffsets), match);
        return match;
    }

    public static IRubyObject match2AndUpdateScope19(IRubyObject receiver, ThreadContext context, IRubyObject value, String scopeOffsets) {
        IRubyObject match = ((RubyRegexp)receiver).op_match19(context, value);
        updateScopeWithCaptures(context, decodeCaptureOffsets(scopeOffsets), match);
        return match;
    }

    public static void updateScopeWithCaptures(ThreadContext context, int[] scopeOffsets, IRubyObject result) {
        Ruby runtime = context.runtime;
        if (result.isNil()) { // match2 directly calls match so we know we can count on result
            IRubyObject nil = runtime.getNil();

            for (int i = 0; i < scopeOffsets.length; i++) {
                context.getCurrentScope().setValue(nil, scopeOffsets[i], 0);
            }
        } else {
            RubyMatchData matchData = (RubyMatchData)context.getBackRef();
            // FIXME: Mass assignment is possible since we know they are all locals in the same
            //   scope that are also contiguous
            IRubyObject[] namedValues = matchData.getNamedBackrefValues(runtime);

            for (int i = 0; i < scopeOffsets.length; i++) {
                context.getCurrentScope().setValue(namedValues[i], scopeOffsets[i] & 0xffff, scopeOffsets[i] >> 16);
            }
        }
    }

    public static RubyArray argsPush(RubyArray first, IRubyObject second) {
        return ((RubyArray)first.dup()).append(second);
    }

    public static RubyArray argsCat(IRubyObject first, IRubyObject second) {
        Ruby runtime = first.getRuntime();
        IRubyObject secondArgs;
        secondArgs = Helpers.splatValue19(second);

        return ((RubyArray) Helpers.ensureRubyArray(runtime, first).dup()).concat(secondArgs);
    }

    public static String encodeParameterList(ArgsNode argsNode) {
        StringBuilder builder = new StringBuilder();
        
        boolean added = false;
        if (argsNode.getPre() != null) {
            for (Node preNode : argsNode.getPre().childNodes()) {
                if (added) builder.append(';');
                added = true;
                if (preNode instanceof MultipleAsgn19Node) {
                    builder.append("nil");
                } else {
                    builder.append("q").append(((ArgumentNode)preNode).getName());
                }
            }
        }

        if (argsNode.getOptArgs() != null) {
            for (Node optNode : argsNode.getOptArgs().childNodes()) {
                if (added) builder.append(';');
                added = true;
                builder.append("o");
                if (optNode instanceof OptArgNode) {
                    builder.append(((OptArgNode)optNode).getName());
                } else if (optNode instanceof LocalAsgnNode) {
                    builder.append(((LocalAsgnNode)optNode).getName());
                } else if (optNode instanceof DAsgnNode) {
                    builder.append(((DAsgnNode)optNode).getName());
                }
            }
        }

        if (argsNode.getRestArg() >= 0) {
            if (added) builder.append(';');
            added = true;
            if (argsNode.getRestArgNode() instanceof UnnamedRestArgNode) {
                if (((UnnamedRestArgNode) argsNode.getRestArgNode()).isStar()) builder.append("R");
            } else {
                builder.append("r").append(argsNode.getRestArgNode().getName());
            }
        }

        if (argsNode.getPost() != null) {
            for (Node postNode : argsNode.getPost().childNodes()) {
                if (added) builder.append(';');
                added = true;
                if (postNode instanceof MultipleAsgn19Node) {
                    builder.append("nil");
                } else {
                    builder.append("q").append(((ArgumentNode)postNode).getName());
                }
            }
        }

        if (argsNode.getBlock() != null) {
            if (added) builder.append(';');
            added = true;
            builder.append("b").append(argsNode.getBlock().getName());
        }

        if (!added) builder.append("NONE");

        return builder.toString();
    }

    public static RubyArray parameterListToParameters(Ruby runtime, String[] parameterList, boolean isLambda) {
        RubyArray parms = RubyArray.newEmptyArray(runtime);

        for (String param : parameterList) {
            if (param.equals("NONE")) break;

            RubyArray elem = RubyArray.newEmptyArray(runtime);
            if (param.equals("nil")) {
                // marker for masgn args (the parens in "a, b, (c, d)"
                elem.add(RubySymbol.newSymbol(runtime, isLambda ? "req" : "opt"));
                parms.add(elem);
                continue;
            }

            if (param.length() == 0) System.out.println(Arrays.toString(parameterList));
            if (param.charAt(0) == 'q') {
                // required/normal arg
                elem.add(RubySymbol.newSymbol(runtime, isLambda ? "req" : "opt"));
            } else if (param.charAt(0) == 'r') {
                // named rest arg
                elem.add(RubySymbol.newSymbol(runtime, "rest"));
            } else if (param.charAt(0) == 'R') {
                // unnamed rest arg (star)
                elem.add(RubySymbol.newSymbol(runtime, "rest"));
                parms.add(elem);
                continue;
            } else if (param.charAt(0) == 'o') {
                // optional arg
                elem.add(RubySymbol.newSymbol(runtime, "opt"));
                if (param.length() == 1) {
                    // no name; continue
                    parms.add(elem);
                    continue;
                }
            } else if (param.charAt(0) == 'b') {
                // block arg
                elem.add(RubySymbol.newSymbol(runtime, "block"));
            }
            
            if (param.length() > 1) {
                elem.add(RubySymbol.newSymbol(runtime, param.substring(1)));
            }
            
            parms.add(elem);
        }

        return parms;
    }

    public static RubyString getDefinedCall(ThreadContext context, IRubyObject self, IRubyObject receiver, String name) {
        RubyClass metaClass = receiver.getMetaClass();
        DynamicMethod method = metaClass.searchMethod(name);
        Visibility visibility = method.getVisibility();

        if (visibility != Visibility.PRIVATE &&
                (visibility != Visibility.PROTECTED || metaClass.getRealClass().isInstance(self)) && !method.isUndefined()) {
            return context.runtime.getDefinedMessage(DefinedMessage.METHOD);
        }

        if (receiver.callMethod(context, "respond_to_missing?",
            new IRubyObject[]{context.runtime.newSymbol(name), context.runtime.getFalse()}).isTrue()) {
            return context.runtime.getDefinedMessage(DefinedMessage.METHOD);
        }
        return null;
    }

    public static RubyString getDefinedNot(Ruby runtime, RubyString definition) {
        if (definition != null) {
            definition = runtime.getDefinedMessage(DefinedMessage.METHOD);
        }

        return definition;
    }

    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, MethodNames method) {
        RubyClass metaclass = self.getMetaClass();
        String name = method.realName();
        return getMethodCached(context, metaclass, method.ordinal(), name).call(context, self, metaclass, name);
    }

    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, MethodNames method, IRubyObject arg0) {
        RubyClass metaclass = self.getMetaClass();
        String name = method.realName();
        return getMethodCached(context, metaclass, method.ordinal(), name).call(context, self, metaclass, name, arg0);
    }
    
    private static DynamicMethod getMethodCached(ThreadContext context, RubyClass metaclass, int index, String name) {
        if (metaclass.getClassIndex() == ClassIndex.NO_INDEX) return metaclass.searchMethod(name);
        return context.runtimeCache.getMethod(context, metaclass, metaclass.getClassIndex().ordinal() * (index + 1), name);
    }
    
    public static IRubyObject lastElement(IRubyObject[] ary) {
        return ary[ary.length - 1];
    }
    
    public static RubyString appendAsString(RubyString target, IRubyObject other) {
        return target.append(other.asString());
    }
    
    public static RubyString appendAsString19(RubyString target, IRubyObject other) {
        return target.append19(other.asString());
    }

    /**
     * We need to splat incoming array to a block when |a, *b| (any required +
     * rest) or |a, b| (>1 required).
     */
    public static boolean needsSplat19(int requiredCount, boolean isRest) {
        return (isRest && requiredCount > 0) || (!isRest && requiredCount > 1);
    }

    // . Array given to rest should pass itself
    // . Array with rest + other args should extract array
    // . Array with multiple values and NO rest should extract args if there are more than one argument
    // Note: In 1.9 alreadyArray is only relevent from our internal Java code in core libs.  We never use it
    // from interpreter or JIT.  FIXME: Change core lib consumers to stop using alreadyArray param.
    public static IRubyObject[] restructureBlockArgs19(IRubyObject value, Arity arity, Block.Type type, boolean needsSplat, boolean alreadyArray) {
        if (!type.checkArity && arity == Arity.NO_ARGUMENTS) return IRubyObject.NULL_ARRAY;
        
        if (value != null && !(value instanceof RubyArray) && needsSplat) value = Helpers.aryToAry(value);

        IRubyObject[] parameters;
        if (value == null) {
            parameters = IRubyObject.NULL_ARRAY;
        } else if (value instanceof RubyArray && (alreadyArray || needsSplat)) {
            parameters = ((RubyArray) value).toJavaArray();
        } else {
            parameters = new IRubyObject[] { value };
        }

        return parameters;
    }

    public static boolean BEQ(ThreadContext context, IRubyObject value1, IRubyObject value2) {
        return value1.op_equal(context, value2).isTrue();
    }

    public static boolean BNE(ThreadContext context, IRubyObject value1, IRubyObject value2) {
        boolean eql = value2 == context.nil || value2 == UndefinedValue.UNDEFINED ?
                value1 == value2 : value1.op_equal(context, value2).isTrue();

        return !eql;
    }

    public static RubyModule checkIsRubyModule(ThreadContext context, Object object) {
        if (!(object instanceof RubyModule)) {
            throw context.runtime.newTypeError("no outer class/module");
        }

        return (RubyModule)object;
    }

    public static IRubyObject invokeModuleBody(ThreadContext context, CompiledIRMethod method) {
        RubyModule implClass = method.getImplementationClass();

        return method.call(context, implClass, implClass, "");
    }

    public static RubyClass newClassForIR(ThreadContext context, String name, IRubyObject self, RubyModule classContainer, Object superClass, boolean meta) {
        if (meta) return classContainer.getMetaClass();

        RubyClass sc = null;

        if (superClass != null) {
            if (!(superClass instanceof RubyClass)) {
                throw context.runtime.newTypeError("superclass must be Class (" + superClass + " given)");
            }

            sc = (RubyClass) superClass;
        }


        return classContainer.defineOrGetClassUnder(name, sc);
    }

    public static RubyString appendByteList(RubyString target, ByteList source) {
        target.getByteList().append(source);
        return target;
    }

    public static RubyString appendByteList19(RubyString target, ByteList source, int codeRange) {
        target.cat19(source, codeRange);
        return target;
    }

    public static RubyString shortcutAppend18(RubyString string, IRubyObject object) {
        if (object instanceof RubyFixnum || object instanceof RubyFloat || object instanceof RubySymbol) {
            return string.append(object);
        } else {
            return string.append(object.asString());
        }
    }

    public static RubyString shortcutAppend(RubyString string, IRubyObject object) {
        if (object instanceof RubyFixnum || object instanceof RubyFloat || object instanceof RubySymbol) {
            return string.append19(object);
        } else {
            return string.append19(object.asString());
        }
    }

    public static void irCheckArgsArrayArity(ThreadContext context, RubyArray args, int required, int opt, int rest) {
        int numArgs = args.size();
        if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt)))) {
            Arity.raiseArgumentError(context.runtime, numArgs, required, required + opt);
        }
    }
    
    public static RubyArray irSplat(ThreadContext context, IRubyObject maybeAry) {
        return context.is19 ? splatValue19(maybeAry) : splatValue(maybeAry);
    }

    public static IRubyObject irToAry(ThreadContext context, IRubyObject receiver, boolean dontToAryArrays) {
        if (dontToAryArrays && receiver instanceof RubyArray) {
            return receiver;
        } else {
            IRubyObject ary = Helpers.aryToAry(receiver);
            if (ary instanceof RubyArray) {
                return ary;
            } else {
                String receiverType = receiver.getType().getName();
                throw context.runtime.newTypeError("can't convert " + receiverType + " to Array (" + receiverType + "#to_ary gives " + ary.getType().getName() + ")");
            }
        }
    }

    public static IRubyObject irReqdArgMultipleAsgn(ThreadContext context, RubyArray rubyArray, int preArgsCount, int index, int postArgsCount) {
        int n = rubyArray.getLength();
        if (preArgsCount == -1) {
            return rubyArray.entry(index);
        } else {
            int remaining = n - preArgsCount;
            if (remaining <= index) {
                return context.nil;
            } else {
                return (remaining > postArgsCount) ? rubyArray.entry(n - postArgsCount + index) : rubyArray.entry(preArgsCount + index);
            }
        }
    }

    public static IRubyObject irNot(ThreadContext context, IRubyObject obj) {
        return context.runtime.newBoolean(!(obj.isTrue()));
    }

    public static IRubyObject irLoadOptArg(int minReqd, int argIndex, IRubyObject[] args) {
        // FIXME: Missing kwargs 2.0 support (kwArgHashCount value)
        int kwArgHashCount = 0;
        if (minReqd + kwArgHashCount < args.length) {
            return args[argIndex];
        } else {
            return UndefinedValue.UNDEFINED;
        }
    }

    public static IRubyObject irPostReqdArg(int argIndex, int preReqdArgsCount, int postReqdArgsCount, IRubyObject[] args) {
        // FIXME: Missing kwargs 2.0 support (kwArgHashCount value)
        int kwArgHashCount = 0;
        int n = args.length;
        int remaining = n - preReqdArgsCount - kwArgHashCount;
        if (remaining <= argIndex) {
            return null;  // For blocks!
        } else {
            return (remaining > postReqdArgsCount) ? args[n - postReqdArgsCount - kwArgHashCount + argIndex] : args[preReqdArgsCount + argIndex];
        }
    }

    private static IRubyObject[] NO_PARAMS = new IRubyObject[0];
    public static IRubyObject irLoadRestArg(ThreadContext context, int minReqd, int argIndex, IRubyObject[] args) {
        // FIXME: Missing kwargs 2.0 support (kwArgHashCount value)
        int kwArgHashCount = 0;
        IRubyObject[] ret;
        int numAvailable = args.length - minReqd - kwArgHashCount;
        if (numAvailable <= 0) {
            ret = NO_PARAMS;
        } else {
            ret = new IRubyObject[numAvailable];
            System.arraycopy(args, argIndex, ret, 0, numAvailable);
        }
        return context.getRuntime().newArray(ret);
    }

    @Deprecated
    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, int index) {
        return invokedynamic(context, self, MethodNames.values()[index]);
    }

    @Deprecated
    public static IRubyObject invokedynamic(ThreadContext context, IRubyObject self, int index, IRubyObject arg0) {
        return invokedynamic(context, self, MethodNames.values()[index], arg0);
    }
    
    public static void throwException(final Throwable e) {
        Helpers.<RuntimeException>throwsUnchecked(e);
    }
    
    private static <T extends Throwable> void throwsUnchecked(Throwable t) throws T {
        throw (T)t;
    }
    
    /**
     * Decode the given value to a Java string using the following rules:
     * 
     * * If the string is all US-ASCII characters, it will be decoded as US-ASCII.
     * * If the string is a unicode encoding, it will be decoded as such.
     * * If the string is any other encoding, it will be encoded as raw bytes
     *   using ISO-8859-1.
     * 
     * This allows non-unicode, non-US-ASCII encodings to be represented in the
     * symbol table as their raw versions, but properly decodes unicode-
     * encoded strings.
     * 
     * @param value the value to decode
     * @return the resulting symbol string
     */
    public static String symbolBytesToString(ByteList value) {
        Encoding encoding = value.getEncoding();
        if (encoding == USASCIIEncoding.INSTANCE || encoding == ASCIIEncoding.INSTANCE) {
            return value.toString(); // raw
        } else if (encoding instanceof UnicodeEncoding) {
            return new String(value.getUnsafeBytes(), value.getBegin(), value.getRealSize(), value.getEncoding().getCharset());
        } else {
            return value.toString(); // raw
        }
    }

    /**
     * Decode a given ByteList to a Java string.
     *
     * @param runtime the current runtime
     * @param value the bytelist
     * @return a Java String representation of the ByteList
     */
    public static String decodeByteList(Ruby runtime, ByteList value) {
        byte[] unsafeBytes = value.getUnsafeBytes();
        int begin = value.getBegin();
        int length = value.length();
        
        Encoding encoding = value.getEncoding();

        if (encoding == UTF8Encoding.INSTANCE) {
            return RubyEncoding.decodeUTF8(unsafeBytes, begin, length);
        }

        Charset charset = runtime.getEncodingService().charsetForEncoding(encoding);

        if (charset == null) {
            try {
                return new String(unsafeBytes, begin, length, encoding.toString());
            } catch (UnsupportedEncodingException uee) {
                return value.toString();
            }
        }

        return RubyEncoding.decode(unsafeBytes, begin, length, charset);
    }
    
    /**
     * Convert a ByteList into a Java String by using its Encoding's Charset. If
     * the Charset is not available, fall back on other logic.
     * 
     * @param byteList the bytelist to decode
     * @return the decoded string
     */
    public static String byteListToString(ByteList byteList) {
        if (byteList.getEncoding().getCharset() != null) {
            return new String(byteList.getUnsafeBytes(), byteList.getBegin(), byteList.getRealSize(), byteList.getEncoding().getCharset());
        }
        
        return byteList.toString();
    }
}
