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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.runtime.load;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.jruby.IRuby;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.BuiltinScript;
import org.jruby.util.JRubyFile;
import org.jruby.util.PreparsedScript;

/**
 * <b>How require works in JRuby</b>
 * When requiring a name from Ruby, JRuby will first remove any file extension it knows about,
 * thereby making it possible to use this string to see if JRuby has already loaded
 * the name in question. Otherwise, JRuby goes through the known suffixes and tries to find
 * a library with this name. The process for finding a library follows this order:
 * <ol>
 * <li>First, check if the name starts with 'jar:', then the path points to a jar-file which is returned.</li>
 * <li>Second, try and see if the name is an absolute pathname to a file and return this.</li>
 * <li>Otherwise, check if the name starts with '\' or '/', if so see if it can be found through the class-loader.</li>
 * <li>Then JRuby looks through the load path trying these variants:
 *   <ol>
 *     <li>See if the current load path entry starts with 'jar:', if so check if this jar-file contains the name</li>
 *     <li>Otherwise JRuby tries to construct a path by combining the entry and the current working directy, and then see if 
 *         a file with the correct name can be reached from this point.</li>
 *     <li>Last, JRuby tries to construct a path beginning with '/', load path entry, and then name, and see if this works</li>
 *   </ol>
 * </li>
 * <li>If all else fails, try to load the name as a resource without adding '/' at the front.</li>
 * <li>When we get to this state, the normal JRuby loading has failed. At this stage JRuby tries to load 
 *     Java native extensions, by following this process:
 *   <ol>
 *     <li>First it checks that we haven't already found a library. If we found a library of type JarredScript, the method continues.</li>
 *     <li>The first step is translating the name given into a valid Java Extension class name. First it splits the string into 
 *     each path segment, and then makes all but the last downcased. After this it takes the last entry, removes all underscores
 *     and capitalizes each part separated by underscores. It then joins everything together and tacks on a 'Service' at the end.
 *     Lastly, it removes all leading dots, to make it a valid Java FWCN.</li>
 *     <li>If the previous library was of type JarredScript, we try to add the jar-file to the classpath</li>
 *     <li>Now JRuby tries to instantiate the class with the name constructed. If this works, we return a ClassExtensionLibrary. Otherwise,
 *     the old library is put back in place, if there was one.
 *   </ol>
 * </li>
 * <li>When all separate methods have been tried and there was no result, a LoadError will be raised.</li>
 * <li>Otherwise, the name will be added to the loaded features, and the library loaded</li>
 * </ol>
 *
 * <b>How to make a class that can get required by JRuby</b>
 * <p>First, decide on what name should be used to require the extension.
 * In this purely hypothetical example, this name will be 'active_record/connection_adapters/jdbc_adapter'.
 * Then create the class name for this require-name, by looking at the guidelines above. Our class should
 * be named active_record.connection_adapters.JdbcAdapterService, and implement one of the library-interfaces.
 * The easiest one is BasicLibraryService, where you define the basicLoad-method, which will get called
 * when your library should be loaded.</p>
 * <p>The next step is to either put your compiled class on JRuby's classpath, or package the class/es inside a
 * jar-file. To package into a jar-file, we first create the file, then rename it to jdbc_adapter.jar. Then 
 * we put this jar-file in the directory active_record/connection_adapters somewhere in JRuby's load path. For
 * example, copying jdbc_adapter.jar into JRUBY_HOME/lib/ruby/site_ruby/1.8/active_record/connection_adapters
 * will make everything work. If you've packaged your extension inside a RubyGem, write a setub.rb-script that 
 * copies the jar-file to this place.</p>
 * <p>If you don't want to have the name of your extension-class to be prescribed, you can also put a file called
 * jruby-ext.properties in your jar-files META-INF directory, where you can use the key <full-extension-name>.impl
 * to make the extension library load the correct class. An example for the above would have a jruby-ext.properties
 * that contained a ruby like: "active_record/connection_adapters/jdbc_adapter=org.jruby.ar.JdbcAdapter". (NOTE: THIS
 * FEATURE IS NOT IMPLEMENTED YET.)</p>
 *
 * @author jpetersen
 */
public class LoadService {
    private static final String JRUBY_BUILTIN_SUFFIX = ".rb";

    private static final String[] suffixes = { ".rb", ".ast.ser", ".rb.ast.ser", ".jar" };
    private static final Pattern suffixPattern = Pattern.compile("^(.*)\\.(jrb|ast\\.ser|rb\\.ast\\.ser|rb|jar)$");

    private final List loadPath = new ArrayList();
    private final Set loadedFeatures = Collections.synchronizedSet(new HashSet());
    private final Set loadedFeaturesInternal = Collections.synchronizedSet(new HashSet());
    private final Map builtinLibraries = new HashMap();

    private final Map jarFiles = new HashMap();

    private final Map autoloadMap = new HashMap();

    private final IRuby runtime;
    
