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
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.runtime;

import org.jruby.IErrno;
import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;


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
    private RubyClass systemStackError = null;

    private Ruby runtime = null;
    private RubyModule errnoModule;
    private RubyClass nativeException;

    public RubyExceptions(Ruby runtime) {
        this.runtime = runtime;
    }
        
    public void initDefaultExceptionClasses() {
        RubyClass exceptionClass = runtime.getClasses().getExceptionClass();
        
        systemExit = runtime.defineClass("SystemExit", exceptionClass);
        fatal = runtime.defineClass("Fatal", exceptionClass);
        interrupt = runtime.defineClass("Interrupt", exceptionClass);
        signalException = runtime.defineClass("SignalException", exceptionClass);
        
        standardError = runtime.defineClass("StandardError", exceptionClass);
        typeError = runtime.defineClass("TypeError", standardError);
        argumentError = runtime.defineClass("ArgumentError", standardError);
        indexError = runtime.defineClass("IndexError", standardError);
        rangeError = runtime.defineClass("RangeError", standardError);
        
        scriptError = runtime.defineClass("ScriptError", exceptionClass);
        syntaxError = runtime.defineClass("SyntaxError", scriptError);
        nameError = runtime.defineClass("NameError", scriptError);
        loadError = runtime.defineClass("LoadError", scriptError);
        notImplementedError = runtime.defineClass("NotImplementedError", scriptError);
        noMethodError = runtime.defineClass("NoMethodError", nameError);
        
        runtimeError = runtime.defineClass("RuntimeError", standardError);
        securityError = runtime.defineClass("SecurityError", standardError);
        noMemError = runtime.defineClass("NoMemError", exceptionClass);
        
        regexpError = runtime.defineClass("RegexpError", standardError);
        
        ioError = runtime.defineClass("IOError", standardError);
        eofError = runtime.defineClass("EOFError", ioError);
        systemCallError = runtime.defineClass("SystemCallError", standardError);
        localJumpError = runtime.defineClass("LocalJumpError", standardError);
        threadError = runtime.defineClass("ThreadError", standardError);
        systemStackError = runtime.defineClass("SystemStackError", exceptionClass);

        nativeException = NativeException.createClass(runtime, runtimeError);
        errnoModule = runtime.defineModule("Errno");

        setSysErr(ENOTEMPTY, "ENOTEMPTY");   
        setSysErr(ERANGE, "ERANGE");      
        setSysErr(ESPIPE, "ESPIPE");      
        setSysErr(ENFILE, "ENFILE");      
        setSysErr(EXDEV, "EXDEV");       
        setSysErr(ENOMEM, "ENOMEM");      
        setSysErr(E2BIG, "E2BIG");       
        setSysErr(ENOENT, "ENOENT");      
        setSysErr(ENOSYS, "ENOSYS");      
        setSysErr(EDOM, "EDOM");        
        setSysErr(ENOSPC, "ENOSPC");      
        setSysErr(EINVAL, "EINVAL");      
        setSysErr(EEXIST, "EEXIST");      
        setSysErr(EAGAIN, "EAGAIN");      
        setSysErr(ENXIO, "ENXIO");       
        setSysErr(EILSEQ, "EILSEQ");      
        setSysErr(ENOLCK, "ENOLCK");      
        setSysErr(EPIPE, "EPIPE");       
        setSysErr(EFBIG, "EFBIG");       
        setSysErr(EISDIR, "EISDIR");      
        setSysErr(EBUSY, "EBUSY");       
        setSysErr(ECHILD, "ECHILD");      
        setSysErr(EIO, "EIO");         
        setSysErr(EPERM, "EPERM");       
        setSysErr(EDEADLOCK, "EDEADLOCK");   
        setSysErr(ENAMETOOLONG, "ENAMETOOLONG");
        setSysErr(EMLINK, "EMLINK");      
        setSysErr(ENOTTY, "ENOTTY");      
        setSysErr(ENOTDIR, "ENOTDIR");     
        setSysErr(EFAULT, "EFAULT");      
        setSysErr(EBADF, "EBADF");       
        setSysErr(EINTR, "EINTR");       
        setSysErr(EWOULDBLOCK, "EWOULDBLOCK"); 
        setSysErr(EDEADLK, "EDEADLK");     
        setSysErr(EROFS, "EROFS");       
        setSysErr(EMFILE, "EMFILE");      
        setSysErr(ENODEV, "ENODEV");      
        setSysErr(EACCES, "EACCES");      
        setSysErr(ENOEXEC, "ENOEXEC");             
        setSysErr(ESRCH, "ESRCH");       
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
        lError.defineConstant("Errno", runtime.newFixnum(i));
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

    public RubyClass getSystemStackError() {
        return systemStackError;
    }
}
