package org.jruby;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.jruby.ast.Node;
import org.jruby.common.RubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.javasupport.JavaSupport;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.Block;
import org.jruby.runtime.CacheMap;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.collections.SinglyLinkedList;

public interface IRuby {

	/**
	 * Retrieve mappings of cached methods to where they have been cached.  When a cached
	 * method needs to be invalidated this map can be used to remove all places it has been
	 * cached.
	 * 
	 * @return the mappings of where cached methods have been stored
	 */
	public CacheMap getCacheMap();

	/**
	 * Evaluates a script and returns a RubyObject.
	 */
	public IRubyObject evalScript(String script);

    public IRubyObject eval(Node node);

    public IRubyObject compileAndRun(Node node);

	public RubyClass getObject();
    
    public RubyModule getKernel();
    
    public RubyClass getString();
    
    public RubyClass getFixnum();
    
    public IRubyObject getTmsStruct();

	/** Returns the "true" instance from the instance pool.
	 * @return The "true" instance.
	 */
	public RubyBoolean getTrue();

	/** Returns the "false" instance from the instance pool.
	 * @return The "false" instance.
	 */
	public RubyBoolean getFalse();

	/** Returns the "nil" singleton instance.
	 * @return "nil"
	 */
	public IRubyObject getNil();
    
    /**
     * @return The NilClass class
     */
    public RubyClass getNilClass();

	public RubyModule getModule(String name);

	/** Returns a class from the instance pool.
	 *
	 * @param name The name of the class.
	 * @return The class.
	 */
	public RubyClass getClass(String name);

	/** Define a new class with name 'name' and super class 'superClass'.
	 *
	 * MRI: rb_define_class / rb_define_class_id
	 *
	 */
	public RubyClass defineClass(String name, RubyClass superClass);

	public RubyClass defineClassUnder(String name, RubyClass superClass, SinglyLinkedList parentCRef);

	/** rb_define_module / rb_define_module_id
	 *
	 */
	public RubyModule defineModule(String name);

	public RubyModule defineModuleUnder(String name, SinglyLinkedList parentCRef);

	/**
	 * In the current context, get the named module. If it doesn't exist a
	 * new module is created.
	 */
	public RubyModule getOrCreateModule(String name);

	/** Getter for property securityLevel.
	 * @return Value of property securityLevel.
	 */
	public int getSafeLevel();

	/** Setter for property securityLevel.
	 * @param safeLevel New value of property securityLevel.
	 */
	public void setSafeLevel(int safeLevel);

	public void secure(int level);

	/** rb_define_global_const
	 *
	 */
	public void defineGlobalConstant(String name, IRubyObject value);

	public IRubyObject getTopConstant(String name);
    
    public String getCurrentDirectory();
    
    public void setCurrentDirectory(String dir);
    
    public long getStartTime();
    
    public InputStream getIn();
    public PrintStream getOut();
    public PrintStream getErr();

	public boolean isClassDefined(String name);

    public boolean isObjectSpaceEnabled();
    
	/** Getter for property rubyTopSelf.
	 * @return Value of property rubyTopSelf.
	 */
	public IRubyObject getTopSelf();

    /** Getter for property isVerbose.
	 * @return Value of property isVerbose.
	 */
	public IRubyObject getVerbose();

	/** Setter for property isVerbose.
	 * @param verbose New value of property isVerbose.
	 */
	public void setVerbose(IRubyObject verbose);

    /** Getter for property isDebug.
	 * @return Value of property isDebug.
	 */
	public IRubyObject getDebug();

	/** Setter for property isDebug.
	 * @param verbose New value of property isDebug.
	 */
	public void setDebug(IRubyObject debug);

    public JavaSupport getJavaSupport();

    /** Defines a global variable
	 */
	public void defineVariable(final GlobalVariable variable);

	/** defines a readonly global variable
	 *
	 */
	public void defineReadonlyVariable(String name, IRubyObject value);

    /**
     * Parse the source specified by the reader and return an AST
     * 
     * @param content to be parsed
     * @param file the name of the file to be used in warnings/errors
     * @param scope that this content is being parsed under
     * @return the top of the AST
     */
	public Node parse(Reader content, String file, DynamicScope scope);

    /**
     * Parse the source specified by the string and return an AST
     * 
     * @param content to be parsed
     * @param file the name of the file to be used in warnings/errors
     * @param scope that this content is being parsed under
     * @return the top of the AST
     */
	public Node parse(String content, String file, DynamicScope scope);

	public ThreadService getThreadService();

	public ThreadContext getCurrentContext();

    /**
	 * Returns the loadService.
	 * @return ILoadService
	 */
	public LoadService getLoadService();

	public RubyWarnings getWarnings();

	public PrintStream getErrorStream();

	public InputStream getInputStream();

	public PrintStream getOutputStream();

	public RubyModule getClassFromPath(String path);

