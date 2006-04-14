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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jruby.IRuby;

public class OSEnvironment {


    /**
     * Handles exceptions from implementors of {@link IOSEnvironmentReader}, 
     * converting the exception to a {@link OSEnvironmentReaderExcepton}
     * @param e
     */
    void handleException(Exception e) {
        throw (OSEnvironmentReaderExcepton)
        	new OSEnvironmentReaderExcepton().initCause(e);
    }
    
    /**
    * Returns the OS environment variables as a Map<RubyString,RubyString>. 
    * If the Java system  property "jruby.env.method" is set then 
    *   the value is used as the classname of a class than implements the IOSEnvironmentReader 
    *   interface and the environment is obtained via this class. 
    * If the "jruby.env.method" is "org.jruby.environment.OSEnvironmentReaderFromFile" then
    *   the java system property "jruby.envfile" should give the location of a file from which 
    *   the environment variables can be loaded.  
    * Otherwise, other default implementations of  IOSEnvironmentReader are tried 
    * to obtain the os environment variables.
    * @param runtime
    * @param System.getProperty("jruby.env.method") 	  
    * @throws OSEnvironmentReaderExcepton
    */  
    public Map getEnvironmentVariableMap(IRuby runtime) {
        String jrubyEnvMethod = System.getProperty("jruby.env.method");

        IOSEnvironmentReader reader;
        
        if (jrubyEnvMethod == null || jrubyEnvMethod.length() < 1) {
            // Try to get environment from Java5 System.getenv()
            reader = getAccessibleOSEnvironment(runtime, OSEnvironmentReaderFromJava5SystemGetenv.class.getName());
            // not Java5 so try getting environment using Runtime exec
            if (reader == null) {
                reader = getAccessibleOSEnvironment(runtime, OSEnvironmentReaderFromRuntimeExec.class.getName());
                //runtime.getWarnings().warn("Getting environment variables using Runtime Exec");
            }  else {
                //runtime.getWarnings().warn("Getting environment variables using Java5 System.getenv()");
            }
        } else {
            // get environment from jruby command line property supplied class
            runtime.getWarnings().warn("Getting environment variables using command line defined method: " + jrubyEnvMethod);
            reader = getAccessibleOSEnvironment(runtime, jrubyEnvMethod);            
        }

    	Map envs = null;

        if (reader != null) {
        	Map variables = null;
        	variables = reader.getVariables(runtime);
			envs = getAsMapOfRubyStrings(runtime,  variables.entrySet());
        }
        
        return envs;
    	
    }

    /**
    * Returns java system properties as a Map<RubyString,RubyString>.
     * @param runtime
     * @return the java system properties as a Map<RubyString,RubyString>.
     */
    public Map getSystemPropertiesMap(IRuby runtime) {
        return getAsMapOfRubyStrings(runtime, System.getProperties().entrySet());
    }
    
    
    private static IOSEnvironmentReader getAccessibleOSEnvironment(IRuby runtime, String classname) {
        IOSEnvironmentReader osenvironment = null;
        try {
            osenvironment = (IOSEnvironmentReader)Class.forName(classname).newInstance();
        } catch (Exception e) {
        	// This should only happen for a command line supplied IOSEnvironmentReader implementation  
            runtime.getWarnings().warn(e.getMessage());
        }
        
        if (osenvironment != null & osenvironment.isAccessible(runtime)) {
            return osenvironment;
        }
        return null;
    }

    
    
	private static Map getAsMapOfRubyStrings(IRuby runtime, Set entrySet) {
		Map envs = new HashMap();
		for (Iterator iter = entrySet.iterator(); iter.hasNext();) {
			Map.Entry entry  = (Map.Entry) iter.next();
            envs.put(runtime.newString((String)entry.getKey()),runtime.newString((String)entry.getValue()));
		}
		return envs;
	}
    

	/**
     * Returns a Map of the variables found in the reader of form var=value
	 * @param reader
	 * @return Map<String,String> of variables found by reading lines from reader.
	 */
	Map getVariablesFrom(BufferedReader reader) {
        Map envs = new HashMap();
		try {
    		String line, envVarName, envVarValue;
    		int equalsPos;
    		while ((line = reader.readLine()) != null) {
    			equalsPos = line.indexOf('=');
    			if (equalsPos >= 1) {
    				envVarName = line.substring(0, equalsPos);
    				envVarValue = line.substring(equalsPos + 1);
    				envs.put(envVarName, envVarValue);
    			}
    		}
    	} catch (IOException e) {
    		envs = null;
    		handleException(e);
    	} finally {
    		try {
    			reader.close();
    		} catch (IOException e) {
    			envs = null;
    			handleException(e);
    		}
    	}
		return envs;
	}
	
	
}
