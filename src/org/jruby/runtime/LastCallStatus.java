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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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
package org.jruby.runtime;

import org.jruby.IRuby;
import org.jruby.util.IdUtil;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class LastCallStatus {
    private static final Object NORMAL = new Object();
    private static final Object PRIVATE = new Object();
    private static final Object PROTECTED = new Object();
    private static final Object VARIABLE = new Object();

    private final IRuby runtime;
    private Object status = NORMAL;

    public LastCallStatus(IRuby runtime) {
        this.runtime = runtime;
    }

    public void setNormal() {
        status = NORMAL;
    }

    public void setPrivate() {
        status = PRIVATE;
    }

    public void setProtected() {
        status = PROTECTED;
    }

    public void setVariable() {
        status = VARIABLE;
    }
    
    // TODO: LastCallStatus should become an enumerated type with equality checks against constants
    public boolean isVariable() {
    	return status == VARIABLE;
    }

    public IRuby getRuntime() {
        return runtime;
    }

    public String errorMessageFormat(String name) {
        String format = "undefined method `%s' for %s%s%s";
        if (status == PRIVATE) {
            format = "private method '%s' called for %s%s%s";
        } else if (status == PROTECTED) {
            format = "protected method '%s' called for %s%s%s";
        } else if (status == VARIABLE) {
            if (IdUtil.isLocal(name)) {
                format = "undefined local variable or method '%s' for %s%s%s";
            }
        }
        return format;
    }
}
