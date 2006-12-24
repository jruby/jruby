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
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
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
 * the name in question. If a .rb or .rb.ast.ser extension is specified, JRuby will only try
 * those extensions when searching. If a .so, .o, .dll, or .jar extension is specified, JRuby
 * will only try .so or .jar when searching. Otherwise, JRuby goes through the known suffixes
 * (.rb, .rb.ast.ser, .so, and .jar) and tries to find a library with this name. The process for finding a library follows this order
 * for all searchable extensions:
 * <ol>
 * <li>First, check if the name starts with 'jar:', then the path points to a jar-file resource which is returned.</li>
 * <li>Second, try searching for the file in the current dir</li>
 * <li>Then JRuby looks through the load path trying these variants:
 *   <ol>
 *     <li>See if the current load path entry starts with 'jar:', if so check if this jar-file contains the name</li>
 *     <li>Otherwise JRuby tries to construct a path by combining the entry and the current working directy, and then see if 
 *         a file with the correct name can be reached from this point.</li>
 *   </ol>
 * </li>
 * <li>If all these fail, try to load the name as a resource from classloader resources, using the bare name as
 *     well as the load path entries</li>
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

    private static final String[] sourceSuffixes = { ".rb", ".rb.ast.ser" };
    private static final String[] extensionSuffixes = { ".so", ".jar" };
    private static final String[] allSuffixes = { ".rb", ".rb.ast.ser", ".so", ".jar" };
    private static final Pattern sourcePattern = Pattern.compile("^(.*)\\.(rb|rb\\.ast\\.ser)$");
    private static final Pattern extensionPattern = Pattern.compile("^(.*)\\.(so|o|dll|jar)$");
    private static final Pattern allPattern = Pattern.compile("^(.*)\\.(rb|rb\\.ast\\.ser|so|o|dll|jar)$");

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
        // add all startup load paths to the list first
        for (Iterator iter = additionalDirectories.iterator(); iter.hasNext();) {
            addPath((String) iter.next());
        }

        // wrap in try/catch for security exceptions in an applet
        try {
          String jrubyHome = System.getProperty("jruby.home");
          if (jrubyHome != null) {
              char sep = '/';
              String rubyDir = jrubyHome + sep + "lib" + sep + "ruby" + sep;

              addPath(rubyDir + "site_ruby" + sep + Constants.RUBY_MAJOR_VERSION);
              addPath(rubyDir + "site_ruby");
              addPath(rubyDir + Constants.RUBY_MAJOR_VERSION);
              addPath(rubyDir + Constants.RUBY_MAJOR_VERSION + sep + "java");

              // Added to make sure we find default distribution files within jar file.
              // TODO: Either make jrubyHome become the jar file or allow "classpath-only" paths
              addPath("lib" + sep + "ruby" + sep + Constants.RUBY_MAJOR_VERSION);
          }
        } catch (AccessControlException accessEx) {
          // ignore, we're in an applet and can't access filesystem anyway
        }
        
        // "." dir is used for relative path loads from a given file, as in require '../foo/bar'
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
            library = findLibraryWithClassloaders(file);
            if (library == null) {
                throw runtime.newLoadError("No such file to load -- " + file);
            }
        }
        try {
            library.load(runtime);
        } catch (IOException e) {
            throw runtime.newLoadError("IO error -- " + file);
        }
    }

    public boolean smartLoad(String file) {
        Library library = null;
        String loadName = file;
        String[] extensionsToSearch = null;
        
        // if an extension is specified, try more targetted searches
        if (file.lastIndexOf('.') > file.lastIndexOf('/')) {
            Matcher matcher = null;
            if ((matcher = sourcePattern.matcher(file)).matches()) {
                // source extensions
                extensionsToSearch = sourceSuffixes;
                
                // trim extension to try other options
                file = matcher.group(1);
            } else if ((matcher = extensionPattern.matcher(file)).matches()) {
                // extension extensions
                extensionsToSearch = extensionSuffixes;
                
                // trim extension to try other options
                file = matcher.group(1);
            } else {
                // unknown extension
                throw runtime.newLoadError("no such file to load -- " + file);
            }
        } else {
            // try all extensions
            extensionsToSearch = allSuffixes;
        }
        
        // First try suffixes with normal loading
        for (int i = 0; i < extensionsToSearch.length; i++) {
            library = findLibrary(file + extensionsToSearch[i]);
            if (library != null) {
                loadName = file + extensionsToSearch[i];
                break;
            }
        }

        // Then try suffixes with classloader loading
        if (library == null) {
            for (int i = 0; i < extensionsToSearch.length; i++) {
                library = findLibraryWithClassloaders(file + extensionsToSearch[i]);
                if (library != null) {
                    loadName = file + extensionsToSearch[i];
                    break;
                }
            }
        }

        library = tryLoadExtension(library,file);

        // no library or extension found, bail out
        if (library == null) {
            throw runtime.newLoadError("no such file to load -- " + file);
        }
        
        if (loadedFeaturesInternal.contains(loadName)) {
            return false;
        }
        
        // attempt to load the found library
        try {
            loadedFeaturesInternal.add(loadName);
            loadedFeatures.add(runtime.newString(loadName));

            library.load(runtime);
            return true;
        } catch (IOException e) {
            loadedFeaturesInternal.remove(loadName);
            loadedFeatures.remove(runtime.newString(loadName));

            throw runtime.newLoadError("IO error -- " + file);
        }
    }

    public boolean require(String file) {
        return smartLoad(file);
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

    public IAutoloadMethod autoloadFor(String name) {
        return (IAutoloadMethod)autoloadMap.get(name);
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

	public void registerRubyBuiltin(String libraryName) {
		registerBuiltin(libraryName + JRUBY_BUILTIN_SUFFIX, new BuiltinScript(libraryName));
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

    private Library findLibraryWithClassloaders(String file) {
        LoadServiceResource resource = findFileInClasspath(file);
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

    /**
     * this method uses the appropriate lookup strategy to find a file.
     * It is used by Kernel#require.
     *
     * @mri rb_find_file
     * @param name the file to find, this is a path name
     * @return the correct file
     */
    private LoadServiceResource findFile(String name) {
        // if a jar URL, return load service resource directly without further searching
        if (name.startsWith("jar:")) {
            try {
                return new LoadServiceResource(new URL(name), name);
            } catch (MalformedURLException e) {
                throw runtime.newIOErrorFromException(e);
            }
        }

        // check current directory; if file exists, retrieve URL and return resource
        try {
            JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(),name);
            if(file.isFile() && file.isAbsolute()) {
                try {
                    return new LoadServiceResource(file.toURL(),name);
                } catch (MalformedURLException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }
        } catch (AccessControlException accessEx) {
            // ignore, applet security
        } catch (IllegalArgumentException illArgEx) {
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

            try {
                JRubyFile current = JRubyFile.create(JRubyFile.create(runtime.getCurrentDirectory(),entry).getAbsolutePath(), name);
                if (current.isFile()) {
                    try {
                        return new LoadServiceResource(current.toURL(), current.getPath());
                    } catch (MalformedURLException e) {
                        throw runtime.newIOErrorFromException(e);
                    }
                }
            } catch (AccessControlException accessEx) {
                // ignore, we're in an applet
            } catch (IllegalArgumentException illArgEx) {
                // ignore; Applet under windows has issues with current dir = "/"
            }
        }

        return null;
    }

    /**
     * this method uses the appropriate lookup strategy to find a file.
     * It is used by Kernel#require.
     *
     * @mri rb_find_file
     * @param name the file to find, this is a path name
     * @return the correct file
     */
    private LoadServiceResource findFileInClasspath(String name) {
        // Look in classpath next (we do not use File as a test since UNC names will match)
        // Note: Jar resources must NEVER begin with an '/'. (previous code said "always begin with a /")
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (Iterator pathIter = loadPath.iterator(); pathIter.hasNext();) {
            String entry = pathIter.next().toString();

            // if entry starts with a slash, skip it since classloader resources never start with a /
            if (entry.charAt(0) == '/') continue;
            
            // otherwise, try to load from classpath (Note: Jar resources always uses '/')
            URL loc = classLoader.getResource(entry + "/" + name);

            // Make sure this is not a directory or unavailable in some way
            if (isRequireable(loc)) {
                return new LoadServiceResource(loc, loc.getPath());
            }
        }

        // if name starts with a / we're done (classloader resources won't load with an initial /)
        if (name.charAt(0) == '/') return null;
        
        // Try to load from classpath without prefix. "A/b.rb" will not load as 
        // "./A/b.rb" in a jar file.
        URL loc = classLoader.getResource(name);

        return isRequireable(loc) ? new LoadServiceResource(loc, loc.getPath()) : null;
    }

    private Library tryLoadExtension(Library library, String file) {
        // This code exploits the fact that all .jar files will be found for the JarredScript feature.
        // This is where the basic extension mechanism gets fixed
        Library oldLibrary = library;
        
        if(library == null || library instanceof JarredScript) {
            // Create package name, by splitting on / and joining all but the last elements with a ".", and downcasing them.
            String[] all = file.split("/");
            StringBuffer finName = new StringBuffer();
            for(int i=0, j=(all.length-1); i<j; i++) {
                finName.append(all[i].toLowerCase()).append(".");
                
            }
            
            // Make the class name look nice, by splitting on _ and capitalize each segment, then joining
            // the, together without anything separating them, and last put on "Service" at the end.
            String[] last = all[all.length-1].split("_");
            for(int i=0, j=last.length; i<j; i++) {
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
}
