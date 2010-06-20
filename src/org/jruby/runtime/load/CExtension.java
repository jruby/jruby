/*
 * Copyright (C) 2010 Tim Felgentreff
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.runtime.load;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.cext.ModuleLoader;

/**
 * This class wraps the {@link ModuleLoader} for loading c-extensions 
 * in JRuby.
 * TODO: Support loading from non-filesystem resources such as Jar files.
 */
public class CExtension implements Library {
    private LoadServiceResource resource;

    public CExtension(LoadServiceResource resource) {
        this.resource = resource;
    }

    public void load(Ruby runtime, boolean wrap) throws IOException {
        String file = resource.getAbsolutePath();
        new ModuleLoader().load(runtime, file);
    }

}
