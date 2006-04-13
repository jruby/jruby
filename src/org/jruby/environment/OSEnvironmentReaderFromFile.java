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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Map;

import org.jruby.IRuby;


/**
 *   Get the os environment from a file.
 *   Requires a java system property "jruby.envfile" to give the location of a file from which 
 *   the environment variables can be loaded.  
 *
 */
class OSEnvironmentReaderFromFile implements IOSEnvironmentReader {

    private static final String JRUBY_ENVFILE = "jruby.envfile";

    private final OSEnvironment environmentReader = new OSEnvironment();
    
    
    /* (non-Javadoc)
     * @see org.jruby.IOSEnvironment#isAccessible()
     */
    public boolean isAccessible(IRuby runtime) {
        String jrubyEnvFilename = System.getProperty(JRUBY_ENVFILE);

        if (jrubyEnvFilename == null || jrubyEnvFilename.length() < 1) {
            return false;
        }
        
        File jrubyEnvFile = new File(jrubyEnvFilename);
        
        return (jrubyEnvFile.exists() && !jrubyEnvFile.isDirectory() && jrubyEnvFile.canRead());
    }
    
    /* (non-Javadoc)
     * @see org.jruby.IOSEnvironment#getVariables()
     */
    public Map getVariables(IRuby runtime) {
        String jrubyEnvFilename = System.getProperty(JRUBY_ENVFILE);

        Map envs = null;
        
        if (jrubyEnvFilename == null || jrubyEnvFilename.length() < 1) {
            environmentReader.handleException(new OSEnvironmentReaderExcepton("Property " + JRUBY_ENVFILE + " not defined."));
        } else {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(jrubyEnvFilename));
            } catch (FileNotFoundException e) {
                envs = null;
                // Very unlikely to happen as isAccessible() should be used first
                environmentReader.handleException(e);
            }
            envs = environmentReader.getVariablesFrom(reader);
        }
        return envs;
    }
}
