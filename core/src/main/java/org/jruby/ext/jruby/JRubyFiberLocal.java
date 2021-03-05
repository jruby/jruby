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
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ExecutionContext;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyClass(name = "JRuby::FiberLocal")
public final class JRubyFiberLocal extends JRubyExecutionContextLocal {
    public JRubyFiberLocal(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }

    @JRubyMethod(name = "with_value", required = 1)
    public IRubyObject withValue(ThreadContext context, IRubyObject value, Block block) {
        final Map<Object, IRubyObject> contextVariables;
        contextVariables = getContextVariables(context);
        final IRubyObject old_value;
        old_value = contextVariables.get(this);
        contextVariables.put(this, value);
        try {
            return block.yieldSpecific(context);
        } finally {
            contextVariables.put(this, old_value);
        }
    }

    protected final ExecutionContext getExecutionContext(ThreadContext context) {
        final ExecutionContext fiber;
        fiber = context.getFiber();
        if (fiber != null) {
            return fiber;
        } else {
            /* root fiber */
            return context.getThread();
        }
    }
    
}
