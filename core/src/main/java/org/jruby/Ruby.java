/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import org.jcodings.specific.UTF8Encoding;
import org.jruby.anno.TypePopulator;
import org.jruby.api.Access;
import org.jruby.api.Create;
import org.jruby.api.Define;
import org.jruby.compiler.Constantizable;
import org.jruby.compiler.NotCompilableException;
import org.jruby.exceptions.LocalJumpError;
import org.jruby.exceptions.SystemExit;
import org.jruby.ext.jruby.JRubyUtilLibrary;
import org.jruby.ext.thread.ConditionVariable;
import org.jruby.ext.thread.Mutex;
import org.jruby.ext.thread.Queue;
import org.jruby.ext.thread.SizedQueue;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.runtime.IRReturnJump;
import org.jruby.java.util.ClassUtils;
import org.jruby.javasupport.Java;
import org.jruby.javasupport.JavaPackage;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaSupportImpl;
import org.jruby.management.Caches;
import org.jruby.management.InlineStats;
import org.jruby.parser.ParserManager;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.TraceEventManager;
import org.jruby.runtime.backtrace.RubyStackTraceElement;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.specialized.RubyObjectSpecializer;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.StrptimeParser;
import org.jruby.util.StrptimeToken;
import org.jruby.util.WeakIdentityHashMap;
import org.jruby.util.collections.ConcurrentWeakHashMap;
import org.jruby.util.io.EncodingUtils;
import org.objectweb.asm.util.TraceClassVisitor;

import jnr.constants.Constant;
import jnr.constants.ConstantSet;
import jnr.constants.platform.Errno;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;

import org.jcodings.Encoding;
import org.joda.time.DateTimeZone;
import org.joni.WarnCallback;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ast.executable.RuntimeCache;
import org.jruby.ast.executable.Script;
import org.jruby.ast.executable.ScriptAndCode;
import org.jruby.common.RubyWarnings;
import org.jruby.compiler.JITCompiler;
import org.jruby.embed.Extension;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.ext.JRubyPOSIXHandler;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.ext.ffi.FFI;
import org.jruby.ext.fiber.ThreadFiber;
import org.jruby.ext.fiber.ThreadFiberLibrary;
import org.jruby.ext.tracepoint.TracePoint;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.Compiler;
import org.jruby.ir.IRManager;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.management.BeanManager;
import org.jruby.management.BeanManagerFactory;
import org.jruby.management.Config;
import org.jruby.parser.Parser;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.platform.Platform;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.EventHook;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.RubyEvent;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.runtime.invokedynamic.MethodNames;
import org.jruby.runtime.load.BasicLibraryService;
import org.jruby.runtime.load.CompiledScriptLoader;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.opto.Invalidator;
import org.jruby.runtime.opto.OptoFactory;
import org.jruby.runtime.profile.ProfileCollection;
import org.jruby.runtime.profile.ProfilingService;
import org.jruby.runtime.profile.ProfilingServiceLookup;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ByteList;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.DefinedMessage;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.SecurityHelper;
import org.jruby.util.SelfFirstJRubyClassLoader;
import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.util.cli.Options;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.SelectorPool;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.net.BindException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import static java.lang.invoke.MethodHandles.explicitCastArguments;
import static java.lang.invoke.MethodHandles.insertArguments;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static org.jruby.RubyBoolean.FALSE_BYTES;
import static org.jruby.RubyBoolean.TRUE_BYTES;
import static org.jruby.RubyRandom.newRandom;
import static org.jruby.RubyRandom.randomSeed;
import static org.jruby.api.Access.errnoModule;
import static org.jruby.api.Access.loadService;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Create.newFrozenString;
import static org.jruby.api.Error.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.parser.ParserType.*;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.types;
import static org.jruby.runtime.Arity.UNLIMITED_ARGUMENTS;

/**
 * The Ruby object represents the top-level of a JRuby "instance" in a given VM.
 * JRuby supports spawning multiple instances in the same JVM. Generally, objects
 * created under these instances are tied to a given runtime, for such details
 * as identity and type, because multiple Ruby instances means there are
 * multiple instances of each class. This means that in multi-runtime mode
 * (or really, multi-VM mode, where each JRuby instance is a ruby "VM"), objects
 * generally can't be transported across runtimes without marshaling.
 *
 * This class roots everything that makes the JRuby runtime function, and
 * provides a number of utility methods for constructing global types and
 * accessing global runtime structures.
 */
public final class Ruby implements Constantizable {

    /**
     * The logger used to log relevant bits.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Ruby.class);

    /**
     * Create and initialize a new JRuby runtime. The properties of the
     * specified RubyInstanceConfig will be used to determine various JRuby
     * runtime characteristics.
     *
     * @param config The configuration to use for the new instance
     * @see org.jruby.RubyInstanceConfig
     */
    private Ruby(RubyInstanceConfig config) {
        this.config             = config;
        this.threadService      = new ThreadService(this);

        profilingServiceLookup = config.isProfiling() ? new ProfilingServiceLookup(this) : null;

        constant = OptoFactory.newConstantWrapper(Ruby.class, this);

        this.jrubyClassLoader = initJRubyClassLoader(config);

        this.staticScopeFactory = new StaticScopeFactory(this);
        this.beanManager        = BeanManagerFactory.create(this, config.isManagementEnabled());
        this.jitCompiler        = new JITCompiler(this);
        this.irManager          = new IRManager(this, config);
        this.parserManager      = new ParserManager(this);
        this.inlineStats        = new InlineStats();
        this.caches             = new Caches();

        this.random = initRandom();

        if (RubyInstanceConfig.CONSISTENT_HASHING_ENABLED) {
            this.hashSeedK0 = -561135208506705104l;
            this.hashSeedK1 = 7114160726623585955l;
        } else {
            this.hashSeedK0 = this.random.nextLong();
            this.hashSeedK1 = this.random.nextLong();
        }

        this.configBean = new Config(this);
        this.runtimeBean = new org.jruby.management.Runtime(this);

        registerMBeans();

        this.runtimeCache = new RuntimeCache();
        runtimeCache.initMethodCache(ClassIndex.MAX_CLASSES.ordinal() * MethodNames.values().length - 1);

        checkpointInvalidator = OptoFactory.newConstantInvalidator(this);

        this.objectSpacer = initObjectSpacer(config);

        posix = POSIXFactory.getPOSIX(new JRubyPOSIXHandler(this), config.isNativeEnabled());
        filenoUtil = new FilenoUtil(posix);

        reinitialize(false);

        // Construct key services
        loadService = this.config.createLoadService(this);
        javaSupport = loadJavaSupport();

        executor = new ThreadPoolExecutor(
                RubyInstanceConfig.POOL_MIN,
                RubyInstanceConfig.POOL_MAX,
                RubyInstanceConfig.POOL_TTL,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new DaemonThreadFactory("Ruby-" + getRuntimeNumber() + "-Worker"));

        fiberExecutor = new ThreadPoolExecutor(
                0,
                Integer.MAX_VALUE,
                RubyInstanceConfig.FIBER_POOL_TTL,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new DaemonThreadFactory("Ruby-" + getRuntimeNumber() + "-Fiber"));

        // initialize the root of the class hierarchy completely
        // Bootstrap the top of the hierarchy
        basicObjectClass = RubyClass.createBootstrapClass(this, "BasicObject", null, RubyBasicObject.BASICOBJECT_ALLOCATOR);
        objectClass = RubyClass.createBootstrapClass(this, "Object", basicObjectClass, RubyObject.OBJECT_ALLOCATOR);
        moduleClass = RubyClass.createBootstrapClass(this, "Module", objectClass, RubyModule.MODULE_ALLOCATOR);
        classClass = RubyClass.createBootstrapClass(this, "Class", moduleClass, RubyClass.CLASS_ALLOCATOR);
        refinementClass = RubyClass.createBootstrapClass(this, "Refinement", moduleClass, RubyModule.MODULE_ALLOCATOR);

        basicObjectClass.setMetaClass(classClass);
        objectClass.setMetaClass(basicObjectClass);
        moduleClass.setMetaClass(classClass);
        classClass.setMetaClass(classClass);
        refinementClass.setMetaClass(classClass);

        var metaClass = basicObjectClass.makeMetaClassBootstrap(this, classClass, classClass);
        metaClass = objectClass.makeMetaClassBootstrap(this, metaClass, classClass);
        metaClass = moduleClass.makeMetaClassBootstrap(this, metaClass, classClass);
        classClass.makeMetaClassBootstrap(this, metaClass, classClass);
        refinementClass.makeMetaClassBootstrap(this, metaClass, classClass);

        RubyObject.finishObjectClass(objectClass);
        RubyModule.finishModuleClass(moduleClass);
        RubyClass.finishClassClass(this, classClass);

        // set constants now that they're initialized
        basicObjectClass.defineConstantBootstrap("BasicObject", basicObjectClass);
        objectClass.defineConstantBootstrap("BasicObject", basicObjectClass);
        objectClass.defineConstantBootstrap("Object", objectClass);
        objectClass.defineConstantBootstrap("Class", classClass);
        objectClass.defineConstantBootstrap("Module", moduleClass);
        objectClass.defineConstantBootstrap("Refinement", refinementClass);

        // specializer for RubyObject subclasses
        objectSpecializer = new RubyObjectSpecializer(this);

        kernelModule = defineModuleBootstrap("Kernel");   // Initialize Kernel and include into Object
        topSelf = new RubyObject(this, objectClass);  // Object is ready, create top self

        // nil, true, and false all are set in TC so they need to be created above (both class and instances).
        // their methods are added afterward since no dispatch happens until after first TC is defined.
        nilClass = RubyClass.newClassBootstrap(this, objectClass, classClass, "NilClass");
        falseClass = RubyClass.newClassBootstrap(this, objectClass, classClass, "FalseClass");
        trueClass = RubyClass.newClassBootstrap(this, objectClass, classClass, "TrueClass");

        nilObject = new RubyNil(this, nilClass);
        nilPrefilledArray = new IRubyObject[NIL_PREFILLED_ARRAY_SIZE];
        for (int i=0; i<NIL_PREFILLED_ARRAY_SIZE; i++) nilPrefilledArray[i] = nilObject;
        singleNilArray = new IRubyObject[] {nilObject};

        falseObject = new RubyBoolean.False(this);
        falseObject.setFrozen(true);
        trueObject = new RubyBoolean.True(this);
        trueObject.setFrozen(true);

        // Set up the main thread in thread service
        threadService.initMainThread();

        // Get the main threadcontext (gets constructed for us)
        final ThreadContext context = getCurrentContext();

        RubyModule.finishCreateModuleClass(context, moduleClass);
        RubyClass.finishCreateClassClass(context, classClass);
        RubyKernel.finishKernelModule(context, kernelModule, config);
        RubyNil.finishNilClass(context, nilClass);
        RubyBoolean.finishFalseClass(context, falseClass);
        RubyBoolean.finishTrueClass(context, trueClass);
        RubyModule.finishRefinementClass(context, refinementClass);
        RubyBasicObject.finishBasicObjectClass(context, basicObjectClass);
        TopSelfFactory.finishTopSelf(context, topSelf, objectClass, false);

        objectClass.includeModule(context, kernelModule);

        // Construct the top-level execution frame and scope for the main thread
        context.prepareTopLevel(objectClass, topSelf);

        // Initialize all the core classes
        comparableModule = RubyComparable.createComparable(context);
        enumerableModule = RubyEnumerable.createEnumerableModule(context);

        stringClass = RubyString.createStringClass(context, objectClass, comparableModule);
        emptyFrozenString = freezeAndDedupString(newEmptyString(context));

        falseString = Create.newString(context, FALSE_BYTES);
        falseString.setFrozen(true);

        nilString = newEmptyString(context);
        nilString.setFrozen(true);
        nilInspectString = newString(RubyNil.nil);
        nilInspectString.setFrozen(true);
        trueString = newString(TRUE_BYTES);
        trueString.setFrozen(true);

        encodingService = new EncodingService(this);

        symbolClass = RubySymbol.createSymbolClass(context, objectClass, comparableModule);
        symbolTable = new RubySymbol.SymbolTable(this);

        threadGroupClass = profile.allowClass("ThreadGroup") ? RubyThreadGroup.createThreadGroupClass(context, objectClass) : null;
        threadClass = profile.allowClass("Thread") ? RubyThread.createThreadClass(context, objectClass) : null;
        exceptionClass = profile.allowClass("Exception") ? RubyException.createExceptionClass(context, objectClass) : null;

        // this is used in some kwargs conversions for numerics below
        hashClass = profile.allowClass("Hash") ? RubyHash.createHashClass(context, objectClass, enumerableModule) : null;

        numericClass = profile.allowClass("Numeric") ? RubyNumeric.createNumericClass(context, objectClass, comparableModule) : null;
        integerClass = profile.allowClass("Integer") ? RubyInteger.createIntegerClass(context, numericClass) : null;
        fixnumClass = profile.allowClass("Fixnum") ? RubyFixnum.createFixnumClass(context, integerClass) : null;

        encodingClass = RubyEncoding.createEncodingClass(context, objectClass);
        converterClass = RubyConverter.createConverterClass(context, objectClass, encodingClass);

        encodingService.defineEncodings(context);
        encodingService.defineAliases(context);

        initDefaultEncodings(context);

        complexClass = profile.allowClass("Complex") ? RubyComplex.createComplexClass(context, numericClass) : null;
        rationalClass = profile.allowClass("Rational") ? RubyRational.createRationalClass(context, numericClass) : null;

        if (profile.allowClass("Array")) {
            arrayClass = RubyArray.createArrayClass(context, objectClass, enumerableModule);
            emptyFrozenArray = Create.newEmptyArray(context);
            emptyFrozenArray.setFrozen(true);
        } else {
            arrayClass = null;
            emptyFrozenArray = null;
        }
        floatClass = profile.allowClass("Float") ? RubyFloat.createFloatClass(context, numericClass) : null;
        randomClass = RubyRandom.createRandomClass(context, objectClass);
        setDefaultRandom(newRandom(context, randomClass, randomSeed(this)));
        ioClass = RubyIO.createIOClass(context, objectClass, enumerableModule);
        ioBufferClass = Options.FIBER_SCHEDULER.load() ?
            RubyIOBuffer.createIOBufferClass(context, objectClass, comparableModule, ioClass) : null;
        structClass = profile.allowClass("Struct") ? RubyStruct.createStructClass(context, objectClass, enumerableModule) : null;
        bindingClass = profile.allowClass("Binding") ? RubyBinding.createBindingClass(context, objectClass) : null;
        // Math depends on all numeric types
        mathModule = profile.allowModule("Math") ? RubyMath.createMathModule(context) : null;
        regexpClass = profile.allowClass("Regexp") ? RubyRegexp.createRegexpClass(context, objectClass) : null;
        rangeClass = profile.allowClass("Range") ? RubyRange.createRangeClass(context, objectClass, enumerableModule) : null;
        objectSpaceModule = profile.allowModule("ObjectSpace") ? RubyObjectSpace.createObjectSpaceModule(context, objectClass) : null;
        gcModule = profile.allowModule("GC") ? RubyGC.createGCModule(context) : null;
        procClass = profile.allowClass("Proc") ? RubyProc.createProcClass(context, objectClass) : null;
        methodClass = profile.allowClass("Method") ? RubyMethod.createMethodClass(context, objectClass) : null;
        if (profile.allowClass("MatchData")) {
            matchDataClass = RubyMatchData.createMatchDataClass(context, objectClass);
            objectClass.defineConstant(context, "MatchingData", matchDataClass);
        } else {
            matchDataClass = null;
        }
        marshalModule = profile.allowModule("Marshal") ? RubyMarshal.createMarshalModule(context) : null;
        dirClass = profile.allowClass("Dir") ? RubyDir.createDirClass(context, objectClass, enumerableModule) : null;
        fileTestModule = profile.allowModule("FileTest") ? RubyFileTest.createFileTestModule(context) : null;
        fileClass = profile.allowClass("File") ? RubyFile.createFileClass(context, ioClass) : null;
        fileStatClass = profile.allowClass("File::Stat") ? RubyFileStat.createFileStatClass(context, objectClass, fileClass, comparableModule) : null;
        processModule = profile.allowModule("Process") ? RubyProcess.createProcessModule(context, objectClass, structClass) : null;
        timeClass = profile.allowClass("Time") ? RubyTime.createTimeClass(context, objectClass, comparableModule) : null;
        unboundMethodClass = profile.allowClass("UnboundMethod") ? RubyUnboundMethod.defineUnboundMethodClass(context, objectClass) : null;

        if (profile.allowModule("Signal")) RubySignal.createSignal(context);

        if (profile.allowClass("Enumerator")) {
            enumeratorClass = RubyEnumerator.defineEnumerator(context, objectClass, enumerableModule);
            generatorClass = RubyGenerator.createGeneratorClass(context, objectClass, enumeratorClass, enumerableModule);
            yielderClass = RubyYielder.createYielderClass(context, objectClass, enumeratorClass);
            chainClass = RubyChain.createChainClass(context, objectClass, enumeratorClass, enumerableModule);
            aseqClass = RubyArithmeticSequence.createArithmeticSequenceClass(context, enumeratorClass, enumerableModule);
            producerClass = RubyProducer.createProducerClass(context, objectClass, enumeratorClass, enumerableModule);
        } else {
            enumeratorClass = null;
            generatorClass = null;
            yielderClass = null;
            chainClass = null;
            aseqClass = null;
            producerClass = null;
        }

        continuationClass = initContinuation(context);

        TracePoint.createTracePointClass(context, objectClass);

        warningCategories = config.getWarningCategories();
        warningModule = RubyWarnings.createWarningModule(context);

        // Initialize exceptions
        initExceptions(context);

        // Thread library utilities
        mutexClass = Mutex.setup(context, threadClass, objectClass);
        conditionVariableClass = ConditionVariable.setup(context, threadClass, objectClass);
        queueClass = Queue.setup(context, threadClass, objectClass);
        closedQueueError = Queue.setupError(context, queueClass, stopIteration, objectClass);
        sizedQueueClass = SizedQueue.setup(context, threadClass, queueClass, objectClass);

        fiberClass = new ThreadFiberLibrary().createFiberClass(context, objectClass);

        dataClass = RubyData.createDataClass(context, objectClass);

        // everything booted, so SizedQueue should be available; set up root fiber
        ThreadFiber.initRootFiber(context, context.getThread());

        // set up defined messages
        initDefinedMessages(context);

        // set up thread statuses
        initThreadStatuses(context);

        // FIXME: This registers itself into static scope as a side-effect.  Let's make this
        // relationship handled either more directly or through a descriptive method
        // FIXME: We need a failing test case for this since removing it did not regress tests
        IRScope top = new IRScriptBody(irManager, "", context.getCurrentScope().getStaticScope());
        top.allocateInterpreterContext(Collections.EMPTY_LIST, 0, IRScope.allocateInitialFlags(top));

        // Initialize the "dummy" class used as a marker
        dummyClass = new RubyClass(this, classClass);
        dummyClass.setFrozen(true);

        // Create global constants and variables
        envObject = RubyGlobal.createGlobalsAndENV(context, globalVariables, config);

        // Prepare LoadService and load path
        loadService(context).init(this.config.getLoadPaths());

        // out of base boot mode
        coreIsBooted = true;

        // Don't load boot-time libraries when debugging IR
        if (!RubyInstanceConfig.DEBUG_PARSER) initBootLibraries(context);

        SecurityHelper.checkCryptoRestrictions(this);

        if (this.config.isProfiling()) initProfiling(context);

        if (this.config.getLoadGemfile()) {
            loadBundler();
        }

        // Done booting JRuby runtime
        runtimeIsBooted = true;
        if ("true".equals(System.getenv("USE_SUBSPAWN"))) {
            if (Platform.IS_WINDOWS) {
                LOG.warn("env USE_SUBSPAWN=true is unsupported on Windows at this time");
            } else {
                loadService(context).require("subspawn/replace-builtin");
            }
        }
    }

    private void initProfiling(ThreadContext context) {
        // additional twiddling for profiled mode
        loadService(context).require("jruby/profiler/shutdown_hook");

        // recache core methods, since they'll have profiling wrappers now
        kernelModule.invalidateCacheDescendants(context); // to avoid already-cached methods
        RubyKernel.recacheBuiltinMethods(this, kernelModule);
        RubyBasicObject.recacheBuiltinMethods(context, basicObjectClass);
    }

    private void initBootLibraries(ThreadContext context) {
        // initialize Java support
        initJavaSupport(context);

        // init Ruby-based kernel
        initRubyKernel();

        // Define blank modules for feature detection in preludes
        if (!this.config.isDisableGems()) {
            Define.defineModule(context, "Gem");
            if (!this.config.isDisableErrorHighlight()) {
                warnings.warn("ErrorHighlight does not currently support JRuby and will not be loaded");
            }
            if (!this.config.isDisableDidYouMean()) Define.defineModule(context, "DidYouMean");
            if (!this.config.isDisableSyntaxSuggest()) Define.defineModule(context, "SyntaxSuggest");
        }

        // Provide some legacy libraries
        loadService.provide("enumerator.rb");
        loadService.provide("rational.rb");
        loadService.provide("complex.rb");
        loadService.provide("thread.rb");
        loadService.provide("fiber.rb");
        loadService.provide("ruby2_keywords.rb");

        // Load preludes
        initRubyPreludes();
    }

    private ObjectSpacer initObjectSpacer(RubyInstanceConfig config) {
        ObjectSpacer objectSpacer;
        if (config.isObjectSpaceEnabled()) {
            objectSpacer = ENABLED_OBJECTSPACE;
        } else {
            objectSpacer = DISABLED_OBJECTSPACE;
        }
        return objectSpacer;
    }

    private JRubyClassLoader initJRubyClassLoader(RubyInstanceConfig config) {
        // force JRubyClassLoader to init if possible
        JRubyClassLoader jrubyClassLoader;
        if (!Ruby.isSecurityRestricted()) {
            if (config.isClassloaderDelegate()){
                jrubyClassLoader = new JRubyClassLoader(config.getLoader());
            }
            else {
                jrubyClassLoader = new SelfFirstJRubyClassLoader(config.getLoader());
            }
        }
        else {
            jrubyClassLoader = null; // a NullClassLoader object would be better ...
        }
        return jrubyClassLoader;
    }

