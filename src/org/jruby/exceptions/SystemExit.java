package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyException;
import org.jruby.RubyFixnum;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class SystemExit extends RaiseException {

    public SystemExit(Ruby runtime, int status) {
        super(RubyException.newException(runtime, runtime.getExceptions().getSystemExit(), ""));
        getException().setInstanceVariable("status", RubyFixnum.newFixnum(runtime, status));
    }
}