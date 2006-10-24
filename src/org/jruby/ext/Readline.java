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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.ext;

import java.io.IOException;
import java.lang.reflect.Method;

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class Readline {
    public static class Service implements Library {
        public void load(final IRuby runtime) throws IOException {
            createReadline(runtime);
        }
    }

    private static Method readlineMeth = null;
    private static Method addToHistMeth = null;
    public static void createReadline(IRuby runtime) {
        try {
            Class readline = Class.forName("org.gnu.readline.Readline");
            Class readlineLib = Class.forName("org.gnu.readline.ReadlineLibrary");
            readline.getMethod("load",new Class[]{readlineLib}).invoke(null,new Object[]{readlineLib.getMethod("byName",new Class[]{String.class}).invoke(null,new Object[]{"GnuReadline"})});
            readline.getMethod("initReadline",new Class[]{String.class}).invoke(null,new Object[]{"Ruby"});
            readlineMeth = readline.getMethod("readline",new Class[]{String.class,Boolean.TYPE});
            addToHistMeth = readline.getMethod("addToHistory",new Class[]{String.class});

            RubyModule mReadline = runtime.defineModule("Readline");
            CallbackFactory readlinecb = runtime.callbackFactory(Readline.class);
            mReadline.defineMethod("readline",readlinecb.getSingletonMethod("s_readline",IRubyObject.class,IRubyObject.class));
            mReadline.module_function(new IRubyObject[]{runtime.newSymbol("readline")});
            IRubyObject hist = runtime.getObject().callMethod("new");
            mReadline.setConstant("HISTORY",hist);
            hist.defineSingletonMethod("push",readlinecb.getSingletonMethod("s_push",IRubyObject.class));
            hist.defineSingletonMethod("pop",readlinecb.getSingletonMethod("s_pop"));
        } catch(Exception e) {
            throw runtime.newLoadError("Missing libreadline-java library; see Readline-HOWTO.txt in docs");
        }
    }

    public static IRubyObject s_readline(IRubyObject recv, IRubyObject prompt, IRubyObject add_to_hist) {
        IRubyObject line = recv.getRuntime().getNil();
        try {
            String v = (String)readlineMeth.invoke(null,new Object[]{prompt.toString(),new Boolean(add_to_hist.isTrue())});
            if(null != v) {
                line = recv.getRuntime().newString(v);
            }
        } catch(Exception ioe) {
            return recv.getRuntime().getNil();
        }
        if(line.isNil()) {
            return recv.getRuntime().newString("");
        }
        return line;
    }

    public static IRubyObject s_push(IRubyObject recv, IRubyObject line) throws Exception {
        addToHistMeth.invoke(null,new Object[]{line.toString()});
        return recv.getRuntime().getNil();
    }

    public static IRubyObject s_pop(IRubyObject recv) throws Exception {
        return recv.getRuntime().getNil();
    }
}// Readline
