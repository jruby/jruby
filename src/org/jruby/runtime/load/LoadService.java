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
 * Copyright (C) 2002-2010 JRuby Community
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipException;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFile;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyString;
import org.jruby.ast.executable.Script;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.RaiseException;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;

/**
 * <b>How require works in JRuby</b>
 * When requiring a name from Ruby, JRuby will first remove any file extension it knows about,
 * thereby making it possible to use this string to see if JRuby has already loaded
 * the name in question. If a .rb extension is specified, JRuby will only try
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
    private final LoadTimer loadTimer;

    public enum SuffixType {
        Source, Extension, Both, Neither;
        
        public static final String[] sourceSuffixes = { ".class", ".rb" };
        public static final String[] extensionSuffixes = { ".jar", ".so", ".bundle", ".dll" };
        private static final String[] allSuffixes = { ".class", ".rb", ".jar", ".so", ".bundle", ".dll" };
        private static final String[] emptySuffixes = { "" };
        
        public String[] getSuffixes() {
            switch (this) {
            case Source:
                return sourceSuffixes;
            case Extension:
                return extensionSuffixes;
            case Both:
                return allSuffixes;
            case Neither:
                return emptySuffixes;
            }
            throw new RuntimeException("Unknown SuffixType: " + this);
        }
    }
    protected static final Pattern sourcePattern = Pattern.compile("\\.(?:rb)$");
    protected static final Pattern extensionPattern = Pattern.compile("\\.(?:so|o|dll|jar)$");

    protected RubyArray loadPath;
    protected RubyArray loadedFeatures;
    protected List loadedFeaturesInternal;
    protected final Map<String, Library> builtinLibraries = new HashMap<String, Library>();

    protected final Map<String, JarFile> jarFiles = new HashMap<String, JarFile>();

    protected final Map<String, IAutoloadMethod> autoloadMap = new HashMap<String, IAutoloadMethod>();

    protected final Ruby runtime;
    
    public LoadService(Ruby runtime) {
        this.runtime = runtime;
        if (RubyInstanceConfig.DEBUG_LOAD_TIMINGS) {
            loadTimer = new TracingLoadTimer();
        } else {
            loadTimer = new LoadTimer();
        }
    }

    public void init(List additionalDirectories) {
        loadPath = RubyArray.newArray(runtime);
        loadedFeatures = RubyArray.newArray(runtime);
        loadedFeaturesInternal = Collections.synchronizedList(loadedFeatures);
        
        // add all startup load paths to the list first
        for (Iterator iter = additionalDirectories.iterator(); iter.hasNext();) {
            addPath((String) iter.next());
        }

        // add $RUBYLIB paths
        RubyHash env = (RubyHash) runtime.getObject().fastGetConstant("ENV");
        RubyString env_rubylib = runtime.newString("RUBYLIB");
        if (env.has_key_p(env_rubylib).isTrue()) {
            String rubylib = env.op_aref(runtime.getCurrentContext(), env_rubylib).toString();
            String[] paths = rubylib.split(File.pathSeparator);
            for (int i = 0; i < paths.length; i++) {
                addPath(paths[i]);
            }
        }

        // wrap in try/catch for security exceptions in an applet
        try {
            String jrubyHome = runtime.getJRubyHome();
            if (jrubyHome != null) {
                char sep = '/';
                String rubyDir = jrubyHome + sep + "lib" + sep + "ruby" + sep;

                // If we're running in 1.9 compat mode, add Ruby 1.9 libs to path before 1.8 libs
                if (runtime.is1_9()) {
                    addPath(rubyDir + "site_ruby" + sep + Constants.RUBY1_9_MAJOR_VERSION);
                    addPath(rubyDir + "site_ruby" + sep + "shared");
                    addPath(rubyDir + "site_ruby" + sep + Constants.RUBY_MAJOR_VERSION);
                    addPath(rubyDir + Constants.RUBY1_9_MAJOR_VERSION);
                } else {
                    // Add 1.8 libs
                    addPath(rubyDir + "site_ruby" + sep + Constants.RUBY_MAJOR_VERSION);
                    addPath(rubyDir + "site_ruby" + sep + "shared");
                    addPath(rubyDir + Constants.RUBY_MAJOR_VERSION);
                }
            }
        } catch(SecurityException ignore) {}
        
        // "." dir is used for relative path loads from a given file, as in require '../foo/bar'
        if (runtime.getSafeLevel() == 0) {
            addPath(".");
        }
    }

    protected void addLoadedFeature(RubyString loadNameRubyString) {
        loadedFeaturesInternal.add(loadNameRubyString);
    }

    protected void addPath(String path) {
        // Empty paths do not need to be added
        if (path == null || path.length() == 0) return;
        
        synchronized(loadPath) {
            loadPath.append(runtime.newString(path.replace('\\', '/')));
        }
    }

    public void load(String file, boolean wrap) {
        if(!runtime.getProfile().allowLoad(file)) {
            throw runtime.newLoadError("No such file to load -- " + file);
        }

        SearchState state = new SearchState(file);
        state.prepareLoadSearch(file);
        
        Library library = findBuiltinLibrary(state, state.searchFile, state.suffixType);
        if (library == null) library = findLibraryWithoutCWD(state, state.searchFile, state.suffixType);

        if (library == null) {
            library = findLibraryWithClassloaders(state, state.searchFile, state.suffixType);
            if (library == null) {
                throw runtime.newLoadError("No such file to load -- " + file);
            }
        }
        try {
            library.load(runtime, wrap);
        } catch (IOException e) {
            if (runtime.getDebug().isTrue()) e.printStackTrace(runtime.getErr());
            throw newLoadErrorFromThrowable(runtime, file, e);
        }
    }

    public SearchState findFileForLoad(String file) throws AlreadyLoaded {
        SearchState state = new SearchState(file);
        state.prepareRequireSearch(file);

        for (LoadSearcher searcher : searchers) {
            if (searcher.shouldTrySearch(state)) {
                searcher.trySearch(state);
            } else {
                continue;
            }
        }

        return state;
    }

    public boolean lockAndRequire(String requireName) {
        Object requireLock;
        try {
            synchronized (requireLocks) {
                requireLock = requireLocks.get(requireName);
                if (requireLock == null) {
                    requireLock = new Object();
                    requireLocks.put(requireName, requireLock);
                }
            }

            synchronized (requireLock) {
                return require(requireName);
            }
        } finally {
            synchronized (requireLocks) {
                requireLocks.remove(requireName);
            }
        }
    }

    protected Map requireLocks = new Hashtable();

    public boolean smartLoad(String file) {
        checkEmptyLoad(file);

        // We don't support .so, but some stdlib require .so directly
        // replace it with .jar to look for an extension type we do support
        if (file.endsWith(".so")) {
            file = file.replaceAll(".so$", ".jar");
        }
        if (Platform.IS_WINDOWS) {
            file = file.replace('\\', '/');
        }
        
        try {
            SearchState state = findFileForLoad(file);
            return tryLoadingLibraryOrScript(runtime, state);
        } catch (AlreadyLoaded al) {
            // Library has already been loaded in some form, bail out
            return false;
        }
    }

    private static class LoadTimer {
        public long startLoad(String file) { return 0L; }
        public void endLoad(String file, long startTime) {}
    }

    private static class TracingLoadTimer extends LoadTimer {
        private final AtomicInteger indent = new AtomicInteger(0);
        private String getIndentString() {
            StringBuilder buf = new StringBuilder();
            int i = indent.get();
            for (int j = 0; j < i; j++) {
                buf.append("  ");
            }
            return buf.toString();
        }
        @Override
        public long startLoad(String file) {
            int i = indent.incrementAndGet();
            System.err.println(getIndentString() + "-> " + file);
            return System.currentTimeMillis();
        }
        @Override
        public void endLoad(String file, long startTime) {
            System.err.println(getIndentString() + "<- " + file + " - "
                    + (System.currentTimeMillis() - startTime) + "ms");
            indent.decrementAndGet();
        }
    }

    public boolean require(String file) {
        if(!runtime.getProfile().allowRequire(file)) {
            throw runtime.newLoadError("No such file to load -- " + file);
        }

        long startTime = loadTimer.startLoad(file);
        try {
            return smartLoad(file);
        } finally {
            loadTimer.endLoad(file, startTime);
        }

    }

    /**
     * Load the org.jruby.runtime.load.Library implementation specified by
     * className. The purpose of using this method is to avoid having static
     * references to the given library class, thereby avoiding the additional
     * classloading when the library is not in use.
     * 
     * @param runtime The runtime in which to load
     * @param libraryName The name of the library, to use for error messages
     * @param className The class of the library
     * @param classLoader The classloader to use to load it
     * @param wrap Whether to wrap top-level in an anonymous module
     */
    public static void reflectedLoad(Ruby runtime, String libraryName, String className, ClassLoader classLoader, boolean wrap) {
        try {
            if (classLoader == null && Ruby.isSecurityRestricted()) {
                classLoader = runtime.getInstanceConfig().getLoader();
            }

            Library library = (Library) classLoader.loadClass(className).newInstance();

            library.load(runtime, false);
        } catch (RaiseException re) {
            throw re;
        } catch (Throwable e) {
            if (runtime.getDebug().isTrue()) e.printStackTrace();
            throw runtime.newLoadError("library `" + libraryName + "' could not be loaded: " + e);
        }
    }

    public IRubyObject getLoadPath() {
        return loadPath;
    }

    public IRubyObject getLoadedFeatures() {
        return loadedFeatures;
    }

    public IAutoloadMethod autoloadFor(String name) {
        return autoloadMap.get(name);
    }

    public void removeAutoLoadFor(String name) {
        autoloadMap.remove(name);
    }

    public IRubyObject autoload(String name) {
        IAutoloadMethod loadMethod = autoloadMap.remove(name);
        if (loadMethod != null) {
            return loadMethod.load(runtime, name);
        }
        return null;
    }

    public void addAutoload(String name, IAutoloadMethod loadMethod) {
        autoloadMap.put(name, loadMethod);
    }

    public void addBuiltinLibrary(String name, Library library) {
        builtinLibraries.put(name, library);
    }

    public void removeBuiltinLibrary(String name) {
        builtinLibraries.remove(name);
    }

    public void removeInternalLoadedFeature(String name) {
        loadedFeaturesInternal.remove(name);
    }

    protected boolean featureAlreadyLoaded(RubyString loadNameRubyString) {
        return loadedFeaturesInternal.contains(loadNameRubyString);
    }

    protected boolean isJarfileLibrary(SearchState state, final String file) {
        return state.library instanceof JarredScript && file.endsWith(".jar");
    }

    protected void removeLoadedFeature(RubyString loadNameRubyString) {

        loadedFeaturesInternal.remove(loadNameRubyString);
    }

    protected void reraiseRaiseExceptions(Throwable e) throws RaiseException {
        if (e instanceof RaiseException) {
            throw (RaiseException) e;
        }
    }
    
    public interface LoadSearcher {
        public boolean shouldTrySearch(SearchState state);
        public void trySearch(SearchState state) throws AlreadyLoaded;
    }
    
    public static class AlreadyLoaded extends Exception {
        private RubyString searchNameString;
        
        public AlreadyLoaded(RubyString searchNameString) {
            this.searchNameString = searchNameString;
        }
        
        public RubyString getSearchNameString() {
            return searchNameString;
        }
    }
    
    public class BailoutSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return true;
        }
    
        public void trySearch(SearchState state) throws AlreadyLoaded {
            for (String suffix : state.suffixType.getSuffixes()) {
                String searchName = state.searchFile + suffix;
                RubyString searchNameString = RubyString.newString(runtime, searchName);
                if (featureAlreadyLoaded(searchNameString)) {
                    throw new AlreadyLoaded(searchNameString);
                }
            }
        }
    }

    public class NormalSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }
        
        public void trySearch(SearchState state) {
            state.library = findLibraryWithoutCWD(state, state.searchFile, state.suffixType);
        }
    }

    public class ClassLoaderSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }
        
        public void trySearch(SearchState state) {
            state.library = findLibraryWithClassloaders(state, state.searchFile, state.suffixType);
        }
    }

    public class ExtensionSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return (state.library == null || state.library instanceof JarredScript) && !state.searchFile.equalsIgnoreCase("");
        }
        
        public void trySearch(SearchState state) {
            // This code exploits the fact that all .jar files will be found for the JarredScript feature.
            // This is where the basic extension mechanism gets fixed
            Library oldLibrary = state.library;

            // Create package name, by splitting on / and joining all but the last elements with a ".", and downcasing them.
            String[] all = state.searchFile.split("/");

            StringBuilder finName = new StringBuilder();
            for(int i=0, j=(all.length-1); i<j; i++) {
                finName.append(all[i].toLowerCase()).append(".");
            }

            try {
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
                if(state.library instanceof JarredScript) {
                    // It's _really_ expensive to check that the class actually exists in the Jar, so
                    // we don't do that now.
                    runtime.getJRubyClassLoader().addURL(((JarredScript)state.library).getResource().getURL());
                }

                // quietly try to load the class
                Class theClass = runtime.getJavaSupport().loadJavaClassQuiet(className);
                state.library = new ClassExtensionLibrary(theClass);
            } catch (Exception ee) {
                state.library = null;
                runtime.getGlobalVariables().clear("$!");
            }

            // If there was a good library before, we go back to that
            if(state.library == null && oldLibrary != null) {
                state.library = oldLibrary;
            }
        }
    }

    public class ScriptClassSearcher implements LoadSearcher {
        public class ScriptClassLibrary implements Library {
            private Script script;

            public ScriptClassLibrary(Script script) {
                this.script = script;
            }

            public void load(Ruby runtime, boolean wrap) {
                runtime.loadScript(script);
            }
        }
        
        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }
        
        public void trySearch(SearchState state) throws RaiseException {
            // no library or extension found, try to load directly as a class
            Script script;
            String className = buildClassName(state.searchFile);
            int lastSlashIndex = className.lastIndexOf('/');
            if (lastSlashIndex > -1 && lastSlashIndex < className.length() - 1 && !Character.isJavaIdentifierStart(className.charAt(lastSlashIndex + 1))) {
                if (lastSlashIndex == -1) {
                    className = "_" + className;
                } else {
                    className = className.substring(0, lastSlashIndex + 1) + "_" + className.substring(lastSlashIndex + 1);
                }
            }
            className = className.replace('/', '.');
            try {
                Class scriptClass = Class.forName(className);
                script = (Script) scriptClass.newInstance();
            } catch (Exception cnfe) {
                throw runtime.newLoadError("no such file to load -- " + state.searchFile);
            }
            state.library = new ScriptClassLibrary(script);
        }
    }

    public static class SearchState {
        public Library library;
        public String loadName;
        public SuffixType suffixType;
        public String searchFile;
        
        public SearchState(String file) {
            loadName = file;
        }

        public void prepareRequireSearch(final String file) {
            // if an extension is specified, try more targetted searches
            if (file.lastIndexOf('.') > file.lastIndexOf('/')) {
                Matcher matcher = null;
                if ((matcher = sourcePattern.matcher(file)).find()) {
                    // source extensions
                    suffixType = SuffixType.Source;

                    // trim extension to try other options
                    searchFile = file.substring(0, matcher.start());
                } else if ((matcher = extensionPattern.matcher(file)).find()) {
                    // extension extensions
                    suffixType = SuffixType.Extension;

                    // trim extension to try other options
                    searchFile = file.substring(0, matcher.start());
                } else {
                    // unknown extension, fall back to search with extensions
                    suffixType = SuffixType.Both;
                    searchFile = file;
                }
            } else {
                // try all extensions
                suffixType = SuffixType.Both;
                searchFile = file;
            }
        }

        public void prepareLoadSearch(final String file) {
            // if a source extension is specified, try all source extensions
            if (file.lastIndexOf('.') > file.lastIndexOf('/')) {
                Matcher matcher = null;
                if ((matcher = sourcePattern.matcher(file)).find()) {
                    // source extensions
                    suffixType = SuffixType.Source;

                    // trim extension to try other options
                    searchFile = file.substring(0, matcher.start());
                } else {
                    // unknown extension, fall back to exact search
                    suffixType = SuffixType.Neither;
                    searchFile = file;
                }
            } else {
                // try only literal search
                suffixType = SuffixType.Neither;
                searchFile = file;
            }
        }
    }
    
    protected boolean tryLoadingLibraryOrScript(Ruby runtime, SearchState state) {
        // attempt to load the found library
        RubyString loadNameRubyString = RubyString.newString(runtime, state.loadName);
        try {
            synchronized (loadedFeaturesInternal) {
                if (loadedFeaturesInternal.contains(loadNameRubyString)) {
                    return false;
                } else {
                    addLoadedFeature(loadNameRubyString);
                }
            }
            
            // otherwise load the library we've found
            state.library.load(runtime, false);
            return true;
        } catch (MainExitException mee) {
            // allow MainExitException to propagate out for exec and friends
            throw mee;
        } catch (Throwable e) {
            if(isJarfileLibrary(state, state.searchFile)) {
                return true;
            }

            removeLoadedFeature(loadNameRubyString);
            reraiseRaiseExceptions(e);

            if(runtime.getDebug().isTrue()) e.printStackTrace(runtime.getErr());
            
            RaiseException re = newLoadErrorFromThrowable(runtime, state.searchFile, e);
            re.initCause(e);
            throw re;
        }
    }

    private static RaiseException newLoadErrorFromThrowable(Ruby runtime, String file, Throwable t) {
        return runtime.newLoadError(String.format("load error: %s -- %s: %s", file, t.getClass().getName(), t.getMessage()));
    }
    
    protected final List<LoadSearcher> searchers = new ArrayList<LoadSearcher>();
    {
        searchers.add(new BailoutSearcher());
        searchers.add(new NormalSearcher());
        searchers.add(new ClassLoaderSearcher());
        searchers.add(new ExtensionSearcher());
        searchers.add(new ScriptClassSearcher());
    }

    protected String buildClassName(String className) {
        // Remove any relative prefix, e.g. "./foo/bar" becomes "foo/bar".
        className = className.replaceFirst("^\\.\\/", "");
        if (className.lastIndexOf(".") != -1) {
            className = className.substring(0, className.lastIndexOf("."));
        }
        className = className.replace("-", "_minus_").replace('.', '_');
        return className;
    }

    protected void checkEmptyLoad(String file) throws RaiseException {
        if (file.equals("")) {
            throw runtime.newLoadError("No such file to load -- " + file);
        }
    }

    protected void debugLogTry(String what, String msg) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            runtime.getErr().println( "LoadService: trying " + what + ": " + msg );
        }
    }

    protected void debugLogFound(String what, String msg) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            runtime.getErr().println( "LoadService: found " + what + ": " + msg );
        }
    }

    protected void debugLogFound( LoadServiceResource resource ) {
        String resourceUrl;
        try {
            resourceUrl = resource.getURL().toString();
        } catch (IOException e) {
            resourceUrl = e.getMessage();
        }
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            runtime.getErr().println( "LoadService: found: " + resourceUrl );
        }
    }
    
    protected Library findBuiltinLibrary(SearchState state, String baseName, SuffixType suffixType) {
        for (String suffix : suffixType.getSuffixes()) {
            String namePlusSuffix = baseName + suffix;
            debugLogTry( "builtinLib",  namePlusSuffix );
            if (builtinLibraries.containsKey(namePlusSuffix)) {
                state.loadName = namePlusSuffix;
                Library lib = builtinLibraries.get(namePlusSuffix);
                debugLogFound( "builtinLib", namePlusSuffix );
                return lib;
            }
        }
        return null;
    }

    protected Library findLibraryWithoutCWD(SearchState state, String baseName, SuffixType suffixType) {
        Library library = null;
        
        switch (suffixType) {
        case Both:
            library = findBuiltinLibrary(state, baseName, SuffixType.Source);
            if (library == null) library = createLibrary(state, tryResourceFromJarURL(state, baseName, SuffixType.Source));
            if (library == null) library = createLibrary(state, tryResourceFromLoadPathOrURL(state, baseName, SuffixType.Source));
            // If we fail to find as a normal Ruby script, we try to find as an extension,
            // checking for a builtin first.
            if (library == null) library = findBuiltinLibrary(state, baseName, SuffixType.Extension);
            if (library == null) library = createLibrary(state, tryResourceFromJarURL(state, baseName, SuffixType.Extension));
            if (library == null) library = createLibrary(state, tryResourceFromLoadPathOrURL(state, baseName, SuffixType.Extension));
            break;
        case Source:
        case Extension:
            // Check for a builtin first.
            library = findBuiltinLibrary(state, baseName, suffixType);
            if (library == null) library = createLibrary(state, tryResourceFromJarURL(state, baseName, suffixType));
            if (library == null) library = createLibrary(state, tryResourceFromLoadPathOrURL(state, baseName, suffixType));
            break;
        case Neither:
            library = createLibrary(state, tryResourceFromJarURL(state, baseName, SuffixType.Neither));
            if (library == null) library = createLibrary(state, tryResourceFromLoadPathOrURL(state, baseName, SuffixType.Neither));
            break;
        }

        return library;
    }

    protected Library findLibraryWithClassloaders(SearchState state, String baseName, SuffixType suffixType) {
        for (String suffix : suffixType.getSuffixes()) {
            String file = baseName + suffix;
            LoadServiceResource resource = findFileInClasspath(file);
            if (resource != null) {
                state.loadName = resolveLoadName(resource, file);
                return createLibrary(state, resource);
            }
        }
        return null;
    }

    protected Library createLibrary(SearchState state, LoadServiceResource resource) {
        if (resource == null) {
            return null;
        }
        String file = state.loadName;
        if (file.endsWith(".so") || file.endsWith(".dll") || file.endsWith(".bundle")) {
            throw runtime.newLoadError("JRuby does not support .so libraries from filesystem");
        } else if (file.endsWith(".jar")) {
            return new JarredScript(resource);
        } else if (file.endsWith(".class")) {
            return new JavaCompiledScript(resource);
        } else {
            return new ExternalScript(resource, file);
        }      
    }

    protected LoadServiceResource tryResourceFromCWD(SearchState state, String baseName,SuffixType suffixType) throws RaiseException {
        LoadServiceResource foundResource = null;
        
        for (String suffix : suffixType.getSuffixes()) {
            String namePlusSuffix = baseName + suffix;
            // check current directory; if file exists, retrieve URL and return resource
            try {
                JRubyFile file = JRubyFile.create(runtime.getCurrentDirectory(), RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                debugLogTry("resourceFromCWD", file.toString());
                if (file.isFile() && file.isAbsolute() && file.canRead()) {
                    boolean absolute = true;
                    String s = namePlusSuffix;
                    if(!namePlusSuffix.startsWith("./")) {
                        s = "./" + s;
                    }
                    foundResource = new LoadServiceResource(file, s, absolute);
                    debugLogFound(foundResource);
                    state.loadName = resolveLoadName(foundResource, namePlusSuffix);
                    break;
                }
            } catch (IllegalArgumentException illArgEx) {
            } catch (SecurityException secEx) {
            }
        }
        
        return foundResource;
    }

    protected LoadServiceResource tryResourceFromHome(SearchState state, String baseName, SuffixType suffixType) throws RaiseException {
        LoadServiceResource foundResource = null;

        RubyHash env = (RubyHash) runtime.getObject().fastGetConstant("ENV");
        RubyString env_home = runtime.newString("HOME");
        if (env.has_key_p(env_home).isFalse()) {
            return null;
        }
        String home = env.op_aref(runtime.getCurrentContext(), env_home).toString();
        String path = baseName.substring(2);

        for (String suffix : suffixType.getSuffixes()) {
            String namePlusSuffix = path + suffix;
            // check home directory; if file exists, retrieve URL and return resource
            try {
                JRubyFile file = JRubyFile.create(home, RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                debugLogTry("resourceFromHome", file.toString());
                if (file.isFile() && file.isAbsolute() && file.canRead()) {
                    boolean absolute = true;

                    state.loadName = file.getPath();
                    foundResource = new LoadServiceResource(file, state.loadName, absolute);
                    debugLogFound(foundResource);
                    break;
                }
            } catch (IllegalArgumentException illArgEx) {
            } catch (SecurityException secEx) {
            }
        }

        return foundResource;
    }
    
    protected LoadServiceResource tryResourceFromJarURL(SearchState state, String baseName, SuffixType suffixType) {
        // if a jar or file URL, return load service resource directly without further searching
        LoadServiceResource foundResource = null;
        if (baseName.startsWith("jar:")) {
            for (String suffix : suffixType.getSuffixes()) {
                String namePlusSuffix = baseName + suffix;
                try {
                    URL url = new URL(namePlusSuffix);
                    debugLogTry("resourceFromJarURL", url.toString());
                    if (url.openStream() != null) {
                        foundResource = new LoadServiceResource(url, namePlusSuffix);
                        debugLogFound(foundResource);
                    }
                } catch (FileNotFoundException e) {
                } catch (MalformedURLException e) {
                    throw runtime.newIOErrorFromException(e);
                } catch (IOException e) {
                    throw runtime.newIOErrorFromException(e);
                }
                if (foundResource != null) {
                    state.loadName = resolveLoadName(foundResource, namePlusSuffix);
                    break; // end suffix iteration
                }
            }
        } else if(baseName.startsWith("file:") && baseName.indexOf("!/") != -1) {
            for (String suffix : suffixType.getSuffixes()) {
                String namePlusSuffix = baseName + suffix;
                try {
                    String jarFile = namePlusSuffix.substring(5, namePlusSuffix.indexOf("!/"));
                    JarFile file = new JarFile(jarFile);
                    String expandedFilename = expandRelativeJarPath(namePlusSuffix.substring(namePlusSuffix.indexOf("!/") + 2));

                    debugLogTry("resourceFromJarURL", expandedFilename.toString());
                    if(file.getJarEntry(expandedFilename) != null) {
                        foundResource = new LoadServiceResource(new URL("jar:file:" + jarFile + "!/" + expandedFilename), namePlusSuffix);
                        debugLogFound(foundResource);
                    }
                } catch(Exception e) {}
                if (foundResource != null) {
                    state.loadName = resolveLoadName(foundResource, namePlusSuffix);
                    break; // end suffix iteration
                }
            }    
        }
        
        return foundResource;
    }
    
    protected LoadServiceResource tryResourceFromLoadPathOrURL(SearchState state, String baseName, SuffixType suffixType) {
        LoadServiceResource foundResource = null;

        // if it's a ./ baseName, use CWD logic
        if (baseName.startsWith("./")) {
            foundResource = tryResourceFromCWD(state, baseName, suffixType);

            if (foundResource != null) {
                state.loadName = resolveLoadName(foundResource, foundResource.getName());
                return foundResource;
            }
        }

        // if it's a ~/ baseName use HOME logic
        if (baseName.startsWith("~/")) {
            foundResource = tryResourceFromHome(state, baseName, suffixType);

            if (foundResource != null) {
                state.loadName = resolveLoadName(foundResource, foundResource.getName());
                return foundResource;
            }
        }

        // if given path is absolute, just try it as-is (with extensions) and no load path
        if (new File(baseName).isAbsolute() || baseName.startsWith("../")) {
            for (String suffix : suffixType.getSuffixes()) {
                String namePlusSuffix = baseName + suffix;
                foundResource = tryResourceAsIs(namePlusSuffix);

                if (foundResource != null) {
                    state.loadName = resolveLoadName(foundResource, namePlusSuffix);
                    return foundResource;
                }
            }

            return null;
        }
        
        Outer: for (int i = 0; i < loadPath.size(); i++) {
            // TODO this is really inefficient, and potentially a problem everytime anyone require's something.
            // we should try to make LoadPath a special array object.
            RubyString entryString = loadPath.eltInternal(i).convertToString();
            String loadPathEntry = entryString.asJavaString();

            if (loadPathEntry.equals(".") || loadPathEntry.equals("")) {
                foundResource = tryResourceFromCWD(state, baseName, suffixType);

                if (foundResource != null) {
                    String ss = foundResource.getName();
                    if(ss.startsWith("./")) {
                        ss = ss.substring(2);
                    }
                    state.loadName = resolveLoadName(foundResource, ss);
                    break Outer;
                }
            } else {
                boolean looksLikeJarURL = loadPathLooksLikeJarURL(loadPathEntry);
                for (String suffix : suffixType.getSuffixes()) {
                    String namePlusSuffix = baseName + suffix;

                    if (looksLikeJarURL) {
                        foundResource = tryResourceFromJarURLWithLoadPath(namePlusSuffix, loadPathEntry);
                    } else if(namePlusSuffix.startsWith("./")) {
                        throw runtime.newLoadError("");
                    } else {
                        foundResource = tryResourceFromLoadPath(namePlusSuffix, loadPathEntry);
                    }

                    if (foundResource != null) {
                        String ss = namePlusSuffix;
                        if(ss.startsWith("./")) {
                            ss = ss.substring(2);
                        }
                        state.loadName = resolveLoadName(foundResource, ss);
                        break Outer; // end suffix iteration
                    }
                }
            }
        }
        
        return foundResource;
    }
    
    protected LoadServiceResource tryResourceFromJarURLWithLoadPath(String namePlusSuffix, String loadPathEntry) {
        LoadServiceResource foundResource = null;
        
        JarFile current = jarFiles.get(loadPathEntry);
        boolean isFileJarUrl = loadPathEntry.startsWith("file:") && loadPathEntry.indexOf("!/") != -1;
        String after = isFileJarUrl ? loadPathEntry.substring(loadPathEntry.indexOf("!/") + 2) + "/" : "";
        String before = isFileJarUrl ? loadPathEntry.substring(0, loadPathEntry.indexOf("!/")) : loadPathEntry;

        if(null == current) {
            try {
                if(loadPathEntry.startsWith("jar:")) {
                    current = new JarFile(loadPathEntry.substring(4));
                } else if (loadPathEntry.endsWith(".jar")) {
                    current = new JarFile(loadPathEntry);
                } else {
                    current = new JarFile(loadPathEntry.substring(5,loadPathEntry.indexOf("!/")));
                }
                jarFiles.put(loadPathEntry,current);
            } catch (ZipException ignored) {
                if (runtime.getInstanceConfig().isDebug()) {
                    runtime.getErr().println("ZipException trying to access " + loadPathEntry + ", stack trace follows:");
                    ignored.printStackTrace(runtime.getErr());
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                throw runtime.newIOErrorFromException(e);
            }
        }
        String canonicalEntry = after+namePlusSuffix;
        if (current != null ) {
            debugLogTry("resourceFromJarURLWithLoadPath", current.getName() + "!/" + canonicalEntry);
            if (current.getJarEntry(canonicalEntry) != null) {
                try {
                    if (loadPathEntry.endsWith(".jar")) {
                        foundResource = new LoadServiceResource(new URL("jar:file:" + loadPathEntry + "!/" + canonicalEntry), "/" + namePlusSuffix);
                    } else if (loadPathEntry.startsWith("file:")) {
                        foundResource = new LoadServiceResource(new URL("jar:" + before + "!/" + canonicalEntry), loadPathEntry + "/" + namePlusSuffix);
                    } else {
                        foundResource =  new LoadServiceResource(new URL("jar:file:" + loadPathEntry.substring(4) + "!/" + namePlusSuffix), loadPathEntry + namePlusSuffix);
                    }
                    debugLogFound(foundResource);
                } catch (MalformedURLException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }
        }
        
        return foundResource;
    }

    protected boolean loadPathLooksLikeJarURL(String loadPathEntry) {
        return loadPathEntry.startsWith("jar:") || loadPathEntry.endsWith(".jar") || (loadPathEntry.startsWith("file:") && loadPathEntry.indexOf("!/") != -1);
    }

    protected LoadServiceResource tryResourceFromLoadPath( String namePlusSuffix,String loadPathEntry) throws RaiseException {
        LoadServiceResource foundResource = null;

        try {
            if (!Ruby.isSecurityRestricted()) {
                String reportedPath = loadPathEntry + "/" + namePlusSuffix;
                JRubyFile actualPath;
                boolean absolute = false;
                // we check length == 0 for 'load', which does not use load path
                if (new File(reportedPath).isAbsolute()) {
                    absolute = true;
                    // it's an absolute path, use it as-is
                    actualPath = JRubyFile.create(loadPathEntry, RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                } else {
                    absolute = false;
                    // prepend ./ if . is not already there, since we're loading based on CWD
                    if (reportedPath.charAt(0) != '.') {
                        reportedPath = "./" + reportedPath;
                    }
                    actualPath = JRubyFile.create(JRubyFile.create(runtime.getCurrentDirectory(), loadPathEntry).getAbsolutePath(), RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                }
                debugLogTry("resourceFromLoadPath", "'" + actualPath.toString() + "' " + actualPath.isFile() + " " + actualPath.canRead());
                if (actualPath.isFile() && actualPath.canRead()) {
                    foundResource = new LoadServiceResource(actualPath, reportedPath, absolute);
                    debugLogFound(foundResource);
                }
            }
        } catch (SecurityException secEx) {
        }

        return foundResource;
    }

    protected LoadServiceResource tryResourceAsIs(String namePlusSuffix) throws RaiseException {
        LoadServiceResource foundResource = null;

        try {
            if (!Ruby.isSecurityRestricted()) {
                String reportedPath = namePlusSuffix;
                File actualPath;
                // we check length == 0 for 'load', which does not use load path
                if (new File(reportedPath).isAbsolute()) {
                    // it's an absolute path, use it as-is
                    actualPath = new File(RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                } else {
                    // prepend ./ if . is not already there, since we're loading based on CWD
                    if (reportedPath.charAt(0) == '.' && reportedPath.charAt(1) == '/') {
                        reportedPath = reportedPath.replaceFirst("\\./", runtime.getCurrentDirectory());
                    }

                    actualPath = JRubyFile.create(runtime.getCurrentDirectory(), RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                }
                debugLogTry("resourceAsIs", actualPath.toString());
                if (actualPath.isFile() && actualPath.canRead()) {
                    foundResource = new LoadServiceResource(actualPath, reportedPath);
                    debugLogFound(foundResource);
                }
            }
        } catch (SecurityException secEx) {
        }

        return foundResource;
    }

    /**
     * this method uses the appropriate lookup strategy to find a file.
     * It is used by Kernel#require.
     *
     * @mri rb_find_file
     * @param name the file to find, this is a path name
     * @return the correct file
     */
    protected LoadServiceResource findFileInClasspath(String name) {
        // Look in classpath next (we do not use File as a test since UNC names will match)
        // Note: Jar resources must NEVER begin with an '/'. (previous code said "always begin with a /")
        ClassLoader classLoader = runtime.getJRubyClassLoader();

        // handle security-sensitive case
        if (Ruby.isSecurityRestricted() && classLoader == null) {
            classLoader = runtime.getInstanceConfig().getLoader();
        }

        // absolute classpath URI, no need to iterate over loadpaths
        if (name.startsWith("classpath:/")) {
            LoadServiceResource foundResource = getClassPathResource(classLoader, name);
            if (foundResource != null) {
                return foundResource;
            }
        } else if (name.startsWith("classpath:")) {
            // "relative" classpath URI
            name = name.substring("classpath:".length());
        }

        for (int i = 0; i < loadPath.size(); i++) {
            // TODO this is really inefficient, and potentially a problem everytime anyone require's something.
            // we should try to make LoadPath a special array object.
            RubyString entryString = loadPath.eltInternal(i).convertToString();
            String entry = entryString.asJavaString();

            // if entry is an empty string, skip it
            if (entry.length() == 0) continue;

            // if entry starts with a slash, skip it since classloader resources never start with a /
            if (entry.charAt(0) == '/' || (entry.length() > 1 && entry.charAt(1) == ':')) continue;

            if (entry.startsWith("classpath:/")) {
                entry = entry.substring("classpath:/".length());
            } else if (entry.startsWith("classpath:")) {
                entry = entry.substring("classpath:".length());
            }

            // otherwise, try to load from classpath (Note: Jar resources always uses '/')
            LoadServiceResource foundResource = getClassPathResource(classLoader, entry + "/" + name);
            if (foundResource != null) {
                return foundResource;
            }
        }

        // if name starts with a / we're done (classloader resources won't load with an initial /)
        if (name.charAt(0) == '/' || (name.length() > 1 && name.charAt(1) == ':')) return null;
        
        // Try to load from classpath without prefix. "A/b.rb" will not load as 
        // "./A/b.rb" in a jar file.
        LoadServiceResource foundResource = getClassPathResource(classLoader, name);
        if (foundResource != null) {
            return foundResource;
        }

        return null;
    }
    
    /* Directories and unavailable resources are not able to open a stream. */
    protected boolean isRequireable(URL loc) {
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

    protected LoadServiceResource getClassPathResource(ClassLoader classLoader, String name) {
        boolean isClasspathScheme = false;

        // strip the classpath scheme first
        if (name.startsWith("classpath:/")) {
            isClasspathScheme = true;
            name = name.substring("classpath:/".length());
        } else if (name.startsWith("classpath:")) {
            isClasspathScheme = true;
            name = name.substring("classpath:".length());
        }

        debugLogTry("fileInClasspath", name);
        URL loc = classLoader.getResource(name);

        if (loc != null) { // got it
            String path = "classpath:/" + name;
            // special case for typical jar:file URLs, but only if the name didn't have
            // the classpath scheme explicitly
            if (!isClasspathScheme && loc.toString().startsWith("jar:file:") && isRequireable(loc)) {
                // Make sure this is not a directory or unavailable in some way
                try {
                    path = loc.toURI().getSchemeSpecificPart();
                } catch (java.net.URISyntaxException urise) {
                    if (runtime.getInstanceConfig().isDebug()) {
                        runtime.getErr().println("URISyntaxException trying to parse " + loc + ", stack trace follows:");
                        urise.printStackTrace(runtime.getErr());
                    }
                    return null;
                }
            }
            LoadServiceResource foundResource = new LoadServiceResource(loc, path);
            debugLogFound(foundResource);
            return foundResource;
        }
        return null;
    }

    // Canonicalization here is only used to expand '.' and '..' in jar
    // paths, not for real files that exist on the filesystem
    private String expandRelativeJarPath(String path) {
        try {
            String cwd = new File(".").getCanonicalPath();
            return new File(path).getCanonicalPath()
                .substring(cwd.length() + 1)
                .replaceAll("\\\\","/");
        } catch(Exception e) {
            return path;
        }
    }

    protected String resolveLoadName(LoadServiceResource foundResource, String previousPath) {
        return previousPath;
    }
}
