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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
package org.jruby.exceptions;

import org.jruby.RubyInstanceConfig;
import org.jruby.internal.runtime.JumpTarget;

/**
 * This class should be used for performance reasons if the
 * Exception don't need a stack trace.
 *
 * @author jpetersen
 */
public class JumpException extends RuntimeException {
    private static final long serialVersionUID = -228162532535826617L;
    
    public static class FlowControlException extends JumpException {
        protected JumpTarget target;
        protected Object value;

        public FlowControlException() {}
        public FlowControlException(JumpTarget target, Object value) {
            this.target = target;
            this.value = value;
        }
        public JumpTarget getTarget() { return target; }
        public void setTarget(JumpTarget target) { this.target = target; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }
    
    public static class BreakJump extends FlowControlException { public BreakJump(JumpTarget t, Object v) { super(t, v); }}
    public static class NextJump extends FlowControlException { public NextJump(Object v) { super(null, v); }}
    public static class RetryJump extends FlowControlException {}
    public static final RetryJump RETRY_JUMP = new RetryJump();
    public static class ThrowJump extends FlowControlException { public ThrowJump(JumpTarget t, Object v) { super(t, v); }}
    public static class RedoJump extends FlowControlException {}
    public static final RedoJump REDO_JUMP = new RedoJump();
    public static class SpecialJump extends FlowControlException {}
    public static final SpecialJump SPECIAL_JUMP = new SpecialJump();
    public static class ReturnJump extends FlowControlException { public ReturnJump(JumpTarget t, Object v) { target = t; value = v; }}
    
    /**
     * Constructor for flow-control-only JumpExceptions.
     */
    public JumpException() {
    }
    
    /**
     * Constructor for JumpException.
     * @param msg
     */
    public JumpException(String msg) {
        super(msg);
    }
    
    public JumpException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    /** This method don't do anything for performance reasons.
     *
     * @see Throwable#fillInStackTrace()
     */
    public Throwable fillInStackTrace() {
        if (RubyInstanceConfig.JUMPS_HAVE_BACKTRACE) {
            return originalFillInStackTrace();
        }
        
        return this;
    }
    
    protected Throwable originalFillInStackTrace() {
        return super.fillInStackTrace();
    }
}
