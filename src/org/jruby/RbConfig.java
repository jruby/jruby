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
package org.jruby;

import org.jruby.runtime.Constants;

import java.io.File;

public class RbConfig {

    public static void createRbConfig(Ruby runtime) {
        RubyModule configModule = runtime.defineModule("Config");
        RubyHash configHash = RubyHash.newHash(runtime);
        configModule.defineConstant("CONFIG", configHash);

        String[] versionParts = Constants.RUBY_VERSION.split("\\.");

        configHash.aset(RubyString.newString(runtime, "MAJOR"),
                        RubyString.newString(runtime, versionParts[0]));
        configHash.aset(RubyString.newString(runtime, "MINOR"),
                        RubyString.newString(runtime, versionParts[1]));
        configHash.aset(RubyString.newString(runtime, "TEENY"),
                        RubyString.newString(runtime, versionParts[2]));

        configHash.aset(RubyString.newString(runtime, "bindir"),
                        RubyString.newString(runtime, new File(System.getProperty("jruby.home") + File.separator + "bin").getAbsolutePath()));
        configHash.aset(RubyString.newString(runtime, "RUBY_INSTALL_NAME"),
                        RubyString.newString(runtime, System.getProperty("jruby.script")));
    }
}
