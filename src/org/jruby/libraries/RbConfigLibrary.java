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
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter
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
package org.jruby.libraries;

import org.jruby.IRuby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.runtime.Constants;
import org.jruby.runtime.load.Library;
import org.jruby.util.NormalizedFile;

public class RbConfigLibrary implements Library {
    /**
     * Just enough configuration settings (most don't make sense in Java) to run the rubytests
     * unit tests. The tests use <code>bindir</code>, <code>RUBY_INSTALL_NAME</code> and
     * <code>EXEEXT</code>.
     */
    public void load(IRuby runtime) {
        RubyModule configModule = runtime.defineModule("Config");
        RubyHash configHash = RubyHash.newHash(runtime);
        configModule.defineConstant("CONFIG", configHash);

        String[] versionParts = Constants.RUBY_VERSION.split("\\.");
        setConfig(configHash, "MAJOR", versionParts[0]);
        setConfig(configHash, "MINOR", versionParts[1]);
        setConfig(configHash, "TEENY", versionParts[2]);
        setConfig(configHash, "ruby_version", versionParts[0] + '.' + versionParts[1]);
        setConfig(configHash, "arch", "java");

        setConfig(configHash, "bindir", new NormalizedFile(System.getProperty("jruby.home"), "bin").getAbsolutePath());
        setConfig(configHash, "RUBY_INSTALL_NAME", System.getProperty("jruby.script").replace('\\', '/'));
        setConfig(configHash, "ruby_install_name", System.getProperty("jruby.script").replace('\\', '/'));
        setConfig(configHash, "SHELL", System.getProperty("jruby.shell").replace('\\', '/'));
        setConfig(configHash, "prefix", new NormalizedFile(System.getProperty("jruby.home")).getAbsolutePath());
        
        String libdir = System.getProperty("jruby.lib");
        if (libdir == null) {
        	libdir = new NormalizedFile(System.getProperty("jruby.home"), "lib").getAbsolutePath();
        } else {
            libdir = new NormalizedFile(libdir).getAbsolutePath();
        }
        
        setConfig(configHash, "libdir", libdir);
        setConfig(configHash, "rubylibdir", 	new NormalizedFile(libdir, "ruby/1.8").getAbsolutePath());
        setConfig(configHash, "sitedir", 		new NormalizedFile(libdir, "ruby/site_ruby").getAbsolutePath());
        setConfig(configHash, "sitelibdir", 	new NormalizedFile(libdir, "ruby/site_ruby/1.8").getAbsolutePath());
        setConfig(configHash, "sitearchdir", 	new NormalizedFile(libdir, "ruby/site_ruby/1.8/java").getAbsolutePath());
        setConfig(configHash, "configure_args", "");
        setConfig(configHash, "datadir", new NormalizedFile(System.getProperty("jruby.home"), "share").getAbsolutePath());
        setConfig(configHash, "mandir", new NormalizedFile(System.getProperty("jruby.home"), "man").getAbsolutePath());
        setConfig(configHash, "sysconfdir", new NormalizedFile(System.getProperty("jruby.home"), "etc").getAbsolutePath());
        
        if (isWindows()) {
        	setConfig(configHash, "EXEEXT", ".exe");
        } else {
        	setConfig(configHash, "EXEEXT", "");
        }
    }

    private static void setConfig(RubyHash configHash, String key, String value) {
        IRuby runtime = configHash.getRuntime();
        configHash.aset(runtime.newString(key), runtime.newString(value));
    }
    
    private static boolean isWindows() {
    	return System.getProperty("os.name", "").startsWith("Windows");
    }
}
