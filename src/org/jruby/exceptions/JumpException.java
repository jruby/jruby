/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.exceptions;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyLocalJumpError.Reason;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This class should be used for performance reasons if the
 * Exception don't need a stack trace.
 *
 * @author jpetersen
 */
public class JumpException extends RuntimeException {
    private static final long serialVersionUID = -228162532535826617L;
    
    public static class FlowControlException extends JumpException implements Unrescuable {
        protected int target;
        protected Object value;
        protected final Reason reason;

        public FlowControlException(Reason reason) {
            this.reason = reason;
        }
        public FlowControlException(Reason reason, int target, Object value) {
            this.reason = reason;
            this.target = target;
            this.value = value;
        }
        public int getTarget() { return target; }
        public void setTarget(int target) { this.target = target; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }

        public RaiseException buildException(Ruby runtime) {
            switch (reason) {
                case RETURN:
                case BREAK:
                case NEXT:
                    // takes an argument
                    return runtime.newLocalJumpError(reason, (IRubyObject)value, "unexpected " + reason);
                case REDO:
                case RETRY:
                    // no argument
                    return runtime.newLocalJumpError(reason, runtime.getNil(), "unexpected " + reason);
                case NOREASON:
                default:
                    // different message or no reason given
                    return runtime.newLocalJumpError(reason, runtime.getNil(), "no reason");
            }
        }
    }
    
    public static class BreakJump extends FlowControlException { public BreakJump(int t, Object v) { super(Reason.BREAK, t, v); } }
    public static class NextJump extends FlowControlException { public NextJump(Object v) { super(Reason.NEXT, 0, v); } }
    public static class RetryJump extends FlowControlException { public RetryJump() {super(Reason.RETRY); } }
    public static final RetryJump RETRY_JUMP = new RetryJump();
    public static class RedoJump extends FlowControlException { public RedoJump() {super(Reason.REDO); } }
    public static final RedoJump REDO_JUMP = new RedoJump();
    public static class SpecialJump extends FlowControlException { public SpecialJump() {super(Reason.NOREASON); } }
    public static final SpecialJump SPECIAL_JUMP = new SpecialJump();
    public static class ReturnJump extends FlowControlException { public ReturnJump(int t, Object v) { super(Reason.RETURN, t, v); }}
    
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
    @Override
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
