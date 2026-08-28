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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
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

import org.jruby.Ruby;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.builtin.IRubyObject;

public class UndefinedAccessor implements IAccessor {
    private final Ruby runtime;
    private final GlobalVariable globalVariable;
    private final String name;

    /**
     * Constructor for UndefinedAccessor.
     */
    public UndefinedAccessor(Ruby runtime, GlobalVariable globalVariable, String name) {
        assert runtime != null;
        assert globalVariable != null;
        assert name != null;

        this.runtime = runtime;
        this.globalVariable = globalVariable;
        this.name = name;
    }

    /**
     * @see org.jruby.runtime.IAccessor#getValue()
     */
    public IRubyObject getValue() {
        if (runtime.isVerbose()) {
            runtime.getWarnings().warning(ID.ACCESSOR_NOT_INITIALIZED, "global variable '" + name + "' not initialized");
        }
        return runtime.getNil();
    }

    /**
     * @see org.jruby.runtime.IAccessor#setValue(IRubyObject)
     */
    public IRubyObject setValue(IRubyObject newValue) {
        assert newValue != null;
        globalVariable.setAccessor(new ValueAccessor(newValue));
        return newValue;
    }
}
