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

import org.jruby.runtime.Constants;
import org.jruby.runtime.load.Library;
import org.jruby.RubyHash;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyString;

import java.io.File;

public class RbConfig implements Library {
    private RubyHash configHash;

    public void load(Ruby runtime) {
        RubyModule configModule = runtime.defineModule("Config");
        configHash = RubyHash.newHash(runtime);
        configModule.defineConstant("CONFIG", configHash);

        String[] versionParts = Constants.RUBY_VERSION.split("\\.");
        setConfig("MAJOR", versionParts[0]);
        setConfig("MINOR", versionParts[1]);
        setConfig("TEENY", versionParts[2]);

        setConfig("bindir",
                  new File(System.getProperty("jruby.home") + File.separator + "bin").getAbsolutePath());
        setConfig("RUBY_INSTALL_NAME", System.getProperty("jruby.script"));
    }

    private void setConfig(String key, String value) {
        Ruby runtime = configHash.getRuntime();
        configHash.aset(RubyString.newString(runtime, key),
                        RubyString.newString(runtime, value));
    }
}