	/** Prints an error with backtrace to the error stream.
	 *
	 * MRI: eval.c - error_print()
	 *
	 */
	public void printError(RubyException excp);

	/** This method compiles and interprets a Ruby script.
	 *
	 *  It can be used if you want to use JRuby as a Macro language.
	 *
	 */
	public void loadScript(RubyString scriptName, RubyString source,
			boolean wrap);

	public void loadScript(String scriptName, Reader source, boolean wrap);

	public void loadNode(String scriptName, Node node, boolean wrap);

	/** Loads, compiles and interprets a Ruby file.
	 *  Used by Kernel#require.
	 *
	 *  @mri rb_load
	 */
	public void loadFile(File file, boolean wrap);

	/** Call the trace function
	 *
	 * MRI: eval.c - call_trace_func
	 *
	 */
	public void callTraceFunction(ThreadContext context, String event, ISourcePosition position,
			IRubyObject self, String name, IRubyObject type);

	public RubyProc getTraceFunction();

	public void setTraceFunction(RubyProc traceFunction);

	public GlobalVariables getGlobalVariables();
	public void setGlobalVariables(GlobalVariables variables);

	public CallbackFactory callbackFactory(Class type);

	/**
	 * Push block onto exit stack.  When runtime environment exits
	 * these blocks will be evaluated.
	 * 
	 * @return the element that was pushed onto stack
	 */
	public IRubyObject pushExitBlock(RubyProc proc);

	/**
	 * Make sure Kernel#at_exit procs get invoked on runtime shutdown.
	 * This method needs to be explicitly called to work properly.
	 * I thought about using finalize(), but that did not work and I
	 * am not sure the runtime will be at a state to run procs by the
	 * time Ruby is going away.  This method can contain any other
	 * things that need to be cleaned up at shutdown.  
	 */
	public void tearDown();

	public RubyArray newArray();

	public RubyArray newArray(IRubyObject object);

	public RubyArray newArray(IRubyObject car, IRubyObject cdr);

	public RubyArray newArray(IRubyObject[] objects);

	public RubyArray newArray(List list);

	public RubyArray newArray(int size);

	public RubyBoolean newBoolean(boolean value);

	public RubyFileStat newRubyFileStat(File file);

	public RubyFixnum newFixnum(long value);

	public RubyFloat newFloat(double value);

	public RubyNumeric newNumeric();

    public RubyProc newProc();

    public RubyBinding newBinding();
    public RubyBinding newBinding(Block block);

	public RubyString newString(String string);

	public RubySymbol newSymbol(String string);

    public RaiseException newArgumentError(String message);
    
    public RaiseException newArgumentError(int got, int expected);
    
    public RaiseException newErrnoEBADFError();

    public RaiseException newErrnoEINVALError();

    public RaiseException newErrnoENOENTError();

    public RaiseException newErrnoESPIPEError();

    public RaiseException newErrnoEBADFError(String message);

    public RaiseException newErrnoEINVALError(String message);

    public RaiseException newErrnoENOENTError(String message);

    public RaiseException newErrnoESPIPEError(String message);

    public RaiseException newErrnoEEXISTError(String message);

    public RaiseException newIndexError(String message);
    
    public RaiseException newSecurityError(String message);
    
    public RaiseException newSystemCallError(String message);

    public RaiseException newTypeError(String message);
    
    public RaiseException newThreadError(String message);
    
    public RaiseException newSyntaxError(String message);

    public RaiseException newRangeError(String message);

    public RaiseException newNotImplementedError(String message);

    public RaiseException newNoMethodError(String message, String name);

    public RaiseException newNameError(String message, String name);

    public RaiseException newLocalJumpError(String message);

    public RaiseException newLoadError(String message);

    public RaiseException newFrozenError(String objectType);

    public RaiseException newSystemStackError(String message);
    
    public RaiseException newSystemExit(int status);
    
    public RaiseException newIOError(String message);
    
    public RaiseException newIOErrorFromException(IOException ioe);
    
    public RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType);

    public RaiseException newEOFError();
    
    public RaiseException newZeroDivisionError();

	public RubySymbol.SymbolTable getSymbolTable();

	public void setStackTraces(int stackTraces);

	public int getStackTraces();

	public void setRandomSeed(long randomSeed);

	public long getRandomSeed();

	public Random getRandom();

	public ObjectSpace getObjectSpace();

	public Hashtable getIoHandlers();

	public RubyFixnum[] getFixnumCache();

	public long incrementRandomSeedSequence();

    public RubyTime newTime(long milliseconds);

	public boolean isGlobalAbortOnExceptionEnabled();

	public void setGlobalAbortOnExceptionEnabled(boolean b);

	public boolean isDoNotReverseLookupEnabled();

	public void setDoNotReverseLookupEnabled(boolean b);

    public boolean registerInspecting(Object obj);
    public void unregisterInspecting(Object obj);

    public void setEncoding(String encoding);
    public String getEncoding();
}
