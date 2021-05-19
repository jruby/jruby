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

package org.jruby.ext.jruby;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyObject;
import org.jruby.RubyProc;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public abstract class JRubyExecutionContextLocal extends RubyObject {
    private IRubyObject default_value;
    private RubyProc default_proc;

    public JRubyExecutionContextLocal(Ruby runtime, RubyClass type) {
        super(runtime, type);
        default_value = runtime.getNil();
        default_proc = null;
    }

    @JRubyMethod(name = "initialize", optional = 1, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        if (block.isGiven()) {
            if (args.length != 0) {
                throw context.runtime.newArgumentError("wrong number of arguments");
            }
            default_proc = block.getProcObject();
            if (default_proc == null) {
                default_proc = RubyProc.newProc(context.runtime, block, block.type == Block.Type.LAMBDA ? block.type : Block.Type.PROC);
            }
        } else {
            if (args.length == 1) {
                default_value = args[0];
            } else if (args.length != 0) {
                throw context.runtime.newArgumentError("wrong number of arguments");
            }
        }
        return context.nil;
    }

    @JRubyMethod(name = "default")
    public IRubyObject getDefault() {
        return default_value;
    }

    @JRubyMethod(name = "default_proc")
    public IRubyObject getDefaultProc() {
        return (default_proc != null) ? default_proc : getRuntime().getNil();
    }

    @JRubyMethod(name = "value", required = 0)
    public IRubyObject getValue(ThreadContext context) {
        final IRubyObject value;
        final Map<Object, IRubyObject> contextVariables;
        contextVariables = getContextVariables(context);
        value = contextVariables.get(this);
        if (value != null) {
            return value;
        }
        if (default_proc != null) {
            // pre-set for the sake of terminating recursive calls
            contextVariables.put(this, context.nil);
            final IRubyObject new_value;
            new_value = default_proc.call(context, IRubyObject.NULL_ARRAY);
            contextVariables.put(this, new_value);
            return new_value;
        }
        return default_value;
    }

    @JRubyMethod(name = "value=", required = 1)
    public IRubyObject setValue(ThreadContext context, IRubyObject value) {
        getContextVariables(context).put(this, value);
        return value;
    }

    protected final Map<Object, IRubyObject> getContextVariables(ThreadContext context) {
        return getExecutionContext(context).getContextVariables();
    }

    protected abstract ExecutionContext getExecutionContext(ThreadContext context);
    
}
