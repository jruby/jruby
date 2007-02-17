/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
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
import org.jruby.runtime.MethodSelectorTable;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.KCode;
import org.jruby.util.collections.SinglyLinkedList;

public interface IRuby {

    /**
     * Retrieve mappings of cached methods to where they have been cached.  When a cached
     * method needs to be invalidated this map can be used to remove all places it has been
     * cached.
     *
     * @return the mappings of where cached methods have been stored
     */
    CacheMap getCacheMap();

    /**
     * The contents of the runtimeInformation map are dumped with the JVM exits if
     * JRuby has been invoked via the Main class. Otherwise these contents can be used
     * by embedders to track development-time runtime information such as profiling
     * or logging data during execution.
     *
     * @return the runtimeInformation map
     * @see org.jruby.Main#runInterpreter
     */
    Map getRuntimeInformation();

    MethodSelectorTable getSelectorTable();

    /**
     * Evaluates a script and returns a RubyObject.
     */
    IRubyObject evalScript(String script);

    IRubyObject eval(Node node);

    IRubyObject compileAndRun(Node node);
    IRubyObject ycompileAndRun(Node node);

    RubyClass getObject();

    RubyModule getKernel();

    RubyClass getString();

    RubyClass getFixnum();

    RubyClass getArray();

    IRubyObject getTmsStruct();

    /** Returns the "true" instance from the instance pool.
     * @return The "true" instance.
     */
    RubyBoolean getTrue();

    /** Returns the "false" instance from the instance pool.
     * @return The "false" instance.
     */
    RubyBoolean getFalse();

    /** Returns the "nil" singleton instance.
     * @return "nil"
     */
    IRubyObject getNil();

    /**
     * @return The NilClass class
     */
    RubyClass getNilClass();

    RubyModule getModule(String name);

    /** Returns a class from the instance pool.
     *
     * @param name The name of the class.
     * @return The class.
     */
    RubyClass getClass(String name);

    /** Define a new class with name 'name' and super class 'superClass'.
     *
     * MRI: rb_define_class / rb_define_class_id
     *
     */
    RubyClass defineClass(String name, RubyClass superClass, ObjectAllocator allocator);

    RubyClass defineClassUnder(String name, RubyClass superClass, ObjectAllocator allocator, SinglyLinkedList parentCRef);

    /** rb_define_module / rb_define_module_id
     *
     */
    RubyModule defineModule(String name);

    RubyModule defineModuleUnder(String name, SinglyLinkedList parentCRef);

    /**
     * In the current context, get the named module. If it doesn't exist a
     * new module is created.
     */
    RubyModule getOrCreateModule(String name);

    /** Getter for property securityLevel.
     * @return Value of property securityLevel.
     */
    int getSafeLevel();

    /** Setter for property securityLevel.
     * @param safeLevel New value of property securityLevel.
     */
    void setSafeLevel(int safeLevel);

    void secure(int level);

    KCode getKCode();

    void setKCode(KCode kcode);

    /** rb_define_global_const
     *
     */
    void defineGlobalConstant(String name, IRubyObject value);

    IRubyObject getTopConstant(String name);

    String getCurrentDirectory();

    void setCurrentDirectory(String dir);

    long getStartTime();

    InputStream getIn();
    PrintStream getOut();
    PrintStream getErr();

    boolean isClassDefined(String name);

    boolean isObjectSpaceEnabled();

    /** Getter for property rubyTopSelf.
     * @return Value of property rubyTopSelf.
     */
    IRubyObject getTopSelf();

    /** Getter for property isVerbose.
     * @return Value of property isVerbose.
     */
    IRubyObject getVerbose();

    /** Setter for property isVerbose.
     * @param verbose New value of property isVerbose.
     */
    void setVerbose(IRubyObject verbose);

    /** Getter for property isDebug.
     * @return Value of property isDebug.
     */
    IRubyObject getDebug();

    /** Setter for property isDebug.
     * @param verbose New value of property isDebug.
     */
    void setDebug(IRubyObject debug);

    JavaSupport getJavaSupport();

    JRubyClassLoader getJRubyClassLoader();

    /** Defines a global variable
     */
    void defineVariable(final GlobalVariable variable);

    /** defines a readonly global variable
     *
     */
    void defineReadonlyVariable(String name, IRubyObject value);

    /**
     * Parse the source specified by the reader and return an AST
     *
     * @param content to be parsed
     * @param file the name of the file to be used in warnings/errors
     * @param scope that this content is being parsed under
     * @return the top of the AST
     */
    Node parse(Reader content, String file, DynamicScope scope);

    /**
     * Parse the source specified by the string and return an AST
     *
     * @param content to be parsed
     * @param file the name of the file to be used in warnings/errors
     * @param scope that this content is being parsed under
     * @return the top of the AST
     */
    Node parse(String content, String file, DynamicScope scope);

    ThreadService getThreadService();

    ThreadContext getCurrentContext();

    /**
     * Returns the loadService.
     * @return ILoadService
     */
    LoadService getLoadService();

    RubyWarnings getWarnings();

    PrintStream getErrorStream();

    InputStream getInputStream();

    PrintStream getOutputStream();

    RubyModule getClassFromPath(String path);

