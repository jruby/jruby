/*
 **** BEGIN LICENSE BLOCK *****
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

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.NoMatchingPatternKeyError;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.ArgumentError;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.runtime.ThreadContext.resetCallInfo;
import static org.jruby.runtime.Visibility.PRIVATE;

/**
 * The Java representation of a Ruby ArgumentError.
 *
 * @see ArgumentError
 */
@JRubyClass(name="NoMatchingPatternKeyError", parent="StandardError")
public class RubyNoMatchingPatternKeyError extends RubyStandardError {
    private IRubyObject key;
    private IRubyObject matchee;

    protected RubyNoMatchingPatternKeyError(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    protected RubyNoMatchingPatternKeyError(Ruby runtime, RubyClass exceptionClass, String message) {
        super(runtime, exceptionClass, message);
    }

    static RubyClass define(ThreadContext context, RubyClass StandardError) {
        return defineClass(context, "NoMatchingPatternKeyError", StandardError, RubyNoMatchingPatternKeyError::new).
                defineMethods(context, RubyNoMatchingPatternKeyError.class);
    }

    @Override
    protected RaiseException constructThrowable(String message) {
        return new NoMatchingPatternKeyError(message, this);
    }

    private final static String[] INITIALIZE_KEYWORDS = new String[] {"key", "matchee"};

    private void setValues(ThreadContext context, IRubyObject message, IRubyObject key, IRubyObject matchee) {
        this.message = message;
        this.key = key == null ? context.nil : key;
        this.matchee = matchee == null ? context.nil : matchee;
    }

    @JRubyMethod(visibility = PRIVATE, optional = 2, keywords = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        int callInfo = resetCallInfo(context);

        switch (args.length) {
            case 0:
                setValues(context, context.nil, context.nil, context.nil);
                break;
            case 1:
                if ((callInfo & ThreadContext.CALL_KEYWORD) != 0) {
                    IRubyObject[] opts = ArgsUtil.extractKeywordArgs(context, ((RubyHash) args[0]), INITIALIZE_KEYWORDS);
                    setValues(context, context.nil, opts[0], opts[1]);
                } else {
                    setValues(context, args[0], context.nil, context.nil);
                }
                break;
            case 2:
                if ((callInfo & ThreadContext.CALL_KEYWORD) == 0) throw argumentError(context, 2, 0, 1);
                IRubyObject[] opts = ArgsUtil.extractKeywordArgs(context, ((RubyHash) args[1]), INITIALIZE_KEYWORDS);
                setValues(context, args[0], opts[0], opts[1]);
                break;
            default:
                throw argumentError(context, args.length, 0, 1);
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject key(ThreadContext context) {
        return key;
    }

    @JRubyMethod
    public IRubyObject matchee(ThreadContext context) {
        return matchee;
    }
}
