/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.java.JavaInterop;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.object.Layout;
import com.oracle.truffle.api.object.Property;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import jnr.constants.platform.Errno;
import org.jcodings.EncodingDB;
import org.jcodings.specific.UTF8Encoding;
import org.jcodings.transcode.EConvFlags;
import org.jcodings.util.CaseInsensitiveBytesHash;
import org.jcodings.util.CaseInsensitiveBytesHash.CaseInsensitiveBytesHashEntry;
import org.jruby.Main;
import org.jruby.ext.ffi.Platform;
import org.jruby.ext.ffi.Platform.OS_TYPE;
import org.jruby.runtime.Constants;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.builtins.CoreMethodNodeManager;
import org.jruby.truffle.builtins.PrimitiveManager;
import org.jruby.truffle.cext.CExtNodesFactory;
import org.jruby.truffle.core.array.ArrayNodesFactory;
import org.jruby.truffle.core.array.TruffleArrayNodesFactory;
import org.jruby.truffle.core.basicobject.BasicObjectNodesFactory;
import org.jruby.truffle.core.binding.BindingNodesFactory;
import org.jruby.truffle.core.binding.TruffleBindingNodesFactory;
import org.jruby.truffle.core.bool.FalseClassNodesFactory;
import org.jruby.truffle.core.bool.TrueClassNodesFactory;
import org.jruby.truffle.core.dir.DirNodesFactory;
import org.jruby.truffle.core.encoding.EncodingConverterNodesFactory;
import org.jruby.truffle.core.encoding.EncodingManager;
import org.jruby.truffle.core.encoding.EncodingNodesFactory;
import org.jruby.truffle.core.encoding.EncodingOperations;
import org.jruby.truffle.core.encoding.TruffleEncodingNodesFactory;
import org.jruby.truffle.core.exception.ExceptionNodesFactory;
import org.jruby.truffle.core.exception.NameErrorNodesFactory;
import org.jruby.truffle.core.exception.NoMethodErrorNodesFactory;
import org.jruby.truffle.core.exception.SystemCallErrorNodesFactory;
import org.jruby.truffle.core.fiber.FiberNodesFactory;
import org.jruby.truffle.core.hash.HashNodesFactory;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.kernel.TruffleKernelNodesFactory;
import org.jruby.truffle.core.klass.ClassNodes;
import org.jruby.truffle.core.klass.ClassNodesFactory;
import org.jruby.truffle.core.method.MethodNodesFactory;
import org.jruby.truffle.core.method.UnboundMethodNodesFactory;
import org.jruby.truffle.core.module.ModuleNodes;
import org.jruby.truffle.core.module.ModuleNodesFactory;
import org.jruby.truffle.core.mutex.ConditionVariableNodesFactory;
import org.jruby.truffle.core.mutex.MutexNodesFactory;
import org.jruby.truffle.core.numeric.BignumNodesFactory;
import org.jruby.truffle.core.numeric.FixnumNodesFactory;
import org.jruby.truffle.core.numeric.FloatNodesFactory;
import org.jruby.truffle.core.numeric.IntegerNodesFactory;
import org.jruby.truffle.core.numeric.TruffleFixnumNodesFactory;
import org.jruby.truffle.core.objectspace.ObjectSpaceNodesFactory;
import org.jruby.truffle.core.proc.ProcNodesFactory;
import org.jruby.truffle.core.queue.QueueNodesFactory;
import org.jruby.truffle.core.queue.SizedQueueNodesFactory;
import org.jruby.truffle.core.range.RangeNodesFactory;
import org.jruby.truffle.core.regexp.MatchDataNodesFactory;
import org.jruby.truffle.core.regexp.RegexpNodesFactory;
import org.jruby.truffle.core.rope.TruffleRopesNodesFactory;
import org.jruby.truffle.core.rubinius.AtomicReferenceNodesFactory;
import org.jruby.truffle.core.rubinius.ByteArrayNodesFactory;
import org.jruby.truffle.core.rubinius.IOBufferPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.IOPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.NativeFunctionPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.RandomizerPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.RubiniusTypeNodesFactory;
import org.jruby.truffle.core.rubinius.StatPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.UndefinedPrimitiveNodesFactory;
import org.jruby.truffle.core.rubinius.WeakRefPrimitiveNodesFactory;
import org.jruby.truffle.core.string.StringNodesFactory;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.string.TruffleStringNodesFactory;
import org.jruby.truffle.core.symbol.SymbolNodesFactory;
import org.jruby.truffle.core.thread.ThreadBacktraceLocationLayoutImpl;
import org.jruby.truffle.core.thread.ThreadBacktraceLocationNodesFactory;
import org.jruby.truffle.core.thread.ThreadNodesFactory;
import org.jruby.truffle.core.time.TimeNodesFactory;
import org.jruby.truffle.core.tracepoint.TracePointNodesFactory;
import org.jruby.truffle.debug.TruffleDebugNodesFactory;
import org.jruby.truffle.extra.TruffleGraalNodesFactory;
import org.jruby.truffle.extra.TrufflePosixNodesFactory;
import org.jruby.truffle.extra.ffi.PointerPrimitiveNodesFactory;
import org.jruby.truffle.gem.bcrypt.BCryptNodesFactory;
import org.jruby.truffle.interop.InteropNodesFactory;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.TruffleBootNodesFactory;
import org.jruby.truffle.language.TruffleSafeNodesFactory;
import org.jruby.truffle.language.backtrace.BacktraceFormatter;
import org.jruby.truffle.language.control.JavaException;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.TruffleFatalException;
import org.jruby.truffle.language.globals.GlobalVariableStorage;
import org.jruby.truffle.language.globals.GlobalVariables;
import org.jruby.truffle.language.loader.CodeLoader;
import org.jruby.truffle.language.loader.SourceLoader;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.objects.FreezeNode;
import org.jruby.truffle.language.objects.FreezeNodeGen;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;
import org.jruby.truffle.parser.ParserContext;
import org.jruby.truffle.platform.RubiniusTypes;
import org.jruby.truffle.platform.signal.SignalManager;
import org.jruby.truffle.stdlib.CoverageNodesFactory;
import org.jruby.truffle.stdlib.EtcNodesFactory;
import org.jruby.truffle.stdlib.ObjSpaceNodesFactory;
import org.jruby.truffle.stdlib.bigdecimal.BigDecimalNodesFactory;
import org.jruby.truffle.stdlib.digest.DigestNodesFactory;
import org.jruby.truffle.stdlib.psych.PsychEmitterNodesFactory;
import org.jruby.truffle.stdlib.psych.PsychParserNodesFactory;
import org.jruby.truffle.stdlib.psych.YAMLEncoding;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.io.EncodingUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class CoreLibrary {

    private static final String CLI_RECORD_SEPARATOR = org.jruby.util.cli.Options.CLI_RECORD_SEPARATOR.load();

    private static final Property ALWAYS_FROZEN_PROPERTY = Property.create(Layouts.FROZEN_IDENTIFIER, Layout.createLayout().createAllocator().constantLocation(true), 0);

    private final RubyContext context;

    private final Source source = initCoreSource();
    private final SourceSection sourceSection = source.createUnavailableSection();

    private final DynamicObject argumentErrorClass;
    private final DynamicObject arrayClass;
    private final DynamicObjectFactory arrayFactory;
    private final DynamicObject basicObjectClass;
    private final DynamicObject bignumClass;
    private final DynamicObjectFactory bignumFactory;
    private final DynamicObject bindingClass;
    private final DynamicObjectFactory bindingFactory;
    private final DynamicObject classClass;
    private final DynamicObject complexClass;
    private final DynamicObject dirClass;
    private final DynamicObject encodingClass;
    private final DynamicObjectFactory encodingFactory;
    private final DynamicObject encodingConverterClass;
    private final DynamicObject encodingErrorClass;
    private final DynamicObject exceptionClass;
    private final DynamicObject falseClass;
    private final DynamicObject fiberClass;
    private final DynamicObjectFactory fiberFactory;
    private final DynamicObject fixnumClass;
    private final DynamicObject floatClass;
    private final DynamicObject floatDomainErrorClass;
    private final DynamicObject hashClass;
    private final DynamicObjectFactory hashFactory;
    private final DynamicObject integerClass;
    private final DynamicObject indexErrorClass;
    private final DynamicObject ioErrorClass;
    private final DynamicObject loadErrorClass;
    private final DynamicObject localJumpErrorClass;
    private final DynamicObject lookupTableClass;
    private final DynamicObject matchDataClass;
    private final DynamicObjectFactory matchDataFactory;
    private final DynamicObject moduleClass;
    private final DynamicObject nameErrorClass;
    private final DynamicObjectFactory nameErrorFactory;
    private final DynamicObject nilClass;
    private final DynamicObject noMethodErrorClass;
    private final DynamicObjectFactory noMethodErrorFactory;
    private final DynamicObject notImplementedErrorClass;
    private final DynamicObject numericClass;
    private final DynamicObject objectClass;
    private final DynamicObjectFactory objectFactory;
    private final DynamicObject procClass;
    private final DynamicObjectFactory procFactory;
    private final DynamicObject processModule;
    private final DynamicObject rangeClass;
    private final DynamicObjectFactory intRangeFactory;
    private final DynamicObjectFactory longRangeFactory;
    private final DynamicObject rangeErrorClass;
    private final DynamicObject rationalClass;
    private final DynamicObject regexpClass;
    private final DynamicObjectFactory regexpFactory;
    private final DynamicObject regexpErrorClass;
    private final DynamicObject rubyTruffleErrorClass;
    private final DynamicObject runtimeErrorClass;
    private final DynamicObject systemStackErrorClass;
    private final DynamicObject securityErrorClass;
    private final DynamicObject standardErrorClass;
    private final DynamicObject stringClass;
    private final DynamicObjectFactory stringFactory;
    private final DynamicObject symbolClass;
    private final DynamicObjectFactory symbolFactory;
    private final DynamicObject syntaxErrorClass;
    private final DynamicObject systemCallErrorClass;
    private final DynamicObject systemExitClass;
    private final DynamicObject threadClass;
    private final DynamicObjectFactory threadFactory;
    private final DynamicObject threadBacktraceClass;
    private final DynamicObject threadBacktraceLocationClass;
    private final DynamicObjectFactory threadBacktraceLocationFactory;
    private final DynamicObject timeClass;
    private final DynamicObjectFactory timeFactory;
    private final DynamicObject transcodingClass;
    private final DynamicObject trueClass;
    private final DynamicObject typeErrorClass;
    private final DynamicObject zeroDivisionErrorClass;
    private final DynamicObject enumerableModule;
    private final DynamicObject errnoModule;
    private final DynamicObject kernelModule;
    private final DynamicObject rubiniusModule;
    private final DynamicObject rubiniusFFIModule;
    private final DynamicObject rubiniusFFIPointerClass;
    private final DynamicObject signalModule;
    private final DynamicObject truffleModule;
    private final DynamicObject truffleBootModule;
    private final DynamicObject truffleInteropModule;
    private final DynamicObject truffleKernelModule;
    private final DynamicObject bigDecimalClass;
    private final DynamicObject encodingCompatibilityErrorClass;
    private final DynamicObject methodClass;
    private final DynamicObjectFactory methodFactory;
    private final DynamicObject unboundMethodClass;
    private final DynamicObjectFactory unboundMethodFactory;
    private final DynamicObject byteArrayClass;
    private final DynamicObjectFactory byteArrayFactory;
    private final DynamicObjectFactory statFactory;
    private final DynamicObject fiberErrorClass;
    private final DynamicObject threadErrorClass;
    private final DynamicObject internalBufferClass;
    private final DynamicObject weakRefClass;
    private final DynamicObjectFactory weakRefFactory;
    private final DynamicObject objectSpaceModule;
    private final DynamicObject psychModule;
    private final DynamicObject psychParserClass;
    private final DynamicObject randomizerClass;
    private final DynamicObjectFactory randomizerFactory;
    private final DynamicObject atomicReferenceClass;
    private final DynamicObject handleClass;
    private final DynamicObjectFactory handleFactory;
    private final DynamicObject ioClass;

    private final DynamicObject argv;
    private final GlobalVariables globalVariables;
    private final DynamicObject mainObject;
    private final DynamicObject nilObject;
    private final DynamicObject rubiniusUndefined;
    private final DynamicObject digestClass;
    private final DynamicObjectFactory digestFactory;

    @CompilationFinal private DynamicObject eagainWaitReadable;
    @CompilationFinal private DynamicObject eagainWaitWritable;
    @CompilationFinal private DynamicObject interopForeignClass;

    private final Map<Errno, DynamicObject> errnoClasses = new HashMap<>();

    @CompilationFinal private InternalMethod basicObjectSendMethod;
    @CompilationFinal private InternalMethod truffleBootMainMethod;

    @CompilationFinal private GlobalVariableStorage loadPathStorage;
    @CompilationFinal private GlobalVariableStorage loadedFeaturesStorage;

    private static final Object systemObject = TruffleOptions.AOT ? null : JavaInterop.asTruffleObject(System.class);

    private final String coreLoadPath;

    @TruffleBoundary
    private static Source initCoreSource() {
        return Source.newBuilder("").name("(core)").mimeType(RubyLanguage.MIME_TYPE).build();
    }

    private String buildCoreLoadPath() {
        String path = context.getOptions().CORE_LOAD_PATH;

        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (path.startsWith(SourceLoader.TRUFFLE_SCHEME)) {
            return path;
        }

        try {
            return new File(path).getCanonicalPath();
        } catch (IOException e) {
            throw new JavaException(e);
        }
    }

    private enum State {
        INITIALIZING,
        LOADING_RUBY_CORE,
        LOADED
    }

    private State state = State.INITIALIZING;

    private static class CoreLibraryNode extends RubyNode {

        @Child SingletonClassNode singletonClassNode;
        @Child FreezeNode freezeNode;

        public CoreLibraryNode(RubyContext context) {
            super(context);
            this.singletonClassNode = SingletonClassNodeGen.create(context, null, null);
            this.freezeNode = FreezeNodeGen.create(context, null, null);
            adoptChildren();
        }

        public SingletonClassNode getSingletonClassNode() {
            return singletonClassNode;
        }

        public DynamicObject getSingletonClass(Object object) {
            return singletonClassNode.executeSingletonClass(object);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return nil();
        }

    }

    private final CoreLibraryNode node;

    public CoreLibrary(RubyContext context) {
        this.context = context;
        this.coreLoadPath = buildCoreLoadPath();
        this.node = new CoreLibraryNode(context);

        // Nothing in this constructor can use RubyContext.getCoreLibrary() as we are building it!
        // Therefore, only initialize the core classes and modules here.

        // Create the cyclic classes and modules

        classClass = ClassNodes.createClassClass(context);

        basicObjectClass = ClassNodes.createBootClass(context, classClass, null, "BasicObject");
        Layouts.CLASS.setInstanceFactoryUnsafe(basicObjectClass, Layouts.BASIC_OBJECT.createBasicObjectShape(basicObjectClass, basicObjectClass));

        objectClass = ClassNodes.createBootClass(context, classClass, basicObjectClass, "Object");
        objectFactory = Layouts.BASIC_OBJECT.createBasicObjectShape(objectClass, objectClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(objectClass, objectFactory);

        moduleClass = ClassNodes.createBootClass(context, classClass, objectClass, "Module");
        Layouts.CLASS.setInstanceFactoryUnsafe(moduleClass, Layouts.MODULE.createModuleShape(moduleClass, moduleClass));

        // Close the cycles
        // Set superclass of Class to Module
        Layouts.MODULE.getFields(classClass).parentModule = Layouts.MODULE.getFields(moduleClass).start;
        Layouts.MODULE.getFields(moduleClass).addDependent(classClass);
        Layouts.CLASS.setSuperclass(classClass, moduleClass);
        Layouts.MODULE.getFields(classClass).newVersion();

        // Set constants in Object and lexical parents
        Layouts.MODULE.getFields(classClass).getAdoptedByLexicalParent(context, objectClass, "Class", node);
        Layouts.MODULE.getFields(basicObjectClass).getAdoptedByLexicalParent(context, objectClass, "BasicObject", node);
        Layouts.MODULE.getFields(objectClass).getAdoptedByLexicalParent(context, objectClass, "Object", node);
        Layouts.MODULE.getFields(moduleClass).getAdoptedByLexicalParent(context, objectClass, "Module", node);

        // Create Exception classes

        // Exception
        exceptionClass = defineClass("Exception");
        Layouts.CLASS.setInstanceFactoryUnsafe(exceptionClass, Layouts.EXCEPTION.createExceptionShape(exceptionClass, exceptionClass));

        // NoMemoryError
        defineClass(exceptionClass, "NoMemoryError");

        // RubyTruffleError
        rubyTruffleErrorClass = defineClass(exceptionClass, "RubyTruffleError");

        // StandardError
        standardErrorClass = defineClass(exceptionClass, "StandardError");
        argumentErrorClass = defineClass(standardErrorClass, "ArgumentError");
        encodingErrorClass = defineClass(standardErrorClass, "EncodingError");
        fiberErrorClass = defineClass(standardErrorClass, "FiberError");
        ioErrorClass = defineClass(standardErrorClass, "IOError");
        localJumpErrorClass = defineClass(standardErrorClass, "LocalJumpError");
        regexpErrorClass = defineClass(standardErrorClass, "RegexpError");
        runtimeErrorClass = defineClass(standardErrorClass, "RuntimeError");
        threadErrorClass = defineClass(standardErrorClass, "ThreadError");
        typeErrorClass = defineClass(standardErrorClass, "TypeError");
        zeroDivisionErrorClass = defineClass(standardErrorClass, "ZeroDivisionError");

        // StandardError > RangeError
        rangeErrorClass = defineClass(standardErrorClass, "RangeError");
        floatDomainErrorClass = defineClass(rangeErrorClass, "FloatDomainError");

        // StandardError > IndexError
        indexErrorClass = defineClass(standardErrorClass, "IndexError");
        defineClass(indexErrorClass, "KeyError");

        // StandardError > IOError
        defineClass(ioErrorClass, "EOFError");

        // StandardError > NameError
        nameErrorClass = defineClass(standardErrorClass, "NameError");
        nameErrorFactory = Layouts.NAME_ERROR.createNameErrorShape(nameErrorClass, nameErrorClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(nameErrorClass, nameErrorFactory);
        noMethodErrorClass = defineClass(nameErrorClass, "NoMethodError");
        noMethodErrorFactory = Layouts.NO_METHOD_ERROR.createNoMethodErrorShape(noMethodErrorClass, noMethodErrorClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(noMethodErrorClass, noMethodErrorFactory);

        // StandardError > SystemCallError
        systemCallErrorClass = defineClass(standardErrorClass, "SystemCallError");
        Layouts.CLASS.setInstanceFactoryUnsafe(systemCallErrorClass, Layouts.SYSTEM_CALL_ERROR.createSystemCallErrorShape(systemCallErrorClass, systemCallErrorClass));

        errnoModule = defineModule("Errno");

        for (Errno errno : Errno.values()) {
            if (errno.defined()) {
                if (errno.equals(Errno.EWOULDBLOCK) && Errno.EWOULDBLOCK.intValue() == Errno.EAGAIN.intValue()){
                    continue; // Don't define it as a class, define it as constant later.
                }
                errnoClasses.put(errno, defineClass(errnoModule, systemCallErrorClass, errno.name()));
            }
        }

        // ScriptError
        DynamicObject scriptErrorClass = defineClass(exceptionClass, "ScriptError");
        loadErrorClass = defineClass(scriptErrorClass, "LoadError");
        notImplementedErrorClass = defineClass(scriptErrorClass, "NotImplementedError");
        syntaxErrorClass = defineClass(scriptErrorClass, "SyntaxError");

        // SecurityError
        securityErrorClass = defineClass(exceptionClass, "SecurityError");

        // SignalException
        DynamicObject signalExceptionClass = defineClass(exceptionClass, "SignalException");
        defineClass(signalExceptionClass, "Interrupt");

        // SystemExit
        systemExitClass = defineClass(exceptionClass, "SystemExit");

        // SystemStackError
        systemStackErrorClass = defineClass(exceptionClass, "SystemStackError");

        // Create core classes and modules

        numericClass = defineClass("Numeric");
        complexClass = defineClass(numericClass, "Complex");
        floatClass = defineClass(numericClass, "Float");
        integerClass = defineClass(numericClass, "Integer");
        fixnumClass = defineClass(integerClass, "Fixnum");
        bignumClass = defineClass(integerClass, "Bignum");
        bignumFactory = alwaysFrozen(Layouts.BIGNUM.createBignumShape(bignumClass, bignumClass));
        Layouts.CLASS.setInstanceFactoryUnsafe(bignumClass, bignumFactory);
        rationalClass = defineClass(numericClass, "Rational");

        // Classes defined in Object

        arrayClass = defineClass("Array");
        arrayFactory = Layouts.ARRAY.createArrayShape(arrayClass, arrayClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(arrayClass, arrayFactory);
        bindingClass = defineClass("Binding");
        bindingFactory = Layouts.BINDING.createBindingShape(bindingClass, bindingClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(bindingClass, bindingFactory);
        final DynamicObject conditionVariableClass = defineClass("ConditionVariable");
        Layouts.CLASS.setInstanceFactoryUnsafe(conditionVariableClass, Layouts.CONDITION_VARIABLE.createConditionVariableShape(conditionVariableClass, conditionVariableClass));
        dirClass = defineClass("Dir");
        Layouts.CLASS.setInstanceFactoryUnsafe(dirClass, Layouts.DIR.createDirShape(dirClass, dirClass));
        encodingClass = defineClass("Encoding");
        encodingFactory = Layouts.ENCODING.createEncodingShape(encodingClass, encodingClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(encodingClass, encodingFactory);
        falseClass = defineClass("FalseClass");
        fiberClass = defineClass("Fiber");
        fiberFactory = Layouts.FIBER.createFiberShape(fiberClass, fiberClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(fiberClass, fiberFactory);
        defineModule("FileTest");
        hashClass = defineClass("Hash");
        hashFactory = Layouts.HASH.createHashShape(hashClass, hashClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(hashClass, hashFactory);
        matchDataClass = defineClass("MatchData");
        matchDataFactory = Layouts.MATCH_DATA.createMatchDataShape(matchDataClass, matchDataClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(matchDataClass, matchDataFactory);
        methodClass = defineClass("Method");
        methodFactory = Layouts.METHOD.createMethodShape(methodClass, methodClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(methodClass, methodFactory);
        final DynamicObject mutexClass = defineClass("Mutex");
        Layouts.CLASS.setInstanceFactoryUnsafe(mutexClass, Layouts.MUTEX.createMutexShape(mutexClass, mutexClass));
        nilClass = defineClass("NilClass");
        final DynamicObjectFactory nilFactory = alwaysFrozen(Layouts.CLASS.getInstanceFactory(nilClass));
        Layouts.CLASS.setInstanceFactoryUnsafe(nilClass, nilFactory);
        procClass = defineClass("Proc");
        procFactory = Layouts.PROC.createProcShape(procClass, procClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(procClass, procFactory);
        processModule = defineModule("Process");
        DynamicObject queueClass = defineClass("Queue");
        Layouts.CLASS.setInstanceFactoryUnsafe(queueClass, Layouts.QUEUE.createQueueShape(queueClass, queueClass));
        DynamicObject sizedQueueClass = defineClass(queueClass, "SizedQueue");
        Layouts.CLASS.setInstanceFactoryUnsafe(sizedQueueClass, Layouts.SIZED_QUEUE.createSizedQueueShape(sizedQueueClass, sizedQueueClass));
        rangeClass = defineClass("Range");
        Layouts.CLASS.setInstanceFactoryUnsafe(rangeClass, Layouts.OBJECT_RANGE.createObjectRangeShape(rangeClass, rangeClass));
        intRangeFactory = Layouts.INT_RANGE.createIntRangeShape(rangeClass, rangeClass);
        longRangeFactory = Layouts.LONG_RANGE.createLongRangeShape(rangeClass, rangeClass);
        regexpClass = defineClass("Regexp");
        regexpFactory = Layouts.REGEXP.createRegexpShape(regexpClass, regexpClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(regexpClass, regexpFactory);
        stringClass = defineClass("String");
        stringFactory = Layouts.STRING.createStringShape(stringClass, stringClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(stringClass, stringFactory);
        symbolClass = defineClass("Symbol");
        symbolFactory = alwaysFrozen(Layouts.SYMBOL.createSymbolShape(symbolClass, symbolClass));
        Layouts.CLASS.setInstanceFactoryUnsafe(symbolClass, symbolFactory);

        threadClass = defineClass("Thread");
        threadClass.define("@abort_on_exception", false);
        threadFactory = Layouts.THREAD.createThreadShape(threadClass, threadClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(threadClass, threadFactory);

        threadBacktraceClass = defineClass(threadClass, objectClass, "Backtrace");
        threadBacktraceLocationClass = defineClass(threadBacktraceClass, objectClass, "Location");
        threadBacktraceLocationFactory = ThreadBacktraceLocationLayoutImpl.INSTANCE.createThreadBacktraceLocationShape(threadBacktraceLocationClass, threadBacktraceLocationClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(threadBacktraceLocationClass, threadBacktraceLocationFactory);
        timeClass = defineClass("Time");
        timeFactory = Layouts.TIME.createTimeShape(timeClass, timeClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(timeClass, timeFactory);
        trueClass = defineClass("TrueClass");
        unboundMethodClass = defineClass("UnboundMethod");
        unboundMethodFactory = Layouts.UNBOUND_METHOD.createUnboundMethodShape(unboundMethodClass, unboundMethodClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(unboundMethodClass, unboundMethodFactory);
        ioClass = defineClass("IO");
        Layouts.CLASS.setInstanceFactoryUnsafe(ioClass, Layouts.IO.createIOShape(ioClass, ioClass));
        internalBufferClass = defineClass(ioClass, objectClass, "InternalBuffer");
        Layouts.CLASS.setInstanceFactoryUnsafe(internalBufferClass, Layouts.IO_BUFFER.createIOBufferShape(internalBufferClass, internalBufferClass));
        weakRefClass = defineClass(basicObjectClass, "WeakRef");
        weakRefFactory = Layouts.WEAK_REF_LAYOUT.createWeakRefShape(weakRefClass, weakRefClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(weakRefClass, weakRefFactory);
        final DynamicObject tracePointClass = defineClass("TracePoint");
        Layouts.CLASS.setInstanceFactoryUnsafe(tracePointClass, Layouts.TRACE_POINT.createTracePointShape(tracePointClass, tracePointClass));

        // Modules

        DynamicObject comparableModule = defineModule("Comparable");
        defineModule("Config");
        enumerableModule = defineModule("Enumerable");
        defineModule("GC");
        kernelModule = defineModule("Kernel");
        defineModule("Math");
        objectSpaceModule = defineModule("ObjectSpace");
        signalModule = defineModule("Signal");

        // The rest

        encodingCompatibilityErrorClass = defineClass(encodingClass, encodingErrorClass, "CompatibilityError");

        encodingConverterClass = defineClass(encodingClass, objectClass, "Converter");
        Layouts.CLASS.setInstanceFactoryUnsafe(encodingConverterClass, Layouts.ENCODING_CONVERTER.createEncodingConverterShape(encodingConverterClass, encodingConverterClass));

        truffleModule = defineModule("Truffle");
        truffleInteropModule = defineModule(truffleModule, "Interop");
        defineModule(truffleModule, "CExt");
        defineModule(truffleModule, "Debug");
        defineModule(truffleModule, "Digest");
        defineModule(truffleModule, "ObjSpace");
        defineModule(truffleModule, "Etc");
        defineModule(truffleModule, "Encoding");
        defineModule(truffleModule, "Coverage");
        defineModule(truffleModule, "Graal");
        defineModule(truffleModule, "Ropes");
        defineModule(truffleModule, "GC");
        defineModule(truffleModule, "Array");
        defineModule(truffleModule, "String");
        truffleBootModule = defineModule(truffleModule, "Boot");
        defineModule(truffleModule, "Fixnum");
        defineModule(truffleModule, "Safe");
        defineModule(truffleModule, "System");
        truffleKernelModule = defineModule(truffleModule, "Kernel");
        defineModule(truffleModule, "Process");
        defineModule(truffleModule, "Binding");
        defineModule(truffleModule, "POSIX");
        psychModule = defineModule("Psych");
        psychParserClass = defineClass(psychModule, objectClass, "Parser");
        final DynamicObject psychHandlerClass = defineClass(psychModule, objectClass, "Handler");
        final DynamicObject psychEmitterClass = defineClass(psychModule, psychHandlerClass, "Emitter");
        Layouts.CLASS.setInstanceFactoryUnsafe(psychEmitterClass, Layouts.PSYCH_EMITTER.createEmitterShape(psychEmitterClass, psychEmitterClass));
        handleClass = defineClass(truffleModule, objectClass, "Handle");
        handleFactory = Layouts.HANDLE.createHandleShape(handleClass, handleClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(handleClass, handleFactory);

        bigDecimalClass = defineClass(truffleModule, numericClass, "BigDecimal");
        Layouts.CLASS.setInstanceFactoryUnsafe(bigDecimalClass, Layouts.BIG_DECIMAL.createBigDecimalShape(bigDecimalClass, bigDecimalClass));

        final DynamicObject gem = defineModule(truffleModule, "Gem");
        defineModule(gem, "BCrypt");

        // Rubinius

        rubiniusModule = defineModule("Rubinius");

        rubiniusFFIModule = defineModule(rubiniusModule, "FFI");
        rubiniusFFIPointerClass = defineClass(rubiniusFFIModule, objectClass, "Pointer");
        Layouts.CLASS.setInstanceFactoryUnsafe(rubiniusFFIPointerClass, Layouts.POINTER.createPointerShape(rubiniusFFIPointerClass, rubiniusFFIPointerClass));

        defineClass(rubiniusModule, objectClass, "Mirror");
        defineModule(rubiniusModule, "Type");

        DynamicObject statClass = defineClass(rubiniusModule, objectClass, "Stat");
        statFactory = Layouts.STAT.createStatShape(statClass, statClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(statClass, statFactory);

        byteArrayClass = defineClass(rubiniusModule, objectClass, "ByteArray");
        byteArrayFactory = Layouts.BYTE_ARRAY.createByteArrayShape(byteArrayClass, byteArrayClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(byteArrayClass, byteArrayFactory);
        lookupTableClass = defineClass(rubiniusModule, hashClass, "LookupTable");
        defineClass(rubiniusModule, objectClass, "StringData");
        transcodingClass = defineClass(encodingClass, objectClass, "Transcoding");
        randomizerClass = defineClass(rubiniusModule, objectClass, "Randomizer");
        atomicReferenceClass = defineClass(rubiniusModule, objectClass, "AtomicReference");
        Layouts.CLASS.setInstanceFactoryUnsafe(atomicReferenceClass,
                Layouts.ATOMIC_REFERENCE.createAtomicReferenceShape(atomicReferenceClass, atomicReferenceClass));
        randomizerFactory = Layouts.RANDOMIZER.createRandomizerShape(randomizerClass, randomizerClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(randomizerClass, randomizerFactory);

        // Standard library

        digestClass = defineClass(truffleModule, basicObjectClass, "Digest");
        digestFactory = Layouts.DIGEST.createDigestShape(digestClass, digestClass);
        Layouts.CLASS.setInstanceFactoryUnsafe(digestClass, digestFactory);

        // Include the core modules

        includeModules(comparableModule);

        // Create some key objects

        mainObject = objectFactory.newInstance();
        nilObject = nilFactory.newInstance();
        argv = Layouts.ARRAY.createArray(arrayFactory, null, 0);
        rubiniusUndefined = objectFactory.newInstance();

        globalVariables = new GlobalVariables(nilObject);

        // No need for new version since it's null before which is not cached
        assert Layouts.CLASS.getSuperclass(basicObjectClass) == null;
        Layouts.CLASS.setSuperclass(basicObjectClass, nilObject);
    }

    private static DynamicObjectFactory alwaysFrozen(DynamicObjectFactory factory) {
        return factory.getShape().addProperty(ALWAYS_FROZEN_PROPERTY).createFactory();
    }

    private void includeModules(DynamicObject comparableModule) {
        assert RubyGuards.isRubyModule(comparableModule);

        Layouts.MODULE.getFields(objectClass).include(context, node, kernelModule);

        Layouts.MODULE.getFields(numericClass).include(context, node, comparableModule);
        Layouts.MODULE.getFields(symbolClass).include(context, node, comparableModule);

        Layouts.MODULE.getFields(arrayClass).include(context, node, enumerableModule);
        Layouts.MODULE.getFields(dirClass).include(context, node, enumerableModule);
        Layouts.MODULE.getFields(hashClass).include(context, node, enumerableModule);
        Layouts.MODULE.getFields(rangeClass).include(context, node, enumerableModule);
    }

    public void initialize() {
        initializeGlobalVariables();
        initializeConstants();
        initializeSignalConstants();
    }

    public void addCoreMethods(PrimitiveManager primitiveManager) {
        final CoreMethodNodeManager coreMethodNodeManager =
                new CoreMethodNodeManager(context, node.getSingletonClassNode(), primitiveManager);

        // Sorted alphabetically to avoid duplicates
        List<List<? extends NodeFactory<? extends RubyNode>>> factories = Arrays.asList(
                ArrayNodesFactory.getFactories(),
                AtomicReferenceNodesFactory.getFactories(),
                BasicObjectNodesFactory.getFactories(),
                BCryptNodesFactory.getFactories(),
                BigDecimalNodesFactory.getFactories(),
                BignumNodesFactory.getFactories(),
                BindingNodesFactory.getFactories(),
                ByteArrayNodesFactory.getFactories(),
                CExtNodesFactory.getFactories(),
                ClassNodesFactory.getFactories(),
                ConditionVariableNodesFactory.getFactories(),
                CoverageNodesFactory.getFactories(),
                DigestNodesFactory.getFactories(),
                DirNodesFactory.getFactories(),
                EncodingConverterNodesFactory.getFactories(),
                EncodingNodesFactory.getFactories(),
                EtcNodesFactory.getFactories(),
                ExceptionNodesFactory.getFactories(),
                FalseClassNodesFactory.getFactories(),
                FiberNodesFactory.getFactories(),
                FixnumNodesFactory.getFactories(),
                FloatNodesFactory.getFactories(),
                HashNodesFactory.getFactories(),
                IntegerNodesFactory.getFactories(),
                InteropNodesFactory.getFactories(),
                IOBufferPrimitiveNodesFactory.getFactories(),
                IOPrimitiveNodesFactory.getFactories(),
                KernelNodesFactory.getFactories(),
                MainNodesFactory.getFactories(),
                MatchDataNodesFactory.getFactories(),
                MathNodesFactory.getFactories(),
                MethodNodesFactory.getFactories(),
                ModuleNodesFactory.getFactories(),
                MutexNodesFactory.getFactories(),
                NameErrorNodesFactory.getFactories(),
                NativeFunctionPrimitiveNodesFactory.getFactories(),
                NoMethodErrorNodesFactory.getFactories(),
                ObjectNodesFactory.getFactories(),
                ObjectSpaceNodesFactory.getFactories(),
                ObjSpaceNodesFactory.getFactories(),
                PointerPrimitiveNodesFactory.getFactories(),
                ProcessNodesFactory.getFactories(),
                ProcNodesFactory.getFactories(),
                PsychEmitterNodesFactory.getFactories(),
                PsychParserNodesFactory.getFactories(),
                QueueNodesFactory.getFactories(),
                RandomizerPrimitiveNodesFactory.getFactories(),
                RangeNodesFactory.getFactories(),
                RegexpNodesFactory.getFactories(),
                RubiniusTypeNodesFactory.getFactories(),
                SizedQueueNodesFactory.getFactories(),
                StatPrimitiveNodesFactory.getFactories(),
                StringNodesFactory.getFactories(),
                SymbolNodesFactory.getFactories(),
                SystemCallErrorNodesFactory.getFactories(),
                ThreadBacktraceLocationNodesFactory.getFactories(),
                ThreadNodesFactory.getFactories(),
                TimeNodesFactory.getFactories(),
                TracePointNodesFactory.getFactories(),
                TrueClassNodesFactory.getFactories(),
                TruffleArrayNodesFactory.getFactories(),
                TruffleBindingNodesFactory.getFactories(),
                TruffleBootNodesFactory.getFactories(),
                TruffleDebugNodesFactory.getFactories(),
                TruffleEncodingNodesFactory.getFactories(),
                TruffleFixnumNodesFactory.getFactories(),
                TruffleGCNodesFactory.getFactories(),
                TruffleGraalNodesFactory.getFactories(),
                TruffleKernelNodesFactory.getFactories(),
                TrufflePosixNodesFactory.getFactories(),
                TruffleProcessNodesFactory.getFactories(),
                TruffleRopesNodesFactory.getFactories(),
                TruffleSafeNodesFactory.getFactories(),
                TruffleStringNodesFactory.getFactories(),
                TruffleSystemNodesFactory.getFactories(),
                UnboundMethodNodesFactory.getFactories(),
                UndefinedPrimitiveNodesFactory.getFactories(),
                VMPrimitiveNodesFactory.getFactories(),
                WeakRefPrimitiveNodesFactory.getFactories()
                );

        int nFactories = factories.size();
        int threads = 8;
        int chunk = nFactories / threads;

        List<Callable<Void>> tasks = new ArrayList<>(threads);
        for (int t = 0; t < threads; t++) {
            final int nb = t;
            tasks.add(() -> {
                int start = nb * chunk;
                int end = nb == threads - 1 ? nFactories : (nb + 1) * chunk;
                for (int i = start; i < end; i++) {
                    coreMethodNodeManager.addCoreMethodNodes(factories.get(i));
                }
                return null;
            });
        }

        for (Future<Void> future : ForkJoinPool.commonPool().invokeAll(tasks)) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new JavaException(e);
            } catch (ExecutionException e) {
                throw new JavaException(e.getCause());
            }
        }

        coreMethodNodeManager.allMethodInstalled();

        basicObjectSendMethod = getMethod(basicObjectClass, "__send__");
        truffleBootMainMethod = getMethod(node.getSingletonClass(truffleBootModule), "main");
    }

    private InternalMethod getMethod(DynamicObject module, String name) {
        InternalMethod method = Layouts.MODULE.getFields(module).getMethod(name);
        if (method == null) {
            throw new AssertionError();
        }
        return method;
    }

    private void initializeGlobalVariables() {
        GlobalVariables globals = globalVariables;

        loadPathStorage = globals.put("$LOAD_PATH",
                Layouts.ARRAY.createArray(arrayFactory, null, 0));
        globals.alias("$:", loadPathStorage);

        loadedFeaturesStorage = globals.put("$LOADED_FEATURES",
                Layouts.ARRAY.createArray(arrayFactory, null, 0));
        globals.alias("$\"", loadedFeaturesStorage);

        globals.put("$,", nilObject);
        globals.put("$*", argv);
        globals.put("$0", StringOperations.createString(context, StringOperations.encodeRope(context.getInstanceConfig().displayedFileName(), UTF8Encoding.INSTANCE)));

        globals.put("$DEBUG", context.getInstanceConfig().isDebug());

        final Object verbose;

        switch (context.getInstanceConfig().getVerbosity()) {
            case NIL:
                verbose = getNilObject();
                break;
            case FALSE:
                verbose = false;
                break;
            case TRUE:
                verbose = true;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        globals.put("$VERBOSE", verbose);

        final DynamicObject defaultRecordSeparator = StringOperations.createString(context, StringOperations.encodeRope(CLI_RECORD_SEPARATOR, UTF8Encoding.INSTANCE));
        node.freezeNode.executeFreeze(defaultRecordSeparator);
        globals.put("$/", defaultRecordSeparator);

        globals.put("$SAFE", 0);
    }

    private void initializeConstants() {
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_CHAR", RubiniusTypes.TYPE_CHAR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_UCHAR", RubiniusTypes.TYPE_UCHAR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_BOOL", RubiniusTypes.TYPE_BOOL);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_SHORT", RubiniusTypes.TYPE_SHORT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_USHORT", RubiniusTypes.TYPE_USHORT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_INT", RubiniusTypes.TYPE_INT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_UINT", RubiniusTypes.TYPE_UINT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_LONG", RubiniusTypes.TYPE_LONG);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_ULONG", RubiniusTypes.TYPE_ULONG);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_LL", RubiniusTypes.TYPE_LL);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_ULL", RubiniusTypes.TYPE_ULL);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_FLOAT", RubiniusTypes.TYPE_FLOAT);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_DOUBLE", RubiniusTypes.TYPE_DOUBLE);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_PTR", RubiniusTypes.TYPE_PTR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_VOID", RubiniusTypes.TYPE_VOID);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_STRING", RubiniusTypes.TYPE_STRING);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_STRPTR", RubiniusTypes.TYPE_STRPTR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_CHARARR", RubiniusTypes.TYPE_CHARARR);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_ENUM", RubiniusTypes.TYPE_ENUM);
        Layouts.MODULE.getFields(rubiniusFFIModule).setConstant(context, node, "TYPE_VARARGS", RubiniusTypes.TYPE_VARARGS);

        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_VERSION", StringOperations.createString(context, StringOperations.encodeRope(Constants.RUBY_VERSION, UTF8Encoding.INSTANCE)));
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "JRUBY_VERSION", StringOperations.createString(context, StringOperations.encodeRope(Constants.VERSION, UTF8Encoding.INSTANCE)));
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_PATCHLEVEL", 0);
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_REVISION", Constants.RUBY_REVISION);
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_ENGINE", StringOperations.createString(context, StringOperations.encodeRope(Constants.ENGINE + "+truffle", UTF8Encoding.INSTANCE)));
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_PLATFORM", StringOperations.createString(context, StringOperations.encodeRope(Constants.PLATFORM, UTF8Encoding.INSTANCE)));
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_RELEASE_DATE", StringOperations.createString(context, StringOperations.encodeRope(Constants.COMPILE_DATE, UTF8Encoding.INSTANCE)));
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_DESCRIPTION", StringOperations.createString(context, StringOperations.encodeRope(OutputStrings.getVersionString(), UTF8Encoding.INSTANCE)));
        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "RUBY_COPYRIGHT", StringOperations.createString(context, StringOperations.encodeRope(OutputStrings.getCopyrightString(), UTF8Encoding.INSTANCE)));

        // BasicObject knows itself
        Layouts.MODULE.getFields(basicObjectClass).setConstant(context, node, "BasicObject", basicObjectClass);

        Layouts.MODULE.getFields(objectClass).setConstant(context, node, "ARGV", argv);

        Layouts.MODULE.getFields(rubiniusModule).setConstant(context, node, "UNDEFINED", rubiniusUndefined);
        Layouts.MODULE.getFields(rubiniusModule).setConstant(context, node, "LIBC", Platform.LIBC);

        Layouts.MODULE.getFields(processModule).setConstant(context, node, "CLOCK_MONOTONIC", ProcessNodes.CLOCK_MONOTONIC);
        Layouts.MODULE.getFields(processModule).setConstant(context, node, "CLOCK_REALTIME", ProcessNodes.CLOCK_REALTIME);

        if (Platform.getPlatform().getOS() == OS_TYPE.LINUX) {
            // Naming is not very consistent here, we just follow MRI
            Layouts.MODULE.getFields(processModule).setConstant(context, node, "CLOCK_THREAD_CPUTIME_ID", ProcessNodes.CLOCK_THREAD_CPUTIME);
            Layouts.MODULE.getFields(processModule).setConstant(context, node, "CLOCK_MONOTONIC_RAW", ProcessNodes.CLOCK_MONOTONIC_RAW);
        }

        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "INVALID_MASK", EConvFlags.INVALID_MASK);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "INVALID_REPLACE", EConvFlags.INVALID_REPLACE);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "UNDEF_MASK", EConvFlags.UNDEF_MASK);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "UNDEF_REPLACE", EConvFlags.UNDEF_REPLACE);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "UNDEF_HEX_CHARREF", EConvFlags.UNDEF_HEX_CHARREF);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "PARTIAL_INPUT", EConvFlags.PARTIAL_INPUT);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "AFTER_OUTPUT", EConvFlags.AFTER_OUTPUT);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "UNIVERSAL_NEWLINE_DECORATOR", EConvFlags.UNIVERSAL_NEWLINE_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "CRLF_NEWLINE_DECORATOR", EConvFlags.CRLF_NEWLINE_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "CR_NEWLINE_DECORATOR", EConvFlags.CR_NEWLINE_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "XML_TEXT_DECORATOR", EConvFlags.XML_TEXT_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "XML_ATTR_CONTENT_DECORATOR", EConvFlags.XML_ATTR_CONTENT_DECORATOR);
        Layouts.MODULE.getFields(encodingConverterClass).setConstant(context, node, "XML_ATTR_QUOTE_DECORATOR", EConvFlags.XML_ATTR_QUOTE_DECORATOR);

        Layouts.MODULE.getFields(psychParserClass).setConstant(context, node, "ANY", YAMLEncoding.YAML_ANY_ENCODING.ordinal());
        Layouts.MODULE.getFields(psychParserClass).setConstant(context, node, "UTF8", YAMLEncoding.YAML_UTF8_ENCODING.ordinal());
        Layouts.MODULE.getFields(psychParserClass).setConstant(context, node, "UTF16LE", YAMLEncoding.YAML_UTF16LE_ENCODING.ordinal());
        Layouts.MODULE.getFields(psychParserClass).setConstant(context, node, "UTF16BE", YAMLEncoding.YAML_UTF16BE_ENCODING.ordinal());

        // Java interop
        final DynamicObject javaModule = defineModule(truffleModule, "Java");
        Layouts.MODULE.getFields(javaModule).setConstant(context, node, "System", systemObject);

        // Errno constants
        for (Map.Entry<Errno, DynamicObject> entry : errnoClasses.entrySet()) {
            final Errno errno = entry.getKey();
            final DynamicObject errnoClass = entry.getValue();
            Layouts.CLASS.getFields(errnoClass).setConstant(context, node, "Errno", errno.intValue());
        }

        if (Errno.EWOULDBLOCK.intValue() == Errno.EAGAIN.intValue()) {
            Layouts.MODULE.getFields(errnoModule).setConstant(context, node, Errno.EWOULDBLOCK.name(), errnoClasses.get(Errno.EAGAIN));
        }

    }

    private void initializeSignalConstants() {
        Object[] signals = new Object[SignalManager.SIGNALS_LIST.size()];

        int i = 0;
        for (Map.Entry<String, Integer> signal : SignalManager.SIGNALS_LIST.entrySet()) {
            DynamicObject signalName = StringOperations.createString(context, StringOperations.encodeRope(signal.getKey(), UTF8Encoding.INSTANCE));
            Object[] objects = new Object[]{ signalName, signal.getValue() };
            signals[i++] = Layouts.ARRAY.createArray(arrayFactory, objects, objects.length);
        }

        Layouts.MODULE.getFields(signalModule).setConstant(context, node, "SIGNAL_LIST", Layouts.ARRAY.createArray(arrayFactory, signals, signals.length));
    }

    private DynamicObject defineClass(String name) {
        return defineClass(objectClass, name);
    }

    private DynamicObject defineClass(DynamicObject superclass, String name) {
        assert RubyGuards.isRubyClass(superclass);
        return ClassNodes.createInitializedRubyClass(context, objectClass, superclass, name);
    }

    private DynamicObject defineClass(DynamicObject lexicalParent, DynamicObject superclass, String name) {
        assert RubyGuards.isRubyModule(lexicalParent);
        assert RubyGuards.isRubyClass(superclass);
        return ClassNodes.createInitializedRubyClass(context, lexicalParent, superclass, name);
    }

    private DynamicObject defineModule(String name) {
        return defineModule(objectClass, name);
    }

    private DynamicObject defineModule(DynamicObject lexicalParent, String name) {
        assert RubyGuards.isRubyModule(lexicalParent);
        return ModuleNodes.createModule(context, moduleClass, lexicalParent, name, node);
    }

    public void loadRubyCore() {
        // Load Ruby core

        try {
            Main.printTruffleTimeMetric("before-load-core");
            state = State.LOADING_RUBY_CORE;

            final List<Future<RubyRootNode>> coreFileFutures = new ArrayList<>();

            if (TruffleOptions.AOT || !context.getOptions().CORE_PARALLEL_LOAD) {
                try {
                    for (int n = 0; n < coreFiles.length; n++) {
                        final RubyRootNode rootNode = context.getCodeLoader().parse(
                                context.getSourceLoader().load(getCoreLoadPath() + coreFiles[n]),
                                UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, node);

                        final CodeLoader.DeferredCall deferredCall = context.getCodeLoader().prepareExecute(
                                ParserContext.TOP_LEVEL,
                                DeclarationContext.TOP_LEVEL,
                                rootNode,
                                null,
                                context.getCoreLibrary().getMainObject());

                        deferredCall.callWithoutCallNode();
                    }
                } catch (IOException e) {
                    throw new JavaException(e);
                }
            } else {
                try {
                    for (int n = 0; n < coreFiles.length; n++) {
                        final int finalN = n;

                        coreFileFutures.add(ForkJoinPool.commonPool().submit(() ->
                                context.getCodeLoader().parse(
                                        context.getSourceLoader().load(getCoreLoadPath() + coreFiles[finalN]),
                                        UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, node)
                        ));
                    }

                    for (int n = 0; n < coreFiles.length; n++) {
                        final RubyRootNode rootNode = coreFileFutures.get(n).get();

                        final CodeLoader.DeferredCall deferredCall = context.getCodeLoader().prepareExecute(
                                ParserContext.TOP_LEVEL,
                                DeclarationContext.TOP_LEVEL,
                                rootNode,
                                null,
                                context.getCoreLibrary().getMainObject());

                        deferredCall.callWithoutCallNode();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw new JavaException(e);
                }
            }

            Main.printTruffleTimeMetric("after-load-core");
        } catch (RaiseException e) {
            final DynamicObject rubyException = e.getException();
            BacktraceFormatter.createDefaultFormatter(getContext()).printBacktrace(context, rubyException, Layouts.EXCEPTION.getBacktrace(rubyException));
            throw new TruffleFatalException("couldn't load the core library", e);
        } finally {
            state = State.LOADED;
        }

        // Get some references to things defined in the Ruby core

        eagainWaitReadable = (DynamicObject) Layouts.MODULE.getFields(ioClass).getConstant("EAGAINWaitReadable").getValue();
        assert Layouts.CLASS.isClass(eagainWaitReadable);

        eagainWaitWritable = (DynamicObject) Layouts.MODULE.getFields(ioClass).getConstant("EAGAINWaitWritable").getValue();
        assert Layouts.CLASS.isClass(eagainWaitWritable);

        interopForeignClass = (DynamicObject) Layouts.MODULE.getFields((DynamicObject) Layouts.MODULE.getFields(truffleModule).getConstant("Interop").getValue()).getConstant("Foreign").getValue();
        assert Layouts.CLASS.isClass(interopForeignClass);
    }

    public void initializePostBoot() {
        if (context.getOptions().PLATFORM_SAFE_LOAD) {
            // Load code that can't be run until everything else is boostrapped, such as pre-loaded Ruby stdlib.

            try {
                Main.printTruffleTimeMetric("before-post-boot");

                try {
                    for (String path : new String[]{"/post-boot/gems.rb", "/post-boot/shims.rb"}) {
                        final RubyRootNode rootNode = context.getCodeLoader().parse(context.getSourceLoader().load(getCoreLoadPath() + path), UTF8Encoding.INSTANCE, ParserContext.TOP_LEVEL, null, true, node);
                        final CodeLoader.DeferredCall deferredCall = context.getCodeLoader().prepareExecute(ParserContext.TOP_LEVEL, DeclarationContext.TOP_LEVEL, rootNode, null, context.getCoreLibrary().getMainObject());
                        deferredCall.callWithoutCallNode();
                    }
                } catch (IOException e) {
                    throw new JavaException(e);
                }

                Main.printTruffleTimeMetric("after-post-boot");
            } catch (RaiseException e) {
                final DynamicObject rubyException = e.getException();
                BacktraceFormatter.createDefaultFormatter(getContext()).printBacktrace(context, rubyException, Layouts.EXCEPTION.getBacktrace(rubyException));
                throw new TruffleFatalException("couldn't load the post-boot code", e);
            }
        }
    }

    private void initializeEncodings() {
        final EncodingManager encodingManager = context.getEncodingManager();
        final CaseInsensitiveBytesHash<EncodingDB.Entry>.CaseInsensitiveBytesHashEntryIterator hei = EncodingDB.getEncodings().entryIterator();

        while (hei.hasNext()) {
            final CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e = hei.next();
            final EncodingDB.Entry encodingEntry = e.value;

            encodingManager.defineEncoding(encodingEntry, e.bytes, e.p, e.end);

            for (String constName : EncodingUtils.encodingNames(e.bytes, e.p, e.end)) {
                final DynamicObject rubyEncoding = context.getEncodingManager().getRubyEncoding(encodingEntry.getIndex());
                Layouts.MODULE.getFields(encodingClass).setConstant(context, node, constName, rubyEncoding);
            }
        }
    }

    private void initializeEncodingAliases() {
        final EncodingManager encodingManager = context.getEncodingManager();
        final CaseInsensitiveBytesHash<EncodingDB.Entry>.CaseInsensitiveBytesHashEntryIterator hei = EncodingDB.getAliases().entryIterator();

        while (hei.hasNext()) {
            final CaseInsensitiveBytesHashEntry<EncodingDB.Entry> e = hei.next();
            final EncodingDB.Entry encodingEntry = e.value;

            // The alias name should be exactly the one in the encodings DB.
            encodingManager.defineAlias(encodingEntry.getIndex(), new String(e.bytes, e.p, e.end));

            // The constant names must be treated by the the <code>encodingNames</code> helper.
            for (String constName : EncodingUtils.encodingNames(e.bytes, e.p, e.end)) {
                final DynamicObject rubyEncoding = context.getEncodingManager().getRubyEncoding(encodingEntry.getIndex());
                Layouts.MODULE.getFields(encodingClass).setConstant(context, node, constName, rubyEncoding);
            }
        }
    }

    public void initializeEncodingManager() {
        initializeEncodings();
        initializeEncodingAliases();

        // External should always have a value, but Encoding.external_encoding{,=} will lazily setup
        final String externalEncodingName = getContext().getInstanceConfig().getExternalEncoding();
        if (externalEncodingName != null && !externalEncodingName.equals("")) {
            final DynamicObject loadedEncoding = getContext().getEncodingManager().getRubyEncoding(externalEncodingName);
            if (loadedEncoding == null) {
                // TODO (nirvdrum 28-Oct-16): This should just print a nice error message and exit with a status code of 1 -- it's essentially an input validation error -- no need to show the user a full trace.
                throw new RuntimeException("unknown encoding name - " + externalEncodingName);
            } else {
                getContext().getEncodingManager().setDefaultExternalEncoding(EncodingOperations.getEncoding(loadedEncoding));
            }
        } else {
            getContext().getEncodingManager().setDefaultExternalEncoding(getContext().getEncodingManager().getLocaleEncoding());
        }

        final String internalEncodingName = getContext().getInstanceConfig().getInternalEncoding();
        if (internalEncodingName != null && !internalEncodingName.equals("")) {
            final DynamicObject rubyEncoding = getContext().getEncodingManager().getRubyEncoding(internalEncodingName);
            if (rubyEncoding == null) {
                // TODO (nirvdrum 28-Oct-16): This should just print a nice error message and exit with a status code of 1 -- it's essentially an input validation error -- no need to show the user a full trace.
                throw new RuntimeException("unknown encoding name - " + internalEncodingName);
            } else {
                getContext().getEncodingManager().setDefaultInternalEncoding(EncodingOperations.getEncoding(rubyEncoding));
            }
        }
    }

    @TruffleBoundary
    public DynamicObject getMetaClass(Object object) {
        if (object instanceof DynamicObject) {
            return Layouts.BASIC_OBJECT.getMetaClass(((DynamicObject) object));
        } else if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueClass;
            } else {
                return falseClass;
            }
        } else if (object instanceof Byte) {
            return fixnumClass;
        } else if (object instanceof Short) {
            return fixnumClass;
        } else if (object instanceof Integer) {
            return fixnumClass;
        } else if (object instanceof Long) {
            return fixnumClass;
        } else if (object instanceof Float) {
            return floatClass;
        } else if (object instanceof Double) {
            return floatClass;
        } else {
            return interopForeignClass;
        }
    }

    @TruffleBoundary
    public DynamicObject getLogicalClass(Object object) {
        if (object instanceof DynamicObject) {
            return Layouts.BASIC_OBJECT.getLogicalClass(((DynamicObject) object));
        } else if (object instanceof Boolean) {
            if ((boolean) object) {
                return trueClass;
            } else {
                return falseClass;
            }
        } else if (object instanceof Byte) {
            return fixnumClass;
        } else if (object instanceof Short) {
            return fixnumClass;
        } else if (object instanceof Integer) {
            return fixnumClass;
        } else if (object instanceof Long) {
            return fixnumClass;
        } else if (object instanceof Float) {
            return floatClass;
        } else if (object instanceof Double) {
            return floatClass;
        } else {
            return interopForeignClass;
        }
    }

    /**
     * Convert a value to a {@code Float}, without doing any lookup.
     */
    public static double toDouble(Object value, DynamicObject nil) {
        assert value != null;

        if (value == nil) {
            return 0;
        }

        if (value instanceof Integer) {
            return (int) value;
        }

        if (value instanceof Long) {
            return (long) value;
        }

        if (RubyGuards.isRubyBignum(value)) {
            return Layouts.BIGNUM.getValue((DynamicObject) value).doubleValue();
        }

        if (value instanceof Double) {
            return (double) value;
        }

        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnsupportedOperationException();
    }

    public static boolean fitsIntoInteger(long value) {
        return ((int) value) == value;
    }

    public static int long2int(long value) {
        assert fitsIntoInteger(value) : value;
        return (int) value;
    }

    public RubyContext getContext() {
        return context;
    }

    public Source getSource() {
        return source;
    }

    public SourceSection getSourceSection() {
        return sourceSection;
    }

    public String getCoreLoadPath() {
        return coreLoadPath;
    }

    public DynamicObject getArrayClass() {
        return arrayClass;
    }

    public DynamicObject getBasicObjectClass() {
        return basicObjectClass;
    }

    public DynamicObject getBigDecimalClass() {
        return bigDecimalClass;
    }

    public DynamicObjectFactory getBindingFactory() {
        return bindingFactory;
    }

    public DynamicObject getClassClass() {
        return classClass;
    }

    public DynamicObject getFalseClass() {
        return falseClass;
    }

    public DynamicObjectFactory getFiberFactory() {
        return fiberFactory;
    }

    public DynamicObject getFixnumClass() {
        return fixnumClass;
    }

    public DynamicObject getFloatClass() {
        return floatClass;
    }

    public DynamicObject getStandardErrorClass() {
        return standardErrorClass;
    }

    public DynamicObject getLoadErrorClass() {
        return loadErrorClass;
    }

    public DynamicObject getMatchDataClass() {
        return matchDataClass;
    }

    public DynamicObjectFactory getMatchDataFactory() {
        return matchDataFactory;
    }

    public DynamicObject getModuleClass() {
        return moduleClass;
    }

    public DynamicObject getNameErrorClass() {
        return nameErrorClass;
    }

    public DynamicObjectFactory getNameErrorFactory() {
        return nameErrorFactory;
    }

    public DynamicObject getNilClass() {
        return nilClass;
    }

    public DynamicObject getNoMethodErrorClass() {
        return noMethodErrorClass;
    }

    public DynamicObjectFactory getNoMethodErrorFactory() {
        return noMethodErrorFactory;
    }

    public DynamicObject getObjectClass() {
        return objectClass;
    }

    public DynamicObjectFactory getObjectFactory() {
        return objectFactory;
    }

    public DynamicObject getProcClass() {
        return procClass;
    }

    public DynamicObject getRangeClass() {
        return rangeClass;
    }

    public DynamicObject getRationalClass() {
        return rationalClass;
    }

    public DynamicObject getRegexpClass() {
        return regexpClass;
    }

    public DynamicObjectFactory getRegexpFactory() {
        return regexpFactory;
    }

    public DynamicObject getRubyTruffleErrorClass() {
        return rubyTruffleErrorClass;
    }

    public DynamicObject getStringClass() {
        return stringClass;
    }

    public DynamicObject getThreadClass() {
        return threadClass;
    }

    public DynamicObjectFactory getThreadFactory() {
        return threadFactory;
    }

    public DynamicObject getTypeErrorClass() {
        return typeErrorClass;
    }

    public DynamicObject getTrueClass() {
        return trueClass;
    }

    public DynamicObject getZeroDivisionErrorClass() {
        return zeroDivisionErrorClass;
    }

    public DynamicObject getKernelModule() {
        return kernelModule;
    }

    public GlobalVariables getGlobalVariables() {
        return globalVariables;
    }

    public DynamicObject getLoadPath() {
        return (DynamicObject) loadPathStorage.getValue();
    }

    public DynamicObject getLoadedFeatures() {
        return (DynamicObject) loadedFeaturesStorage.getValue();
    }

    public DynamicObject getMainObject() {
        return mainObject;
    }

    public DynamicObject getNilObject() {
        return nilObject;
    }

    public DynamicObject getENV() {
        return (DynamicObject) Layouts.MODULE.getFields(objectClass).getConstant("ENV").getValue();
    }

    public DynamicObject getNumericClass() {
        return numericClass;
    }

    public DynamicObjectFactory getUnboundMethodFactory() {
        return unboundMethodFactory;
    }

    public DynamicObjectFactory getMethodFactory() {
        return methodFactory;
    }

    public DynamicObject getComplexClass() {
        return complexClass;
    }

    public DynamicObjectFactory getByteArrayFactory() {
        return byteArrayFactory;
    }

    public DynamicObjectFactory getStatFactory() {
        return statFactory;
    }

    public DynamicObject getLookupTableClass() {
        return lookupTableClass;
    }

    public DynamicObject getTranscodingClass() {
        return transcodingClass;
    }

    public DynamicObject getRubiniusFFIPointerClass() {
        return rubiniusFFIPointerClass;
    }

    public DynamicObject getRubiniusUndefined() {
        return rubiniusUndefined;
    }

    @TruffleBoundary
    public DynamicObject getErrnoClass(Errno errno) {
        return errnoClasses.get(errno);
    }

    public DynamicObject getSymbolClass() {
        return symbolClass;
    }

    public DynamicObjectFactory getSymbolFactory() {
        return symbolFactory;
    }

    public DynamicObject getThreadBacktraceLocationClass() {
        return threadBacktraceLocationClass;
    }

    public DynamicObjectFactory getThreadBacktraceLocationFactory() {
        return threadBacktraceLocationFactory;
    }

    public DynamicObject getInternalBufferClass() {
        return internalBufferClass;
    }

    public boolean isInitializing() {
        return state == State.INITIALIZING;
    }

    public boolean isLoadingRubyCore() {
        return state == State.LOADING_RUBY_CORE;
    }

    public boolean isLoaded() {
        return state == State.LOADED;
    }

    public boolean isSend(InternalMethod method) {
        CallTarget callTarget = method.getCallTarget();
        return callTarget == basicObjectSendMethod.getCallTarget();
    }

    public boolean isTruffleBootMainMethod(SharedMethodInfo info) {
        return info == truffleBootMainMethod.getSharedMethodInfo();
    }

    public DynamicObjectFactory getIntRangeFactory() {
        return intRangeFactory;
    }

    public DynamicObjectFactory getLongRangeFactory() {
        return longRangeFactory;
    }

    public DynamicObject getDigestClass() {
        return digestClass;
    }

    public DynamicObjectFactory getDigestFactory() {
        return digestFactory;
    }

    public DynamicObjectFactory getArrayFactory() {
        return arrayFactory;
    }

    public DynamicObjectFactory getBignumFactory() {
        return bignumFactory;
    }

    public DynamicObjectFactory getProcFactory() {
        return procFactory;
    }

    public DynamicObjectFactory getStringFactory() {
        return stringFactory;
    }

    public DynamicObjectFactory getHashFactory() {
        return hashFactory;
    }

    public DynamicObjectFactory getWeakRefFactory() {
        return weakRefFactory;
    }

    public Object getObjectSpaceModule() {
        return objectSpaceModule;
    }

    public DynamicObjectFactory getRandomizerFactory() {
        return randomizerFactory;
    }

    public DynamicObjectFactory getTimeFactory() {
        return timeFactory;
    }

    public DynamicObject getSystemExitClass() {
        return systemExitClass;
    }

    public DynamicObjectFactory getHandleFactory() {
        return handleFactory;
    }

    public DynamicObject getRuntimeErrorClass() {
        return runtimeErrorClass;
    }

    public DynamicObject getSystemStackErrorClass() {
        return systemStackErrorClass;
    }

    public DynamicObject getArgumentErrorClass() {
        return argumentErrorClass;
    }

    public DynamicObject getIndexErrorClass() {
        return indexErrorClass;
    }

    public DynamicObject getLocalJumpErrorClass() {
        return localJumpErrorClass;
    }

    public DynamicObject getNotImplementedErrorClass() {
        return notImplementedErrorClass;
    }

    public DynamicObject getSyntaxErrorClass() {
        return syntaxErrorClass;
    }

    public DynamicObject getFloatDomainErrorClass() {
        return floatDomainErrorClass;
    }

    public DynamicObject getIOErrorClass() {
        return ioErrorClass;
    }

    public DynamicObject getRangeErrorClass() {
        return rangeErrorClass;
    }

    public DynamicObject getRegexpErrorClass() {
        return regexpErrorClass;
    }

    public DynamicObjectFactory getEncodingFactory() {
        return encodingFactory;
    }

    public DynamicObject getEncodingCompatibilityErrorClass() {
        return encodingCompatibilityErrorClass;
    }

    public DynamicObject getFiberErrorClass() {
        return fiberErrorClass;
    }

    public DynamicObject getThreadErrorClass() {
        return threadErrorClass;
    }

    public DynamicObject getSecurityErrorClass() {
        return securityErrorClass;
    }

    public DynamicObject getSystemCallErrorClass() {
        return systemCallErrorClass;
    }

    public DynamicObject getEagainWaitReadable() {
        return eagainWaitReadable;
    }

    public DynamicObject getEagainWaitWritable() {
        return eagainWaitWritable;
    }

    public DynamicObject getTruffleModule() {
        return truffleModule;
    }

    public Object getTruffleInteropModule() {
        return truffleInteropModule;
    }

    public Object getTruffleKernelModule() {
        return truffleKernelModule;
    }

    private static final String[] coreFiles = {
            "/core/pre.rb",
            "/core/lookuptable.rb",
            "/core/basic_object.rb",
            "/core/mirror.rb",
            "/core/bignum.rb",
            "/core/channel.rb",
            "/core/character.rb",
            "/core/configuration.rb",
            "/core/false.rb",
            "/core/gc.rb",
            "/core/nil.rb",
            "/core/rubinius.rb",
            "/core/stat.rb",
            "/core/string.rb",
            "/core/thread.rb",
            "/core/true.rb",
            "/core/type.rb",
            "/core/weakref.rb",
            "/core/library.rb",
            "/core/truffle/ffi/ffi.rb",
            "/core/truffle/ffi/pointer.rb",
            "/core/truffle/ffi/ffi_file.rb",
            "/core/truffle/ffi/ffi_struct.rb",
            "/core/truffle/support.rb",
            "/core/truffle/boot.rb",
            "/core/io.rb",
            "/core/immediate.rb",
            "/core/string_mirror.rb",
            "/core/module.rb",
            "/core/proc.rb",
            "/core/enumerable_helper.rb",
            "/core/enumerable.rb",
            "/core/enumerator.rb",
            "/core/argf.rb",
            "/core/exception.rb",
            "/core/undefined.rb",
            "/core/hash.rb",
            "/core/array.rb",
            "/core/kernel.rb",
            "/core/identity_map.rb",
            "/core/comparable.rb",
            "/core/numeric_mirror.rb",
            "/core/numeric.rb",
            "/core/truffle/ctype.rb",
            "/core/integer.rb",
            "/core/fixnum.rb",
            "/core/regexp.rb",
            "/core/encoding.rb",
            "/core/env.rb",
            "/core/errno.rb",
            "/core/file.rb",
            "/core/dir.rb",
            "/core/dir_glob.rb",
            "/core/file_test.rb",
            "/core/float.rb",
            "/core/marshal.rb",
            "/core/object_space.rb",
            "/core/range_mirror.rb",
            "/core/range.rb",
            "/core/struct.rb",
            "/core/tms.rb",
            "/core/process.rb",
            "/core/process_mirror.rb",
            "/core/random.rb",
            "/core/signal.rb",
            "/core/splitter.rb",
            "/core/symbol.rb",
            "/core/mutex.rb",
            "/core/throw_catch.rb",
            "/core/time.rb",
            "/core/rational.rb",
            "/core/rationalizer.rb",
            "/core/complex.rb",
            "/core/complexifier.rb",
            "/core/class.rb",
            "/core/binding.rb",
            "/core/math.rb",
            "/core/method.rb",
            "/core/unbound_method.rb",
            "/core/truffle/cext.rb",
            "/core/truffle/interop.rb",
            "/core/rbconfig.rb",
            "/core/main.rb",
            "/core/post.rb"
    };

}
