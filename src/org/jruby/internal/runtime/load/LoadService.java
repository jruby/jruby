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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.util.BuiltinScript;
import org.jruby.exceptions.IOError;
import org.jruby.exceptions.LoadError;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.IAutoloadMethod;
import org.jruby.runtime.load.ILoadService;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class LoadService implements ILoadService {
    private ArrayList loadPath = new ArrayList();
    private ArrayList loadedFeatures = new ArrayList();

    private Map autoloadMap = new HashMap();

    private Ruby runtime;
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
            addPath(runtime, (String)iter.next());
        }
        if (runtime.getSafeLevel() == 0) {
            addPath(runtime, System.getProperty("jruby.lib"));
        }

        String rubyDir = System.getProperty("jruby.home") + File.separatorChar + "lib" + File.separatorChar + "ruby" + File.separatorChar;

        addPath(runtime, rubyDir + "site_ruby" + File.separatorChar + Constants.RUBY_MAJOR_VERSION);
        addPath(runtime, rubyDir + "site_ruby" + File.separatorChar + Constants.RUBY_MAJOR_VERSION + File.separatorChar + "java");
        addPath(runtime, rubyDir + "site_ruby");
        addPath(runtime, rubyDir + Constants.RUBY_MAJOR_VERSION);
        addPath(runtime, rubyDir + Constants.RUBY_MAJOR_VERSION + File.separatorChar + "java");

        if (runtime.getSafeLevel() == 0) {
            loadPath.add(RubyString.newString(runtime, "."));
        }
    }

    private void addPath(Ruby runtime, String path) {
        loadPath.add(RubyString.newString(runtime, path));
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#load(String)
     */
    public boolean load(String file) {
        // FIXME: replace java thing with more generic mechanism
        if (file.equals("java")) {
            BuiltinScript javaSupport = new BuiltinScript("javasupport");
            javaSupport.load(runtime);
            return true;
        }

        if (!file.endsWith(".rb")) {
            file += ".rb";
        }
        URL url = findFile(file);
        String name = url.toString();
        if (name.startsWith("file:")) {
            name = name.substring(5);
        }
        try {
            Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            runtime.loadScript(name, reader, false);
            reader.close();
        } catch (IOException ioExcptn) {
            throw IOError.fromException(runtime, ioExcptn);
        }
        return true;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#require(String)
     */
    public boolean require(String file) {
        RubyString name = RubyString.newString(runtime, file);
        if (!loadedFeatures.contains(name)) {
            if (file.endsWith(".jar")) {
                loadJar(file);
                loadedFeatures.add(name);
                return true;
            }
            if (load(file)) {
                loadedFeatures.add(name);
                return true;
            }
        }
        return false;
    }
    
    private void loadJar(String file) {
        URL jarFile = findFile(file);

        runtime.getJavaSupport().addToClasspath(jarFile);

        try {
            JarInputStream in = new JarInputStream(new BufferedInputStream(jarFile.openStream()));

            Manifest mf = in.getManifest();
            String rubyInit = mf.getMainAttributes().getValue("Ruby-Init");
            if (rubyInit != null) {
                JarEntry entry = in.getNextJarEntry();
                while (entry != null && !entry.getName().equals(rubyInit)) {
                    entry = in.getNextJarEntry();
                }
                if (entry != null) {
                    IRubyObject old = runtime.getGlobalVariables().isDefined("$JAR_URL") ? runtime.getGlobalVariables().get("$JAR_URL") : runtime.getNil();
                    try {
                        runtime.getGlobalVariables().set("$JAR_URL", RubyString.newString(runtime, "jar:" + jarFile + "!/"));
                        runtime.loadScript("init", new InputStreamReader(in), false);
                    } finally {
                        runtime.getGlobalVariables().set("$JAR_URL", old);
                    }
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
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
                    } catch (IOException e) {
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
        }
        throw new LoadError(runtime, "No such file to load -- " + name);
    }
}