    /** Prints an error with backtrace to the error stream.
     *
     * MRI: eval.c - error_print()
     *
     */
    void printError(RubyException excp);

    /** This method compiles and interprets a Ruby script.
     *
     *  It can be used if you want to use JRuby as a Macro language.
     *
     */
    void loadScript(RubyString scriptName, RubyString source, boolean wrap);

    void loadScript(String scriptName, Reader source, boolean wrap);

    void loadNode(String scriptName, Node node, boolean wrap);

    /** Loads, compiles and interprets a Ruby file.
     *  Used by Kernel#require.
     *
     *  @mri rb_load
     */
    void loadFile(File file, boolean wrap);

    /** Call the trace function
     *
     * MRI: eval.c - call_trace_func
     *
     */
    void callTraceFunction(ThreadContext context, String event, ISourcePosition position, IRubyObject self, String name, IRubyObject type);

    RubyProc getTraceFunction();

    void setTraceFunction(RubyProc traceFunction);

    GlobalVariables getGlobalVariables();
    void setGlobalVariables(GlobalVariables variables);

    CallbackFactory callbackFactory(Class type);

    /**
     * Push block onto exit stack.  When runtime environment exits
     * these blocks will be evaluated.
     *
     * @return the element that was pushed onto stack
     */
    IRubyObject pushExitBlock(RubyProc proc);

    /**
     * Make sure Kernel#at_exit procs get invoked on runtime shutdown.
     * This method needs to be explicitly called to work properly.
     * I thought about using finalize(), but that did not work and I
     * am not sure the runtime will be at a state to run procs by the
     * time Ruby is going away.  This method can contain any other
     * things that need to be cleaned up at shutdown.
     */
    void tearDown();

    RubyArray newArray();

    RubyArray newArray(IRubyObject object);

    RubyArray newArray(IRubyObject car, IRubyObject cdr);

    RubyArray newArray(IRubyObject[] objects);
    RubyArray newArrayNoCopy(IRubyObject[] objects);
    
    RubyArray newArray(List list);

    RubyArray newArray(int size);

    RubyBoolean newBoolean(boolean value);

    RubyFileStat newRubyFileStat(String file);

    RubyFixnum newFixnum(long value);

    RubyFloat newFloat(double value);

    RubyNumeric newNumeric();

    RubyProc newProc(boolean isLambda, Block block);

    RubyBinding newBinding();
    RubyBinding newBinding(Block block);

    RubyString newString(String string);

    RubySymbol newSymbol(String string);

    RaiseException newArgumentError(String message);

    RaiseException newArgumentError(int got, int expected);

    RaiseException newErrnoEBADFError();

    RaiseException newErrnoECONNREFUSEDError();

    RaiseException newErrnoEADDRINUSEError();

    RaiseException newErrnoEINVALError();

    RaiseException newErrnoENOENTError();

    RaiseException newErrnoESPIPEError();

    RaiseException newErrnoEBADFError(String message);

    RaiseException newErrnoEINVALError(String message);

    RaiseException newErrnoENOENTError(String message);

    RaiseException newErrnoESPIPEError(String message);

    RaiseException newErrnoEEXISTError(String message);
    
    RaiseException newErrnoEDOMError(String message);

    RaiseException newIndexError(String message);

    RaiseException newSecurityError(String message);

    RaiseException newSystemCallError(String message);

    RaiseException newTypeError(String message);

    RaiseException newThreadError(String message);

    RaiseException newSyntaxError(String message);

    RaiseException newRangeError(String message);

    RaiseException newNotImplementedError(String message);

    RaiseException newNoMethodError(String message, String name);

    RaiseException newNameError(String message, String name);

    RaiseException newLocalJumpError(String message);

    RaiseException newLoadError(String message);

    RaiseException newFrozenError(String objectType);

    RaiseException newSystemStackError(String message);

    RaiseException newSystemExit(int status);

    RaiseException newIOError(String message);

    RaiseException newStandardError(String message);

    RaiseException newIOErrorFromException(IOException ioe);

    RaiseException newTypeError(IRubyObject receivedObject, RubyClass expectedType);

    RaiseException newEOFError();

    RaiseException newZeroDivisionError();

    RaiseException newFloatDomainError(String message);

    RubySymbol.SymbolTable getSymbolTable();

    void setStackTraces(int stackTraces);

    int getStackTraces();

    void setRandomSeed(long randomSeed);

    long getRandomSeed();

    Random getRandom();

    ObjectSpace getObjectSpace();

    Hashtable getIoHandlers();

    RubyFixnum[] getFixnumCache();

    long incrementRandomSeedSequence();

    RubyTime newTime(long milliseconds);

    boolean isGlobalAbortOnExceptionEnabled();

    void setGlobalAbortOnExceptionEnabled(boolean b);

    boolean isDoNotReverseLookupEnabled();

    void setDoNotReverseLookupEnabled(boolean b);

    boolean registerInspecting(Object obj);
    void unregisterInspecting(Object obj);

    Profile getProfile();

    String getJRubyHome();

    RubyInstanceConfig getInstanceConfig();

    /** GET_VM_STATE_VERSION */
    long getGlobalState();

    /** INC_VM_STATE_VERSION */
    void incGlobalState();
}
