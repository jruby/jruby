/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.libraries;

import java.io.File;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.runtime.Constants;
import org.jruby.runtime.load.Library;

public class RbConfig implements Library {
    /**
     * Just enough configuration settings (most don't make sense in Java) to run the rubytests
     * unit tests. The tests use <code>bindir</code>, <code>RUBY_INSTALL_NAME</code> and
     * <code>EXEEXT</code>.
     */
    public void load(Ruby runtime) {
        RubyModule configModule = runtime.defineModule("Config");
        RubyHash configHash = RubyHash.newHash(runtime);
        configModule.defineConstant("CONFIG", configHash);

        String[] versionParts = Constants.RUBY_VERSION.split("\\.");
        setConfig(configHash, "MAJOR", versionParts[0]);
        setConfig(configHash, "MINOR", versionParts[1]);
        setConfig(configHash, "TEENY", versionParts[2]);

        setConfig(configHash, "bindir", new File(System.getProperty("jruby.home"), "bin").getAbsolutePath());
        setConfig(configHash, "RUBY_INSTALL_NAME", System.getProperty("jruby.script"));
        setConfig(configHash, "SHELL", System.getProperty("jruby.shell"));
        
        if (isWindows()) {
        	setConfig(configHash, "EXEEXT", ".exe");
        }
    }

    private static void setConfig(RubyHash configHash, String key, String value) {
        Ruby runtime = configHash.getRuntime();
        configHash.aset(runtime.newString(key), runtime.newString(value));
    }
    
    private static boolean isWindows() {
    	return System.getProperty("os.name", "").startsWith("Windows");
    }
}
