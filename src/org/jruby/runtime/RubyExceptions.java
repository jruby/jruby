package org.jruby.runtime;

import org.jruby.IErrno;
import org.jruby.RubyClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyFixnum;


public class RubyExceptions implements IErrno {
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
    private RubyClass noMethodError = null;
    
    private RubyClass runtimeError = null;
    private RubyClass securityError = null;
    private RubyClass noMemError = null;
    
    private RubyClass regexpError = null;
    
    private RubyClass ioError = null;
    private RubyClass eofError = null;
    private RubyClass systemCallError = null;
    
    private RubyClass localJumpError = null;

    private RubyClass threadError = null;

    private Ruby ruby = null;
    private RubyModule errnoModule;
   
 private RubyClass _ENOTEMPTY   ;
 private RubyClass _ERANGE      ;
 private RubyClass _ESPIPE      ;
 private RubyClass _ENFILE      ;
 private RubyClass _EXDEV       ;
 private RubyClass _ENOMEM      ;
 private RubyClass _E2BIG       ;
 private RubyClass _ENOENT      ;
 private RubyClass _ENOSYS      ;
 private RubyClass _EDOM        ;
 private RubyClass _ENOSPC      ;
 private RubyClass _EINVAL      ;
 private RubyClass _EEXIST      ;
 private RubyClass _EAGAIN      ;
 private RubyClass _ENXIO       ;
 private RubyClass _EILSEQ      ;
 private RubyClass _ENOLCK      ;
 private RubyClass _EPIPE       ;
 private RubyClass _EFBIG       ;
 private RubyClass _EISDIR      ;
 private RubyClass _EBUSY       ;
 private RubyClass _ECHILD      ;
 private RubyClass _EIO         ;
 private RubyClass _EPERM       ;
 private RubyClass _EDEADLOCK       ;
 private RubyClass _ENAMETOOLONG;
 private RubyClass _EMLINK      ;
 private RubyClass _ENOTTY      ;
 private RubyClass _ENOTDIR     ;
 private RubyClass _EFAULT      ;
 private RubyClass _EBADF       ;
 private RubyClass _EINTR       ;
 private RubyClass _EWOULDBLOCK ;
 private RubyClass _EDEADLK     ;
 private RubyClass _EROFS       ;
 private RubyClass _EMFILE      ;
 private RubyClass _ENODEV      ;
 private RubyClass _EACCES      ;
 private RubyClass _ENOEXEC     ;
 private RubyClass _ESRCH       ;




    
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
        noMethodError = ruby.defineClass("NoMethodError", nameError);
        
        runtimeError = ruby.defineClass("RuntimeError", standardError);
        securityError = ruby.defineClass("SecurityError", standardError);
        noMemError = ruby.defineClass("NoMemError", exceptionClass);
        
        regexpError = ruby.defineClass("RegexpError", standardError);
        
        ioError = ruby.defineClass("IOError", standardError);
        eofError = ruby.defineClass("EOFError", ioError);
        systemCallError = ruby.defineClass("SystemCallError", standardError);
        localJumpError = ruby.defineClass("LocalJumpError", standardError);
        threadError = ruby.defineClass("ThreadError", standardError);

        errnoModule = ruby.defineModule("Errno");

        _ENOTEMPTY = setSysErr(ENOTEMPTY, "ENOTEMPTY");   
        _ERANGE = setSysErr(ERANGE, "ERANGE");      
        _ESPIPE = setSysErr(ESPIPE, "ESPIPE");      
        _ENFILE = setSysErr(ENFILE, "ENFILE");      
        _EXDEV = setSysErr(EXDEV, "EXDEV");       
        _ENOMEM = setSysErr(ENOMEM, "ENOMEM");      
        _E2BIG = setSysErr(E2BIG, "E2BIG");       
        _ENOENT = setSysErr(ENOENT, "ENOENT");      
        _ENOSYS = setSysErr(ENOSYS, "ENOSYS");      
        _EDOM = setSysErr(EDOM, "EDOM");        
        _ENOSPC = setSysErr(ENOSPC, "ENOSPC");      
        _EINVAL = setSysErr(EINVAL, "EINVAL");      
        _EEXIST = setSysErr(EEXIST, "EEXIST");      
        _EAGAIN = setSysErr(EAGAIN, "EAGAIN");      
        _ENXIO = setSysErr(ENXIO, "ENXIO");       
        _EILSEQ = setSysErr(EILSEQ, "EILSEQ");      
        _ENOLCK = setSysErr(ENOLCK, "ENOLCK");      
        _EPIPE = setSysErr(EPIPE, "EPIPE");       
        _EFBIG = setSysErr(EFBIG, "EFBIG");       
        _EISDIR = setSysErr(EISDIR, "EISDIR");      
        _EBUSY = setSysErr(EBUSY, "EBUSY");       
        _ECHILD = setSysErr(ECHILD, "ECHILD");      
        _EIO = setSysErr(EIO, "EIO");         
        _EPERM = setSysErr(EPERM, "EPERM");       
        _EDEADLOCK = setSysErr(EDEADLOCK, "EDEADLOCK");   
        _ENAMETOOLONG = setSysErr(ENAMETOOLONG, "ENAMETOOLONG");
        _EMLINK = setSysErr(EMLINK, "EMLINK");      
        _ENOTTY = setSysErr(ENOTTY, "ENOTTY");      
        _ENOTDIR = setSysErr(ENOTDIR, "ENOTDIR");     
        _EFAULT = setSysErr(EFAULT, "EFAULT");      
        _EBADF = setSysErr(EBADF, "EBADF");       
        _EINTR = setSysErr(EINTR, "EINTR");       
        _EWOULDBLOCK = setSysErr(EWOULDBLOCK, "EWOULDBLOCK"); 
        _EDEADLK = setSysErr(EDEADLK, "EDEADLK");     
        _EROFS = setSysErr(EROFS, "EROFS");       
        _EMFILE = setSysErr(EMFILE, "EMFILE");      
        _ENODEV = setSysErr(ENODEV, "ENODEV");      
        _EACCES = setSysErr(EACCES, "EACCES");      
        _ENOEXEC = setSysErr(ENOEXEC, "ENOEXEC");             
        _ESRCH = setSysErr(ESRCH, "ESRCH");       
    } 
    
    /**
     * Creates a system error.
     * @param i the error code (will probably use a java exception instead)
     * @param iName name of the error to define.
     * @return the newly defined error
     **/
    private RubyClass setSysErr(int i, String iName)
    {
        RubyClass lError = errnoModule.defineClassUnder(iName, systemCallError);
        lError.defineConstant("Errno", RubyFixnum.newFixnum(ruby, i));
        return lError;
    }
        
    /** Returns the reference to the Errno module.
     * @return The Errno module.
     */
    public RubyModule getErrnoModule() {
        return errnoModule;
    }



    /**
     * Gets the systemCallError
     * @return Returns a RubyClass
     */
    public RubyClass getSystemCallError() {
        return systemCallError;
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
     * Gets the noMethodError
     * @return Returns a RubyClass
     */
    public RubyClass getNoMethodError() {
        return noMethodError;
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

    public RubyClass getThreadError() {
        return threadError;
    }
}
