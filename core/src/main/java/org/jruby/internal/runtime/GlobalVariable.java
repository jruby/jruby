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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.internal.runtime;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.common.IRubyWarnings;
import org.jruby.runtime.GlobalSite;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author jpetersen
 */
public final class GlobalVariable {
    public enum Scope {GLOBAL, THREAD, FRAME}

    private GlobalSite accessor;
    private ArrayList<IRubyObject> traces = null;
    private boolean tracing;
    private final Scope scope;

    public GlobalVariable(GlobalSite accessor, Scope scope) {
        this.accessor = accessor;
        this.scope = scope;
    }

    public static GlobalVariable newUndefined(Ruby runtime, String name) {
        GlobalSite accessor = new UndefinedAccessor(runtime, name);

        return new GlobalVariable(accessor, Scope.GLOBAL);
    }

    public GlobalSite getAccessor() {
        return accessor;
    }

    public Scope getScope() {
        return scope;
    }

    public ArrayList getTraces() {
        return traces;
    }

    public void addTrace(RubyProc command) {
        if (traces == null) {
            traces = new ArrayList<IRubyObject>();
        }
        traces.add(command);
    }

    public boolean removeTrace(IRubyObject command) {
        if (traces == null || !traces.contains(command)) {
            return false;
        }
        traces.remove(command);
        return true;
    }

    public void removeTraces() {
        traces = null;
    }

    public void setAccessor(GlobalSite accessor) {
        this.accessor = accessor;
    }

    public boolean isTracing() {
        return tracing;
    }

    public void trace(IRubyObject value) {
        if (traces == null) return;

        ThreadContext context = value.getRuntime().getCurrentContext();

        if (context.isWithinTrace()) return;

        try {
            context.setWithinTrace(true);

            for (int i = 0; i < traces.size(); i++) {
                ((RubyProc) traces.get(i)).call(context, new IRubyObject[]{value});
            }
        } finally {
            context.setWithinTrace(false);
        }
    }

}
