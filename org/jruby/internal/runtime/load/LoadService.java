package org.jruby.internal.runtime.load;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.jruby.Ruby;
import org.jruby.RubyString;
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
            loadPath.add(RubyString.newString(runtime, (String)iter.next()));
        }

        if (runtime.getSafeLevel() == 0) {
            loadPath.add(RubyString.newString(runtime, System.getProperty("jruby.lib")));
        }

        String rubyDir = System.getProperty("jruby.home") + File.separatorChar + "lib" + File.separatorChar + "ruby" + File.separatorChar;

        loadPath.add(RubyString.newString(runtime, rubyDir + "site_ruby" + File.separatorChar + Constants.RUBY_MAJOR_VERSION));
        loadPath.add(RubyString.newString(runtime, rubyDir + "site_ruby" + File.separatorChar + Constants.RUBY_MAJOR_VERSION + File.separatorChar + "java"));
        loadPath.add(RubyString.newString(runtime, rubyDir + "site_ruby"));
        loadPath.add(RubyString.newString(runtime, rubyDir + Constants.RUBY_MAJOR_VERSION));
        loadPath.add(RubyString.newString(runtime, rubyDir + Constants.RUBY_MAJOR_VERSION + File.separatorChar + "java"));

        if (runtime.getSafeLevel() == 0) {
            loadPath.add(RubyString.newString(runtime, "."));
        }
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#load(String)
     */
    public boolean load(String file) {
        if (!file.endsWith(".rb")) {
            file += ".rb";
        }
        runtime.loadFile(findFile(file), false);
        return true;
    }

    /**
     * @see org.jruby.runtime.load.ILoadService#require(String)
     */
    public boolean require(String file) {
        RubyString name = RubyString.newString(runtime, file);
        if (!loadedFeatures.contains(name)) {
            if (file.endsWith(".jar")) {
                loadJAR(file);
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
    
    private void loadJAR(String file) {
        File jarFile = findFile(file);

        runtime.getJavaSupport().addToClasspath(jarFile);

        try {
            JarInputStream in = new JarInputStream(new BufferedInputStream(new FileInputStream(jarFile)));
            
            Manifest mf = in.getManifest();
            String rubyInit = mf.getMainAttributes().getValue("Ruby-Init");
            if (rubyInit != null) {
                JarEntry entry = in.getNextJarEntry();
                while (entry != null && !entry.getName().equals(rubyInit)) {
                    entry = in.getNextJarEntry();
                }
                if (entry != null) {
                    runtime.eval(runtime.parse(new InputStreamReader(in), "init"));
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
    private File findFile(String name) {
        for (int i = 0, size = loadPath.size(); i < size; i++) {
            File current = new File(loadPath.get(i).toString(), name);
            if (current.exists()) {
                return current;
            }
        }
        File current = new File(name);
        if (current.exists()) {
            return current;
        }
        throw new LoadError(runtime, "No such file to load -- " + name);
    }
}