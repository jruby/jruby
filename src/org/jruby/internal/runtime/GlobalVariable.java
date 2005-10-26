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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.internal.runtime;

import java.util.ArrayList;

import org.jruby.IRuby;
import org.jruby.RubyProc;
import org.jruby.runtime.IAccessor;

/**
 * 
 * @author jpetersen
 */
public final class GlobalVariable {
    private IAccessor accessor;
    private ArrayList traces = null;
    private boolean tracing;

    public GlobalVariable(IAccessor accessor) {
        this.accessor = accessor;
    }
    
    public static GlobalVariable newUndefined(IRuby runtime, String name) {
        GlobalVariable variable = new GlobalVariable(null);
        variable.setAccessor(new UndefinedAccessor(runtime, variable, name));
        return variable;
    }

    public IAccessor getAccessor() {
        return accessor;
    }

    public ArrayList getTraces() {
        return traces;
    }

    public void addTrace(RubyProc trace) {
        if (traces == null) {
            traces = new ArrayList();
        }
        traces.add(trace);
    }

    public void setAccessor(IAccessor accessor) {
        this.accessor = accessor;
    }
    public boolean isTracing() {
        return tracing;
    }

}
