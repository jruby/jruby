package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class SystemExit extends RaiseException {
    public SystemExit(Ruby runtime, int status) {
        super(runtime, runtime.getExceptions().getSystemExit(), "", true);
        getException().setInstanceVariable("status", RubyFixnum.newFixnum(runtime, status));
    }
}