    private void initDefaultEncodings(ThreadContext context) {
        // External should always have a value, but Encoding.external_encoding{,=} will lazily setup
        String encoding = this.config.getExternalEncoding();
        if (encoding != null && !encoding.isEmpty()) {
            Encoding loadedEncoding = encodingService.loadEncoding(ByteList.create(encoding));
            if (loadedEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
            setDefaultExternalEncoding(loadedEncoding);
        } else {
            Encoding consoleEncoding = encodingService.getConsoleEncoding();
            Encoding availableEncoding = consoleEncoding == null ? encodingService.getLocaleEncoding() : consoleEncoding;
            setDefaultExternalEncoding(availableEncoding);
        }

        // Filesystem should always have a value
        if (Platform.IS_WINDOWS) {
            setDefaultFilesystemEncoding(encodingService.getWindowsFilesystemEncoding(context));
        } else {
            setDefaultFilesystemEncoding(getDefaultExternalEncoding());
        }

        encoding = this.config.getInternalEncoding();
        if (encoding != null && !encoding.isEmpty()) {
            Encoding loadedEncoding = encodingService.loadEncoding(ByteList.create(encoding));
            if (loadedEncoding == null) throw new MainExitException(1, "unknown encoding name - " + encoding);
            setDefaultInternalEncoding(loadedEncoding);
        }
    }

    private Random initRandom() {
        Random myRandom;
        try {
            myRandom = new SecureRandom();
        } catch (Throwable t) {
            LOG.debug("unable to instantiate SecureRandom, falling back on Random", t);
            myRandom = new Random();
        }
        return myRandom;
    }

    public void registerMBeans() {
        this.beanManager.register(jitCompiler);
        this.beanManager.register(configBean);
        this.beanManager.register(getParserManager().getParserStats());
        this.beanManager.register(runtimeBean);
        this.beanManager.register(caches);
        this.beanManager.register(inlineStats);
    }

    void reinitialize(boolean reinitCore) {
        this.doNotReverseLookupEnabled = true;
        this.in                 = config.getInput();
        this.out                = config.getOutput();
        this.err                = config.getError();
        this.objectSpaceEnabled = config.isObjectSpaceEnabled();
        this.siphashEnabled     = config.isSiphashEnabled();
        this.profile            = config.getProfile();
        this.currentDirectory   = config.getCurrentDirectory();
        this.kcode              = config.getKCode();

        if (reinitCore) {
            RubyGlobal.initARGV(this);
            RubyGlobal.initSTDIO(this, globalVariables);
        }
    }

    /**
     * Returns a new instance of the JRuby runtime configured with defaults.
     *
     * @return the JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newInstance() {
        return newInstance(new RubyInstanceConfig()).boot();
    }

    /**
     * Returns a new instance of the JRuby runtime configured as specified.
     *
     * @param config The instance configuration
     * @return The JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newInstance(RubyInstanceConfig config) {
        return new Ruby(config).boot();
    }

    /**
     * Returns a new instance of the JRuby runtime configured and ready for main script execution.
     *
     * This version defers early boot steps, expecting that the normal "main" execution path will be followed
     * ({@link #runFromMain(InputStream, String)}).
     *
     * Also unlike the standard {@link #newInstance()}, this sets the "global" runtime to the new Ruby instance, since
     * it will be associated with the "main" execution path.
     *
     * @param config The instance configuration
     * @return The JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newMain(RubyInstanceConfig config) {
        Ruby ruby = new Ruby(config);

        setGlobalRuntimeFirstTimeOnly(ruby);

        return ruby;
    }

    private void loadRequiredLibraries() {
        ThreadContext context = getCurrentContext();

        // Require in all libraries specified on command line
        for (String scriptName : this.config.getRequiredLibraries()) {
            topSelf.callMethod(context, "require", Create.newString(context, scriptName));
        }
    }

    public Ruby boot() {
        loadRequiredLibraries();

        return this;
    }

    /**
     * Returns a new instance of the JRuby runtime configured with the given
     * input, output and error streams and otherwise default configuration
     * (except where specified system properties alter defaults).
     *
     * @param in the custom input stream
     * @param out the custom output stream
     * @param err the custom error stream
     * @return the JRuby runtime
     * @see org.jruby.RubyInstanceConfig
     */
    public static Ruby newInstance(InputStream in, PrintStream out, PrintStream err) {
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setInput(in);
        config.setOutput(out);
        config.setError(err);
        return newInstance(config);
    }

    /**
     * Tests whether globalRuntime has been instantiated or not.
     *
     * This method is used by singleton model of org.jruby.embed.ScriptingContainer
     * to decide what RubyInstanceConfig should be used. When a global runtime is
     * not there, RubyInstanceConfig of AbstractContextProvider will be used to enact
     * configurations set by a user. When a global runtime is already instantiated,
     * RubyInstanceConfig of the global runtime should be used in ScriptingContaiener.
     *
     * @return true if a global runtime is instantiated, false for other.
     *
     */
    public static boolean isGlobalRuntimeReady() {
        return globalRuntime != null;
    }

    /**
     * Set the global runtime to the given runtime only if it has no been set.
     *
     * @param runtime the runtime to use for global runtime
     */
    private static synchronized void setGlobalRuntimeFirstTimeOnly(Ruby runtime) {
        if (globalRuntime == null) {
            globalRuntime = runtime;
        }
    }

    /**
     * Get the global runtime.
     *
     * @return the global runtime
     */
    public static synchronized Ruby getGlobalRuntime() {
        if (globalRuntime == null) {
            newInstance();
        }
        return globalRuntime;
    }

    /**
     * Convenience method for java integrators who may need to switch the notion
     * of "global" runtime. Use <code>JRuby.runtime.use_as_global_runtime</code>
     * from Ruby code to activate the current runtime as the global one.
     */
    public void useAsGlobalRuntime() {
        synchronized(Ruby.class) {
            globalRuntime = this;
        }
    }

    /**
     * Clear the global runtime.
     */
    public static void clearGlobalRuntime() {
        globalRuntime = null;
    }

    /**
     * Get the thread-local runtime for the current thread, or null if unset.
     *
     * @return the thread-local runtime, or null if unset
     */
    public static Ruby getThreadLocalRuntime() {
        return threadLocalRuntime.get();
    }

    /**
     * Set the thread-local runtime to the given runtime.
     *
     * Note that static threadlocals like this one can leak resources across
     * (for example) application redeploys. If you use this, it is your
     * responsibility to clean it up appropriately.
     *
     * @param ruby the new runtime for thread-local
     */
    public static void setThreadLocalRuntime(Ruby ruby) {
        threadLocalRuntime.set(ruby);
    }

    /**
     * Evaluates a script under the current scope (perhaps the top-level
     * scope) and returns the result (generally the last value calculated).
     * This version goes straight into the interpreter, bypassing compilation
     * and runtime preparation typical to normal script runs.
     *
     * @param script The scriptlet to run
     * @return The result of the eval
     */
    public IRubyObject evalScriptlet(String script) {
        DynamicScope currentScope = getCurrentContext().getCurrentScope();
        DynamicScope newScope = new ManyVarsDynamicScope(getStaticScopeFactory().newEvalScope(currentScope.getStaticScope()), currentScope);

        return evalScriptlet(script, newScope);
    }

    /**
     * Evaluates a script under the current scope (perhaps the top-level
     * scope) and returns the result (generally the last value calculated).
     * This version goes straight into the interpreter, bypassing compilation
     * and runtime preparation typical to normal script runs.
     *
     * This version accepts a scope to use, so you can eval many times against
     * the same scope.
     *
     * @param script The scriptlet to run
     * @param scope The scope to execute against (ManyVarsDynamicScope is
     * recommended, so it can grow as needed)
     * @return The result of the eval
     */
    public IRubyObject evalScriptlet(String script, DynamicScope scope) {
        ThreadContext context = getCurrentContext();
        RootNode rootNode = (RootNode) getParserManager().parseEval("<script>", 0, script, scope).getAST();

        context.preEvalScriptlet(scope);

        try {
            return interpreter.execute(context, rootNode, getTopSelf());
        } finally {
            context.postEvalScriptlet();
        }
    }

    /**
     * Parse and execute the specified script
     * This differs from the other methods in that it accepts a string-based script and
     * parses and runs it as though it were loaded at a command-line. This is the preferred
     * way to start up a new script when calling directly into the Ruby object (which is
     * generally *dis*couraged.
     *
     * Note: This is used by compiler/java_class.rb
     *
     * @param script The contents of the script to run as a normal, root script
     * @return The last value of the script
     */
    public IRubyObject executeScript(String script, String filename) {
        InputStream in = new ByteArrayInputStream(encodeToBytes(script));
        ParseResult root = getParserManager().parseMainFile(filename, 0, in, setupSourceEncoding(getEncodingService().getLocaleEncoding()), getCurrentContext().getCurrentScope(), NORMAL);
        ThreadContext context = getCurrentContext();

        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            context.setFileAndLine(root.getFile(), root.getLine());
            return runInterpreter(root);
        } finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }

    /**
     * Run the script contained in the specified input stream, using the
     * specified filename as the name of the script being executed. The stream
     * will be read fully before being parsed and executed. The given filename
     * will be used for the ruby $PROGRAM_NAME and $0 global variables in this
     * runtime.
     *
     * This method is intended to be called once per runtime, generally from
     * Main or from main-like top-level entry points.
     *
     * As part of executing the script loaded from the input stream, various
     * RubyInstanceConfig properties will be used to determine whether to
     * compile the script before execution or run with various wrappers (for
     * looping, printing, and so on, see jruby -help).
     *
     * @param inputStream The InputStream from which to read the script contents
     * @param filename The filename to use when parsing, and for $PROGRAM_NAME
     * and $0 ruby global variables.
     */
    public void runFromMain(InputStream inputStream, String filename) {
        ThreadContext context = getCurrentContext();

        // this overwrites the default defined in RubyGlobal
        globalVariables.set("$0", newFrozenString(context, filename));

        // set main script and canonical path for require_relative use
        loadService.setMainScript(filename, getCurrentDirectory());

        for (Map.Entry<String, String> entry : config.getOptionGlobals().entrySet()) {
            final IRubyObject varvalue;
            if (entry.getValue() != null) {
                varvalue = newString(entry.getValue());
            } else {
                varvalue = getTrue();
            }
            getGlobalVariables().set('$' + entry.getKey(), varvalue);
        }

        // remaining boot steps before executing main script
        boot();

        if (filename.endsWith(".class")) {
            // we are presumably running a precompiled class; load directly
            IRScope script = CompiledScriptLoader.loadScriptFromFile(this, inputStream, null, filename, false);
            if (script == null) {
                throw new MainExitException(1, "error: .class file specified is not a compiled JRuby script");
            }
            script.setFileName(filename);
            runInterpreter(script);
            return;
        }

        if (Options.COMPILE_CACHE_CLASSES.load()) {
            Script script = tryScriptFromClass(filename);

            if (script != null) {
                runScript(script);
                return;
            }
        }

        ParseResult result = parseFromMain(filename, inputStream);

        // if no DATA, we're done with the stream, shut it down
        if (objectClass.fetchConstant(context, "DATA") == null) {
            try {inputStream.close();} catch (IOException ioe) {}
        }

        String oldFile = context.getFile();
        int oldLine = context.getLine();
        try {
            context.setFileAndLine(result.getFile(), result.getLine());

            if (config.isAssumePrinting() || config.isAssumeLoop()) {
                runWithGetsLoop(result, config.isAssumePrinting(), config.isProcessLineEnds(), config.isSplit());
            } else {
                runNormally(result, getTopSelf(), false);
            }
        } finally {
            context.setFileAndLine(oldFile, oldLine);
        }
    }

    private Script tryScriptFromClass(String filename) {
        // Try loading from precompiled class file
        String clsName = JavaNameMangler.mangledFilenameForStartupClasspath(filename);
        try {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();

            if (systemClassLoader.getResource(clsName + ".class") == null) return null;

            Class scriptClass = systemClassLoader.loadClass(clsName.replace("/", "."));

            if (Options.COMPILE_CACHE_CLASSES_LOGGING.load()) {
                System.err.println("found class " + scriptClass.getName() + " for " + filename);
            }

            Script script = Compiler.getScriptFromClass(scriptClass);

            script.setFilename(filename);
        } catch (ClassNotFoundException cnfe) {
            // ignore and proceed to parse and execute
            if (Options.COMPILE_CACHE_CLASSES_LOGGING.load()) {
                System.err.println("no class found for script " + filename);
            }
        }

        return null;
    }

    /**
     * Parse the script contained in the given input stream, using the given
     * filename as the name of the script, and return the root Node. This
     * is used to verify that the script syntax is valid, for jruby -c. The
     * current scope (generally the top-level scope) is used as the parent
     * scope for parsing.
     *
     * @param inputStream The input stream from which to read the script
     * @param filename The filename to use for parsing
     * @return The root node of the parsed script
     */
    @Deprecated
    public Node parseFromMain(InputStream inputStream, String filename) {
        return (Node) parseFromMain(filename, inputStream).getAST();
    }

