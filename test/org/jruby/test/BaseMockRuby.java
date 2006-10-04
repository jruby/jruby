package org.jruby.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyBinding;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyFileStat;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyProc;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyTime;
import org.jruby.RubySymbol.SymbolTable;
import org.jruby.ast.Node;
import org.jruby.common.RubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ThreadService;
import org.jruby.javasupport.JavaSupport;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.Parser;
import org.jruby.runtime.Block;
import org.jruby.runtime.CacheMap;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.ObjectSpace;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.collections.SinglyLinkedList;

public class BaseMockRuby implements IRuby {

	public CacheMap getCacheMap() {
		throw new MockException();
	}

	public IRubyObject evalScript(String script) {
		throw new MockException();
	}

	public IRubyObject eval(Node node) {
		throw new MockException();
	}

    public RubyClass getObject() {
        throw new MockException();
    }

    public RubyModule getKernel() {
        throw new MockException();
    }
    
    public RubyClass getString() {
        throw new MockException();
    }
    
    public RubyClass getFixnum() {
        throw new MockException();
    }

	public RubyBoolean getTrue() {
		throw new MockException();
	}

	public RubyBoolean getFalse() {
		throw new MockException();
	}

	public IRubyObject getNil() {
		throw new MockException();
	}

    public RubyClass getNilClass() {
        throw new MockException();
    }

	public RubyModule getModule(String name) {
		throw new MockException();
	}

	public RubyClass getClass(String name) {
		throw new MockException();
	}

	public RubyClass defineClass(String name, RubyClass superClass) {
		throw new MockException();
	}

	public RubyClass defineClassUnder(String name, RubyClass superClass,
            SinglyLinkedList parentCRef) {
		throw new MockException();
	}

	public RubyModule defineModule(String name) {
		throw new MockException();
	}

	public RubyModule defineModuleUnder(String name, SinglyLinkedList parentCRef) {
		throw new MockException();
	}

	public RubyModule getOrCreateModule(String name) {
		throw new MockException();
	}

	public int getSafeLevel() {
		throw new MockException();
	}

	public void setSafeLevel(int safeLevel) {
		throw new MockException();
	}

	public void secure(int level) {
		throw new MockException();
	}

	public void defineGlobalConstant(String name, IRubyObject value) {
		throw new MockException();
	}

	public IRubyObject getTopConstant(String name) {
		throw new MockException();
	}

	public boolean isClassDefined(String name) {
		throw new MockException();
	}

	public IRubyObject yield(IRubyObject value) {
		throw new MockException();
	}

	public IRubyObject yield(IRubyObject value, IRubyObject self,
			RubyModule klass, boolean checkArguments) {
		throw new MockException();
	}

	public IRubyObject getTopSelf() {
		throw new MockException();
	}

	public String getSourceFile() {
		throw new MockException();
	}

	public int getSourceLine() {
		throw new MockException();
	}

	public IRubyObject getVerbose() {
		throw new MockException();
	}

	public boolean isBlockGiven() {
		throw new MockException();
	}

	public boolean isFBlockGiven() {
		throw new MockException();
		
	}

	public void setVerbose(IRubyObject verbose) {
		throw new MockException();

	}

	public Visibility getCurrentVisibility() {
		throw new MockException();
		
	}

	public void setCurrentVisibility(Visibility visibility) {
		throw new MockException();

	}

	public void defineVariable(GlobalVariable variable) {
		throw new MockException();

	}

	public void defineReadonlyVariable(String name, IRubyObject value) {
		throw new MockException();

	}

	public Node parse(Reader content, String file) {
		throw new MockException();
		
	}

	public Node parse(String content, String file) {
		throw new MockException();
		
	}

	public Parser getParser() {
		throw new MockException();
		
	}

	public ThreadService getThreadService() {
		throw new MockException();
		
	}

	public ThreadContext getCurrentContext() {
		throw new MockException();
		
	}

	public LoadService getLoadService() {
		throw new MockException();
		
	}

	public RubyWarnings getWarnings() {
		throw new MockException();
		
	}

	public PrintStream getErrorStream() {
		throw new MockException();
		
	}

	public InputStream getInputStream() {
		throw new MockException();
		
	}

	public PrintStream getOutputStream() {
		throw new MockException();
		
	}

	public RubyModule getClassFromPath(String path) {
		throw new MockException();
		
	}

	public void printError(RubyException excp) {
		throw new MockException();

	}

	public void loadScript(RubyString scriptName, RubyString source,
			boolean wrap) {
		throw new MockException();

	}

	public void loadScript(String scriptName, Reader source, boolean wrap) {
		throw new MockException();

	}

	public void loadNode(String scriptName, Node node, boolean wrap) {
		throw new MockException();

	}

	public void loadFile(File file, boolean wrap) {
		throw new MockException();

	}

	public void callTraceFunction(String event, ISourcePosition position,
			IRubyObject self, String name, IRubyObject type) {
		throw new MockException();

	}

	public RubyProc getTraceFunction() {
		throw new MockException();
		
	}

	public void setTraceFunction(RubyProc traceFunction) {
		throw new MockException();

	}

	public GlobalVariables getGlobalVariables() {
		throw new MockException();
		
	}

	public void setGlobalVariables(GlobalVariables variables) {
		throw new MockException();
		
	}

	public CallbackFactory callbackFactory(Class type) {
		throw new MockException();
		
	}

	public IRubyObject pushExitBlock(RubyProc proc) {
		throw new MockException();
		
	}

	public void tearDown() {
		throw new MockException();

	}

	public RubyArray newArray() {
		throw new MockException();
		
	}

