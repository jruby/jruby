package org.jruby.exceptions;

import org.jruby.Ruby;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class SystemExit extends RaiseException {
    public SystemExit(Ruby runtime, int status) {
        super(runtime, runtime.getExceptions().getSystemExit(), "", true);
        getException().setInstanceVariable("status", runtime.newFixnum(status));
    }
}