    public LoadService(IRuby runtime) {
        this.runtime = runtime;
    }

    public void init(List additionalDirectories) {
      for (Iterator iter = additionalDirectories.iterator(); iter.hasNext();) {
        addPath((String) iter.next());
      }
      if (runtime.getSafeLevel() == 0) {
        String jrubyLib = System.getProperty("jruby.lib");
        if (jrubyLib != null) {
          addPath(jrubyLib);
        }
      }
      
      String jrubyHome = System.getProperty("jruby.home");
      if (jrubyHome != null) {
        char sep = '/';
        String rubyDir = jrubyHome + sep + "lib" + sep + "ruby" + sep;
        
        addPath(rubyDir + "site_ruby" + sep + Constants.RUBY_MAJOR_VERSION);
        addPath(rubyDir + "site_ruby" + sep + Constants.RUBY_MAJOR_VERSION + sep + "java");
        addPath(rubyDir + "site_ruby");
        addPath(rubyDir + Constants.RUBY_MAJOR_VERSION);
        addPath(rubyDir + Constants.RUBY_MAJOR_VERSION + sep + "java");
        
        // Added to make sure we find default distribution files within jar file.
        // TODO: Either make jrubyHome become the jar file or allow "classpath-only" paths
        addPath("lib" + sep + "ruby" + sep + Constants.RUBY_MAJOR_VERSION);
      }
      
      if (runtime.getSafeLevel() == 0) {
        addPath(".");
      }
    }

    private void addPath(String path) {
        loadPath.add(runtime.newString(path.replace('\\', '/')));
    }

    public void load(String file) {
        Library library = null;
        
        library = findLibrary(file);

        if (library == null) {
            throw runtime.newLoadError("No such file to load -- " + file);
        }
        try {
            library.load(runtime);
        } catch (IOException e) {
            throw runtime.newLoadError("IO error -- " + file);
        }
    }

    public void smartLoad(String file) {
        Library library = null;
        String loadName = file;
        // This isn't needed, since the require call strips known extensions.
        /*        if (suffixPattern.matcher(file).matches()) {
            // known extension specified specified, try without suffixes
            library = findLibrary(file);
        }
        */

        //        if (library == null) {
            // nothing yet, try suffixes
            for (int i = 0; i < suffixes.length; i++) {
                library = findLibrary(file + suffixes[i]);
                if (library != null) {
                    loadName = file + suffixes[i];
                    break;
                }
            }
            //        }   

        library = tryLoadExtension(library,file);

        if (library == null) {
            throw runtime.newLoadError("No such file to load -- " + file);
        }
        try {
            loadedFeaturesInternal.add(file);
            loadedFeatures.add(runtime.newString(loadName));
            library.load(runtime);
        } catch (IOException e) {
            loadedFeaturesInternal.remove(file);
            loadedFeatures.remove(runtime.newString(loadName));
            throw runtime.newLoadError("IO error -- " + file);
        }
    }

    private Library tryLoadExtension(Library library, String file) {
        // This code exploits the fact that all .jar files will be found for the JarredScript feature.
        // This is where the basic extension mechanism gets fixed
        Library oldLibrary = library;
        if(library == null || library instanceof JarredScript) {
            // Create package name, by splitting on / and joining all but the last elements with a ".", and downcasing them.
            String[] all = file.split("/");
            StringBuffer finName = new StringBuffer();
            for(int i=0,j=(all.length-1);i<j;i++) {
                finName.append(all[i].toLowerCase()).append(".");
                
            }
            // Make the class name look nice, by splitting on _ and capitalize each segment, then joining
            // the, together without anything separating them, and last put on "Service" at the end.
            String[] last = all[all.length-1].split("_");
            for(int i=0,j=last.length;i<j;i++) {
                finName.append(Character.toUpperCase(last[i].charAt(0))).append(last[i].substring(1));
            }
            finName.append("Service");

            // We don't want a package name beginning with dots, so we remove them
            String className = finName.toString().replaceAll("^\\.*","");

            // If there is a jar-file with the required name, we add this to the class path.
            if(library instanceof JarredScript) {
                // It's _really_ expensive to check that the class actually exists in the Jar, so
                // we don't do that now.
                runtime.getJavaSupport().addToClasspath(((JarredScript)library).getResource().getURL());
            }

            try {
                Class theClass = runtime.getJavaSupport().loadJavaClass(className);
                library = new ClassExtensionLibrary(theClass);
            } catch(Exception ee) {
                library = null;
            }
        }
        // If there was a good library before, we go back to that
        if(library == null && oldLibrary != null) {
            library = oldLibrary;
        }
        return library;
    }


    private Library findLibrary(String file) {
        if (builtinLibraries.containsKey(file)) {
            return (Library) builtinLibraries.get(file);
        }
        LoadServiceResource resource = findFile(file);
        if (resource == null) {
            return null;
        }

        if (file.endsWith(".jar")) {
            return new JarredScript(resource);
        } else if (file.endsWith(".rb.ast.ser")) {
        	return new PreparsedScript(resource);
        } else {
            return new ExternalScript(resource, file);
        }
    }

