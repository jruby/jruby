/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.internal.runtime.load;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.IOError;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.ILoadService;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.load.ExternalScript;
import org.jruby.runtime.load.JarredScript;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class LoadService implements ILoadService {
    private ArrayList loadPath = new ArrayList();
    private ArrayList loadedFeatures = new ArrayList();
    private Map builtinLibraries = new HashMap();

    private Map autoloadMap = new HashMap();

    private final Ruby runtime;
    /**
     * Constructor for LoadService.
     */
    public LoadService(Ruby runtime) {
        super();
        this.runtime = runtime;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#init(Ruby, List)
     */
    public void init(Ruby runtime, List additionalDirectories) {
        for (Iterator iter = additionalDirectories.iterator(); iter.hasNext();) {
            addPath((String)iter.next());
        }
        if (runtime.getSafeLevel() == 0) {
            String jrubyLib = System.getProperty("jruby.lib");
            if (jrubyLib != null) {
                addPath(jrubyLib);
            }
        }


        String jrubyHome = System.getProperty("jruby.home");
        if (jrubyHome != null) {
            String rubyDir = jrubyHome + File.separatorChar + "lib" + File.separatorChar + "ruby" + File.separatorChar;

            addPath(rubyDir + "site_ruby" + File.separatorChar + Constants.RUBY_MAJOR_VERSION);
            addPath(rubyDir + "site_ruby" + File.separatorChar + Constants.RUBY_MAJOR_VERSION + File.separatorChar + "java");
            addPath(rubyDir + "site_ruby");
            addPath(rubyDir + Constants.RUBY_MAJOR_VERSION);
            addPath(rubyDir + Constants.RUBY_MAJOR_VERSION + File.separatorChar + "java");
        }

        if (runtime.getSafeLevel() == 0) {
            addPath(".");
        }
    }

    private void addPath(String path) {
        loadPath.add(RubyString.newString(runtime, path));
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#load(String)
     */
    public boolean load(String file) {
        String[] suffixes = new String[] { "", ".rb", ".jar" };
        Library library = null;
        for (int i = 0; i < suffixes.length; i++) {
            library = findLibrary(file + suffixes[i]);
            if (library != null) {
                break;
            }
        }
        if (library == null) {
            throw new LoadError(runtime, "No such file to load -- " + file);
        }
        library.load(runtime);
        return true;
    }

    private Library findLibrary(String file) {
        if (builtinLibraries.containsKey(file)) {
            return (Library) builtinLibraries.get(file);
        }
        URL url = findFile(file);
        if (url == null) {
            return null;
        }
        if (file.endsWith(".jar")) {
            return new JarredScript(url);
        } else {
            return new ExternalScript(url, file);
        }
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#require(String)
     */
    public boolean require(String file) {
        RubyString name = RubyString.newString(runtime, file);
        if (loadedFeatures.contains(name)) {
            return false;
        }
        if (load(file)) {
            loadedFeatures.add(name);
            return true;
        }
        return false;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#getLoadPath()
     */
    public ArrayList getLoadPath() {
        return loadPath;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#getLoadedFeatures()
     */
    public ArrayList getLoadedFeatures() {
        return loadedFeatures;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#isAutoloadDefined(String)
     */
    public boolean isAutoloadDefined(String name) {
        return autoloadMap.containsKey(name);
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#autoload(String)
     */
    public IRubyObject autoload(String name) {
        IAutoloadMethod loadMethod = (IAutoloadMethod)autoloadMap.get(name);
        if (loadMethod != null) {
            return loadMethod.load(runtime, name);
        }
        return null;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#addAutoload(String, IAutoloadMethod)
     */
    public void addAutoload(String name, IAutoloadMethod loadMethod) {
        autoloadMap.put(name, loadMethod);
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#registerBuiltin(String, Library)
     */
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
    private URL findFile(String name) {
        try {
            if (name.startsWith("jar:")) {
                return new URL(name);
            }
            for (int i = 0, size = loadPath.size(); i < size; i++) {
                String entry = loadPath.get(i).toString();
                if (entry.startsWith("jar:")) {
                    try {
                        JarFile current = new JarFile(entry.substring(4));
                        if (current.getJarEntry(name) != null) {
                            return new URL(entry + name);
                        }
                    } catch (FileNotFoundException ignored) {
                    } catch (IOException e) {
                        throw IOError.fromException(runtime, e);
                    }
                } else {
                    File current = new File(entry, name);
                    if (current.exists()) {
                        return current.toURL();
                    }
                }
            }
            File current = new File(name);
            if (current.exists()) {
                return current.toURL();
            }
        } catch (MalformedURLException e) {
            throw new IOError(runtime, e.getMessage());
        }
        return null;
    }
}