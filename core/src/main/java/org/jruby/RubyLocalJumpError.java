/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.LocalJumpError;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * The Java representation of a Ruby LocalJumpError.
 *
 * @see LocalJumpError
 */
@JRubyClass(name="LocalJumpError",parent="StandardError")
public class RubyLocalJumpError extends RubyStandardError {
    public enum Reason {
        REDO, BREAK, NEXT, RETURN, RETRY, NOREASON;
        
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
    
    private static final ObjectAllocator LOCALJUMPERROR_ALLOCATOR = RubyLocalJumpError::new;

    public static RubyClass define(Ruby runtime, RubyClass standardErrorClass) {
        RubyClass nameErrorClass = runtime.defineClass("LocalJumpError", standardErrorClass, LOCALJUMPERROR_ALLOCATOR);
        
        nameErrorClass.defineAnnotatedMethods(RubyLocalJumpError.class);

        return nameErrorClass;
    }
    
    private Reason reason;
    
    private RubyLocalJumpError(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    public RubyLocalJumpError(Ruby runtime, RubyClass exceptionClass, String message, Reason reason, IRubyObject exitValue) {
        super(runtime, exceptionClass, message);
        this.reason = reason;
        setInternalVariable("reason", runtime.newSymbol(reason.toString()));
        setInternalVariable("exit_value", exitValue);
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new LocalJumpError(message, this);
    }

    @JRubyMethod
    public IRubyObject reason() {
        return (IRubyObject)getInternalVariable("reason");
    }
    
    public Reason getReason() {
        return reason;
    }
    
    @JRubyMethod
    public IRubyObject exit_value() {
        return (IRubyObject)getInternalVariable("exit_value");
    }
}
