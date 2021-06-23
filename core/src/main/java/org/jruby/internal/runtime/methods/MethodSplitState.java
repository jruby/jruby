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

package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.internal.runtime.InternalSplitState;
import org.jruby.ir.interpreter.ExitableInterpreterContext;
import org.jruby.ir.interpreter.ExitableInterpreterEngineState;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

class MethodSplitState implements InternalSplitState {
    public final ExitableInterpreterContext eic;
    public final ExitableInterpreterEngineState state;
    public final ThreadContext context;
    public final DynamicScope scope;
    public final RubyModule implClass;
    public final IRubyObject self;
    public final String name;

    public MethodSplitState(ThreadContext context, ExitableInterpreterContext ic, RubyModule clazz, IRubyObject self,
            String name) {
        this.context = context;
        this.eic = ic;
        this.state = ic.getEngineState();
        this.scope = DynamicScope.newDynamicScope(ic.getStaticScope());
        this.implClass = clazz;
        this.self = self;
        this.name = name;
    }
}
