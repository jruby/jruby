/*
 * Created on Jan 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyClass;

/**
 * @author enebo
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ErrnoError extends RaiseException {
    public ErrnoError(Ruby runtime, RubyClass errnoClass, String message) {
        
        super(runtime, errnoClass, message);
    }
    
    public static ErrnoError getErrnoError(Ruby runtime, String errno, 
            String message) {
        RubyClass errnoClass = 
            (RubyClass) runtime.getModule("Errno").getConstant(errno);
        
        return new ErrnoError(runtime, errnoClass, message); 
    }
}
