/*
 **** BEGIN LICENSE BLOCK *****
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

package org.jruby.util;

import com.martiansoftware.nailgun.NGContext;
import org.jruby.Main;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.executable.Script;
import org.jruby.runtime.Constants;

public class NailMain {
    public static final ClassCache<Script> classCache;

    static {
         classCache = new ClassCache<Script>(NailMain.class.getClassLoader(), Constants.JIT_MAX_METHODS_LIMIT);
    }
    public static void nailMain(NGContext context) {
        NailMain main = new NailMain();
        int status = main.run(context);
        if (status != 0) {
            context.exit(status);
        }
        // force a full GC so objects aren't kept alive longer than they should
        System.gc();
    }

    public int run(NGContext context) {
        context.assertLoopbackClient();

        RubyInstanceConfig config = new RubyInstanceConfig();
        Main main = new Main(config);
        
        config.setCurrentDirectory(context.getWorkingDirectory());
        config.setEnvironment(context.getEnv());

        // reuse one cache of compiled bodies
        config.setClassCache(classCache);

        return main.run(context.getArgs()).getStatus();
    }
}