package org.jruby.runtime;

import org.jruby.*;

public class RubyExceptions {
    private RubyClass systemExit = null;
    private RubyClass fatal = null;
    private RubyClass interrupt = null;
    private RubyClass signalException = null;
    
    private RubyClass standardError = null;
    private RubyClass typeError = null;
    private RubyClass argumentError = null;
    private RubyClass indexError = null;
    private RubyClass rangeError = null;
    
    private RubyClass scriptError = null;
    private RubyClass syntaxError = null;
    private RubyClass nameError = null;
    private RubyClass loadError = null;
    private RubyClass notImplementedError = null;
    
    private RubyClass runtimeError = null;
    private RubyClass securityError = null;
    private RubyClass noMemError = null;
    
    private RubyClass regexpError = null;
    
    private RubyClass ioError = null;
    private RubyClass eofError = null;
    
    private RubyClass localJumpError = null;
    
    private Ruby ruby = null;
    
    public RubyExceptions(Ruby ruby) {
        this.ruby = ruby;
    }
        
    public void initDefaultExceptionClasses() {
        RubyClass exceptionClass = ruby.getClasses().getExceptionClass();
        
        systemExit = ruby.defineClass("SystemExit", exceptionClass);
        fatal = ruby.defineClass("Fatal", exceptionClass);
        interrupt = ruby.defineClass("Interrupt", exceptionClass);
        signalException = ruby.defineClass("SignalException", exceptionClass);
        
        standardError = ruby.defineClass("StandardError", exceptionClass);
        typeError = ruby.defineClass("TypeError", standardError);
        argumentError = ruby.defineClass("ArgumentError", standardError);
        indexError = ruby.defineClass("IndexError", standardError);
        rangeError = ruby.defineClass("RangeError", standardError);
        
        scriptError = ruby.defineClass("ScriptError", exceptionClass);
        syntaxError = ruby.defineClass("SyntaxError", scriptError);
        nameError = ruby.defineClass("NameError", scriptError);
        loadError = ruby.defineClass("LoadError", scriptError);
        notImplementedError = ruby.defineClass("NotImplementedError", scriptError);
        
        runtimeError = ruby.defineClass("RuntimeError", standardError);
        securityError = ruby.defineClass("SecurityError", standardError);
        noMemError = ruby.defineClass("NoMemError", exceptionClass);
        
        regexpError = ruby.defineClass("RegexpError", standardError);
        
        ioError = ruby.defineClass("IOError", standardError);
        eofError = ruby.defineClass("EOFError", ioError);

        localJumpError = ruby.defineClass("LocalJumpError", standardError);
    }
    /**
     * Gets the argumentError
     * @return Returns a RubyClass
     */
    public RubyClass getArgumentError() {
        return argumentError;
    }

    /**
     * Gets the fatal
     * @return Returns a RubyClass
     */
    public RubyClass getFatal() {
        return fatal;
    }

    /**
     * Gets the indexError
     * @return Returns a RubyClass
     */
    public RubyClass getIndexError() {
        return indexError;
    }
    /**
     * Gets the interrupt
     * @return Returns a RubyClass
     */
    public RubyClass getInterrupt() {
        return interrupt;
    }
    /**
     * Gets the loadError
     * @return Returns a RubyClass
     */
    public RubyClass getLoadError() {
        return loadError;
    }
    /**
     * Gets the nameError
     * @return Returns a RubyClass
     */
    public RubyClass getNameError() {
        return nameError;
    }
    /**
     * Gets the noMemError
     * @return Returns a RubyClass
     */
    public RubyClass getNoMemError() {
        return noMemError;
    }
    /**
     * Gets the notImplementedError
     * @return Returns a RubyClass
     */
    public RubyClass getNotImplementedError() {
        return notImplementedError;
    }
    /**
     * Gets the rangeError
     * @return Returns a RubyClass
     */
    public RubyClass getRangeError() {
        return rangeError;
    }
    /**
     * Gets the runtimeError
     * @return Returns a RubyClass
     */
    public RubyClass getRuntimeError() {
        return runtimeError;
    }
    /**
     * Gets the scriptError
     * @return Returns a RubyClass
     */
    public RubyClass getScriptError() {
        return scriptError;
    }
    /**
     * Gets the securityError
     * @return Returns a RubyClass
     */
    public RubyClass getSecurityError() {
        return securityError;
    }
    /**
     * Gets the signalException
     * @return Returns a RubyClass
     */
    public RubyClass getSignalException() {
        return signalException;
    }
    /**
     * Gets the standardError
     * @return Returns a RubyClass
     */
    public RubyClass getStandardError() {
        return standardError;
    }
    /**
     * Gets the syntaxError
     * @return Returns a RubyClass
     */
    public RubyClass getSyntaxError() {
        return syntaxError;
    }
    /**
     * Gets the systemExit
     * @return Returns a RubyClass
     */
    public RubyClass getSystemExit() {
        return systemExit;
    }
    /**
     * Gets the typeError
     * @return Returns a RubyClass
     */
    public RubyClass getTypeError() {
        return typeError;
    }
    
    /**
     * Gets the regexpError
     * @return Returns a RubyClass
     */
    public RubyClass getRegexpError() {
        return regexpError;
    }
    
    /**
     * Gets the ioError
     * @return Returns a RubyClass
     */
    public RubyClass getIOError() {
        return ioError;
    }
    
    /**
     * Gets the eofError
     * @return Returns a RubyClass
     */
    public RubyClass getEOFError() {
        return eofError;
    }

    /**
     * Returns the LocalJumpError class.
     * 
     * @return Returns a RubyClass
     */
    public RubyClass getLocalJumpError() {
        return localJumpError;
    }
}