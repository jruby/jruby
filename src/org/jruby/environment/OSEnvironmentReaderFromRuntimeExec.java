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
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
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

package org.jruby.environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.jruby.IRuby;

class OSEnvironmentReaderFromRuntimeExec implements IOSEnvironmentReader {

    private final OSEnvironment environmentReader = new OSEnvironment();

    /* (non-Javadoc)
     * @see org.jruby.IOSEnvironment#isAccessible()
     */
    public boolean isAccessible(IRuby runtime) {
    	return true;
    }
    
    
    private String getEnvCommand() {
		String command;
		String osname = System.getProperty("os.name").toLowerCase();

    	// TODO merge this logic into RubyKernel.runInShell? / possible conflict if jruby.shell System property is set

		if (osname.indexOf("windows 9") > -1) {
			command = "command.com /c set";
		} else if ((osname.indexOf("nt") > -1)
				|| (osname.indexOf("windows 20") > -1)
				|| (osname.indexOf("windows xp") > -1)) {
			command = "cmd.exe /c set";
		} else {
			// Assume some unix variant with an env command
			command = "env";
		}
		return command;
	}
    
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see org.jruby.IOSEnvironment#getVariables()
	 */
    public Map getVariables(IRuby runtime) {
        try {
            Process process = Runtime.getRuntime().exec(getEnvCommand());
            return environmentReader.getVariablesFrom(
                    new BufferedReader(new InputStreamReader(process.getInputStream()))
                    );
		} catch (IOException e) {
            environmentReader.handleException(e);
        }
        
        return null;
    }

}