	public RubyArray newArray(IRubyObject object) {
		throw new MockException();
		
	}

	public RubyArray newArray(IRubyObject car, IRubyObject cdr) {
		throw new MockException();
		
	}

	public RubyArray newArray(IRubyObject[] objects) {
		throw new MockException();
		
	}

	public RubyArray newArray(List list) {
		throw new MockException();
		
	}

	public RubyArray newArray(int size) {
		throw new MockException();
		
	}

	public RubyBoolean newBoolean(boolean value) {
		throw new MockException();
		
	}

	public RubyFileStat newRubyFileStat(File file) {
		throw new MockException();
		
	}

	public RubyFixnum newFixnum(long value) {
		throw new MockException();
		
	}

	public RubyFloat newFloat(double value) {
		throw new MockException();
		
	}

	public RubyNumeric newNumeric() {
		throw new MockException();

    }

    public RubyProc newProc() {
        throw new MockException();
        
    }

    public RubyBinding newBinding() {
        throw new MockException();
        
    }

    public RubyBinding newBinding(Block block) {
        throw new MockException();
        
    }

	public RubyString newString(String string) {
		throw new MockException();
		
	}

	public RubySymbol newSymbol(String string) {
		throw new MockException();
		
	}
    
    public RubyTime newTime(long milliseconds) {
        throw new MockException();
    }

	public RaiseException newArgumentError(String message) {
		throw new MockException();
		
	}

	public RaiseException newArgumentError(int got, int expected) {
		throw new MockException();
		
	}

	public RaiseException newErrnoEBADFError() {
		throw new MockException();
		
	}

	public RaiseException newErrnoEINVALError() {
		throw new MockException();
		
	}

	public RaiseException newErrnoENOENTError() {
		throw new MockException();
		
	}

	public RaiseException newErrnoESPIPEError() {
		throw new MockException();
		
	}

	public RaiseException newErrnoEBADFError(String message) {
		throw new MockException();
		
	}

	public RaiseException newErrnoEINVALError(String message) {
		throw new MockException();
		
	}

	public RaiseException newErrnoENOENTError(String message) {
		throw new MockException();
		
	}

	public RaiseException newErrnoESPIPEError(String message) {
		throw new MockException();
		
	}

	public RaiseException newErrnoEEXISTError(String message) {
		throw new MockException();
		
	}

	public RaiseException newIndexError(String message) {
		throw new MockException();
		
	}

	public RaiseException newSecurityError(String message) {
		throw new MockException();
		
	}

	public RaiseException newSystemCallError(String message) {
		throw new MockException();
		
	}

	public RaiseException newTypeError(String message) {
		throw new MockException();
		
	}

	public RaiseException newThreadError(String message) {
		throw new MockException();
		
	}

	public RaiseException newSyntaxError(String message) {
		throw new MockException();
		
	}

	public RaiseException newRangeError(String message) {
		throw new MockException();
		
	}

	public RaiseException newNotImplementedError(String message) {
		throw new MockException();
		
	}

	public RaiseException newNoMethodError(String message) {
		throw new MockException();
		
	}

	public RaiseException newNameError(String message) {
		throw new MockException();
		
	}

	public RaiseException newLocalJumpError(String message) {
		throw new MockException();
		
	}

	public RaiseException newLoadError(String message) {
		throw new MockException();
		
	}

	public RaiseException newFrozenError(String objectType) {
		throw new MockException();
		
	}

	public RaiseException newSystemStackError(String message) {
		throw new MockException();
		
	}

	public RaiseException newSystemExit(int status) {
		throw new MockException();
		
	}

	public RaiseException newIOError(String message) {
		throw new MockException();
		
	}

	public RaiseException newIOErrorFromException(IOException ioe) {
		throw new MockException();
		
	}

	public RaiseException newTypeError(IRubyObject receivedObject,
			RubyClass expectedType) {
		throw new MockException();
		
	}

	public RaiseException newEOFError() {
		throw new MockException();
		
	}

	public SymbolTable getSymbolTable() {
		throw new MockException();
		
	}

	public void setStackTraces(int stackTraces) {
		throw new MockException();

	}

	public int getStackTraces() {
		throw new MockException();
		
	}

	public void setRandomSeed(long randomSeed) {
		throw new MockException();

	}

	public long getRandomSeed() {
		throw new MockException();
		
	}

	public Random getRandom() {
		throw new MockException();
		
	}

	public ObjectSpace getObjectSpace() {
		throw new MockException();
		
	}

	public Hashtable getIoHandlers() {
		throw new MockException();
		
	}

	public RubyFixnum[] getFixnumCache() {
		throw new MockException();
		
	}

	public long incrementRandomSeedSequence() {
		throw new MockException();
		
	}

	public JavaSupport getJavaSupport() {
		throw new MockException();
	}

    public String getCurrentDirectory() {
        throw new MockException();
    }

    public void setCurrentDirectory(String dir) {
        throw new MockException();
    }

	public RaiseException newZeroDivisionError() {
        throw new MockException();
	}

	public InputStream getIn() {
        throw new MockException();
	}

	public PrintStream getOut() {
        throw new MockException();
	}

	public PrintStream getErr() {
        throw new MockException();
	}

	public boolean isGlobalAbortOnExceptionEnabled() {
        throw new MockException();
	}

	public void setGlobalAbortOnExceptionEnabled(boolean b) {
        throw new MockException();
	}

	public boolean isDoNotReverseLookupEnabled() {
        throw new MockException();
	}

	public void setDoNotReverseLookupEnabled(boolean b) {
        throw new MockException();
	}

}