    public ParseResult parseFromMain(String fileName, InputStream in) {
        return getParserManager().parseMainFile(fileName, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE),
                getTopLevelBinding().getBinding().getDynamicScope(), config.isInlineScript() ? INLINE : MAIN);
    }

    /**
     * Run the given script with a "while gets; end" loop wrapped around it.
     * This is primarily used for the -n command-line flag, to allow writing
     * a short script that processes input lines using the specified code.
     *
     * @param scriptNode The root node of the script to execute
     * @param printing Whether $_ should be printed after each loop (as in the
     * -p command-line flag)
     * @param processLineEnds Whether line endings should be processed by
     * setting $\ to $/ and <code>chop!</code>ing every line read
     * @param split Whether to split each line read using <code>String#split</code>
     * bytecode before executing.
     * @return The result of executing the specified script
     */
    public IRubyObject runWithGetsLoop(ParseResult scriptNode, boolean printing, boolean processLineEnds, boolean split) {
        ThreadContext context = getCurrentContext();

        // We do not want special scope types in IR so we amend the AST tree to contain the elements representing
        // a while gets; ...your code...; end
        scriptNode = getParserManager().addGetsLoop(this, scriptNode, printing, processLineEnds, split);

        Script script = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile) {
            try {
                // FIXME: prism will always fail until this becomes parseresult (will fall back to interp)
                script = tryCompile(scriptNode);
                if (Options.JIT_LOGGING.load()) {
                    LOG.info("successfully compiled: {}", scriptNode.getFile());
                }
            } catch (RaiseException e) {
                throw e;
            } catch (Throwable e) {
                if (Options.JIT_LOGGING.load()) {
                    if (Options.JIT_LOGGING_VERBOSE.load()) {
                        LOG.error("failed to compile: " + scriptNode.getFile(), e);
                    }
                    else {
                        LOG.error("failed to compile: " + scriptNode.getFile() + " - " + e);
                    }
                }
            }
            if (compile && script == null) {
                // IR JIT does not handle all scripts yet, so let those that fail run in interpreter instead
                // FIXME: restore error once JIT should handle everything
            }
        }

        // we do pre and post load outside the "body" versions to pre-prepare
        // and pre-push the dynamic scope we need for lastline
        Helpers.preLoad(context, scriptNode.getStaticScope().getVariables());

        try {
            if (script != null) {
                runScriptBody(script);
            } else {
                runInterpreter(scriptNode);
            }

        } finally {
            Helpers.postLoad(context);
        }

        return getNil();
    }

    /**
     * Run the specified script without any of the loop-processing wrapper
     * code.
     *
     * @param scriptNode The root node of the script to be executed
     * bytecode before execution
 *     @param wrap whether to wrap the execution in an anonymous module
     * @return The result of executing the script
     */
    @Deprecated
    public IRubyObject runNormally(Node scriptNode, boolean wrap) {
        return runNormally(scriptNode, getTopSelf(), wrap);
    }

    @Deprecated
    public IRubyObject runNormally(Node scriptNode, IRubyObject self, boolean wrap) {
        return runNormally((ParseResult) scriptNode, self, wrap);
    }

    public IRubyObject runNormally(ParseResult scriptNode, IRubyObject self, boolean wrap) {
        ScriptAndCode scriptAndCode = null;
        boolean compile = getInstanceConfig().getCompileMode().shouldPrecompileCLI();
        if (compile || config.isShowBytecode()) {
            scriptAndCode = precompileCLI(scriptNode);
        }

        if (scriptAndCode != null) {
            if (config.isShowBytecode()) {
                TraceClassVisitor tracer = new TraceClassVisitor(new PrintWriter(System.err));
                ClassReader reader = new ClassReader(scriptAndCode.bytecode());
                reader.accept(tracer, 0);
                return getNil();
            }

            return runScript(scriptAndCode.script(), self, wrap);
        } else {
            // FIXME: temporarily allowing JIT to fail for $0 and fall back on interpreter
//            failForcedCompile(scriptNode);

            return runInterpreter(scriptNode);
        }
    }

    /**
     * Run the specified script without any of the loop-processing wrapper
     * code.
     *
     * @param scriptNode The root node of the script to be executed
     * bytecode before execution
     * @return The result of executing the script
     */
    @Deprecated
    public IRubyObject runNormally(Node scriptNode) {
        return runNormally(scriptNode, false);
    }

    private ScriptAndCode precompileCLI(ParseResult scriptNode) {
        ScriptAndCode scriptAndCode = null;

        // IR JIT does not handle all scripts yet, so let those that fail run in interpreter instead
        // FIXME: restore error once JIT should handle everything
        try {
            scriptAndCode = tryCompile(scriptNode, new ClassDefiningJRubyClassLoader(getJRubyClassLoader()));
            if (scriptAndCode != null && Options.JIT_LOGGING.load()) {
                LOG.info("done compiling target script: {}", scriptNode.getFile());
            }
        } catch (RaiseException e) {
            throw e;
        } catch (Exception e) {
            if (Options.JIT_LOGGING.load()) {
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    LOG.error("failed to compile target script: " + scriptNode.getFile(), e);
                }
                else {
                    LOG.error("failed to compile target script: " + scriptNode.getFile() + " - " + e);
                }
            }
        }
        return scriptAndCode;
    }

    @Deprecated
    public Script tryCompile(Node node) {
        return tryCompile((ParseResult) node);
    }

    /**
     * Try to compile the code associated with the given Node, returning an
     * instance of the successfully-compiled Script or null if the script could
     * not be compiled.
     *
     * @param result The result to attempt to compiled
     * @return an instance of the successfully-compiled Script, or null.
     */
    public Script tryCompile(ParseResult result) {
        return tryCompile(result, new ClassDefiningJRubyClassLoader(getJRubyClassLoader())).script();
    }

    private ScriptAndCode tryCompile(ParseResult result, ClassDefiningClassLoader classLoader) {
        try {
            return Compiler.getInstance().execute(getCurrentContext(), result, classLoader);
        } catch (NotCompilableException | VerifyError e) {
            if (Options.JIT_LOGGING.load()) {
                if (Options.JIT_LOGGING_VERBOSE.load()) {
                    LOG.error("failed to compile target script: " + result.getFile(), e);
                } else {
                    LOG.error("failed to compile target script: " + result.getFile() + " - " + e.getLocalizedMessage());
                }
            }
            return null;
        }
    }

    public IRubyObject runScript(Script script) {
        return runScript(script, false);
    }

    public IRubyObject runScript(Script script, boolean wrap) {
        return runScript(script, getTopSelf(), wrap);
    }

    public IRubyObject runScript(Script script, IRubyObject self, boolean wrap) {
        return script.load(getCurrentContext(), self, wrap);
    }

    /**
     * This is used for the "gets" loop, and we bypass 'load' to use an
     * already-prepared, already-pushed scope for the script body.
     */
    public IRubyObject runScriptBody(Script script) {
        return script.__file__(getCurrentContext(), getTopSelf(), Block.NULL_BLOCK);
    }

    public IRubyObject runInterpreter(ThreadContext context, ParseResult parseResult, IRubyObject self) {
        try {
            return interpreter.execute(context, parseResult, self);
        } catch (IRReturnJump ex) {
            /* We happen to not push script scope as a dynamic scope or at least we seem to get rid of it.
             * This will capture any return which says it should return to a script scope as the reasonable
             * exit point.  We still raise when jump off point is anything else since that is a bug.
             */
            if (!ex.methodToReturnFrom.getStaticScope().getIRScope().isScriptScope()) {
                System.err.println("Unexpected 'return' escaped the runtime from " + ex.returnScope.getIRScope() + " to " + ex.methodToReturnFrom.getStaticScope().getIRScope());
                System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(ex, false));
                Throwable t = ex;
                while ((t = t.getCause()) != null) {
                    System.err.println("Caused by:");
                    System.err.println(ThreadContext.createRawBacktraceStringFromThrowable(t, false));
                }
            }
        }

        return context.nil;
   }

    public IRubyObject runInterpreter(ThreadContext context,  Node rootNode, IRubyObject self) {
        assert rootNode != null : "scriptNode is not null";
        return interpreter.execute(context, (ParseResult) rootNode, self);
    }

    public IRubyObject runInterpreter(Node scriptNode) {
        return runInterpreter(getCurrentContext(), scriptNode, getTopSelf());
    }

    public IRubyObject runInterpreter(ParseResult parseResult) {
        return runInterpreter(getCurrentContext(), parseResult, getTopSelf());
    }

    /**
     * This is used for the "gets" loop, and we bypass 'load' to use an
     * already-prepared, already-pushed scope for the script body.
     */
    public IRubyObject runInterpreterBody(Node scriptNode) {
        assert scriptNode != null : "scriptNode is not null";
        assert scriptNode instanceof RootNode : "scriptNode is not a RootNode";

        return runInterpreter(scriptNode);
    }

    @Deprecated
    public Parser getParser() {
        return getParserManager().getParser();
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }

    public JITCompiler getJITCompiler() {
        return jitCompiler;
    }

    public InlineStats getInlineStats() {
        return inlineStats;
    }

    /**
     * Get the Caches management object.
     *
     * @return the current runtime's Caches management object
     */
    public Caches getCaches() {
        return caches;
    }

    public int allocSymbolId() {
        return symbolLastId.incrementAndGet();
    }
    public int allocModuleId() {
        return moduleLastId.incrementAndGet();
    }

    /**
     * A collection of all natural Module instances in the system.
     *
     * Instances of Module, which are themselves modules, do not have ancestors and can't be traversed by walking down
     * from BasicObject. We track them separately here for purposes of ObjectSpace.each_object.
     *
     * @param module the true module to add to the allModules collection
     */
    public void addModule(RubyModule module) {
        assert module.getMetaClass() == moduleClass;

        allModules.put(module, RubyBasicObject.NEVER);
    }

    /**
     * Walk all natural Module instances in the system.
     *
     * This will only include direct instances of Module, not instances of Class.
     *
     * @param func the consumer to call for each module
     */
    public void eachModule(Consumer<RubyModule> func) {
        Enumeration<RubyModule> e = allModules.keys();
        while (e.hasMoreElements()) {
            func.accept(e.nextElement());
        }
    }

    @Deprecated(since = "10.0")
    public RubyModule getModule(String name) {
        return Access.getModule(getCurrentContext(), name);
    }

    /**
     * Retrieve the class with the given name from the Object namespace.
     *
     * @param name The name of the class
     * @return The class
     */
    @Deprecated(since = "10.0")
    public RubyClass getClass(String name) {
        return Access.getClass(getCurrentContext(), name);
    }

    /**
     * Retrieve the class with the given name from the Object namespace. The
     * module name must be an interned string, but this method will be faster
     * than the non-interned version.
     *
     * @param internedName the name of the class; <em>must</em> be an interned String!
     * @return
     */
    @Deprecated
    public RubyClass fastGetClass(String internedName) {
        return Access.getClass(getCurrentContext(), internedName);
    }

    /**
     * A variation of defineClass that allows passing in an array of supplementary
     * call sites for improving dynamic invocation performance.
     *
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @return The new class
     */
    @Deprecated(since = "10.0")
    public RubyClass defineClass(String name, RubyClass superClass, ObjectAllocator allocator, CallSite[] callSites) {
        return defineClassUnder(getCurrentContext(), name, superClass, allocator, objectClass, callSites);
    }

    @Deprecated(since = "10.0")
    public RubyClass defineClass(String name, RubyClass superClass, ObjectAllocator allocator) {
        return defineClassUnder(getCurrentContext(), name, superClass, allocator, objectClass, null);
    }

    /**
     * Define a new class with the given name under the given module or class
     * namespace. Roughly equivalent to rb_define_class_under in MRI.
     *
     * If the name specified is already bound, its value will be returned if:
     * * It is a class
     * * No new superclass is being defined
     *
     * @param name The name for the new class
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @param parent The namespace under which to define the new class
     * @return The new class
     */
    @Extension
    @Deprecated(since = "10.0")
    public RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator, RubyModule parent) {
        return defineClassUnder(runtimeError.getCurrentContext(), name, superClass, allocator, parent, null);
    }

    @Deprecated(since = "10.0")
    public RubyClass defineClassUnder(String id, RubyClass superClass, ObjectAllocator allocator, RubyModule parent, CallSite[] callSites) {
        return defineClassUnder(getCurrentContext(), id, superClass, allocator, parent, callSites);
    }

    /**
     * A variation of defineClassUnder that allows passing in an array of
     * supplementary call sites to improve dynamic invocation.  This is an internal API.  Please
     * use {@link org.jruby.RubyModule#defineClassUnder(ThreadContext, String, RubyClass, ObjectAllocator)} instead.
     *
     * @param context the current thread context
     * @param id The name for the new class as an ISO-8859_1 String (id-value)
     * @param superClass The super class for the new class
     * @param allocator An ObjectAllocator instance that can construct
     * instances of the new class.
     * @param parent The namespace under which to define the new class
     * @param callSites The array of call sites to add
     * @return The new class
     */
    public RubyClass defineClassUnder(ThreadContext context, String id, RubyClass superClass, ObjectAllocator allocator,
                                      RubyModule parent, CallSite[] callSites) {
        IRubyObject object = parent.getConstantAt(context, id);
        if (object != null) return foundExistingClass(context, id, superClass, allocator, object);

        boolean parentIsObject = parent == objectClass;

        if (superClass == null) superClass = determineSuperClass(context, id, parent, parentIsObject);

        return RubyClass.newClass(context, superClass, id, allocator, parent, !parentIsObject, callSites);
    }

    private RubyClass determineSuperClass(ThreadContext context, String id, RubyModule parent, boolean parentIsObject) {
        IRubyObject className = parentIsObject ? ids(this, id) :
                parent.toRubyString(context).append(newString("::")).append(ids(this, id));
        warn(context, str(this, "no super class for '", className, "', Object assumed"));

        return objectClass;
    }

    private RubyClass foundExistingClass(ThreadContext context, String id, RubyClass superClass, ObjectAllocator allocator, IRubyObject obj) {
        if (!(obj instanceof RubyClass klazz)) throw typeError(context, str(this, ids(this, id), " is not a class"));

        if (klazz.getSuperClass().getRealClass() != superClass) {
            throw typeError(context, str(this, "superclass mismatch for ", ids(this, id)));
        }
        // If we define a class in Ruby, but later want to allow it to be defined in Java, the allocator needs to be updated
        if (klazz.getAllocator() != allocator) klazz.allocator(allocator);

        return klazz;
    }

    /**
     * Define a new module under the Object namespace. Roughly equivalent to
     * rb_define_module in MRI.
     *
     * @param name The name of the new module
     * @return The new module
     */
    @Deprecated(since = "10.0")
    public RubyModule defineModule(String name) {
        return defineModuleUnder(getCurrentContext(), name, objectClass);
    }

    /**
     * This is only for defining kernel.  The reason we have this is so all other define methods
     * can count on ThreadContext being available.  These are defined before that point.
     * @param name The name for the new class
     * @return The new module
     */
    public RubyModule defineModuleBootstrap(String name) {
        return RubyModule.newModuleBootstrap(this, name, objectClass);
    }

    @Deprecated(since = "10.0")
    public RubyModule defineModuleUnder(String name, RubyModule parent) {
        return defineModuleUnder(getCurrentContext(), name, parent);
    }

    /**
     * Define a new module with the given name under the given module or
     * class namespace. Roughly equivalent to rb_define_module_under in MRI.
     * This is an internal API.  It is still used in early bootstrapping for
     * setting up Kernel since Kernel needs to exist before the first ThreadContext is created.
     *
     * @param name The name of the new module
     * @param parent The class or module namespace under which to define the
     * module
     * @return The new module
     */
    public RubyModule defineModuleUnder(ThreadContext context, String name, RubyModule parent) {
        IRubyObject moduleObj = parent.getConstantAt(context, name);

        boolean parentIsObject = parent == objectClass;

        return moduleObj != null ?
                foundExistingModule(context, parent, moduleObj, parentIsObject) :
                RubyModule.newModule(context, name, parent, !parentIsObject, null, -1);
    }

    private RubyModule foundExistingModule(ThreadContext context, RubyModule parent, IRubyObject moduleObj,
                                           boolean parentIsObject) {
        if (moduleObj.isModule()) return (RubyModule) moduleObj;

        RubyString typeName = parentIsObject ?
                types(this, moduleObj.getMetaClass()) : types(this, parent, moduleObj.getMetaClass());

        throw typeError(context, str(this, typeName, " is not a module"));
    }

    /**
     * From Object, retrieve the named module. If it doesn't exist a
     * new module is created.
     *
     * @param id The name of the module
     * @return The existing or new module
     * @deprecated Use {@link org.jruby.api.Define#defineModule(ThreadContext, String)} OR
     * {@link org.jruby.RubyModule#defineModuleUnder(ThreadContext, String)}.
     */
    @Deprecated(since = "10.0")
    public RubyModule getOrCreateModule(String id) {
        var context = getCurrentContext();
        IRubyObject module = objectClass.getConstantAt(context, id);
        if (module == null) {
            module = Define.defineModule(context, id);
        } else if (!module.isModule()) {
            throw typeError(context, str(this, ids(this, id), " is not a Module"));
        }

        return (RubyModule) module;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    /** rb_define_global_const
     * Define a constant on the global namespace (i.e. Object) with the given
     * name and value.
     *
     * @param name the name
     * @param value the value
     * @deprecated Use {@link RubyModule#defineConstant(ThreadContext, String, IRubyObject)} with a reference
     * to Object.
     */
    @Deprecated(since = "10.0")
    public void defineGlobalConstant(String name, IRubyObject value) {
        objectClass.defineConstant(name, value);
    }

    /**
     * Fetch a constant from the global namespace (i.e. Object) with the given
     * name.
     *
     * @param name the name
     * @return the value
     */
    @Deprecated(since = "10.0")
    public IRubyObject fetchGlobalConstant(String name) {
        return objectClass.fetchConstant(getCurrentContext(), name, false);
    }

    @Deprecated(since = "10.0")
    public boolean isClassDefined(String name) {
        return Access.getModule(getCurrentContext(), name) != null;
    }

    public JavaSupport loadJavaSupport() {
        return new JavaSupportImpl(this);
    }

    private void loadBundler() {
        loadService.loadFromClassLoader(getClassLoader(), "jruby/bundler/startup.rb", false);
    }

    @SuppressWarnings("ReturnValueIgnored")
    private boolean doesReflectionWork() {
        try {
            ClassLoader.class.getDeclaredMethod("getResourceAsStream", String.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void initDefinedMessages(ThreadContext context) {
        for (DefinedMessage definedMessage : DefinedMessage.values()) {
            var str = freezeAndDedupString(Create.newString(context, ByteList.create(definedMessage.getText())));
            definedMessages.put(definedMessage, str);
        }
    }

    private void initThreadStatuses(ThreadContext context) {
        for (RubyThread.Status status : RubyThread.Status.values()) {
            threadStatuses.put(status, freezeAndDedupString(Create.newString(context, status.bytes)));
        }
    }

    @SuppressWarnings("deprecation")
    private RubyClass initContinuation(ThreadContext context) {
        // Bare-bones class for backward compatibility
        if (profile.allowClass("Continuation")) {
            // Some third-party code (racc's cparse ext, at least) uses RubyContinuation directly, so we need this.
            // Most functionality lives in continuation.rb now.
            return RubyContinuation.createContinuation(context, objectClass);
        }
        return null;
    }

    public static final int NIL_PREFILLED_ARRAY_SIZE = RubyArray.ARRAY_DEFAULT_SIZE * 8;
    private final IRubyObject nilPrefilledArray[];

    public IRubyObject[] getNilPrefilledArray() {
        return nilPrefilledArray;
    }

    private void initExceptions(ThreadContext context) {
        ifAllowed("StandardError",          (ruby) -> standardError = RubyStandardError.define(context, exceptionClass));
        ifAllowed("RubyError",              (ruby) -> runtimeError = RubyRuntimeError.define(context, standardError));
        ifAllowed("FrozenError",            (ruby) -> frozenError = RubyFrozenError.define(context, runtimeError));
        ifAllowed("IOError",                (ruby) -> ioError = RubyIOError.define(context, standardError));
        ifAllowed("IO::TimeoutError",       (ruby) -> ioTimeoutError = RubyIO.RubyIOTimeoutError.define(context, ioClass, ioError));
        ifAllowed("ScriptError",            (ruby) -> scriptError = RubyScriptError.define(context, exceptionClass));
        ifAllowed("RangeError",             (ruby) -> rangeError = RubyRangeError.define(context, standardError));
        ifAllowed("SignalException",        (ruby) -> signalException = RubySignalException.define(context, exceptionClass));
        ifAllowed("NameError",              (ruby) -> {
            nameError = RubyNameError.define(context, standardError);
            nameErrorMessage = RubyNameError.RubyNameErrorMessage.define(context, objectClass, nameError);
        });
        ifAllowed("NoMethodError",          (ruby) -> noMethodError = RubyNoMethodError.define(context, nameError));
        ifAllowed("SystemExit",             (ruby) -> systemExit = RubySystemExit.define(context, exceptionClass));
        ifAllowed("LocalJumpError",         (ruby) -> localJumpError = RubyLocalJumpError.define(context, standardError));
        ifAllowed("SystemCallError",        (ruby) -> systemCallError = RubySystemCallError.define(context, standardError));
        ifAllowed("Fatal",                  (ruby) -> fatal = RubyFatal.define(context, exceptionClass, objectClass));
        ifAllowed("Interrupt",              (ruby) -> interrupt = RubyInterrupt.define(context, signalException));
        ifAllowed("TypeError",              (ruby) -> typeError = RubyTypeError.define(context, standardError));
        ifAllowed("NoMatchingPatternError", (ruby) -> noMatchingPatternError = RubyNoMatchingPatternError.define(context, standardError));
        ifAllowed("NoMatchingPatternKeyError", (ruby) -> noMatchingPatternKeyError = RubyNoMatchingPatternKeyError.define(context, standardError));
        ifAllowed("ArgumentError",          (ruby) -> argumentError = RubyArgumentError.define(context, standardError));
        ifAllowed("UncaughtThrowError",     (ruby) -> uncaughtThrowError = RubyUncaughtThrowError.define(context, argumentError));
        ifAllowed("IndexError",             (ruby) -> indexError = RubyIndexError.define(context, standardError));
        ifAllowed("StopIteration",          (ruby) -> stopIteration = RubyStopIteration.define(context, indexError));
        ifAllowed("SyntaxError",            (ruby) -> syntaxError = RubySyntaxError.define(context, scriptError));
        ifAllowed("LoadError",              (ruby) -> loadError = RubyLoadError.define(context, scriptError));
        ifAllowed("NotImplementedError",    (ruby) -> notImplementedError = RubyNotImplementedError.define(context, scriptError));
        ifAllowed("SecurityError",          (ruby) -> securityError = RubySecurityError.define(context, exceptionClass));
        ifAllowed("NoMemoryError",          (ruby) -> noMemoryError = RubyNoMemoryError.define(context, exceptionClass));
        ifAllowed("RegexpError",            (ruby) -> regexpError = RubyRegexpError.define(context, standardError));
        // Proposal to RubyCommons for interrupting Regexps
        ifAllowed("InterruptedRegexpError", (ruby) -> interruptedRegexpError = RubyInterruptedRegexpError.define(context, regexpError));
        ifAllowed("EOFError",               (ruby) -> eofError = RubyEOFError.define(context, ioError));
        ifAllowed("ThreadError",            (ruby) -> threadError = RubyThreadError.define(context, standardError));
        ifAllowed("ConcurrencyError",       (ruby) -> concurrencyError = RubyConcurrencyError.define(context, threadError));
        ifAllowed("SystemStackError",       (ruby) -> systemStackError = RubySystemStackError.define(context, exceptionClass));
        ifAllowed("ZeroDivisionError",      (ruby) -> zeroDivisionError = RubyZeroDivisionError.define(context, standardError));
        ifAllowed("FloatDomainError",       (ruby) -> floatDomainError = RubyFloatDomainError.define(context, rangeError));
        ifAllowed("EncodingError",          (ruby) -> {
            encodingError = RubyEncodingError.define(context, standardError);
            encodingCompatibilityError = RubyEncodingError.RubyCompatibilityError.define(context, encodingError, encodingClass);
            invalidByteSequenceError = RubyEncodingError.RubyInvalidByteSequenceError.define(context, encodingError, encodingClass);
            undefinedConversionError = RubyEncodingError.RubyUndefinedConversionError.define(context, encodingError, encodingClass);
            converterNotFoundError = RubyEncodingError.RubyConverterNotFoundError.define(context, encodingError, encodingClass);
        });
        ifAllowed("Fiber",                  (ruby) -> fiberError = RubyFiberError.define(context, standardError));
        ifAllowed("KeyError",               (ruby) -> keyError = RubyKeyError.define(context, indexError));
        ifAllowed("DomainError",            (ruby) -> mathDomainError = RubyDomainError.define(context, argumentError, mathModule));

        setRegexpTimeoutError(regexpClass.defineClassUnder(context, "TimeoutError", getRegexpError(), RubyRegexpError::new));

        RubyClass runtimeError = this.runtimeError;
        ObjectAllocator runtimeErrorAllocator = runtimeError.getAllocator();

        if (Options.FIBER_SCHEDULER.load()) {
            bufferLockedError = ioBufferClass.defineClassUnder(context, "LockedError", runtimeError, runtimeErrorAllocator);
            bufferAllocationError = ioBufferClass.defineClassUnder(context, "AllocationError", runtimeError, runtimeErrorAllocator);
            bufferAccessError = ioBufferClass.defineClassUnder(context, "AccessError", runtimeError, runtimeErrorAllocator);
            bufferInvalidatedError = ioBufferClass.defineClassUnder(context, "InvalidatedError", runtimeError, runtimeErrorAllocator);
            bufferMaskError = ioBufferClass.defineClassUnder(context, "MaskError", runtimeError, runtimeErrorAllocator);
        }

        initErrno(context);

        if (profile.allowClass("NativeException")) nativeException = NativeException.createClass(context, runtimeError, objectClass);
    }

    private void ifAllowed(String name, Consumer<Ruby> callback) {
        if (profile.allowClass(name)) {
            callback.accept(this);
        }
    }

    private final Map<Integer, RubyClass> errnos = new HashMap<>();

    public RubyClass getErrno(int n) {
        return errnos.get(n);
    }

    /**
     * Create module Errno's Variables.  We have this method since Errno does not have its
     * own java class.
     */
    private void initErrno(ThreadContext context) {
        if (profile.allowModule("Errno")) {
            errnoModule = Define.defineModule(context, "Errno");
            try {
                // define EAGAIN now, so that future EWOULDBLOCK will alias to it
                // see MRI's error.c and its explicit ordering of Errno definitions.
                createSysErr(context, Errno.EAGAIN.intValue(), Errno.EAGAIN.name());

                for (Errno e : Errno.values()) {
                    if (e == Errno.EAGAIN) continue; // already defined above

                    if (Character.isUpperCase(e.name().charAt(0))) createSysErr(context, e.intValue(), e.name());
                }

                // map ENOSYS to NotImplementedError
                errnos.put(Errno.ENOSYS.intValue(), notImplementedError);
            } catch (Exception e) {
                // dump the trace and continue
                // this is currently only here for Android, which seems to have
                // bugs in its enumeration logic
                // http://code.google.com/p/android/issues/detail?id=2812
                LOG.error(e);
            }
        }
    }

    /**
     * Creates a system error.
     * @param i the error code (will probably use a java exception instead)
     * @param name of the error to define.
     **/
    private void createSysErr(ThreadContext context, int i, String name) {
        if (profile.allowClass(name)) {
            if (errnos.get(i) == null) {
                errnos.put(i, errnoModule(context).defineClassUnder(context, name, systemCallError, systemCallError.getAllocator()).
                        defineConstant(context, "Errno", asFixnum(context, i)));
            } else { // already defined a class for this errno, reuse it (JRUBY-4747)
                errnoModule(context).defineConstant(context, name, errnos.get(i));
            }
        }
    }

    /**
     * Load libraries expected to be present after a normal boot.
     *
     * This used to register lazy "builtins" that were shipped with JRuby but did not have a file on the filesystem
     * to load via normal `require` logic. Because of how this interacted (badly) with require-hooking tools like
     * bootsnap, we have moved to having all builtins as actual files rather than special virtual entries.
     */
    private void initJavaSupport(ThreadContext context) {
        // load JRuby internals, which loads Java support
        // if we can't use reflection, 'jruby' and 'java' won't work; no load.
        boolean reflectionWorks = doesReflectionWork();

        if (reflectionWorks) {
            new Java().load(context.runtime, false);
            new JRubyUtilLibrary().load(context.runtime, false);

            loadService.provide("java.rb");
            loadService.provide("jruby/util.rb");
        }
    }

    private void initRubyKernel() {
        // load Ruby parts of core
        loadService.loadFromClassLoader(getClassLoader(), "jruby/kernel.rb", false);
    }

    private void initRubyPreludes() {
        // We cannot load any .rb and debug new parser features
        if (RubyInstanceConfig.DEBUG_PARSER) return;

        // load Ruby parts of core
        loadService.loadFromClassLoader(getClassLoader(), "jruby/preludes.rb", false);
    }

    public IRManager getIRManager() {
        return irManager;
    }

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    public IRubyObject getTopSelf() {
        return topSelf;
    }

    public void setCurrentDirectory(String dir) {
        currentDirectory = dir;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setCurrentLine(int line) {
        currentLine = line;
    }

    public int getCurrentLine() {
        return currentLine;
    }

    public void setArgsFile(IRubyObject argsFile) {
        this.argsFile = argsFile;
    }

    public IRubyObject getArgsFile() {
        return argsFile;
    }

    // Nothing uses this anymore
    @Deprecated(since = "10.0")
    public RubyModule getEtc() {
        return etcModule;
    }

    public void setEtc(RubyModule etcModule) {
        this.etcModule = etcModule;
    }

    public RubyClass getObject() {
        return objectClass;
    }

    public RubyClass getBasicObject() {
        return basicObjectClass;
    }

    public RubyClass getModule() {
        return moduleClass;
    }

    public RubyClass getRefinement() {
        return refinementClass;
    }

    public RubyClass getClassClass() {
        return classClass;
    }

    public RubyModule getKernel() {
        return kernelModule;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Cached DynamicMethod objects, used for direct dispatch or for short
    // circuiting dynamic invocation logic.
    ///////////////////////////////////////////////////////////////////////////

    public DynamicMethod getPrivateMethodMissing() {
        return privateMethodMissing;
    }

    public void setPrivateMethodMissing(DynamicMethod method) {
        privateMethodMissing = method;
    }

    public DynamicMethod getProtectedMethodMissing() {
        return protectedMethodMissing;
    }

    public void setProtectedMethodMissing(DynamicMethod method) {
        protectedMethodMissing = method;
    }

    public DynamicMethod getVariableMethodMissing() {
        return variableMethodMissing;
    }

    public void setVariableMethodMissing(DynamicMethod method) {
        variableMethodMissing = method;
    }

    public DynamicMethod getSuperMethodMissing() {
        return superMethodMissing;
    }

    public void setSuperMethodMissing(DynamicMethod method) {
        superMethodMissing = method;
    }

    public DynamicMethod getNormalMethodMissing() {
        return normalMethodMissing;
    }

    public void setNormalMethodMissing(DynamicMethod method) {
        normalMethodMissing = method;
    }

    public DynamicMethod getDefaultMethodMissing() {
        return defaultMethodMissing;
    }

    public boolean isDefaultMethodMissing(DynamicMethod method) {
        return defaultMethodMissing == method || defaultModuleMethodMissing == method;

    }

    public void setDefaultMethodMissing(DynamicMethod method, DynamicMethod moduleMethod) {
        defaultMethodMissing = method;
        defaultModuleMethodMissing = moduleMethod;
    }

    public DynamicMethod getRespondToMethod() {
        return respondTo;
    }

    public void setRespondToMethod(DynamicMethod rtm) {
        this.respondTo = rtm;
    }

    public DynamicMethod getRespondToMissingMethod() {
        return respondToMissing;
    }

    public void setRespondToMissingMethod(DynamicMethod rtmm) {
        this.respondToMissing = rtmm;
    }

    public RubyClass getDummy() {
        return dummyClass;
    }

    public RubyModule getComparable() {
        return comparableModule;
    }

    public RubyClass getNumeric() {
        return numericClass;
    }

    public RubyClass getFloat() {
        return floatClass;
    }

    public RubyClass getInteger() {
        return integerClass;
    }

    public RubyClass getFixnum() {
        return fixnumClass;
    }

    public RubyClass getComplex() {
        return complexClass;
    }

    public RubyClass getRational() {
        return rationalClass;
    }

    public RubyModule getEnumerable() {
        return enumerableModule;
    }

    public RubyClass getEnumerator() {
        return enumeratorClass;
    }

    public RubyClass getYielder() {
        return yielderClass;
    }

    public RubyClass getGenerator() {
        return generatorClass;
    }

    public RubyClass getChain() {
        return chainClass;
    }

    public RubyClass getArithmeticSequence() {
        return aseqClass;
    }

    public RubyClass getProducer() {
        return producerClass;
    }

    public RubyClass getFiber() {
        return fiberClass;
    }

    public RubyClass getString() {
        return stringClass;
    }

    public RubyClass getEncoding() {
        return encodingClass;
    }

    public RubyClass getConverter() {
        return converterClass;
    }

    public RubyClass getSymbol() {
        return symbolClass;
    }

    public RubyClass getArray() {
        return arrayClass;
    }

    public RubyClass getHash() {
        return hashClass;
    }

    public RubyClass getRange() {
        return rangeClass;
    }

    /** Returns the "true" instance from the instance pool.
     * @return The "true" instance.
     */
    public RubyBoolean getTrue() {
        return trueObject;
    }

    public RubyString getTrueString() {
        return trueString;
    }

    public RubyString getNilString() {
        return nilString;
    }

    public RubyString getNilInspectString() {
        return nilInspectString;
    }

    /** Returns the "false" instance from the instance pool.
     * @return The "false" instance.
     */
    public RubyBoolean getFalse() {
        return falseObject;
    }

    public RubyString getFalseString() {
        return falseString;
    }

    /** Returns the "nil" singleton instance.
     * @return "nil"
     */
    public IRubyObject getNil() {
        return nilObject;
    }

    public IRubyObject[] getSingleNilArray() {
        return singleNilArray;
    }

    public RubyClass getNilClass() {
        return nilClass;
    }

    public RubyClass getTrueClass() {
        return trueClass;
    }

    public RubyClass getFalseClass() {
        return falseClass;
    }

    public RubyClass getProc() {
        return procClass;
    }

    public RubyClass getBinding() {
        return bindingClass;
    }

    public RubyClass getMethod() {
        return methodClass;
    }

    public RubyClass getUnboundMethod() {
        return unboundMethodClass;
    }

    public RubyClass getMatchData() {
        return matchDataClass;
    }

    public RubyClass getRegexp() {
        return regexpClass;
    }

    public RubyClass getTime() {
        return timeClass;
    }

    public RubyModule getMath() {
        return mathModule;
    }

    public RubyModule getMarshal() {
        return marshalModule;
    }

    /**
     * @return
     * @deprecated Use {@link org.jruby.api.Access#integerClass(ThreadContext)} instead.
     */
    @Deprecated(since = "10.0")
    public RubyClass getBignum() {
        return integerClass;
    }

    public RubyClass getDateError() {
        return this.dateErrorClass;
    }

    public void setDateError(RubyClass dateError) {
        this.dateErrorClass = dateError;
    }

    public RubyClass getDir() {
        return dirClass;
    }

    public RubyClass getFile() {
        return fileClass;
    }

    public RubyClass getFileStat() {
        return fileStatClass;
    }

    public RubyModule getFileTest() {
        return fileTestModule;
    }

    public RubyClass getIO() {
        return ioClass;
    }

    public RubyClass getIOBuffer() {
        return ioBufferClass;
    }

    public RubyClass getThread() {
        return threadClass;
    }

    public RubyClass getThreadGroup() {
        return threadGroupClass;
    }

    public RubyThreadGroup getDefaultThreadGroup() {
        return defaultThreadGroup;
    }
    void setDefaultThreadGroup(RubyThreadGroup defaultThreadGroup) {
        this.defaultThreadGroup = defaultThreadGroup;
    }

    public RubyClass getContinuation() {
        return continuationClass;
    }

    public RubyClass getStructClass() {
        return structClass;
    }

    public RubyClass getRandomClass() {
        return randomClass;
    }

    public IRubyObject getTmsStruct() {
        return tmsStruct;
    }
    void setTmsStruct(RubyClass tmsStruct) {
        this.tmsStruct = tmsStruct;
    }

    public IRubyObject getPasswdStruct() {
        return passwdStruct;
    }
    public void setPasswdStruct(RubyClass passwdStruct) {
        this.passwdStruct = passwdStruct;
    }

    public IRubyObject getGroupStruct() {
        return groupStruct;
    }
    public void setGroupStruct(RubyClass groupStruct) {
        this.groupStruct = groupStruct;
    }

    public RubyModule getGC() {
        return gcModule;
    }

    public RubyModule getObjectSpaceModule() {
        return objectSpaceModule;
    }

    public RubyModule getProcess() {
        return processModule;
    }

    public RubyClass getProcStatus() {
        return procStatusClass;
    }
    void setProcStatus(RubyClass procStatusClass) {
        this.procStatusClass = procStatusClass;
    }

    public RubyModule getProcUID() {
        return procUIDModule;
    }
    void setProcUID(RubyModule procUIDModule) {
        this.procUIDModule = procUIDModule;
    }

    public RubyModule getProcGID() {
        return procGIDModule;
    }
    void setProcGID(RubyModule procGIDModule) {
        this.procGIDModule = procGIDModule;
    }

    public RubyModule getProcSysModule() {
        return procSysModule;
    }
    void setProcSys(RubyModule procSysModule) {
        this.procSysModule = procSysModule;
    }

    public RubyModule getPrecision() {
        return precisionModule;
    }
    void setPrecision(RubyModule precisionModule) {
        this.precisionModule = precisionModule;
    }

    public RubyHash getENV() {
        return envObject;
    }

    public RubyClass getLocation() {
        return locationClass;
    }

    public void setLocation(RubyClass location) {
        this.locationClass = location;
    }

    public RubyClass getMutex() {
        return mutexClass;
    }

    public RubyClass getConditionVariable() {
        return conditionVariableClass;
    }

    public RubyClass getQueue() {
        return queueClass;
    }

    public RubyClass getClosedQueueError() {
        return closedQueueError;
    }

    public RubyClass getSizedQueueClass() {
        return sizedQueueClass;
    }

    public RubyModule getWarning() {
        return warningModule;
    }

    public RubyModule getErrno() {
        return errnoModule;
    }

    public RubyClass getException() {
        return exceptionClass;
    }

    public RubyClass getNameError() {
        return nameError;
    }

    public RubyClass getNameErrorMessage() {
        return nameErrorMessage;
    }

    public RubyClass getNoMethodError() {
        return noMethodError;
    }

    public RubyClass getSignalException() {
        return signalException;
    }

    public RubyClass getRangeError() {
        return rangeError;
    }

    public RubyClass getSystemExit() {
        return systemExit;
    }

    public RubyClass getLocalJumpError() {
        return localJumpError;
    }

    public RubyClass getNativeException() {
        return nativeException;
    }

    public RubyClass getSystemCallError() {
        return systemCallError;
    }

    public RubyClass getKeyError() {
        return keyError;
    }

    public RubyClass getFatal() {
        return fatal;
    }

    public RubyClass getInterrupt() {
        return interrupt;
    }

    public RubyClass getTypeError() {
        return typeError;
    }

    public RubyClass getNoMatchingPatternError() {
        return noMatchingPatternError;
    }

    public RubyClass getArgumentError() {
        return argumentError;
    }

    public RubyClass getUncaughtThrowError() {
        return uncaughtThrowError;
    }

    public RubyClass getIndexError() {
        return indexError;
    }

    public RubyClass getStopIteration() {
        return stopIteration;
    }

    public RubyClass getSyntaxError() {
        return syntaxError;
    }

    public RubyClass getStandardError() {
        return standardError;
    }

    public RubyClass getRuntimeError() {
        return runtimeError;
    }

    public RubyClass getFrozenError() {
        return frozenError;
    }

    public RubyClass getIOError() {
        return ioError;
    }

    public RubyClass getIOTimeoutError() {
        return ioTimeoutError;
    }

    public RubyClass getLoadError() {
        return loadError;
    }

    public RubyClass getNotImplementedError() {
        return notImplementedError;
    }

    public RubyClass getSecurityError() {
        return securityError;
    }

    public RubyClass getNoMemoryError() {
        return noMemoryError;
    }

    public RubyClass getRegexpError() {
        return regexpError;
    }

    public RubyClass getInterruptedRegexpError() {
        return interruptedRegexpError;
    }

    public RubyClass getEOFError() {
        return eofError;
    }

    public RubyClass getThreadError() {
        return threadError;
    }

    public RubyClass getConcurrencyError() {
        return concurrencyError;
    }

    public RubyClass getSystemStackError() {
        return systemStackError;
    }

    public RubyClass getZeroDivisionError() {
        return zeroDivisionError;
    }

    public RubyClass getFloatDomainError() {
        return floatDomainError;
    }

    public RubyClass getMathDomainError() {
        return mathDomainError;
    }

    public RubyClass getEncodingError() {
        return encodingError;
    }

    public RubyClass getEncodingCompatibilityError() {
        return encodingCompatibilityError;
    }

    public RubyClass getConverterNotFoundError() {
        return converterNotFoundError;
    }

    public RubyClass getFiberError() {
        return fiberError;
    }

    public RubyClass getUndefinedConversionError() {
        return undefinedConversionError;
    }

    public RubyClass getInvalidByteSequenceError() {
        return invalidByteSequenceError;
    }

    public RubyClass getBufferLockedError() {
        return bufferLockedError;
    }

    public RubyClass getBufferAllocationError() {
        return bufferAllocationError;
    }

    public RubyClass getBufferAccessError() {
        return bufferAccessError;
    }

    public RubyClass getBufferInvalidatedError() {
        return bufferInvalidatedError;
    }

    public RubyClass getBufferMaskError() {
        return bufferMaskError;
    }

    public RubyClass getData() {
        return dataClass;
    }

    /** The default Ruby Random object for this runtime */
    private RubyRandom defaultRandom;

    public RubyRandom getDefaultRandom() {
        return defaultRandom;
    }

    public void setDefaultRandom(RubyRandom random) {
        this.defaultRandom = random;
    }

    /**
     * @return $VERBOSE value
     */
    public IRubyObject getVerbose() {
        return verboseValue;
    }

    /**
     * @return $VERBOSE value as a Java primitive
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * If the user explicitly disabled warnings using: {@link #setWarningsEnabled(boolean)} return false.
     *
     * Otherwise fallback to a $VERBOSE value check (which is the default behavior).
     *
     * @return whether warnings are enabled
     */
    public boolean warningsEnabled() {
        return warningsEnabled && verboseWarnings;
    }

    /**
     * Setter that enables/disabled warnings (without changing verbose mode).
     * @param warningsEnabled
     * @see #setVerbose(IRubyObject)
     */
    public void setWarningsEnabled(final boolean warningsEnabled) {
        this.warningsEnabled = warningsEnabled;
    }

    /**
     * Sets the runtime verbosity ($VERBOSE global which usually gets set to nil/false or true).
     * <p>Note: warnings get enabled whenever the verbose level is set to a value that is not nil.</p>
     * @param verbose the verbose ruby value
     */
    public void setVerbose(final IRubyObject verbose) {
        this.verbose = verbose.isTrue();
        this.verboseValue = verbose;
        verboseWarnings = !verbose.isNil();
    }

    /**
     * Sets the $VERBOSE level
     * @param verbose null, true and false are all valid
     * @see #setVerbose(IRubyObject)
     */
    public void setVerbose(final Boolean verbose) {
        setVerbose(verbose == null ? nilObject : (verbose ? trueObject : falseObject));
    }

    /**
     * @return $DEBUG value
     */
    public IRubyObject getDebug() {
        return debug ? trueObject : falseObject;
    }

    /**
     * @return $DEBUG value as a boolean
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Setter for property isDebug.
     * @param debug the $DEBUG value
     */
    public void setDebug(IRubyObject debug) {
        setDebug(debug.isTrue());
    }

    /**
     * Sets the $DEBUG flag
     * @param debug
     */
    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    /**
     * Get the current enabled warning categories.
     *
     * @return a set of the currently-enabled warning categories
     */
    public Set<RubyWarnings.Category> getWarningCategories() {
        return warningCategories;
    }

    public JavaSupport getJavaSupport() {
        return javaSupport;
    }

    public RubyObjectSpecializer getObjectSpecializer() {
        return objectSpecializer;
    }

    public static ClassLoader getClassLoader() {
        // we try to getService the classloader that loaded JRuby, falling back on System
        ClassLoader loader = Ruby.class.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        return loader;
    }

    public JRubyClassLoader getJRubyClassLoader() {
        return jrubyClassLoader;
    }

    /** Defines a global variable
     */
    public void defineVariable(final GlobalVariable variable, org.jruby.internal.runtime.GlobalVariable.Scope scope) {
        globalVariables.define(variable.name(), new IAccessor() {
            @Override
            public IRubyObject getValue() {
                return variable.get();
            }

            @Override
            public IRubyObject setValue(IRubyObject newValue) {
                return variable.set(newValue);
            }
        }, scope);
    }

    /** defines a readonly global variable
     *
     */
    public void defineReadonlyVariable(String name, IRubyObject value, org.jruby.internal.runtime.GlobalVariable.Scope scope) {
        globalVariables.defineReadonly(name, new ValueAccessor(value), scope);
    }

    // Obsolete parseFile function
    @Deprecated
    public Node parseFile(InputStream in, String file, DynamicScope scope) {
        // Note: We don't know what the caller so we have to assume it may be a toplevel binding use so it uses main parse.
        return (Node) getParserManager().parseMainFile(file, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN).getAST();
    }

    @Deprecated
    public ParseResult parseFile(String file, InputStream in, DynamicScope scope) {
        // Note: We don't know what the caller so we have to assume it may be a toplevel binding use so it uses main parse.
       return getParserManager().parseMainFile(file, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN);
    }

    @Deprecated
    public Node parseFile(InputStream in, String file, DynamicScope scope, int lineNumber) {
        // Note: We don't know what the caller so we have to assume it may be a toplevel binding use so it uses main parse.
        return (Node) getParserManager().parseMainFile(file, lineNumber, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN).getAST();
    }

    @Deprecated
    public ParseResult parseFile(String file, InputStream in, DynamicScope scope, int lineNumber) {
        // Note: We don't know what the caller so we have to assume it may be a toplevel binding use so it uses main parse.
        return getParserManager().parseMainFile(file, lineNumber, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN);
    }

    @Deprecated
    public Node parseFileFromMain(InputStream in, String file, DynamicScope scope) {
        return (Node) getParserManager().parseMainFile(file, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN).getAST();
    }

    @Deprecated
    public ParseResult parseFileFromMain(String file, InputStream in, DynamicScope scope) {
        return getParserManager().parseMainFile(file, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN);
    }

    @Deprecated
    private Node parseFileFromMainAndGetAST(InputStream in, String file, DynamicScope scope) {
        // Note: We don't know what the caller so we have to assume it may be a toplevel binding use so it uses main parse.
        return (Node) getParserManager().parseMainFile(file, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN).getAST();
    }

    @Deprecated
    private Node parseFileAndGetAST(InputStream in, String file, DynamicScope scope, int lineNumber, boolean isFromMain) {
         if (isFromMain) {
             return (Node) getParserManager().parseMainFile(file, lineNumber, in, setupSourceEncoding(UTF8Encoding.INSTANCE), scope, MAIN).getAST();
         } else {
             return (Node) getParserManager().parseFile(file, lineNumber, in, setupSourceEncoding(UTF8Encoding.INSTANCE)).getAST();
         }
     }

    @Deprecated
    public Node parseInline(InputStream in, String file, DynamicScope scope) {
        return (Node) getParserManager().parseMainFile(file, 0, in, setupSourceEncoding(getEncodingService().getLocaleEncoding()), scope, INLINE).getAST();
    }

    public Encoding setupSourceEncoding(Encoding defaultEncoding) {
        if (config.getSourceEncoding() == null) return defaultEncoding;

        if (config.isVerbose()) {
            config.getError().println("-K is specified; it is for 1.8 compatibility and may cause odd behavior");
        }
        return getEncodingService().getEncodingFromString(config.getSourceEncoding());
    }

    @Deprecated
    public Node parseEval(String source, String file, DynamicScope scope, int lineNumber) {
        return (Node) getParserManager().parseEval(file, lineNumber, source, scope).getAST();
    }

    private byte[] encodeToBytes(String string) {
        Charset charset = getDefaultCharset();

        return charset == null ? string.getBytes() : string.getBytes(charset);
    }

    @Deprecated
    public ParseResult parseEval(ByteList source, String file, DynamicScope scope, int lineNumber) {
        return getParserManager().parseEval(file, lineNumber, source, scope);
    }

    @Deprecated
    public Node parse(ByteList content, String file, DynamicScope scope, int lineNumber, boolean extraPositionInformation) {
        InputStream in = new ByteArrayInputStream(content.getUnsafeBytes(), content.begin(), content.length());
        if (extraPositionInformation) {
            return (Node) getParserManager().parseMainFile(file, lineNumber, in, content.getEncoding(), scope, INLINE).getAST();
        } else {
            return (Node) getParserManager().parseFile(file, lineNumber, in, content.getEncoding()).getAST();
        }
    }

    public ThreadService getThreadService() {
        return threadService;
    }

    public ThreadContext getCurrentContext() {
        return ThreadService.getCurrentContext(threadService);
    }

    /**
     * Returns the loadService.
     * @return ILoadService
     */
    public LoadService getLoadService() {
        return loadService;
    }

    /**
     * This is an internal encoding if actually specified via default_internal=
     * or passed in via -E.
     *
     * @return null or encoding
     */
    public Encoding getDefaultInternalEncoding() {
        return encodingService.getDefaultInternalEncoding();
    }

    public void setDefaultInternalEncoding(Encoding defaultInternalEncoding) {
        encodingService.setDefaultInternalEncoding(defaultInternalEncoding);
    }

    public Encoding getDefaultExternalEncoding() {
        return encodingService.getDefaultExternalEncoding();
    }

    public void setDefaultExternalEncoding(Encoding defaultExternalEncoding) {
        encodingService.setDefaultExternalEncoding(defaultExternalEncoding);
    }

    public Encoding getDefaultFilesystemEncoding() {
        return encodingService.getDefaultFilesystemEncoding();
    }

    public void setDefaultFilesystemEncoding(Encoding defaultFilesystemEncoding) {
        encodingService.setDefaultFilesystemEncoding(defaultFilesystemEncoding);
    }

    /**
     * Get the default java.nio.charset.Charset for the current default internal encoding.
     */
    public Charset getDefaultCharset() {
        try {
            return EncodingUtils.charsetForEncoding(getDefaultEncoding());
        } catch (UnsupportedCharsetException e) {
            return null;
        }
    }

    /**
     * Return the default internal encoding, if set, or UTF-8 by default.
     *
     * @return the default encoding used for new Ruby strings
     */
    public Encoding getDefaultEncoding() {
        Encoding enc = getDefaultInternalEncoding();
        if (enc == null) {
            enc = UTF8Encoding.INSTANCE;
        }
        return enc;
    }

    public EncodingService getEncodingService() {
        return encodingService;
    }

    public RubyWarnings getWarnings() {
        return warnings;
    }

    WarnCallback getRegexpWarnings() {
        return regexpWarnings;
    }

    public IRubyObject getStderr() {
        return getGlobalVariables().get("$stderr");
    }

    /**
     * Return the original stderr with which this runtime was initialized.
     *
     * Used for fast-path comparisons when printing error info directly to stderr.
     *
     * @return the original stderr with which this runtime was initialized
     */
    public IRubyObject getOriginalStderr() {
        return originalStderr;
    }

    void setOriginalStderr(IRubyObject stderr) {
        this.originalStderr = stderr;
    }

    public PrintStream getErrorStream() {
        // FIXME: We can't guarantee this will always be a RubyIO...so the old code here is not safe
        /*java.io.OutputStream os = ((RubyIO) getGlobalVariables().getService("$stderr")).getOutStream();
        if(null != os) {
            return new PrintStream(os);
        } else {
            return new PrintStream(new org.jruby.util.SwallowingOutputStream());
        }*/
        return new PrintStream(new IOOutputStream(getGlobalVariables().get("$stderr")));
    }

    public InputStream getInputStream() {
        return new IOInputStream(getGlobalVariables().get("$stdin"));
    }

    public PrintStream getOutputStream() {
        return new PrintStream(new IOOutputStream(getGlobalVariables().get("$stdout")));
    }

    public RubyModule getClassFromPath(final String path) {
        return getClassFromPath(path, getTypeError(), true);
    }

    /**
     * Find module from a string (e.g. Foo, Foo::Bar::Car).
     *
     * @param path the path to be searched.
     * @param undefinedExceptionClass exception type to be thrown when it cannot be found.
     * @param flexibleSearch use getConstant vs getConstantAt (former will find inherited constants from parents and fire const_missing).
     * @return the module or null when flexible search is false and a constant cannot be found.
     */
    public RubyModule getClassFromPath(final String path, RubyClass undefinedExceptionClass, boolean flexibleSearch) {
        var context = getCurrentContext();

        if (path.length() == 0 || path.charAt(0) == '#') {
            throw typeError(context, str(this, "can't retrieve anonymous class ", ids(this, path)));
        }

        RubyModule clazz = getObject();
        int pbeg = 0, p = 0;
        for (int length = path.length(); p < length; ) {
            while ( p < length && path.charAt(p) != ':' ) p++;

            final String str = path.substring(pbeg, p);

            if ( p < length && path.charAt(p) == ':' ) {
                if ( ++p < length && path.charAt(p) != ':' ) {
                    throw classPathUndefinedException(path, undefinedExceptionClass, p);
                }
                pbeg = ++p;
            }

            // FIXME: JI depends on const_missing getting called from Marshal.load (ruby objests do not).  We should marshal JI objects differently so we do not differentiate here.
            IRubyObject cc = flexibleSearch || isJavaPackageOrJavaClassProxyType(clazz) ?
                    clazz.getConstant(context, str) : clazz.getConstantAt(context, str);

            if (cc == null) throw classPathUndefinedException(path, undefinedExceptionClass, p);

            if (!(cc instanceof RubyModule mod)) {
                throw typeError(context, str(this, ids(this, path), " does not refer to class/module"));
            }
            clazz = mod;
        }

        return clazz;
    }

    private RaiseException classPathUndefinedException(String path, RubyClass undefinedExceptionClass, int p) {
        return newRaiseException(undefinedExceptionClass, str(this, "undefined class/module ", ids(this, path.substring(0, p))));
    }

    private static boolean isJavaPackageOrJavaClassProxyType(final RubyModule type) {
        return type instanceof JavaPackage || ClassUtils.isJavaClassProxyType(type);
    }

    /**
     * Prints a Ruby exception with backtrace to the configured stderr stream.
     *
     * MRI: eval.c - error_print()
     *
     */
    public void printError(final RubyException ex) {
        if (ex == null) return;

        boolean formatted =
                getStderr() == getOriginalStderr() &&
                        getErr() == System.err &&
                        getPosix().isatty(FileDescriptor.err);

        String backtrace = config.getTraceType().printBacktrace(ex, formatted);
        printErrorString(backtrace);
    }

    /**
     * Prints an exception to System.err.
     *
     * @param ex
     */
    public void printError(final Throwable ex) {
        if (ex instanceof RaiseException) {
            printError(((RaiseException) ex).getException());
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ex.printStackTrace(new PrintStream(baos));

        try {
            printErrorString(baos.toByteArray());
        } catch (Exception e) {
            try {
                System.err.write(baos.toByteArray());
            } catch (IOException ioe) {
                ioe.initCause(e);
                throw new RuntimeException("BUG: could not write exception trace", ioe);
            }
        }
    }

    /**
     * Prints a string directly to the stderr channel, if default, or via dynamic dispatch otherwise.
     *
     * @param msg the string to print
     */
    public void printErrorString(String msg) {
        IRubyObject stderr = getStderr();

        WritableByteChannel writeChannel;
        if (stderr == getOriginalStderr() &&
                (writeChannel = ((RubyIO) stderr).getOpenFile().fd().chWrite) != null) {
            Writer writer = Channels.newWriter(writeChannel, "UTF-8");
            try {
                writer.write(msg);
                writer.flush();
            } catch (IOException ioe) {
                // ignore as in CRuby
            }
        } else {
            getErrorStream().print(msg);
        }
    }

    /**
     * Prints a string directly to the stderr channel, if default, or via dynamic dispatch otherwise.
     *
     * @param msg the string to print
     */
    public void printErrorString(byte[] msg) {
        IRubyObject stderr = getGlobalVariables().get("$stderr");

        try {
            WritableByteChannel writeChannel;
            if (stderr == getOriginalStderr() &&
                    (writeChannel = ((RubyIO) stderr).getOpenFile().fd().chWrite) != null) {
                    writeChannel.write(ByteBuffer.wrap(msg));
            } else {
                getErrorStream().write(msg);
            }
        } catch (IOException ioe) {
            // ignore as in CRuby
        }
    }

    static final String ROOT_FRAME_NAME = "(root)";

    public void loadFile(String scriptName, InputStream in, boolean wrap) {
        IRubyObject self = wrap ? getTopSelf().rbClone() : getTopSelf();

        if (!wrap && Options.COMPILE_CACHE_CLASSES.load()) {
            Script script = tryScriptFromClass(scriptName);

            if (script != null) {
                runScript(script, self, wrap);
                return;
            }
        }

        ThreadContext context = getCurrentContext();

        try {
            ParseResult parseResult;
            context.preNodeEval(self);
            parseResult = getParserManager().parseFile(scriptName, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE));

            // toss an anonymous module into the search path
            if (wrap) wrapWithModule((RubyBasicObject) self, parseResult);

            runInterpreter(context, parseResult, self);
        } finally {
            context.postNodeEval();
        }
    }

    public void loadScope(IRScope scope, boolean wrap) {
        IRubyObject self = wrap ? getTopSelf().rbClone() : getTopSelf();

        if (wrap) {
            // toss an anonymous module into the search path
            scope.getStaticScope().setModule(new RubyModule(this));
        }

        runInterpreter(getCurrentContext(), scope, self);
    }

    public void compileAndLoadFile(String filename, InputStream in, boolean wrap) {
        IRubyObject self = wrap ? getTopSelf().rbClone() : getTopSelf();

        if (!wrap && Options.COMPILE_CACHE_CLASSES.load()) {
            Script script = tryScriptFromClass(filename);

            if (script != null) {
                runScript(script, self, wrap);
                return;
            }
        }

        ParseResult parseResult = getParserManager().parseFile(filename, 0, in, setupSourceEncoding(UTF8Encoding.INSTANCE));

        if (wrap) {
            wrapWithModule((RubyBasicObject) self, parseResult);
        } else {
            parseResult.getStaticScope().setModule(getObject());
        }

        runNormally(parseResult, self, wrap);
    }

    public StaticScope setupWrappedToplevel(IRubyObject self, StaticScope top) {
        RubyModule wrapper = loadService.getWrapperSelf();

        if (wrapper == null || wrapper.isNil()) {
            wrapper = new RubyModule(this);
        }

        // toss an anonymous module into the search path
        ((RubyBasicObject) self).extend(new IRubyObject[] {wrapper});
        StaticScope newTop = staticScopeFactory.newLocalScope(null);
        top.setPreviousCRefScope(newTop);
        top.setModule(wrapper);

        return newTop;
    }

    private void wrapWithModule(RubyBasicObject self, ParseResult result) {
        setupWrappedToplevel(self, result.getStaticScope());
    }

    public void loadScript(Script script) {
        loadScript(script, false);
    }

    public void loadScript(Script script, boolean wrap) {
        script.load(getCurrentContext(), getTopSelf(), wrap);
    }

    /**
     * Load the given BasicLibraryService instance, wrapping it in Ruby framing
     * to ensure it is isolated from any parent scope.
     *
     * @param extName The name of the extension, to go on the frame wrapping it
     * @param extension The extension object to load
     * @param wrap Whether to use a new "self" for toplevel
     */
    public void loadExtension(String extName, BasicLibraryService extension, boolean wrap) {
        ThreadContext context = getCurrentContext();
        var topSelf = new RubyObject(this, objectClass);
        IRubyObject self = wrap ? TopSelfFactory.finishTopSelf(context, topSelf, objectClass, true) : getTopSelf();

        try {
            context.preExtensionLoad(self);

            extension.basicLoad(this);
        } catch (IOException ioe) {
            throw newIOErrorFromException(ioe);
        } finally {
            context.postNodeEval();
        }
    }

    public void addBoundMethod(String className, String methodName, String rubyName) {
        Map<String, String> javaToRuby = boundMethods.computeIfAbsent(className, s -> new ConcurrentHashMap<>(2, 0.9f, 2));
        javaToRuby.putIfAbsent(methodName, rubyName);
    }

    public void addBoundMethods(String className, String... tuples) {
        Map<String, String> javaToRuby = boundMethods.computeIfAbsent(className, s -> new ConcurrentHashMap<>(2, 0.9f, 2));
        for (int i = 0; i < tuples.length; i += 2) {
            javaToRuby.putIfAbsent(tuples[i], tuples[i+1]);
        }
    }

    // Used by generated populators
    public void addBoundMethods(int tuplesIndex, String... classNamesAndTuples) {
        Map<String, String> javaToRuby = new HashMap<>((classNamesAndTuples.length - tuplesIndex) / 2 + 1, 1);
        for (int i = tuplesIndex; i < classNamesAndTuples.length; i += 2) {
            javaToRuby.put(classNamesAndTuples[i], classNamesAndTuples[i+1]);
        }

        for (int i = 0; i < tuplesIndex; i++) {
            String className = classNamesAndTuples[i];
            Map<String, String> javaToRubyForClass = boundMethods.computeIfAbsent(className, s -> new ConcurrentHashMap<>((int)(javaToRuby.size() / 0.9f) + 1, 0.9f, 2));
            for (Map.Entry<String, String> entry : javaToRuby.entrySet()) {
                javaToRubyForClass.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
    }

    public Map<String, Map<String, String>> getBoundMethods() {
        return boundMethods;
    }

    private final TraceEventManager traceEvents = new TraceEventManager(this);

    public TraceEventManager getTraceEvents() {
        return traceEvents;
    }

    public GlobalVariables getGlobalVariables() {
        return globalVariables;
    }

    /**
     * Add an exit function to be run on runtime exit. Functions are run in FILO order.
     *
     * @param func the function to be run
     */
    public void pushExitFunction(ExitFunction func) {
        exitBlocks.add(0, func);
    }

    /**
     * Push block onto exit stack.  When runtime environment exits
     * these blocks will be evaluated.
     *
     * @return the element that was pushed onto stack
     */
    public IRubyObject pushExitBlock(RubyProc proc) {
        ProcExitFunction func = new ProcExitFunction(proc);

        pushExitFunction(func);

        return proc;
    }

    /**
     * Add a post-termination exit function that should be run to shut down JRuby internal services.
     *
     * This will be run toward the end of teardown, after all user code has finished executing (e.g. at_exit
     * hooks and user-defined finalizers). The exit functions registered here are run in FILO order.
     *
     * @param postExit the {@link ExitFunction} to run after user exit hooks have completed
     */
    public void pushPostExitFunction(ExitFunction postExit) {
        postExitBlocks.add(0, postExit);
    }

    /**
     * It is possible for looping or repeated execution to encounter the same END
     * block multiple times.  Rather than store extra runtime state we will just
     * make sure it is not already registered.  at_exit by contrast can push the
     * same block many times (and should use pushExitBlock).
     */
    public void pushEndBlock(RubyProc proc) {
        if (alreadyRegisteredEndBlock(proc)) return;
        pushExitBlock(proc);
    }

    private boolean alreadyRegisteredEndBlock(RubyProc newProc) {
        if (exitBlocks.stream().anyMatch((func) -> func.matches(newProc))) {
            return true;
        }
        return false;
    }

    // use this for JRuby-internal finalizers
    public void addInternalFinalizer(Finalizable finalizer) {
        internalFinalizersMutex.lock();
        try {
            internalFinalizers.put(finalizer, null);
        } finally {
            internalFinalizersMutex.unlock();
        }
    }

    // this method is for finalizers registered via ObjectSpace
    public void addFinalizer(Finalizable finalizer) {
        finalizersMutex.lock();
        try {
            finalizers.put(finalizer, null);
        } finally {
            finalizersMutex.unlock();
        }
    }

    public void removeInternalFinalizer(Finalizable finalizer) {
        internalFinalizersMutex.lock();
        try {
            internalFinalizers.remove(finalizer);
        } finally {
            internalFinalizersMutex.unlock();
        }
    }

    public void removeFinalizer(Finalizable finalizer) {
        finalizersMutex.lock();
        try {
            finalizers.remove(finalizer);
        } finally {
            finalizersMutex.unlock();
        }
    }

    /**
     * Make sure Kernel#at_exit procs getService invoked on runtime shutdown.
     * This method needs to be explicitly called to work properly.
     * I thought about using finalize(), but that did not work and I
     * am not sure the runtime will be at a state to run procs by the
     * time Ruby is going away.  This method can contain any other
     * things that need to be cleaned up at shutdown.
     */
    public void tearDown() {
        tearDown(true);
    }

    // tearDown(boolean) has been added for embedding API. When an error
    // occurs in Ruby code, JRuby does system exit abruptly, no chance to
    // catch exception. This makes debugging really hard. This is why
    // tearDown(boolean) exists.
    public void tearDown(boolean systemExit) {
        final ThreadContext context = getCurrentContext();

        int status = userTeardown(context);

        systemTeardown(context);

        if (systemExit && status != 0) {
            throw newSystemExit(status);
        }

        // This is a rather gross way to ensure nobody else performs the same clearing of globalRuntime followed by
        // initializing a new runtime, which would cause our clear below to clear the wrong runtime. Synchronizing
        // against the class is a problem, but the overhead of teardown and creating new containers should outstrip
        // a global synchronize around a few field accesses. -CON
        if (this == globalRuntime) {
            synchronized (Ruby.class) {
                if (this == globalRuntime) {
                    globalRuntime = null;
                }
            }
        }
    }

    @SuppressWarnings("ReturnValueIgnored")
    private void systemTeardown(final ThreadContext context) {
        // Run post-user exit hooks, such as for shutting down internal JRuby services
        while (!postExitBlocks.isEmpty()) {
            ExitFunction fun = postExitBlocks.remove(0);
            fun.applyAsInt(context); // return value ignored
        }

        internalFinalizersMutex.lock();
        try {
            if (!internalFinalizers.isEmpty()) {
                for (Iterator<Finalizable> finalIter = new ArrayList<>(
                        internalFinalizers.keySet()).iterator(); finalIter.hasNext();) {
                    Finalizable f = finalIter.next();
                    if (f != null) {
                        try {
                            f.finalize();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    finalIter.remove();
                }
            }
        } finally {
            internalFinalizersMutex.unlock();
        }

        getBeanManager().unregisterCompiler();
        getBeanManager().unregisterConfig();
        getBeanManager().unregisterParserStats();
        getBeanManager().unregisterMethodCache();
        getBeanManager().unregisterRuntime();

        getSelectorPool().cleanup();

        // NOTE: its intentional that we're not doing releaseClassLoader();

        if (config.isProfilingEntireRun()) {
            // not using logging because it's formatted
            ProfileCollection profileCollection = threadService.getMainThread().getContext().getProfileCollection();
            printProfileData(profileCollection);
        }

        if (Options.PARSER_SUMMARY.load()) parserManager.getParserStats().printParserStatistics();

        // shut down executors
        getJITCompiler().shutdown();
        getExecutor().shutdown();
        getFiberExecutor().shutdown();

        // Fetches (and unsets) the SIGEXIT handler, if one exists.
        IRubyObject trapResult = RubySignal.__jtrap_osdefault_kernel(context, getNil(), newString("EXIT"));
        if (trapResult instanceof RubyArray ary) {
            IRubyObject[] trapResultEntries = ary.toJavaArray(context);
            IRubyObject exitHandlerProc = trapResultEntries[0];
            if (exitHandlerProc instanceof RubyProc) {
                ((RubyProc) exitHandlerProc).call(context, getSingleNilArray());
            }
        }

        // Shut down and replace thread service after all other hooks and finalizers have run
        threadService.teardown();
        threadService = new ThreadService(this);

        // Release classloader resources
        releaseClassLoader();

        // Tear down LoadService
        loadService.tearDown();

        // Clear runtime tables to aid GC
        boundMethods.clear();
        allModules.clear();
        constantNameInvalidators.clear();
        symbolTable.clear();
        javaSupport = loadJavaSupport();
    }

    private int userTeardown(ThreadContext context) {
        int status = 0;

        // FIXME: 73df3d230b9d92c7237d581c6366df1b92ad9b2b exposed no toplevel scope existing anymore (I think the
        // bogus scope I removed was playing surrogate toplevel scope and wallpapering this bug).  For now, add a
        // bogus scope back for at_exit block run.  This is buggy if at_exit is capturing vars.
        if (!context.hasAnyScopes()) {
            StaticScope topStaticScope = getStaticScopeFactory().newLocalScope(null);
            context.pushScope(new ManyVarsDynamicScope(topStaticScope, null));
        }

        // Run all exit functions from user hooks like at_exit
        while (!exitBlocks.isEmpty()) {
            ExitFunction fun = exitBlocks.remove(0);
            int ret = fun.applyAsInt(context);
            if (ret != 0) {
                status = ret;
            }
        }

        if (finalizers != null) {
            synchronized (finalizersMutex) {
                for (Iterator<Finalizable> finalIter = new ArrayList<>(
                        finalizers.keySet()).iterator(); finalIter.hasNext();) {
                    Finalizable f = finalIter.next();
                    if (f != null) {
                        try {
                            f.finalize();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    finalIter.remove();
                }
            }
        }
        return status;
    }

    /**
     * By default {@link #tearDown(boolean)} does not release the class-loader's
     * resources as threads might be still running accessing the classes/packages
     * even after the runtime has been torn down.
     *
     * This method exists to handle such cases, e.g. with embedded uses we always
     * release the runtime loader but not otherwise - you should do that manually.
     */
    public void releaseClassLoader() {
        if (jrubyClassLoader != null) {
            jrubyClassLoader.close();
        }
    }

    /**
     * TDOD remove the synchronized. Synchronization should be a implementation detail of the ProfilingService.
     * @param profileData
     */
    public synchronized void printProfileData( ProfileCollection profileData ) {
        getProfilingService().newProfileReporter(getCurrentContext()).report(profileData);
    }

    /**
     * Simple getter for #profilingServiceLookup to avoid direct property access
     * @return #profilingServiceLookup
     */
    private ProfilingServiceLookup getProfilingServiceLookup() {
        return profilingServiceLookup;
    }

    /**
     *
     * @return the, for this ruby instance, configured implementation of ProfilingService, or null
     */
    public ProfilingService getProfilingService() {
        ProfilingServiceLookup lockup = getProfilingServiceLookup();
        return lockup == null ? null : lockup.getService();
    }

    // new factory methods ------------------------------------------------------------------------

    @Deprecated(since = "10.0")
    public RubyArray newEmptyArray() {
        return RubyArray.newEmptyArray(this);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArray() {
        return RubyArray.newArray(this.getCurrentContext());
    }

    @Deprecated(since = "10.0")
    public RubyArray newArrayLight() {
        return RubyArray.newArrayLight(this);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArray(IRubyObject object) {
        return RubyArray.newArray(this, object);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArray(IRubyObject car, IRubyObject cdr) {
        return RubyArray.newArray(this, car, cdr);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArray(IRubyObject... objects) {
        return RubyArray.newArray(this, objects);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArrayNoCopy(IRubyObject... objects) {
        return RubyArray.newArrayNoCopy(this, objects);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArrayNoCopyLight(IRubyObject... objects) {
        return RubyArray.newArrayNoCopyLight(this, objects);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArray(List<IRubyObject> list) {
        return RubyArray.newArray(this, list);
    }

    @Deprecated(since = "10.0")
    public RubyArray newArray(int size) {
        return RubyArray.newArray(this.getCurrentContext(), size);
    }

    public RubyArray getEmptyFrozenArray() {
        return emptyFrozenArray;
    }

    public RubyString getEmptyFrozenString() {
        return emptyFrozenString;
    }

    public RubyBoolean newBoolean(boolean value) {
        return value ? trueObject : falseObject;
    }

    public RubyFileStat newFileStat(String filename, boolean lstat) {
        return RubyFileStat.newFileStat(this, filename, lstat);
    }

    public RubyFileStat newFileStat(FileDescriptor descriptor) {
        return RubyFileStat.newFileStat(this, descriptor);
    }

    public RubyFixnum newFixnum(long value) {
        return RubyFixnum.newFixnum(this, value);
    }

    public RubyFixnum newFixnum(int value) {
        return RubyFixnum.newFixnum(this, value);
    }

    public RubyFixnum newFixnum(Constant value) {
        return RubyFixnum.newFixnum(this, value.intValue());
    }

    public RubyFloat newFloat(double value) {
        return RubyFloat.newFloat(this, value);
    }

    public RubyNumeric newNumeric() {
        return RubyNumeric.newNumeric(this);
    }

    public RubyRational newRational(long num, long den) {
        return RubyRational.newRationalRaw(this, newFixnum(num), newFixnum(den));
    }

    public RubyRational newRationalReduced(long num, long den) {
        return (RubyRational)RubyRational.newRationalConvert(getCurrentContext(), newFixnum(num), newFixnum(den));
    }

    public RubyProc newProc(Block.Type type, Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) return block.getProcObject();

        return RubyProc.newProc(this, block, type);
    }

    public RubyProc newBlockPassProc(Block.Type type, Block block) {
        if (type != Block.Type.LAMBDA && block.getProcObject() != null) return block.getProcObject();

        return RubyProc.newProc(this, block, type);
    }

    public RubyBinding newBinding() {
        return RubyBinding.newBinding(this, getCurrentContext().currentBinding());
    }

    public RubyBinding newBinding(Binding binding) {
        return RubyBinding.newBinding(this, binding);
    }

    public RubyString newString() {
        return RubyString.newString(this, new ByteList());
    }

    public RubyString newString(String string) {
        return RubyString.newString(this, string);
    }

    public RubyString newDeduplicatedString(String string) {
        return freezeAndDedupString(RubyString.newString(this, string));
    }

    public RubyString newString(ByteList byteList) {
        return RubyString.newString(this, byteList);
    }

    /**
     * Create a new Symbol or lookup a symbol from an ISO_8859_1 "id" String.  This is more of an internal method
     * where if you had, for example, a multi-byte string in UTF-8 then you would dump those bytes
     * into a String with a charset of ISO_8859_1.  These raw bytes are considered the "id" of the symbol.
     * Our internal symbol table is based on ISO_8859_1 raw string values which can return back the properly
     * encoded Symbol instance.
     *
     * @param name raw bytes to store into the symbol table or use to lookup an existing symbol.
     * @return A Ruby Symbol representing the name/id.
     */
    public RubySymbol newSymbol(String name) {
        return symbolTable.getSymbol(name);
    }

    public RubySymbol newSymbol(String name, Encoding encoding) {
        ByteList byteList = RubyString.encodeBytelist(name, encoding);
        return symbolTable.getSymbol(byteList);
    }

    public RubySymbol newSymbol(ByteList name) {
        return symbolTable.getSymbol(name);
    }

    /**
     * Faster than {@link #newSymbol(String)} if you already have an interned
     * name String. Don't intern your string just to call this version - the
     * overhead of interning will more than wipe out any benefit from the faster
     * lookup.
     *
     * @param internedName the symbol name, <em>must</em> be interned! if in
     *                     doubt, call {@link #newSymbol(String)} instead.
     * @return the symbol for name
     */
    public RubySymbol fastNewSymbol(String internedName) {
        //        assert internedName == internedName.intern() : internedName + " is not interned";

        return symbolTable.fastGetSymbol(internedName);
    }

    public RubyTime newTime(long milliseconds) {
        return RubyTime.newTime(this, milliseconds);
    }

    public RaiseException newRuntimeError(String message) {
        return newRaiseException(getRuntimeError(), message);
    }

    public RaiseException newArgumentError(String message) {
        return newRaiseException(getArgumentError(), message);
    }

    /**
     * @param got
     * @param expected
     * @return
     * @deprecated Use {@link org.jruby.api.Error#argumentError(ThreadContext, int, int)} instead.
     */
    @Deprecated(since = "10.0")
    public RaiseException newArgumentError(int got, int expected) {
        return newArgumentError(got, expected, expected);
    }

    public RaiseException newArgumentError(int got, int min, int max) {
        if (min == max) {
            return newRaiseException(getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + ")");
        } else if (max == UNLIMITED_ARGUMENTS) {
            return newRaiseException(getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + "+)");
        } else {
            return newRaiseException(getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + ".." + max + ")");
        }
    }

    @Deprecated(since = "10.0")
    public RaiseException newArgumentError(String name, int got, int expected) {
        return newArgumentError(name, got, expected, expected);
    }

    @Deprecated(since = "10.0")
    public RaiseException newArgumentError(String name, int got, int min, int max) {
        if (min == max) {
            return newRaiseException(getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + ")");
        } else if (max == UNLIMITED_ARGUMENTS) {
            return newRaiseException(getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + "+)");
        } else {
            return newRaiseException(getArgumentError(), "wrong number of arguments (given " + got + ", expected " + min + ".." + max + ")");
        }
    }

    public RaiseException newErrnoEBADFError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EBADF"), "Bad file descriptor");
    }

    public RaiseException newErrnoEISCONNError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EISCONN"), "Socket is already connected");
    }

    public RaiseException newErrnoEINPROGRESSError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EINPROGRESS"), "Operation now in progress");
    }

    public RaiseException newErrnoEINPROGRESSWritableError() {
        var context = getCurrentContext();
        return newLightweightErrnoException(getIO().getClass(context, "EINPROGRESSWaitWritable"), "");
    }

    public RaiseException newErrnoENOPROTOOPTError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOPROTOOPT"), "Protocol not available");
    }

    public RaiseException newErrnoEPIPEError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EPIPE"), "Broken pipe");
    }

    public RaiseException newErrnoECONNABORTEDError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ECONNABORTED"),
                "An established connection was aborted by the software in your host machine");
    }

    public RaiseException newErrnoECONNREFUSEDError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ECONNREFUSED"), "Connection refused");
    }

    public RaiseException newErrnoECONNREFUSEDError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ECONNREFUSED"), message);
    }

    public RaiseException newErrnoECONNRESETError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ECONNRESET"), "Connection reset by peer");
    }

    public RaiseException newErrnoEADDRINUSEError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EADDRINUSE"), "Address in use");
    }

    public RaiseException newErrnoEADDRINUSEError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EADDRINUSE"), message);
    }

    public RaiseException newErrnoEHOSTUNREACHError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EHOSTUNREACH"), message);
    }

    public RaiseException newErrnoEINVALError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EINVAL"), "Invalid file");
    }

    public RaiseException newErrnoELOOPError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ELOOP"), "Too many levels of symbolic links");
    }

    public RaiseException newErrnoEMFILEError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EMFILE"), "Too many open files");
    }

    public RaiseException newErrnoENFILEError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENFILE"), "Too many open files in system");
    }

    public RaiseException newErrnoENOENTError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOENT"), "File not found");
    }

    public RaiseException newErrnoEACCESError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EACCES"), message);
    }

    public RaiseException newErrnoEAGAINError(String message) {
        var context = getCurrentContext();
        return newLightweightErrnoException(getErrno().getClass(context, "EAGAIN"), message);
    }

    public RaiseException newErrnoEAGAINReadableError(String message) {
        var context = getCurrentContext();
        return newLightweightErrnoException(getIO().getClass(context, "EAGAINWaitReadable"), message);
    }

    public RaiseException newErrnoEAGAINWritableError(String message) {
        var context = getCurrentContext();
        return newLightweightErrnoException(getIO().getClass(context, "EAGAINWaitWritable"), message);
    }

    public RaiseException newErrnoEISDirError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EISDIR"), message);
    }

    public RaiseException newErrnoEPERMError(String name) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EPERM"), "Operation not permitted - " + name);
    }

    public RaiseException newErrnoEISDirError() {
        return newErrnoEISDirError("Is a directory");
    }

    public RaiseException newErrnoESPIPEError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ESPIPE"), "Illegal seek");
    }

    public RaiseException newErrnoEBADFError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EBADF"), message);
    }

    public RaiseException newErrnoEINPROGRESSError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EINPROGRESS"), message);
    }

    public RaiseException newErrnoEINPROGRESSWritableError(String message) {
        var context = getCurrentContext();
        return newLightweightErrnoException(getIO().getClass(context, "EINPROGRESSWaitWritable"), message);
    }

    public RaiseException newErrnoEISCONNError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EISCONN"), message);
    }

    public RaiseException newErrnoEINVALError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EINVAL"), message);
    }

    public RaiseException newErrnoENOTDIRError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOTDIR"), message);
    }

    public RaiseException newErrnoENOTEMPTYError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOTEMPTY"), message);
    }

    public RaiseException newErrnoENOTSOCKError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOTSOCK"), message);
    }

    public RaiseException newErrnoENOTCONNError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOTCONN"), message);
    }

    public RaiseException newErrnoENOTCONNError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOTCONN"), "Socket is not connected");
    }

    public RaiseException newErrnoENOENTError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENOENT"), message);
    }

    public RaiseException newErrnoEOPNOTSUPPError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EOPNOTSUPP"), message);
    }

    public RaiseException newErrnoESPIPEError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ESPIPE"), message);
    }

    public RaiseException newErrnoEEXISTError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EEXIST"), message);
    }

    public RaiseException newErrnoEDOMError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EDOM"), "Domain error - " + message);
    }

    public RaiseException newErrnoEDOMError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EDOM"), "Numerical argument out of domain");
    }

    public RaiseException newErrnoECHILDError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ECHILD"), "No child processes");
    }

    public RaiseException newErrnoEADDRNOTAVAILError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EADDRNOTAVAIL"), message);
    }

    public RaiseException newErrnoESRCHError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ESRCH"), null);
    }

    public RaiseException newErrnoEWOULDBLOCKError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EWOULDBLOCK"), null);
    }

    public RaiseException newErrnoEDESTADDRREQError(String func) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EDESTADDRREQ"), func);
    }

    public RaiseException newErrnoENETUNREACHError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ENETUNREACH"), null);
    }

    public RaiseException newErrnoEMSGSIZEError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EMSGSIZE"), null);
    }

    public RaiseException newErrnoEXDEVError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EXDEV"), message);
    }

    public RaiseException newIndexError(String message) {
        return newRaiseException(getIndexError(), message);
    }

    public RaiseException newSecurityError(String message) {
        return newRaiseException(getSecurityError(), message);
    }

    public RaiseException newSystemCallError(String message) {
        return newRaiseException(getSystemCallError(), message);
    }

    public RaiseException newKeyError(String message, IRubyObject recv, IRubyObject key) {
        return new RubyKeyError(this, getKeyError(), message, recv, key).toThrowable();
    }

    public RaiseException newErrnoEINTRError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EINTR"), "Interrupted");
    }

    public RaiseException newErrnoEAFNOSUPPORTError(String message) {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "EAFNOSUPPORT"), message);
    }

    public RaiseException newErrnoETIMEDOUTError() {
        var context = getCurrentContext();
        return newRaiseException(getErrno().getClass(context, "ETIMEDOUT"), "Broken pipe");
    }

    public RaiseException newErrnoFromLastPOSIXErrno() {
        RubyClass errnoClass = getErrno(getPosix().errno());
        if (errnoClass == null) errnoClass = systemCallError;

        return newRaiseException(errnoClass, null);
    }

    public RaiseException newErrnoFromInt(int errno, String methodName, String message) {
        if (Platform.IS_WINDOWS && ("stat".equals(methodName) || "lstat".equals(methodName))) {
            if (errno == 20047) return newErrnoENOENTError(message); // boo:bar UNC stat failure
            if (errno == Errno.ESRCH.intValue()) return newErrnoENOENTError(message); // ESRCH on stating ""
        }

        return newErrnoFromInt(errno, message);
    }

    public RaiseException newErrnoFromInt(int errno, String message) {
        RubyClass errnoClass = getErrno(errno);
        if (errnoClass != null) {
            return newRaiseException(errnoClass, message);
        } else {
            return newSystemCallError("Unknown Error (" + errno + ") - " + message);
        }
    }

    public RaiseException newErrnoFromErrno(Errno errno, String message) {
        if (errno == null || errno == Errno.__UNKNOWN_CONSTANT__) {
            return newSystemCallError(message);
        }
        return newErrnoFromInt(errno.intValue(), message);
    }

    public RaiseException newErrnoFromInt(int errno) {
        Errno errnoObj = Errno.valueOf(errno);
        if (errnoObj == null) {
            return newSystemCallError("Unknown Error (" + errno + ")");
        }
        String message = errnoObj.description();
        return newErrnoFromInt(errno, message);
    }

    private final static Pattern ADDR_NOT_AVAIL_PATTERN = Pattern.compile("assign.*address");

    public RaiseException newErrnoFromBindException(BindException be, String contextMessage) {
        Errno errno = Helpers.errnoFromException(be);

        if (errno != null) {
            return newErrnoFromErrno(errno, contextMessage);
        }

        // Messages may differ so revert to old behavior (jruby/jruby#6322)
        return newErrnoEADDRFromBindException(be, contextMessage);
    }

    public RaiseException newErrnoEADDRFromBindException(BindException be, String contextMessage) {
        String msg = be.getMessage();
        if (msg == null) {
            msg = "bind";
        } else {
            msg = "bind - " + msg;
        }
        if (contextMessage != null) {
            msg = msg + contextMessage;
        }
        // This is ugly, but what can we do, Java provides the same BindingException
        // for both EADDRNOTAVAIL and EADDRINUSE, so we differentiate the errors
        // based on BindException's message.
        if(ADDR_NOT_AVAIL_PATTERN.matcher(msg).find()) {
            return newErrnoEADDRNOTAVAILError(msg);
        } else {
            return newErrnoEADDRINUSEError(msg);
        }
    }

    @Deprecated
    public RaiseException newTypeError(String message) {
        return newRaiseException(getTypeError(), message);
    }

    public RaiseException newThreadError(String message) {
        return newRaiseException(getThreadError(), message);
    }

    public RaiseException newConcurrencyError(String message) {
        return newRaiseException(getConcurrencyError(), message);
    }

    public RaiseException newSyntaxError(String message) {
        return newRaiseException(getSyntaxError(), message);
    }

    public RaiseException newSyntaxError(String message, String path) {
        RaiseException syntaxError = newRaiseException(getSyntaxError(), message);

        syntaxError.getException().setInstanceVariable("@path", newString(path));

        return syntaxError;
    }

    public RaiseException newRegexpError(String message) {
        return newRaiseException(getRegexpError(), message);
    }

    public RaiseException newInterruptedRegexpError(String message) {
        return newRaiseException(getInterruptedRegexpError(), message);
    }

    public RaiseException newRangeError(String message) {
        return newRaiseException(getRangeError(), message);
    }

    public RaiseException newNotImplementedError(String message) {
        return newRaiseException(getNotImplementedError(), message);
    }

    @Deprecated(since = "9.4-")
    public RaiseException newInvalidEncoding(String message) {
        var context = getCurrentContext();
        return newRaiseException(Access.getClass(context, "Iconv", "InvalidEncoding"), message);
    }

    @Deprecated(since = "9.4-")
    public RaiseException newIllegalSequence(String message) {
        var context = getCurrentContext();
        return newRaiseException(Access.getClass(context, "Iconv", "IllegalSequence"), message);
    }

    /**
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject, boolean)
     */
    public RaiseException newNameError(String message, IRubyObject recv, IRubyObject name) {
        return newNameError(message, recv, name, false);
    }

    /**
     * Construct a NameError that formats its message with an sprintf format string.
     *
     * The arguments given to sprintf are as follows:
     *
     * 0: the name that failed
     * 1: the receiver object that failed
     * 2: a ":" character for non-singleton recv, blank otherwise
     * 3: the name of the a non-singleton recv's class, blank if recv is a singleton
     *
     * Passing a string with no format characters will warn in verbose mode and error in debug mode.
     *
     * See jruby/jruby#3934.
     *
     * @param message an sprintf format string for the message
     * @param recv the receiver object
     * @param name the name that failed
     * @param privateCall whether the failure was due to method visibility
     * @return a new NameError
     */
    public RaiseException newNameError(String message, IRubyObject recv, IRubyObject name, boolean privateCall) {
        IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, name);
        RubyException err = RubyNameError.newNameError(getCurrentContext(), getNameError(), msg, name, privateCall);

        return err.toThrowable();
    }

    /**
     * Construct a NameError that formats its message with an sprintf format string.
     *
     * This version just accepts a java.lang.String for the name.
     *
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject)
     */
    public RaiseException newNameError(String message, IRubyObject recv, String name) {
        return newNameError(message, recv, name, false);
    }

    /**
     * Construct a NameError that formats its message with an sprintf format string and has private_call? set to given.
     *
     * This version just accepts a java.lang.String for the name.
     *
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject)
     */
    public RaiseException newNameError(String message, IRubyObject recv, String name, boolean privateCall) {
        RubySymbol nameSym = newSymbol(name);
        return newNameError(message, recv, nameSym, privateCall);
    }

    /**
     * Construct a NameError with the given pre-formatted message, name, and optional original exception.
     *
     * If the original exception is given, and either we are in verbose mode with printWhenVerbose set to true
     * or we are in debug mode.
     *
     * @param message the pre-formatted message for the NameError
     * @param name the name that failed
     * @param exception the original exception, or null
     * @param printWhenVerbose whether to log this exception when verbose mode is enabled
     * @return a new NameError
     */
    public RaiseException newNameError(String message, String name, Throwable exception, boolean printWhenVerbose) {
        if (exception != null) {
            if (printWhenVerbose && isVerbose()) {
                LOG.error(exception);
            } else if (isDebug()) {
                LOG.debug(exception);
            }
        }

        return new RubyNameError(this, getNameError(), message, name).toThrowable();
    }

    public RaiseException newNameError(String message, IRubyObject name, Throwable exception, boolean printWhenVerbose) {
        if (exception != null) {
            if (printWhenVerbose && isVerbose()) {
                LOG.error(exception);
            } else if (isDebug()) {
                LOG.debug(exception);
            }
        }

        return new RubyNameError(this, getNameError(), message, name).toThrowable();
    }

    /**
     * Construct a NameError with a pre-formatted message and name.
     *
     * @param message the pre-formatted message for the error
     * @param name the name that failed
     * @return a new NameError
     * @deprecated Use {@link org.jruby.api.Error#nameError(ThreadContext, String, String)}
     */
    @Deprecated(since = "10.0")
    public RaiseException newNameError(String message, String name) {
        return newNameError(message, name, null, false);
    }

    /**
     * @deprecated Use {@link org.jruby.api.Error#nameError(ThreadContext, String, String)}
     */
    @Deprecated(since = "10.0")
    public RaiseException newNameError(String message, IRubyObject name) {
        return newNameError(message, name, (Throwable) null, false);
    }


    /**
     * Construct a NameError with an optional originating exception and a pre-formatted message.
     *
     * This is the same as calling {@link #newNameError(String, String, Throwable, boolean)} with a null
     * originating exception and false for verbose-mode logging.
     *
     * @param message a formatted string message for the error
     * @param name the name that failed
     * @param origException the original exception, or null if none
     * @return a new NameError
     * @deprecated Use {@link org.jruby.api.Error#nameError(ThreadContext, String, String, Throwable)} instead.
     */
    @Deprecated(since = "10.0")
    public RaiseException newNameError(String message, String name, Throwable origException) {
        return newNameError(message, name, origException, false);
    }

    /**
     * @see Ruby#newNoMethodError(String, IRubyObject, String, RubyArray, boolean)
     */
    public RaiseException newNoMethodError(String message, IRubyObject recv, String name, RubyArray args) {
        return newNoMethodError(message, recv, name, args, false);
    }

    /**
     * Construct a NoMethodError that formats its message with an sprintf format string.
     *
     * This works like {@link #newNameError(String, IRubyObject, IRubyObject)} but accepts
     * a java.lang.String for name and a RubyArray of the original call arguments.
     *
     * @see Ruby#newNameError(String, IRubyObject, IRubyObject)
     *
     * @return a new NoMethodError
     */
    public RaiseException newNoMethodError(String message, IRubyObject recv, String name, RubyArray args, boolean privateCall) {
        RubySymbol nameStr = newSymbol(name);
        IRubyObject msg = new RubyNameError.RubyNameErrorMessage(this, message, recv, nameStr);
        RubyException err = RubyNoMethodError.newNoMethodError(getNoMethodError(), msg, nameStr, args, privateCall);

        return err.toThrowable();
    }

    /**
     * Construct a NoMethodError with a pre-formatted message.
     *
     * @param message the pre-formatted message
     * @param name the name that failed
     * @param args the original arguments to the call that failed
     * @return a new NoMethodError
     */
    public RaiseException newNoMethodError(String message, String name, IRubyObject args) {
        return new RubyNoMethodError(this, getNoMethodError(), message, name, args).toThrowable();
    }

    public RaiseException newLocalJumpError(RubyLocalJumpError.Reason reason, IRubyObject exitValue, String message) {
        return new RubyLocalJumpError(this, getLocalJumpError(), message, reason, exitValue).toThrowable();
    }

    public RaiseException newLocalJumpErrorNoBlock() {
        return newLocalJumpError(RubyLocalJumpError.Reason.NOREASON, getNil(), "no block given");
    }

    public RaiseException newRedoLocalJumpError() {
        return newLocalJumpError(RubyLocalJumpError.Reason.REDO, getNil(), "unexpected redo");
    }

    public RaiseException newLoadError(String message) {
        return newRaiseException(getLoadError(), message);
    }

    public RaiseException newLoadError(String message, String path) {
        RaiseException loadError = newRaiseException(getLoadError(), message);
        loadError.getException().setInstanceVariable("@path", newString(path));
        return loadError;
    }

    public RaiseException newFrozenError(String objectType, IRubyObject receiver) {
        ThreadContext context = getCurrentContext();

        IRubyObject inspected = context.safeRecurse(Ruby::inspectFrozenObject, this, receiver, "inspect", true);
        String message = "can't modify frozen " + objectType + ": " + inspected.convertToString().toString();

        return newFrozenError(receiver, message);
    }

    @Deprecated(since = "10.0")
    public RaiseException newFrozenError(IRubyObject receiver, String message) {
        ThreadContext context = getCurrentContext();

        return RubyFrozenError.newFrozenError(context,
                        newString(message),
                        receiver)
                .toThrowable();
    }

    public RaiseException newFrozenError(IRubyObject receiver) {
        return newFrozenError(receiver.getType().toString(), receiver);
    }

    private static IRubyObject inspectFrozenObject(ThreadContext context, Ruby runtime, IRubyObject obj, boolean recur) {
        return recur ? Create.newString(context, " ...") : obj.inspect(context);
    }

    public RaiseException newSystemStackError(String message) {
        return newRaiseException(getSystemStackError(), message);
    }

    public RaiseException newSystemStackError(String message, StackOverflowError error) {
        if ( isDebug() ) LOG.debug(error);
        return newRaiseException(getSystemStackError(), message);
    }

    public RaiseException newSystemExit(int status) {
        return RubySystemExit.newInstance(this.getCurrentContext(), status, "exit").toThrowable();
    }

    public RaiseException newSystemExit(int status, String message) {
        return RubySystemExit.newInstance(this.getCurrentContext(), status, message).toThrowable();
    }

    /**
     * Prepare a throwable IOError with the given message.
     *
     * This constructor should not be used to create a RubyException object to be raised on a different thread.
     *
     * @see RaiseException#from(Ruby, RubyClass, String)
     *
     * @param message the message for the new IOError
     * @return a fully-prepared IOError throwable
     */
    public RaiseException newIOError(String message) {
        return newRaiseException(getIOError(), message);
    }

    public RaiseException newStandardError(String message) {
        return newRaiseException(getStandardError(), message);
    }

    /**
     * Java does not give us enough information for specific error conditions
     * so we are reduced to divining them through string matches...
     *
     * TODO: Should ECONNABORTED get thrown earlier in the descriptor itself or is it ok to handle this late?
     * TODO: Should we include this into Errno code somewhere do we can use this from other places as well?
     */
    public RaiseException newIOErrorFromException(final IOException ex) {
        return Helpers.newIOErrorFromException(this, ex);
    }

    @Deprecated
    public RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType) {
        ThreadContext context = getCurrentContext();
        return createTypeError(context, createTypeErrorMessage(context, receivedObject, expectedType));
    }

    @Deprecated
    public RaiseException newTypeError(IRubyObject receivedObject, RubyModule expectedType) {
        ThreadContext context = getCurrentContext();
        return createTypeError(context, createTypeErrorMessage(context, receivedObject, expectedType));
    }

    @Deprecated
    public RaiseException newTypeError(IRubyObject receivedObject, String expectedType) {
        return newRaiseException(getTypeError(),
                str(this, "wrong argument type ",
                        receivedObject.getMetaClass().getRealClass().toRubyString(getCurrentContext()),
                        " (expected ", ids(this, expectedType), ")"));
    }

    public RaiseException newEOFError() {
        return newRaiseException(getEOFError(), "End of file reached");
    }

    public RaiseException newEOFError(String message) {
        return newRaiseException(getEOFError(), message);
    }

    public RaiseException newZeroDivisionError() {
        return newRaiseException(getZeroDivisionError(), "divided by 0");
    }

    public RaiseException newFloatDomainError(String message){
        return newRaiseException(getFloatDomainError(), message);
    }

    public RaiseException newMathDomainError(String message) {
        return newRaiseException(getMathDomainError(), "Numerical argument is out of domain - \"" + message + "\"");
    }

    public RaiseException newEncodingError(String message){
        return newRaiseException(getEncodingError(), message);
    }

    public RaiseException newEncodingCompatibilityError(String message){
        return newRaiseException(getEncodingCompatibilityError(), message);
    }

    public RaiseException newConverterNotFoundError(String message) {
        return newRaiseException(getConverterNotFoundError(), message);
    }

    public RaiseException newFiberError(String message) {
        return newRaiseException(getFiberError(), message);
    }

    public RaiseException newUndefinedConversionError(String message) {
        return newRaiseException(getUndefinedConversionError(), message);
    }

    public RaiseException newInvalidByteSequenceError(String message) {
        return newRaiseException(getInvalidByteSequenceError(), message);
    }

    public RaiseException newBufferLockedError(String message) {
        return newRaiseException(getBufferLockedError(), message);
    }

    public RaiseException newBufferAllocationError(String message) {
        return newRaiseException(getBufferAllocationError(), message);
    }

    public RaiseException newBufferAccessError(String message) {
        return newRaiseException(getBufferAccessError(), message);
    }

    public RaiseException newBufferInvalidatedError(String message) {
        return newRaiseException(getBufferInvalidatedError(), message);
    }

    public RaiseException newBufferMaskError(String message) {
        return newRaiseException(getBufferMaskError(), message);
    }

    /**
     * Construct a new RaiseException wrapping a new Ruby exception object appropriate to the given exception class.
     *
     * There are additional forms of this construction logic in {@link RaiseException#from}.
     *
     * This constructor should not be used to create a RubyException object to be raised on a different thread.
     *
     * @see RaiseException#from(Ruby, RubyClass, String)
     *
     * @param exceptionClass the exception class from which to construct the exception object
     * @param message a simple message for the exception
     * @return a new RaiseException wrapping a new Ruby exception
     */
    public RaiseException newRaiseException(RubyClass exceptionClass, String message) {
        IRubyObject cause = getCurrentContext().getErrorInfo();

        RaiseException exception = RaiseException.from(this, exceptionClass, message);

        if (cause != null && !cause.isNil()) {
            exception.getException().setCause(cause);
        }

        return exception;
    }

    /**
     * Generate one of the ERRNO exceptions. This differs from the normal logic
     * by avoiding the generation of a backtrace. Many ERRNO values are expected,
     * such as EAGAIN, and JRuby pays a very high cost to generate backtraces that
     * are never used. The flags -Xerrno.backtrace=true or the property
     * jruby.errno.backtrace=true forces all errno exceptions to generate a backtrace.
     *
     * @param exceptionClass
     * @param message
     * @return
     */
    private RaiseException newLightweightErrnoException(RubyClass exceptionClass, String message) {
        if (RubyInstanceConfig.ERRNO_BACKTRACE) {
            return RaiseException.from(this, exceptionClass, message);
        } else {
            return RaiseException.from(this, exceptionClass, ERRNO_BACKTRACE_MESSAGE, disabledBacktrace());
        }
    }

    /**
     * Generate a StopIteration exception. This differs from the normal logic
     * by avoiding the generation of a backtrace. StopIteration is used by
     * Enumerator to end an external iteration, and so generating a full
     * backtrace is usually unreasonable overhead. The flag
     * -Xstop_iteration.backtrace=true or the property
     * jruby.stop_iteration.backtrace=true forces all StopIteration exceptions
     * to generate a backtrace.
     *
     * @param message the message for the exception
     */
    public RaiseException newStopIteration(IRubyObject result, String message) {
        final ThreadContext context = getCurrentContext();

        if (message == null) message = STOPIERATION_BACKTRACE_MESSAGE;

        RubyException ex = RubyStopIteration.newInstance(context, result, message);

        if (!RubyInstanceConfig.STOPITERATION_BACKTRACE) {
            ex.setBacktrace(context, disabledBacktrace());
        }

        return ex.toThrowable();
    }

    private IRubyObject disabledBacktrace() {
        return RubyArray.newEmptyArray(this);
    }

    // Equivalent of Data_Wrap_Struct
    public RubyObject.Data newData(RubyClass objectClass, Object sval) {
        return new RubyObject.Data(this, objectClass, sval);
    }

    public RubySymbol.SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public ObjectSpace getObjectSpace() {
        return objectSpace;
    }

    public InputStream getIn() {
        return in;
    }

    public PrintStream getOut() {
        return out;
    }

    public PrintStream getErr() {
        return err;
    }

    public boolean isAbortOnException() {
        return abortOnException;
    }

    public void setAbortOnException(final boolean abortOnException) {
        this.abortOnException = abortOnException;
    }

    @Deprecated
    public boolean isGlobalAbortOnExceptionEnabled() {
        return abortOnException;
    }

    @Deprecated
    public void setGlobalAbortOnExceptionEnabled(boolean enable) {
        abortOnException = enable;
    }

    @Deprecated
    public IRubyObject getReportOnException() {
        return reportOnException ? getTrue() : getFalse();
    }

    public boolean isReportOnException() {
        return reportOnException;
    }

    public void setReportOnException(IRubyObject enable) {
        reportOnException = enable.isTrue();
    }

    public void setReportOnException(boolean enable) {
        reportOnException = enable;
    }

    public boolean isDoNotReverseLookupEnabled() {
        return doNotReverseLookupEnabled;
    }

    public void setDoNotReverseLookupEnabled(boolean b) {
        doNotReverseLookupEnabled = b;
    }

    private final ThreadLocal<Map<Object, Object>> inspect = new ThreadLocal<>();
    public void registerInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        if (val == null) inspect.set(val = new IdentityHashMap<>(8));
        val.put(obj, null);
    }

    public boolean isInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        return val != null && val.containsKey(obj);
    }

    public void unregisterInspecting(Object obj) {
        Map<Object, Object> val = inspect.get();
        if (val != null ) val.remove(obj);
    }

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public void setObjectSpaceEnabled(boolean objectSpaceEnabled) {
        this.objectSpaceEnabled = objectSpaceEnabled;
    }

    // You cannot set siphashEnabled property except via RubyInstanceConfig to avoid mixing hash functions.
    public boolean isSiphashEnabled() {
        return siphashEnabled;
    }

    public long getStartTime() {
        return startTime;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getJRubyHome() {
        return config.getJRubyHome();
    }

    public void setJRubyHome(String home) {
        config.setJRubyHome(home);
    }

    public RubyInstanceConfig getInstanceConfig() {
        return config;
    }

    public static boolean isSecurityRestricted() {
        return securityRestricted;
    }

    public static void setSecurityRestricted(boolean restricted) {
        securityRestricted = restricted;
    }

    public POSIX getPosix() {
        return posix;
    }

    /**
     * Get the native POSIX associated with this runtime.
     *
     * If native is not supported, this will return null.
     *
     * @return a native POSIX, or null if native is not supported
     */
    public POSIX getNativePosix() {
        POSIX nativePosix = this.nativePosix;
        if (nativePosix == null && config.isNativeEnabled()) {
            this.nativePosix = nativePosix = POSIXFactory.getNativePOSIX(new JRubyPOSIXHandler(this));
        }
        return nativePosix;
    }

    public void setRecordSeparatorVar(GlobalVariable recordSeparatorVar) {
        this.recordSeparatorVar = recordSeparatorVar;
    }

    public GlobalVariable getRecordSeparatorVar() {
        return recordSeparatorVar;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public ExecutorService getFiberExecutor() {
        return fiberExecutor;
    }

    public Map<String, DateTimeZone> getTimezoneCache() {
        return timeZoneCache;
    }

    public Invalidator getConstantInvalidator(String constantName) {
        Invalidator invalidator = constantNameInvalidators.get(constantName);
        if (invalidator != null) {
            return invalidator;
        } else {
            return addConstantInvalidator(constantName);
        }
    }

    private Invalidator addConstantInvalidator(String constantName) {
        final Invalidator invalidator = OptoFactory.newConstantInvalidator(this);
        constantNameInvalidators.putIfAbsent(constantName, invalidator);

        // fetch the invalidator back from the ConcurrentHashMap to ensure that
        // only one invalidator for a given constant name is ever used:
        return constantNameInvalidators.get(constantName);
    }

    public Invalidator getCheckpointInvalidator() {
        return checkpointInvalidator;
    }

    /**
     * Define all constants from the given jnr-constants enum which are defined on the current platform.
     *
     * @param module the module in which we want to define the constants
     * @param enumClass the enum class of the constants to define
     * @param <C> the enum type, which must implement {@link Constant}.
     * @deprecated Use {@link org.jruby.RubyModule#defineConstantsFrom(ThreadContext, Class)} instead.
     */
    @Deprecated(since = "10.0")
    public <C extends Enum<C> & Constant> void loadConstantSet(RubyModule module, Class<C> enumClass) {
        module.defineConstantsFrom(getCurrentContext(), enumClass);
    }

    /**
     * Define all constants from the named jnr-constants set which are defined on the current platform.
     *
     * @param module the module in which we want to define the constants
     * @param constantSetName the name of the constant set from which to get the constants
     */
    @Deprecated(since = "10.0")
    public void loadConstantSet(RubyModule module, String constantSetName) {
        var context = getCurrentContext();
        for (Constant c : ConstantSet.getConstantSet(constantSetName)) {
            if (c.defined() && Character.isUpperCase(c.name().charAt(0))) {
                module.defineConstant(context, c.name(), newFixnum(c.intValue()));
            }
        }
    }

    /**
     * Get a new serial number for a new DynamicMethod instance
     * @return a new serial number
     */
    public long getNextDynamicMethodSerial() {
        return dynamicMethodSerial.getAndIncrement();
    }

    /**
     * Get a new generation number for a module or class.
     *
     * @return a new generation number
     */
    public int getNextModuleGeneration() {
        return (int) MODULE_GENERATION.getAndAdd(this, 1);
    }

    /**
     * Get the global object used to synchronize class-hierarchy modifications like
     * cache invalidation, subclass sets, and included hierarchy sets.
     *
     * @return The object to use for locking when modifying the hierarchy
     */
    public Object getHierarchyLock() {
        return hierarchyLock;
    }

    /**
     * Get the runtime-global selector pool
     *
     * @return a SelectorPool from which to getService Selector instances
     */
    public SelectorPool getSelectorPool() {
        return selectorPool;
    }

    /**
     * Get the core class RuntimeCache instance, for doing dynamic calls from
     * core class methods.
     */
    public RuntimeCache getRuntimeCache() {
        return runtimeCache;
    }

    public List<StrptimeToken> getCachedStrptimePattern(String pattern) {
        List<StrptimeToken> tokens = strptimeFormatCache.get(pattern);

        if (tokens == null) {
            tokens = new StrptimeParser().compilePattern(pattern);
            strptimeFormatCache.put(pattern, tokens);
        }

        return tokens;
    }

    /**
     * Add a method, so it can be printed out later.
     *
     * @param id raw name String of the method to be profiled
     * @param method
     */
    void addProfiledMethod(final String id, final DynamicMethod method) {
        if (!config.isProfiling() || method.isUndefined()) return;

        getProfilingService().addProfiledMethod(id, method);
    }

    /**
     * Increment the count of exceptions generated by code in this runtime.
     */
    public void incrementExceptionCount() {
        int ignored = (int) EXCEPTION_COUNT.getAndAdd(this, 1);
    }

    /**
     * Get the current exception count.
     *
     * @return he current exception count
     */
    public int getExceptionCount() {
        return exceptionCount;
    }

    /**
     * Increment the count of backtraces generated by code in this runtime.
     */
    public void incrementBacktraceCount() {
        int ignored = (int) BACKTRACE_COUNT.getAndAdd(this, 1);
    }

    /**
     * Get the current backtrace count.
     *
     * @return the current backtrace count
     */
    public int getBacktraceCount() {
        return backtraceCount;
    }

    /**
     * Increment the count of backtraces generated for warnings in this runtime.
     */
    public void incrementWarningCount() {
        int ignored = (int) WARNING_COUNT.getAndAdd(this, 1);
    }

    /**
     * Get the current backtrace count.
     *
     * @return the current backtrace count
     */
    public int getWarningCount() {
        return warningCount;
    }

    /**
     * Increment the count of backtraces generated by code in this runtime.
     */
    public void incrementCallerCount() {
        int ignored = (int) CALLER_COUNT.getAndAdd(this, 1);
    }

    /**
     * Get the current backtrace count.
     *
     * @return the current backtrace count
     */
    public int getCallerCount() {
        return callerCount;
    }

    public boolean isBootingCore() {
        return !coreIsBooted;
    }

    public boolean isBooting() {
        return !runtimeIsBooted;
    }

    public CoverageData getCoverageData() {
        CoverageData coverageData = this.coverageData;
        if (coverageData != null) return coverageData;

        COVERAGE_DATA.compareAndSet(this, null, new CoverageData());

        return this.coverageData;
    }

    public boolean isCoverageEnabled() {
        CoverageData coverageData = this.coverageData;

        return coverageData != null && coverageData.isCoverageEnabled();
    }

    public long getHashSeedK0() {
        return hashSeedK0;
    }

    public long getHashSeedK1() {
        return hashSeedK1;
    }

    public StaticScopeFactory getStaticScopeFactory() {
        return staticScopeFactory;
    }

    public FFI getFFI() {
        return ffi;
    }

    public void setFFI(FFI ffi) {
        this.ffi = ffi;
    }

    public RubyString getDefinedMessage(DefinedMessage definedMessage) {
        return definedMessages.get(definedMessage);
    }

    public RubyString getThreadStatus(RubyThread.Status status) {
        return threadStatuses.get(status);
    }

    /**
     * Given a Ruby string, cache a deduplicated FString version of it, or find an
     * existing copy already prepared. This is used to reduce in-memory
     * duplication of pre-frozen or known-frozen strings.
     *
     * If the incoming string is already an FString, attempt to cache it directly
     * without creating a new instance.
     *
     * The logic here reads like this:
     *
     * 1. If the string is not a natural String object, just freeze and return it.
     * 2. Use an {@link FStringEqual} wrapper to look up the deduplicated string.
     * 3. If there's a dedup in the cache, return the dedup.
     * 4. Otherwise, attempt to cache and return an FString version of the string.
     *
     * @see RubyString#dupAsFString(Ruby)
     * @see #cacheFString(RubyString.FString)
     *
     * @param string the string to deduplicate if an equivalent does not already exist
     * @return the deduplicated FString version of the string
     */
    public RubyString freezeAndDedupString(RubyString string) {
        var context = getCurrentContext();
        if (!string.isBare(context)) {
            // never cache a non-natural String
            string.setFrozen(true);
            return string;
        }

        // Populate thread-local wrapper
        try (FStringEqual wrapper = FStringEqual.cached(this, string)) {
            WeakReference<RubyString.FString> dedupedRef = dedupMap.get(wrapper);
            RubyString.FString deduped;

            if (dedupedRef == null || (deduped = dedupedRef.get()) == null) {
                // We will insert wrapper one way or another so clear from threadlocal
                DEDUP_WRAPPER_CACHE.remove();

                // Ensure we have an FString
                deduped = string.dupAsFString(this);

                return cacheFString(deduped);
            }

            return deduped;
        }
    }

    /**
     * Given a ByteList, cache a deduplicated FString version of it, or find an
     * existing copy already prepared. This is equivalent to calling {@link #freezeAndDedupString(RubyString)}
     * with a new FString based on the given ByteList.
     *
     * @param bytes the ByteList to deduplicate if an equivalent does not already exist
     * @return the deduplicated FString version of the ByteList
     */
    public RubyString freezeAndDedupString(ByteList bytes) {
        return freezeAndDedupString(new RubyString.FString(this, bytes, RubyString.scanForCodeRange(bytes)));
    }

    /**
     * Insert the given FString into the deduplicated FString cache, or retrieve the equivalent FString from the cache.
     *
     * The logic here reads like this:
     *
     * 1. Create a new wrapper to avoid reusing it, since we might insert it.
     * 2. Atomically insert the new cache entry or replace a GCed entry that already exists.
     * 3. Return the deduplicated fstring.
     *
     * @param candidate the fstring to dedup if an equivalent does not already exist
     * @return the deduped version of the fstring
     */
    private RubyString.FString cacheFString(RubyString.FString candidate) {
        // new uncached wrapper since it may get inserted as a new key
        var wrapper = new FStringEqual(candidate);

        // try to get or compute until we have a result
        while (true) {
            // re-get reference if it is non-null and populated, or replace with new reference
            var dedupedRef =
                    dedupMap.compute(wrapper,
                            (key, old) -> old != null && old.get() != null ? old : new WeakReference<>(candidate));

            // return result if not vacated between lookup and access
            var deduped = dedupedRef.get();
            if (deduped != null) return deduped;
        }
    }

    public ParserManager getParserManager() {
        return parserManager;
    }

    // FIXME: This feels like a race but this was in parser for 2 decades without synchronization?
    public void defineDATA(IRubyObject io) {
        IRubyObject verbose = getVerbose();
        setVerbose(getNil());
        objectClass.defineConstant(getCurrentContext(), "DATA", io);
        setVerbose(verbose);
    }

    public void setTopLevelBinding(RubyBinding rubyBinding) {
        this.topLevelBinding = rubyBinding;
    }

    public RubyBinding getTopLevelBinding() {
        return topLevelBinding;
    }

    public void setRubyTimeout(IRubyObject timeout) {
        this.regexpTimeout = timeout;
    }

    public IRubyObject getRubyTimeout() {
        return regexpTimeout;
    }

    public void setRegexpTimeoutError(RubyClass error) {
        this.regexpTimeoutError = error;
    }

    public RubyClass getRegexpTimeoutError() {
        return regexpTimeoutError;
    }

    public void setChdirThread(RubyThread thread) {
        this.chdirCurrentThread = thread;
        this.chdirLocation = thread == null ? null : thread.getContext().getSingleBacktrace();
    }

    public RubyThread getChdirThread() { return this.chdirCurrentThread; }

    public RubyStackTraceElement getChdirLocation() { return this.chdirLocation; }

    /**
     * Because RubyString.equals does not consider encoding, and MRI's logic for deduplication does need to consider
     * encoding, we use a wrapper object as the key. These wrappers need to be used on all get operations, so if we
     * don't need to insert anything we reuse that wrapper the next time.
     *
     * Wrappers are constructed and thread-local cached in in {@link #DEDUP_WRAPPER_CACHE};
     */
    static class FStringEqual implements Closeable {
        RubyString string;

        static FStringEqual cached(Ruby runtime, RubyString string) {
            FStringEqual wrapper = runtime.DEDUP_WRAPPER_CACHE.get();
            wrapper.string = string;
            return wrapper;
        }

        FStringEqual() {}

        FStringEqual(RubyString string) {
            this.string = string;
        }

        public void close() {
            this.string = null;
        }

        public boolean equals(Object other) {
            if (other instanceof FStringEqual) {
                RubyString otherString = ((FStringEqual) other).string;
                RubyString string = this.string;

                if (string == null || otherString == null) return false;

                return string.equals(otherString) && string.getEncoding() == otherString.getEncoding();
            }
            return false;
        }

        public int hashCode() {
            RubyString string = this.string;

            if (string == null) return 0;

            return string.hashCode();
        }
    }

    private final ThreadLocal<FStringEqual> DEDUP_WRAPPER_CACHE = ThreadLocal.withInitial(FStringEqual::new);

    public int getRuntimeNumber() {
        return runtimeNumber;
    }

    /**
     * @see org.jruby.compiler.Constantizable
     */
    @Override
    public Object constant() {
        return constant;
    }

    /**
     * Set the base Class#new method.
     *
     * @param baseNewMethod
     */
    public void setBaseNewMethod(DynamicMethod baseNewMethod) {
        this.baseNewMethod = baseNewMethod;
    }

    /**
     * Get the base Class#new method.
     *
     * @return the base Class#new method
     */
    public DynamicMethod getBaseNewMethod() {
        return baseNewMethod;
    }

    /**
     * Get the "nullToNil" method handle filter for this runtime.
     *
     * @return a method handle suitable for filtering a single IRubyObject value from null to nil
     */
    public MethodHandle getNullToNilHandle() {
        MethodHandle nullToNil = this.nullToNil;

        if (nullToNil != null) return nullToNil;

        nullToNil = InvokeDynamicSupport.findStatic(Helpers.class, "nullToNil", methodType(IRubyObject.class, IRubyObject.class, IRubyObject.class));
        nullToNil = insertArguments(nullToNil, 1, nilObject);
        nullToNil = explicitCastArguments(nullToNil, methodType(IRubyObject.class, Object.class));

        return this.nullToNil = nullToNil;
    }

    public MethodHandle getNullToUndefinedHandle() {
        MethodHandle filter = this.nullToUndefined;

        if (filter != null) return filter;

        filter = InvokeDynamicSupport.findStatic(Helpers.class, "nullToUndefined", methodType(IRubyObject.class, IRubyObject.class));
        filter = explicitCastArguments(filter, methodType(IRubyObject.class, Object.class));

        return this.nullToUndefined = filter;
    }

    public FilenoUtil getFilenoUtil() {
        return filenoUtil;
    }

    /**
     * @return Class$ -&gt; extension initializer map
     * <p>Note: Internal API, subject to change!</p>
     */
    public Map<Class, Consumer<RubyModule>> getJavaExtensionDefinitions() { return javaExtensionDefinitions; }

    public interface RecursiveFunctionEx<T> extends ThreadContext.RecursiveFunctionEx<T> {
        IRubyObject call(ThreadContext context, T state, IRubyObject obj, boolean recur);
    }

    private final ConcurrentHashMap<String, Invalidator> constantNameInvalidators =
        new ConcurrentHashMap<String, Invalidator>(
            16    /* default initial capacity */,
            0.75f /* default load factory */,
            1     /* concurrency level - mostly reads here so this can be 1 */);

    private final Invalidator checkpointInvalidator;
    private ThreadService threadService;

    private final POSIX posix;
    private POSIX nativePosix;

    private final ObjectSpace objectSpace = new ObjectSpace();

    private final RubySymbol.SymbolTable symbolTable;

    private boolean abortOnException = false; // Thread.abort_on_exception
    private boolean reportOnException = true; // Thread.report_on_exception
    private boolean doNotReverseLookupEnabled = false;
    private volatile boolean objectSpaceEnabled;
    private boolean siphashEnabled;

    // Global timeout value.  Nil == no timeout set.
    private IRubyObject regexpTimeout;
    private RubyClass regexpTimeoutError;

    // Default objects
    private final IRubyObject topSelf;
    private RubyBinding topLevelBinding;
    private final RubyNil nilObject;
    private final IRubyObject[] singleNilArray;
    private final RubyBoolean trueObject;
    private final RubyBoolean falseObject;
    private final RubyString trueString;
    private final RubyString falseString;
    private final RubyString nilString;
    private final RubyString nilInspectString;
    final RubyFixnum[] fixnumCache = new RubyFixnum[2 * RubyFixnum.CACHE_OFFSET];
    final Object[] fixnumConstants = new Object[fixnumCache.length];

    private boolean warningsEnabled = true; // global flag to be able to disable warnings regardless of $VERBOSE
    private boolean verboseWarnings; // whether warnings are enabled based on $VERBOSE
    private boolean verbose, debug;
    private IRubyObject verboseValue;

    // Set of categories we care about (set defined when creating warnings).
    private final Set<RubyWarnings.Category> warningCategories;

    private RubyThreadGroup defaultThreadGroup;

    /**
     * All the core classes we keep hard references to. These are here largely
     * so that if someone redefines String or Array we won't start blowing up
     * creating strings and arrays internally. They also provide much faster
     * access than going through normal hash lookup on the Object class.
     */
    private final RubyClass basicObjectClass;
    private final RubyClass objectClass;
    private final RubyClass moduleClass;
    private final RubyClass refinementClass;
    private final RubyClass classClass;
    private final RubyClass nilClass;
    private final RubyClass trueClass;
    private final RubyClass falseClass;
    private final RubyClass numericClass;
    private final RubyClass floatClass;
    private final RubyClass integerClass;
    private final RubyClass fixnumClass;
    private final RubyClass complexClass;
    private final RubyClass rationalClass;
    private final RubyClass enumeratorClass;
    private final RubyClass yielderClass;
    private final RubyClass fiberClass;
    private final RubyClass generatorClass;
    private final RubyClass chainClass;
    private final RubyClass aseqClass;
    private final RubyClass producerClass;
    private final RubyClass arrayClass;
    private final RubyClass hashClass;
    private final RubyClass rangeClass;
    private final RubyClass stringClass;
    private final RubyClass encodingClass;
    private final RubyClass converterClass;
    private final RubyClass symbolClass;
    private final RubyClass procClass;
    private final RubyClass bindingClass;
    private final RubyClass methodClass;
    private final RubyClass unboundMethodClass;
    private final RubyClass matchDataClass;
    private final RubyClass regexpClass;
    private final RubyClass timeClass;
    private final RubyClass dirClass;
    private RubyClass dateErrorClass;
    private final RubyClass fileClass;
    private final RubyClass fileStatClass;
    private final RubyClass ioClass;
    private final RubyClass ioBufferClass;
    private final RubyClass threadClass;
    private final RubyClass threadGroupClass;
    private final RubyClass continuationClass;
    private final RubyClass structClass;
    private final RubyClass exceptionClass;
    private final RubyClass dummyClass;
    private final RubyClass randomClass;
    private final RubyClass mutexClass;
    private final RubyClass conditionVariableClass;
    private final RubyClass queueClass;
    private final RubyClass closedQueueError;
    private final RubyClass sizedQueueClass;
    private final RubyClass dataClass;

    private RubyClass tmsStruct;
    private RubyClass passwdStruct;
    private RubyClass groupStruct;
    private RubyClass procStatusClass;
    private RubyClass runtimeError;
    private RubyClass frozenError;
    private RubyClass ioError;
    private RubyClass ioTimeoutError;
    private RubyClass scriptError;
    private RubyClass nameError;
    private RubyClass nameErrorMessage;
    private RubyClass noMethodError;
    private RubyClass signalException;
    private RubyClass rangeError;
    private RubyClass systemExit;
    private RubyClass localJumpError;
    private RubyClass nativeException;
    private RubyClass systemCallError;
    private RubyClass fatal;
    private RubyClass interrupt;
    private RubyClass typeError;
    private RubyClass noMatchingPatternError;
    private RubyClass noMatchingPatternKeyError;
    private RubyClass argumentError;
    private RubyClass uncaughtThrowError;
    private RubyClass indexError;
    private RubyClass stopIteration;
    private RubyClass syntaxError;
    private RubyClass standardError;
    private RubyClass loadError;
    private RubyClass notImplementedError;
    private RubyClass securityError;
    private RubyClass noMemoryError;
    private RubyClass regexpError;
    private RubyClass eofError;
    private RubyClass threadError;
    private RubyClass concurrencyError;
    private RubyClass systemStackError;
    private RubyClass zeroDivisionError;
    private RubyClass floatDomainError;
    private RubyClass mathDomainError;
    private RubyClass encodingError;
    private RubyClass encodingCompatibilityError;
    private RubyClass converterNotFoundError;
    private RubyClass undefinedConversionError;
    private RubyClass invalidByteSequenceError;
    private RubyClass fiberError;
    private RubyClass keyError;
    private RubyClass locationClass;
    private RubyClass interruptedRegexpError;
    private RubyClass bufferLockedError;
    private RubyClass bufferAllocationError;
    private RubyClass bufferAccessError;
    private RubyClass bufferInvalidatedError;
    private RubyClass bufferMaskError;

    /**
     * All the core modules we keep direct references to, for quick access and
     * to ensure they remain available.
     */
    private final RubyModule kernelModule;
    private final RubyModule comparableModule;
    private final RubyModule enumerableModule;
    private final RubyModule mathModule;
    private final RubyModule marshalModule;
    private final RubyModule fileTestModule;
    private final RubyModule gcModule;
    private final RubyModule objectSpaceModule;
    private final RubyModule processModule;
    private final RubyModule warningModule;

    private RubyModule etcModule;
    private RubyModule procUIDModule;
    private RubyModule procGIDModule;
    private RubyModule procSysModule;
    private RubyModule precisionModule;
    private RubyModule errnoModule;

    private  RubyThread chdirCurrentThread;
    private  RubyStackTraceElement chdirLocation;

    private DynamicMethod privateMethodMissing, protectedMethodMissing, variableMethodMissing,
            superMethodMissing, normalMethodMissing, defaultMethodMissing, defaultModuleMethodMissing,
            respondTo, respondToMissing;

    // record separator var, to speed up io ops that use it
    private GlobalVariable recordSeparatorVar;

    // former java.lang.System concepts now internalized for MVM
    private volatile String currentDirectory;

    // The "current line" global variable
    private volatile int currentLine = 0;

    private volatile IRubyObject argsFile;

    private final long startTime = System.currentTimeMillis();

    final RubyInstanceConfig config;

    private InputStream in;
    private PrintStream out;
    private PrintStream err;

    // Java support
    private JavaSupport javaSupport;
    private final JRubyClassLoader jrubyClassLoader;

    public static final Module JRUBY_MODULE = Ruby.class.getModule();

    // Object Specializer
    private final RubyObjectSpecializer objectSpecializer;

    // Management/monitoring
    private final BeanManager beanManager;

    private final InlineStats inlineStats;

    // Compilation
    private final JITCompiler jitCompiler;

    // Cache invalidation
    private final Caches caches;

    // Note: this field and the following static initializer
    // must be located be in this order!
    private volatile static boolean securityRestricted = false;
    static {
        if (SafePropertyAccessor.isSecurityProtected("jruby.reflected.handles")) {
            // can't read non-standard properties
            securityRestricted = true;
        } else {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                try {
                    sm.checkCreateClassLoader();
                } catch (SecurityException se) {
                    // can't create custom classloaders
                    securityRestricted = true;
                }
            }
        }
    }

    private final LoadService loadService;

    private final EncodingService encodingService;

    private final GlobalVariables globalVariables = new GlobalVariables(this);
    private final RubyWarnings warnings = new RubyWarnings(this);
    private final WarnCallback regexpWarnings = message -> getWarnings().warn(message);

    /**
     * Reserved for userland at_exit logic that runs before internal services start shutting down.
     */
    private final List<ExitFunction> exitBlocks = Collections.synchronizedList(new LinkedList<>());

    /**
     * Registry of shutdown operations that should happen after all user code has been run (e.g. at_exit hooks).
     */
    private final List<ExitFunction> postExitBlocks = Collections.synchronizedList(new LinkedList<>());

    private Profile profile;

    private KCode kcode = KCode.NONE;

    // Atomic integers for symbol and method IDs
    private final AtomicInteger symbolLastId = new AtomicInteger(128);
    private final AtomicInteger moduleLastId = new AtomicInteger(0);

    // Weak map of all natural instances of Module in the system (not including Classes).
    // a ConcurrentMap<RubyModule, ?> is used to emulate WeakHashSet<RubyModule>
    // NOTE: module instances are unique and we only addModule from <init> - could use a ConcurrentLinkedQueue
    private final ConcurrentWeakHashMap<RubyModule, Object> allModules = new ConcurrentWeakHashMap<>(128);

    private final Map<String, DateTimeZone> timeZoneCache = new HashMap<>();

    /**
     * A list of "external" finalizers (the ones, registered via ObjectSpace),
     * weakly referenced, to be executed on tearDown.
     */
    private final Map<Finalizable, Object> finalizers = new WeakIdentityHashMap();

    /**
     * A list of JRuby-internal finalizers,  weakly referenced,
     * to be executed on tearDown.
     */
    private final Map<Finalizable, Object> internalFinalizers = new WeakIdentityHashMap();

    // mutex that controls modifications of user-defined finalizers
    private final ReentrantLock finalizersMutex = new ReentrantLock();

    // mutex that controls modifications of internal finalizers
    private final ReentrantLock internalFinalizersMutex = new ReentrantLock();

    // A thread pool to use for executing this runtime's Ruby threads
    private final ExecutorService executor;

    // A thread pool to use for running fibers
    private final ExecutorService fiberExecutor;

    // A global object lock for class hierarchy mutations
    private final Object hierarchyLock = new Object();

    // An atomic long for generating DynamicMethod serial numbers
    private final AtomicLong dynamicMethodSerial = new AtomicLong(1);

    // An atomic int for generating class generation numbers
    private volatile int moduleGeneration = 1;

    // VarHandle for moduleGeneration
    private static final VarHandle MODULE_GENERATION;

    // A list of Java class+method names to include in backtraces
    private final Map<String, Map<String, String>> boundMethods = new ConcurrentHashMap<>();

    // A soft pool of selectors for blocking IO operations
    private final SelectorPool selectorPool = new SelectorPool();

    // A global cache for Java-to-Ruby calls
    private final RuntimeCache runtimeCache;

    // Message for Errno exceptions that will not generate a backtrace
    public static final String ERRNO_BACKTRACE_MESSAGE = "errno backtraces disabled; run with -Xerrno.backtrace=true to enable";

    // Message for Errno exceptions that will not generate a backtrace
    public static final String STOPIERATION_BACKTRACE_MESSAGE = "StopIteration backtraces disabled; run with -Xstop_iteration.backtrace=true to enable";

    // Count of RaiseExceptions generated by code running in this runtime
    private volatile int exceptionCount;

    // VarHandle for exceptionCount;
    private static final VarHandle EXCEPTION_COUNT;

    // Count of exception backtraces generated by code running in this runtime
    private volatile int backtraceCount;

    // VarHandle for backtraceCount
    private static final VarHandle BACKTRACE_COUNT;

    // Count of Kernel#caller backtraces generated by code running in this runtime
    private volatile int callerCount;

    // VarHandle for callerCount
    private static final VarHandle CALLER_COUNT;

    // Count of built-in warning backtraces generated by code running in this runtime
    private volatile int warningCount;

    // VarHandle for warningCount
    private static final VarHandle WARNING_COUNT;

    static {
        try {
            MODULE_GENERATION = lookup().findVarHandle(Ruby.class, "moduleGeneration", int.class);
            EXCEPTION_COUNT = lookup().findVarHandle(Ruby.class, "exceptionCount", int.class);
            BACKTRACE_COUNT = lookup().findVarHandle(Ruby.class, "backtraceCount", int.class);
            CALLER_COUNT = lookup().findVarHandle(Ruby.class, "callerCount", int.class);
            WARNING_COUNT = lookup().findVarHandle(Ruby.class, "warningCount", int.class);
            COVERAGE_DATA = lookup().findVarHandle(Ruby.class, "coverageData", CoverageData.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final boolean coreIsBooted;
    private final boolean runtimeIsBooted;

    private final RubyHash envObject;

    private volatile CoverageData coverageData;

    private static final VarHandle COVERAGE_DATA;

    /** The "global" runtime. Set to the first runtime created, normally. */
    private static volatile Ruby globalRuntime;

    /** The "thread local" runtime. Set to the global runtime if unset. */
    private static final ThreadLocal<Ruby> threadLocalRuntime = new ThreadLocal<>();

    /** The runtime-local random number generator. Uses SecureRandom if permissions allow. */
    final Random random;

    /** The runtime-local seed for hash randomization */
    private final long hashSeedK0;
    private final long hashSeedK1;

    private final StaticScopeFactory staticScopeFactory;

    private final IRManager irManager;

    private FFI ffi;

    /** Used to find the ProfilingService implementation to use. If profiling is disabled it's null */
    private final ProfilingServiceLookup profilingServiceLookup;

    private final EnumMap<DefinedMessage, RubyString> definedMessages = new EnumMap<>(DefinedMessage.class);
    private final EnumMap<RubyThread.Status, RubyString> threadStatuses = new EnumMap<>(RubyThread.Status.class);

    private IRubyObject originalStderr;

    public interface ObjectSpacer {
        void addToObjectSpace(Ruby runtime, boolean useObjectSpace, IRubyObject object);
    }

    private static final ObjectSpacer DISABLED_OBJECTSPACE = (runtime, useObjectSpace, object) -> {
    };

    private static final ObjectSpacer ENABLED_OBJECTSPACE = (runtime, useObjectSpace, object) -> {
        if (useObjectSpace) runtime.objectSpace.add(object);
    };

    private final ObjectSpacer objectSpacer;

    public void addToObjectSpace(boolean useObjectSpace, IRubyObject object) {
        objectSpacer.addToObjectSpace(this, useObjectSpace, object);
    }

    public interface ExitFunction extends ToIntFunction<ThreadContext> {
        default boolean matches(Object o) { return o == this; }
    }

    private class ProcExitFunction implements ExitFunction {
        private final RubyProc proc;

        public ProcExitFunction(RubyProc proc) {
            this.proc = proc;
        }

        public boolean matches(Object o) {
            return (o instanceof RubyProc) && ((RubyProc) o).getBlock().getBody() == proc.getBlock().getBody();
        }

        @Override
        public int applyAsInt(ThreadContext context) {
            try {
                // IRubyObject oldExc = context.runtime.getGlobalVariables().get("$!"); // Save $!
                proc.call(context, IRubyObject.NULL_ARRAY);

            } catch (LocalJumpError rj) {
                // END { return } can generally be statically determined during build time so we generate the LJE
                // then.  This if captures the static side of this. See IReturnJump below for dynamic case
                RubyLocalJumpError rlje = (RubyLocalJumpError) rj.getException();
                String filename = proc.getBlock().getBinding().filename;

                if (rlje.getReason() == RubyLocalJumpError.Reason.RETURN) {
                    Ruby.this.getWarnings().warn(filename, "unexpected return");
                } else {
                    Ruby.this.getWarnings().warn(filename, "break from proc-closure");
                }

            } catch (SystemExit exit) {
                RubyException raisedException = exit.getException();
                // adopt new exit code
                // see jruby/jruby#5437 and related issues
                return toInt(context, raisedException.callMethod(context, "status"));
            } catch (RaiseException re) {
                // display and set error result but do not propagate other errors raised during at_exit
                Ruby.this.printError(re.getException());
                return 1;

            } catch (IRReturnJump e) {
                // This capture dynamic returns happening in an end block where it cannot be statically determined
                // (like within an eval.

                // This is partially similar to code in eval_error.c:error_handle but with less actual cases.
                // IR treats END blocks are closures and as such we see this special non-local return jump type
                // bubble this far out as we exec each END proc.
                Ruby.this.getWarnings().warn(proc.getBlock().getBinding().filename, "unexpected return");
            }

            return 0; // no errors
        }
    }

    private final RubyArray emptyFrozenArray;
    private final RubyString emptyFrozenString;

    /**
     * A map from Ruby string data to a pre-frozen global version of that string.
     *
     * Access must be synchronized.
     */
    private final Map<FStringEqual, WeakReference<RubyString.FString>> dedupMap = new ConcurrentWeakHashMap<>();

    private static final AtomicInteger RUNTIME_NUMBER = new AtomicInteger(0);
    private final int runtimeNumber = RUNTIME_NUMBER.getAndIncrement();

    private final Config configBean;
    private final org.jruby.management.Runtime runtimeBean;

    private final FilenoUtil filenoUtil;

    private final Interpreter interpreter = new Interpreter();

    /**
     * A representation of this runtime as a JIT-optimizable constant. Used for e.g. invokedynamic binding of runtime
     * accesses.
     */
    private final Object constant;

    /**
     * The built-in Class#new method, so we can bind more directly to allocate and initialize.
     */
    private DynamicMethod baseNewMethod;

    /**
     * The nullToNil filter for this runtime.
     */
    private MethodHandle nullToNil;
    private MethodHandle nullToUndefined;

    public final ClassValue<TypePopulator> POPULATORS = new ClassValue<>() {
        @Override
        protected TypePopulator computeValue(Class<?> type) {
            return RubyModule.loadPopulatorFor(type);
        }
    };

    public final JavaSites sites = new JavaSites();

    private final Map<Class, Consumer<RubyModule>> javaExtensionDefinitions = new WeakHashMap<>(); // caller-syncs

    // For strptime processing we cache the parsed format strings since most applications
    // reuse the same formats over and over.  This is also unbounded (for now) since all applications
    // I know of use very few of them.  Even if there are many the size of these lists are modest.
    private final Map<String, List<StrptimeToken>> strptimeFormatCache = new ConcurrentHashMap<>();

    transient RubyString tzVar;

    ParserManager parserManager;

    @Deprecated
    public RaiseException newErrnoEADDRFromBindException(BindException be) {
        return newErrnoEADDRFromBindException(be, null);
    }

    @Deprecated
    public RaiseException newFrozenError(String objectType) {
        return newFrozenError(objectType, null);
    }

    @Deprecated
    public RaiseException newFrozenError(RubyModule type) {
        return newRaiseException(getFrozenError(), str(this, "can't modify frozen ", types(this, type)));
    }

    @Deprecated
    public RaiseException newFrozenError(String objectType, boolean runtimeError) {
        return newRaiseException(getFrozenError(), str(this, "can't modify frozen ", ids(this, objectType)));
    }

    @Deprecated
    public synchronized void addEventHook(EventHook hook) {
        traceEvents.addEventHook(getCurrentContext(), hook);
    }

    @Deprecated
    public synchronized void removeEventHook(EventHook hook) {
        traceEvents.removeEventHook(hook);
    }

    @Deprecated
    public void setTraceFunction(RubyProc traceFunction) {
        traceEvents.setTraceFunction(traceFunction);
    }

    @Deprecated
    public void setTraceFunction(TraceEventManager.CallTraceFuncHook hook, RubyProc traceFunction) {
        traceEvents.setTraceFunction(hook, traceFunction);
    }

    @Deprecated
    public void removeAllCallEventHooksFor(ThreadContext context) {
        traceEvents.removeAllCallEventHooksFor(context);
    }

    @Deprecated
    public void callEventHooks(ThreadContext context, RubyEvent event, String file, int line, String name, IRubyObject type) {
        traceEvents.callEventHooks(context, event, file, line, name, type);
    }

    @Deprecated
    public boolean hasEventHooks() {
        return traceEvents.hasEventHooks();
    }

    @Deprecated
    public void setENV(RubyHash env) {
    }

}
