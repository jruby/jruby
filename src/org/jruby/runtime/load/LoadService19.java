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
 * Copyright (C) 2002-2010 JRuby Community
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

import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

import java.security.AccessControlException;
import org.jruby.RubyFile;

public class LoadService19 extends LoadService {
    private boolean canGetAbsolutePath = true;
    
    public LoadService19(Ruby runtime) {
        super(runtime);
    }

    @Override
    protected String resolveLoadName(LoadServiceResource foundResource, String previousPath) {
        if (canGetAbsolutePath) {
            try {
                String path = foundResource.getAbsolutePath();
                if (Platform.IS_WINDOWS) {
                    path = path.replace('\\', '/');
                }
                return path;
            } catch (AccessControlException ace) {
                // can't get absolute path in this security context, so we give up forever
                runtime.getWarnings().warn("can't canonicalize loaded names due to security restrictions; disabling");
                canGetAbsolutePath = false;
            }
        }
        return super.resolveLoadName(foundResource, previousPath);
    }

    @Override
    protected String getFileName(JRubyFile file, String nameWithSuffix) {
        return file.getAbsolutePath();
    }

    @Override
    protected String getLoadPathEntry(IRubyObject entry) {
        return RubyFile.get_path(entry.getRuntime().getCurrentContext(), entry).asJavaString();
    }
}