    public boolean require(String file) {
        String filestr = suffixPattern.matcher(file).replaceAll("$1");
        if (loadedFeaturesInternal.contains(filestr)) {
            return false;
        }
        smartLoad(filestr);
        return true;
    }

    public List getLoadPath() {
        return loadPath;
    }

    public Set getLoadedFeatures() {
        return loadedFeatures;
    }

    public boolean isAutoloadDefined(String name) {
        return autoloadMap.containsKey(name);
    }

    public IRubyObject autoload(String name) {
        IAutoloadMethod loadMethod = (IAutoloadMethod)autoloadMap.remove(name);
        if (loadMethod != null) {
            return loadMethod.load(runtime, name);
        }
        return null;
    }

    public void addAutoload(String name, IAutoloadMethod loadMethod) {
        autoloadMap.put(name, loadMethod);
    }

    public void registerBuiltin(String name, Library library) {
        builtinLibraries.put(name, library);
    }

    /**
     * this method uses the appropriate lookup strategy to find a file.
     * It is used by Kernel#require.
     *
     * @mri rb_find_file
     * @param name the file to find, this is a path name
     * @return the correct file
     */
    private LoadServiceResource findFile(String name) {
        //        try {
            if (name.startsWith("jar:")) {
                try {
                    return new LoadServiceResource(new URL(name), name);
                } catch (MalformedURLException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }

            //            ClassLoader classLoader = Thread.currentThread().getContextClassLoader(); 

            JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(),name);
            if(file.isFile() && file.isAbsolute()) {
                try {
                    return new LoadServiceResource(file.toURL(),name);
                } catch (MalformedURLException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }
                        
            ClassLoader classLoader = runtime.getJavaSupport().getJavaClassLoader();
            String xname = name.replace('\\', '/');
            
            // Look in classpath next (we do not use File as a test since UNC names will match)
            // Note: Jar resources must always begin with an '/'.
            if (xname.charAt(0) == '/') {
                URL loc = classLoader.getResource(xname);

                // Make sure this is not a directory or unavailable in some way
                if (isRequireable(loc)) {
                	return new LoadServiceResource(loc, loc.getPath());
                }
            }
            
            for (Iterator pathIter = loadPath.iterator(); pathIter.hasNext();) {
                String entry = pathIter.next().toString();
                if (entry.startsWith("jar:")) {
                    JarFile current = (JarFile)jarFiles.get(entry);
                    if(null == current) {
                        try {
                            current = new JarFile(entry.substring(4));
                            jarFiles.put(entry,current);
                        } catch (FileNotFoundException ignored) {
                        } catch (IOException e) {
                            throw runtime.newIOErrorFromException(e);
                        }
                    }
                    if (current.getJarEntry(name) != null) {
                        try {
                            return new LoadServiceResource(new URL(entry + name), entry + name);
                        } catch (MalformedURLException e) {
                            throw runtime.newIOErrorFromException(e);
                        }
                    }
                } 

                //               	// Load from local filesystem
                //                NormalizedFile current = (NormalizedFile)new NormalizedFile(entry, name).getAbsoluteFile();
                JRubyFile current = JRubyFile.create(JRubyFile.create(runtime.getCurrentDirectory(),entry).getAbsolutePath(), name);
                if (current.isFile()) {
                    try {
                        return new LoadServiceResource(current.toURL(), current.getPath());
                    } catch (MalformedURLException e) {
                        throw runtime.newIOErrorFromException(e);
                    }
                }
                
                // otherwise, try to load from classpath (Note: Jar resources always uses '/')
                URL loc = classLoader.getResource(entry.replace('\\', '/') + "/" + xname);

                // Make sure this is not a directory or unavailable in some way
                if (isRequireable(loc)) {
                	return new LoadServiceResource(loc, loc.getPath());
                }
            }

            // Try to load from classpath without prefix. "A/b.rb" will not load as 
            // "./A/b.rb" in a jar file. (Note: Jar resources always uses '/')
            URL loc = classLoader.getResource(xname);

            return isRequireable(loc) ? new LoadServiceResource(loc, loc.getPath()) : null;
            //        } catch (MalformedURLException e) {
            //            throw runtime.newIOErrorFromException(e);
            //        }
    }
    
    /* Directories and unavailable resources are not able to open a stream. */
    private boolean isRequireable(URL loc) {
        if (loc != null) {
        	if (loc.getProtocol().equals("file") && new java.io.File(loc.getFile()).isDirectory()) {
        		return false;
        	}
        	
        	try {
                    loc.openConnection();
                    return true;
            } catch (Exception e) {}
        }
        return false;
    }

	public void registerRubyBuiltin(String libraryName) {
		registerBuiltin(libraryName + JRUBY_BUILTIN_SUFFIX, new BuiltinScript(libraryName));
	}
}
