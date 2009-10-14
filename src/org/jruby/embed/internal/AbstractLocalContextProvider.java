/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.internal;

import java.io.File;
import java.util.Arrays;
import org.jruby.embed.LocalContextProvider;
import java.util.List;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PropertyName;
import org.jruby.util.ClassCache;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public abstract class AbstractLocalContextProvider implements LocalContextProvider {
    protected List loadPaths = null;
    protected ClassCache classCache = null;
    protected RubyInstanceConfig config = new RubyInstanceConfig();
    protected LocalVariableBehavior behavior = LocalVariableBehavior.TRANSIENT;

    public void setLoadPaths(List loadPaths) {
        this.loadPaths = loadPaths;
    }

    public void setClassCache(ClassCache classCache) {
        this.classCache = classCache;
    }

    public RubyInstanceConfig getRubyInstanceConfig() {
        return config;
    }

    protected LocalContext getInstance() {
        if (loadPaths == null) {
            String paths = System.getProperty(PropertyName.CLASSPATH.toString());
            if (paths == null) {
                paths = System.getProperty("java.class.path");
            }
            loadPaths = Arrays.asList(paths.split(File.pathSeparator));
        }
        if (config == null) {
            config = new RubyInstanceConfig();
            config.setCompileMode(CompileMode.OFF);
        }
        if (classCache != null) {
            config.setClassCache(classCache);
        }
        return new LocalContext(loadPaths, config, behavior);
    }
}
