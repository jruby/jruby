/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2005 Thomas E. Enebo <enebo@acm.org>
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
package org.jruby.runtime.load;

import java.io.IOException;
import java.io.InputStream;
import org.jruby.Ruby;

import static org.jruby.util.JRubyFile.normalizeSeps;
import static org.jruby.RubyFile.canonicalize;

public class ExternalScript implements Library {
    private final LoadServiceResource resource;
    
    public ExternalScript(LoadServiceResource resource, String name) {
        this.resource = resource;
    }

    public void load(Ruby runtime, boolean wrap) {
        InputStream in = null;
        try {
            in = resource.getInputStream();
            String name = normalizeSeps(resource.getName());

            if (runtime.getInstanceConfig().getCompileMode().shouldPrecompileAll()) {
                runtime.compileAndLoadFile(name, in, wrap);
            } else {
                java.io.File path = resource.getPath();

                if(path != null && !resource.isAbsolute()) {
                    // Note: We use RubyFile's canonicalize rather than Java's,
                    // because Java's will follow symlinks and result in __FILE__
                    // being set to the target of the symlink rather than the
                    // filename provided.
                    name = normalizeSeps(canonicalize(path.getPath()));
                }

                runtime.loadFile(name, new LoadServiceResourceInputStream(in), wrap);
            }


        } catch (IOException e) {
            throw runtime.newIOErrorFromException(e);
        } finally {
            try { in.close(); } catch (Exception ex) {}
        }
    }

    @Override
    public String toString() {
        return "ExternalScript: " + resource.getName();
    }
}
