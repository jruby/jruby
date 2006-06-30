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
import org.jruby.RubyString;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.BuiltinScript;
import org.jruby.util.PreparsedScript;
import org.jruby.util.NormalizedFile;

/**
 *
 * @author jpetersen
 */
public class LoadService {
    private static final String JRUBY_BUILTIN_SUFFIX = ".jrb";

	private static final String[] suffixes = { JRUBY_BUILTIN_SUFFIX, ".ast.ser", ".rb.ast.ser", ".rb", ".jar" };
    private static final Pattern suffixPattern = Pattern.compile(".*\\.(jrb|ast\\.ser|rb\\.ast\\.ser|rb|jar)$");

    private final List loadPath = new ArrayList();
    private final Set loadedFeatures = Collections.synchronizedSet(new HashSet());
    private final Map builtinLibraries = new HashMap();

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

        if (suffixPattern.matcher(file).matches()) {
            // known extension specified specified, try without suffixes
            library = findLibrary(file);
        }

        if (library == null) {
            // nothing yet, try suffixes
            for (int i = 0; i < suffixes.length; i++) {
                library = findLibrary(file + suffixes[i]);
                if (library != null) {
                    break;
                }
            }
        }   

        if (library == null) {
            throw runtime.newLoadError("No such file to load -- " + file);
        }
        try {
            library.load(runtime);
        } catch (IOException e) {
            throw runtime.newLoadError("IO error -- " + file);
        }
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
        RubyString name = runtime.newString(file);
        if (loadedFeatures.contains(name)) {
            return false;
        }
        
        loadedFeatures.add(name);
        
        try {
	        smartLoad(file);
	        return true;
        } catch (RuntimeException e) {
            loadedFeatures.remove(name);
            throw e;
        }
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
        try {
            if (name.startsWith("jar:")) {
                return new LoadServiceResource(new URL(name), name);
            }

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader(); 

            // FIXME: We may only want to do one of the following two lookups; however, Ruby is obviously not
            // thread safe with a global CWD, and I don't know if we'll get around that
            NormalizedFile file = new NormalizedFile(name);
            // Name matches an absolute path that exists.
            if (file.isAbsolute() && file.exists() && file.isFile()) {
                return new LoadServiceResource(file.toURL(), name);
            }
            
            // Try with CWD from Ruby
            String cwd = runtime.getCurrentDirectory();
            file = (NormalizedFile)new NormalizedFile(cwd, name).getAbsoluteFile();
            if (file.isAbsolute() && file.exists() && file.isFile()) {
                return new LoadServiceResource(file.toURL(), name);
            }
            
            // Look in classpath next (we do not use File as a test since UNC names will match)
            // Note: Jar resources must always begin with an '/'.
            if (name.startsWith("/") || name.startsWith("\\")) {
                URL loc = classLoader.getResource(name.replace('\\', '/'));

                // Make sure this is not a directory or unavailable in some way
                if (isRequireable(loc)) {
                	return new LoadServiceResource(loc, loc.getPath());
                }
            }
            
            for (Iterator pathIter = loadPath.iterator(); pathIter.hasNext();) {
                String entry = pathIter.next().toString();
                if (entry.startsWith("jar:")) {
                    try {
                        JarFile current = new JarFile(entry.substring(4));
                        if (current.getJarEntry(name) != null) {
                            return new LoadServiceResource(new URL(entry + name), entry + name);
                        }
                    } catch (FileNotFoundException ignored) {
                    } catch (IOException e) {
                        throw runtime.newIOErrorFromException(e);
                    }
                } 

               	// Load from local filesystem
                NormalizedFile current = (NormalizedFile)new NormalizedFile(entry, name).getAbsoluteFile();
                if (current.exists() && current.isFile()) {
                	return new LoadServiceResource(current.toURL(), new NormalizedFile(entry, name).getPath());
                }
                
                // otherwise, try to load from classpath (Note: Jar resources always uses '/')
                URL loc = classLoader.getResource(entry.replace('\\', '/') + "/" + name.replace('\\', '/'));

                // Make sure this is not a directory or unavailable in some way
                if (isRequireable(loc)) {
                	return new LoadServiceResource(loc, loc.getPath());
                }
            }

            // Try to load from classpath without prefix. "A/b.rb" will not load as 
            // "./A/b.rb" in a jar file. (Note: Jar resources always uses '/')
            URL loc = classLoader.getResource(name.replace('\\', '/'));

            return isRequireable(loc) ? new LoadServiceResource(loc, loc.getPath()) : null;
        } catch (MalformedURLException e) {
            throw runtime.newIOErrorFromException(e);
        }
    }
    
    /* Directories and unavailable resources are not able to open a stream. */
    private boolean isRequireable(URL loc) {
        if (loc != null) {
        	if (loc.getProtocol().equals("file") && new NormalizedFile(loc.getFile()).isDirectory()) {
        		return false;
        	}
        	
        	try {
            	loc.openStream().close();
            	return true;
            } catch (Exception e) {}
        }
        return false;
    }

	public void registerRubyBuiltin(String libraryName) {
		registerBuiltin(libraryName + JRUBY_BUILTIN_SUFFIX, new BuiltinScript(libraryName));
	}
}
