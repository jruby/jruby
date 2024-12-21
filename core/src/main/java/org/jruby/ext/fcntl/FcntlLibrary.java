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

package org.jruby.ext.fcntl;

import java.io.IOException;

import jnr.constants.Constant;
import jnr.constants.ConstantSet;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Define.defineModule;

/**
 * Populates all the constants for Fcntl from Constantine
 */
public class FcntlLibrary implements Library {
    // TODO: FD_CLOEXEC is mysterious and we are not sure how constantine should include this.
    // We did it here for now
    public static final int FD_CLOEXEC = 1;

    public void load(final Ruby runtime, boolean wrap) throws IOException {
        var context = runtime.getCurrentContext();
        var Fcntl = defineModule(context, "Fcntl").
                defineConstant(context, "FD_CLOEXEC", RubyFixnum.newFixnum(runtime, FD_CLOEXEC));

        loadConstantSet(context, Fcntl, "Fcntl");
        loadConstantSet(context, Fcntl, "OpenFlags");
    }

    /**
     * Define all constants from the named jnr-constants set which are defined on the current platform.
     *
     * @param module the module in which we want to define the constants
     * @param constantSetName the name of the constant set from which to get the constants
     */
    private static void loadConstantSet(ThreadContext context, RubyModule module, String constantSetName) {
        for (Constant c : ConstantSet.getConstantSet(constantSetName)) {
            if (c.defined() && Character.isUpperCase(c.name().charAt(0))) {
                module.defineConstant(context, c.name(), asFixnum(context, c.intValue()));
            }
        }
    }
}
