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
 * Copyright (C) 2002-2011 JRuby Community
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

import org.jruby.util.collections.StringArraySet;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.JRubyFile;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.util.URLUtil.getPath;

/**
 * <h2>How require works in JRuby</h2>
 *
 * When requiring a name from Ruby, JRuby will first remove any file
 * extension it knows about, thereby making it possible to use this string
 * to see if JRuby has already loaded the name in question. If a .rb
 * extension is specified, JRuby will only try those extensions when
 * searching. If a .so, .o, .dll, or .jar extension is specified, JRuby will
 * only try .so or .jar when searching. Otherwise, JRuby goes through the
 * known suffixes (.rb, .rb.ast.ser, .so, and .jar) and tries to find a
 * library with this name. The process for finding a library follows this
 * order for all searchable extensions:
 *
 * <ol>
 * <li>First, check if the name starts with 'jar:', then the path points to
 * a jar-file resource which is returned.</li>
 * <li>Second, try searching for the file in the current dir</li>
 * <li>Then JRuby looks through the load path trying these variants:
 *   <ol>
 *     <li>See if the current load path entry starts with 'jar:', if so
 *     check if this jar-file contains the name</li>
 *     <li>Otherwise JRuby tries to construct a path by combining the entry
 *     and the current working directy, and then see if a file with the
 *     correct name can be reached from this point.</li>
 *   </ol>
 * </li>
 * <li>If all these fail, try to load the name as a resource from
 * classloader resources, using the bare name as well as the load path
 * entries</li>
 * <li>When we get to this state, the normal JRuby loading has failed. At
 * this stage JRuby tries to load Java native extensions, by following this
 * process:
 *   <ol>
 *     <li>First it checks that we haven't already found a library. If we
 *     found a library of type JarredScript, the method continues.</li>
 *
 *     <li>The first step is translating the name given into a valid Java
 *     Extension class name. First it splits the string into each path
 *     segment, and then makes all but the last downcased. After this it
 *     takes the last entry, removes all underscores and capitalizes each
 *     part separated by underscores. It then joins everything together and
 *     tacks on a 'Service' at the end. Lastly, it removes all leading dots,
 *     to make it a valid Java FWCN.</li>
 *
 *     <li>If the previous library was of type JarredScript, we try to add
 *     the jar-file to the classpath</li>
 *
 *     <li>Now JRuby tries to instantiate the class with the name
 *     constructed. If this works, we return a ClassExtensionLibrary.
 *     Otherwise, the old library is put back in place, if there was one.
 *   </ol>
 * </li>
 * <li>When all separate methods have been tried and there was no result, a
 * LoadError will be raised.</li>
 * <li>Otherwise, the name will be added to the loaded features, and the
 * library loaded</li>
 * </ol>
 *
 * <h2>How to make a class that can get required by JRuby</h2>
 *
 * <p>First, decide on what name should be used to require the extension. In
 * this purely hypothetical example, this name will be
 * 'active_record/connection_adapters/jdbc_adapter'. Then create the class
 * name for this require-name, by looking at the guidelines above. Our class
 * should be named active_record.connection_adapters.JdbcAdapterService, and
 * implement one of the library-interfaces. The easiest one is
 * BasicLibraryService, where you define the basicLoad-method, which will
 * get called when your library should be loaded.</p>
 *
 * <p>The next step is to either put your compiled class on JRuby's
 * classpath, or package the class/es inside a jar-file. To package into a
 * jar-file, we first create the file, then rename it to jdbc_adapter.jar.
 * Then we put this jar-file in the directory
 * active_record/connection_adapters somewhere in JRuby's load path. For
 * example, copying jdbc_adapter.jar into
 * JRUBY_HOME/lib/ruby/site_ruby/1.8/active_record/connection_adapters will
 * make everything work. If you've packaged your extension inside a RubyGem,
 * write a setub.rb-script that copies the jar-file to this place.</p>
 *
 * @author jpetersen
 */
public class LoadService {
    private static final Logger LOG = LoggerFactory.getLogger("LoadService");

    private final LoadTimer loadTimer;

    public enum SuffixType {
        Source, Extension, Both, Neither;

        private static final String[] emptySuffixes = { "" };
        // NOTE: always search .rb first for speed
        public static final String[] sourceSuffixes = { ".rb", ".class" };
        public static final String[] extensionSuffixes;
        private static final String[] allSuffixes;

        static {                // compute based on platform
            extensionSuffixes = new String[2];
            extensionSuffixes[0] = ".jar";
            if (Platform.IS_WINDOWS) {
                extensionSuffixes[1] = ".dll";
            } else if (Platform.IS_MAC) { // TODO: BSD also?
                extensionSuffixes[1] = ".bundle";
            } else {
                extensionSuffixes[1] = ".so";
            }
            allSuffixes = new String[sourceSuffixes.length + extensionSuffixes.length];
            System.arraycopy(sourceSuffixes, 0, allSuffixes, 0, sourceSuffixes.length);
            System.arraycopy(extensionSuffixes, 0, allSuffixes, sourceSuffixes.length, extensionSuffixes.length);
        }

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
    protected static final Pattern extensionPattern = Pattern.compile("\\.(?:so|o|dll|bundle|jar)$");

    protected RubyArray loadPath;
    protected StringArraySet loadedFeatures;
    protected final Map<String, Library> builtinLibraries = new HashMap<String, Library>();

    protected final Map<String, JarFile> jarFiles = new HashMap<String, JarFile>();

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

        String jrubyHome = runtime.getJRubyHome();
        loadedFeatures = new StringArraySet(runtime);

        // add all startup load paths to the list first
        for (Iterator iter = additionalDirectories.iterator(); iter.hasNext();) {
            addPath((String) iter.next());
        }

        // add $RUBYLIB paths
        RubyHash env = (RubyHash) runtime.getObject().getConstant("ENV");
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
            if (jrubyHome != null) {
                char sep = '/';
                String rubyDir = jrubyHome + sep + "lib" + sep + "ruby" + sep;

                if (runtime.is1_9()) {
                    addPath(rubyDir + "site_ruby" + sep + Constants.RUBY1_9_MAJOR_VERSION);

                    // shared lib
                    addPath(rubyDir + "shared");

                    // MRI standard lib
                    addPath(rubyDir + Constants.RUBY1_9_MAJOR_VERSION);
                } else {
                    addPath(rubyDir + "site_ruby" + sep + Constants.RUBY_MAJOR_VERSION);

                    // shared lib
                    addPath(rubyDir + "shared");

                    // MRI standard lib
                    addPath(rubyDir + Constants.RUBY_MAJOR_VERSION);
                }
            }

        } catch(SecurityException ignore) {}

        // "." dir is used for relative path loads from a given file, as in require '../foo/bar'
        if (!runtime.is1_9() && runtime.getSafeLevel() == 0) {
            addPath(".");
        }
    }

    protected void addLoadedFeature(String name) {
        loadedFeatures.append(RubyString.newString(runtime, name));
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
            throw runtime.newLoadError("no such file to load -- " + file);
        }

        SearchState state = new SearchState(file);
        state.prepareLoadSearch(file);

        Library library = findBuiltinLibrary(state, state.searchFile, state.suffixType);
        if (library == null) library = findLibraryWithoutCWD(state, state.searchFile, state.suffixType);

        if (library == null) {
            library = findLibraryWithClassloaders(state, state.searchFile, state.suffixType);
            if (library == null) {
                throw runtime.newLoadError("no such file to load -- " + file);
            }
        }
        try {
            library.load(runtime, wrap);
        } catch (IOException e) {
            if (runtime.getDebug().isTrue()) e.printStackTrace(runtime.getErr());
            throw newLoadErrorFromThrowable(runtime, file, e);
        }
    }

    public void loadFromClassLoader(ClassLoader classLoader, String file, boolean wrap) {
        SearchState state = new SearchState(file);
        state.prepareLoadSearch(file);

        Library library = null;
        LoadServiceResource resource = getClassPathResource(classLoader, file);
        if (resource != null) {
            state.loadName = resolveLoadName(resource, file);
            library = createLibrary(state, resource);
        }
        if (library == null) {
            throw runtime.newLoadError("no such file to load -- " + file);
        }
        try {
            library.load(runtime, wrap);
        } catch (IOException e) {
            if (runtime.getDebug().isTrue()) e.printStackTrace(runtime.getErr());
            throw newLoadErrorFromThrowable(runtime, file, e);
        }
    }

    public SearchState findFileForLoad(String file) {
        if (Platform.IS_WINDOWS) {
            file = file.replace('\\', '/');
        }
        // Even if we don't support .so, some stdlib require .so directly.
        // Replace it with .jar to look for a java extension
        // JRUBY-5033: The ExtensionSearcher will locate C exts, too, this way.
        if (file.endsWith(".so")) {
            file = file.replaceAll(".so$", ".jar");
        }

        SearchState state = new SearchState(file);
        state.prepareRequireSearch(file);

        for (LoadSearcher searcher : searchers) {
            if (searcher.shouldTrySearch(state)) {
                if (!searcher.trySearch(state)) {
                    return null;
                }
            }
        }

        return state;
    }

    public boolean require(String requireName) {
        return requireCommon(requireName, true) == RequireState.LOADED;
    }

    public boolean autoloadRequire(String requireName) {
        return requireCommon(requireName, false) != RequireState.CIRCULAR;
    }

    private enum RequireState {
        LOADED, ALREADY_LOADED, CIRCULAR
    };

    private RequireState requireCommon(String requireName, boolean circularRequireWarning) {
        // check for requiredName without extension.
        if (featureAlreadyLoaded(requireName)) {
            return RequireState.ALREADY_LOADED;
        }

        if (!requireLocks.lock(requireName)) {
            if (circularRequireWarning && runtime.isVerbose() && runtime.is1_9()) {
                warnCircularRequire(requireName);
            }
            return RequireState.CIRCULAR;
        }
        try {
            if (!runtime.getProfile().allowRequire(requireName)) {
                throw runtime.newLoadError("no such file to load -- " + requireName);
            }

            // check for requiredName again now that we're locked
            if (featureAlreadyLoaded(requireName)) {
                return RequireState.ALREADY_LOADED;
            }

            // numbers from loadTimer does not include lock waiting time.
            long startTime = loadTimer.startLoad(requireName);
            try {
                boolean loaded = smartLoadInternal(requireName);
                return loaded ? RequireState.LOADED : RequireState.ALREADY_LOADED;
            } finally {
                loadTimer.endLoad(requireName, startTime);
            }
        } finally {
            requireLocks.unlock(requireName);
        }
    }
    
    protected final RequireLocks requireLocks = new RequireLocks();

    private class RequireLocks {
        private final Map<String, ReentrantLock> pool;
        // global lock for require must be fair
        private final ReentrantLock globalLock;

        private RequireLocks() {
            this.pool = new HashMap<String, ReentrantLock>();
            this.globalLock = new ReentrantLock(true);
        }

        /**
         * Get exclusive lock for the specified requireName. Acquire sync object
         * for the requireName from the pool, then try to lock it. NOTE: This
         * lock is not fair for now.
         * 
         * @param requireName
         *            just a name for the lock.
         * @return If the sync object already locked by current thread, it just
         *         returns false without getting a lock. Otherwise true.
         */
        private boolean lock(String requireName) {
            ReentrantLock lock;

            while (true) {
                synchronized (pool) {
                    lock = pool.get(requireName);
                    if (lock == null) {
                        if (runtime.getInstanceConfig().isGlobalRequireLock()) {
                            lock = globalLock;
                        } else {
                            lock = new ReentrantLock();
                        }
                        pool.put(requireName, lock);
                    } else if (lock.isHeldByCurrentThread()) {
                        return false;
                    }
                }

                lock.lock();

                // repeat until locked object still in requireLocks.
                synchronized (pool) {
                    if (pool.get(requireName) == lock) {
                        // the object is locked && the lock is in the pool
                        return true;
                    }
                    // go next try
                    lock.unlock();
                }
            }
        }

        /**
         * Unlock the lock for the specified requireName.
         * 
         * @param requireName
         *            name of the lock to be unlocked.
         */
        private void unlock(String requireName) {
            synchronized (pool) {
                ReentrantLock lock = pool.get(requireName);
                if (lock != null) {
                    assert lock.isHeldByCurrentThread();
                    lock.unlock();
                    pool.remove(requireName);
                }
            }
        }
    }

    protected void warnCircularRequire(String requireName) {
        runtime.getWarnings().warn("loading in progress, circular require considered harmful - " + requireName);
        // it's a hack for c:rb_backtrace impl.
        // We should introduce new method to Ruby.TraceType when rb_backtrace is widely used not only for this purpose.
        RaiseException ex = new RaiseException(runtime, runtime.getRuntimeError(), null, false);
        String trace = runtime.getInstanceConfig().getTraceType().printBacktrace(ex.getException(), runtime.getPosix().isatty(FileDescriptor.err));
        // rb_backtrace dumps to stderr directly.
        System.err.print(trace.replaceFirst("[^\n]*\n", ""));
    }

    /**
     * This method did require the specified file without getting a lock.
     * Now we offer safe version only. Use {@link LoadService#require(String)} instead.
     */
    @Deprecated
    public boolean smartLoad(String file) {
        return require(file);
    }

    private boolean smartLoadInternal(String file) {
        checkEmptyLoad(file);
        SearchState state = findFileForLoad(file);
        if (state == null) {
            return false;
        }
        if (state.library == null) {
            throw runtime.newLoadError("no such file to load -- " + state.searchFile);
        }

        // check with long name
        if (featureAlreadyLoaded(state.loadName)) {
            return false;
        }

        boolean loaded = tryLoadingLibraryOrScript(runtime, state);
        if (loaded) {
            addLoadedFeature(state.loadName);
        }
        return loaded;
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
            indent.incrementAndGet();
            LOG.info(getIndentString() + "-> " + file);
            return System.currentTimeMillis();
        }
        @Override
        public void endLoad(String file, long startTime) {
            LOG.info(getIndentString() + "<- " + file + " - "
                    + (System.currentTimeMillis() - startTime) + "ms");
            indent.decrementAndGet();
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

            Object libObject = classLoader.loadClass(className).newInstance();
            if (libObject instanceof Library) {
                Library library = (Library)libObject;
                library.load(runtime, false);
            } else if (libObject instanceof BasicLibraryService) {
                BasicLibraryService service = (BasicLibraryService)libObject;
                service.basicLoad(runtime);
            } else {
                // invalid type of library, raise error
                throw runtime.newLoadError("library `" + libraryName + "' is not of type Library or BasicLibraryService");
            }
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

    public void addBuiltinLibrary(String name, Library library) {
        builtinLibraries.put(name, library);
    }

    public void removeBuiltinLibrary(String name) {
        builtinLibraries.remove(name);
    }

    public void removeInternalLoadedFeature(String name) {
        RubyString nameRubyString = runtime.newString(name);
        loadedFeatures.delete(runtime.getCurrentContext(), nameRubyString, Block.NULL_BLOCK);
    }

    protected boolean featureAlreadyLoaded(String name) {
        return loadedFeatures.containsString(name);
    }

    protected boolean isJarfileLibrary(SearchState state, final String file) {
        return state.library instanceof JarredScript && file.endsWith(".jar");
    }

    protected void reraiseRaiseExceptions(Throwable e) throws RaiseException {
        if (e instanceof RaiseException) {
            throw (RaiseException) e;
        }
    }

    public interface LoadSearcher {
        /**
         * @param state
         * @return true if trySearch should be called.
         */
        public boolean shouldTrySearch(SearchState state);

        /**
         * @param state
         * @return false if loadSearch must be bail-out.
         */
        public boolean trySearch(SearchState state);
    }

    public class BailoutSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }

        protected boolean trySearch(String file, SuffixType suffixType) {
            for (String suffix : suffixType.getSuffixes()) {
                String searchName = file + suffix;
                if (featureAlreadyLoaded(searchName)) {
                    return false;
                }
            }
            return true;
        }

        public boolean trySearch(SearchState state) {
            return trySearch(state.searchFile, state.suffixType);
        }
    }

    public class SourceBailoutSearcher extends BailoutSearcher {
        public boolean shouldTrySearch(SearchState state) {
            // JRUBY-5032: Load extension files if they are required
            // explicitly, and even if an rb file of the same name
            // has already been loaded (effectively skipping the search for a source file).
            return !extensionPattern.matcher(state.loadName).find();
        }

        // According to Rubyspec, source files should be loaded even if an equally named
        // extension is loaded already. So we use the bailout search twice, once only
        // for source files and once for whatever suffix type the state determines
        public boolean trySearch(SearchState state) {
            return super.trySearch(state.searchFile, SuffixType.Source);
        }
    }

    public class NormalSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }

        public boolean trySearch(SearchState state) {
            state.library = findLibraryWithoutCWD(state, state.searchFile, state.suffixType);
            return true;
        }
    }

    public class ClassLoaderSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }

        public boolean trySearch(SearchState state) {
            state.library = findLibraryWithClassloaders(state, state.searchFile, state.suffixType);
            return true;
        }
    }

    public class ExtensionSearcher implements LoadSearcher {
        public boolean shouldTrySearch(SearchState state) {
            return (state.library == null || state.library instanceof JarredScript) && !state.searchFile.equalsIgnoreCase("");
        }

        public boolean trySearch(SearchState state) {
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
                Class theClass = runtime.getJavaSupport().loadJavaClass(className);
                state.library = new ClassExtensionLibrary(className + ".java", theClass);
            } catch (ClassNotFoundException cnfe) {
                if (runtime.isDebug()) cnfe.printStackTrace();
                // we ignore this and assume the jar is not an extension
            } catch (UnsupportedClassVersionError ucve) {
                if (runtime.isDebug()) ucve.printStackTrace();
                throw runtime.newLoadError("JRuby ext built for wrong Java version in `" + finName + "': " + ucve);
            } catch (IOException ioe) {
                if (runtime.isDebug()) ioe.printStackTrace();
                throw runtime.newLoadError("IOException loading extension `" + finName + "`: " + ioe);
            } catch (Exception e) {
                if (runtime.isDebug()) e.printStackTrace();
                throw runtime.newLoadError("Exception loading extension `" + finName + "`: " + e);
            }

            // If there was a good library before, we go back to that
            if(state.library == null && oldLibrary != null) {
                state.library = oldLibrary;
            }
            return true;
        }
    }

    public class ScriptClassSearcher implements LoadSearcher {
        public class ScriptClassLibrary implements Library {
            private Script script;

            public ScriptClassLibrary(Script script) {
                this.script = script;
            }

            public void load(Ruby runtime, boolean wrap) {
                runtime.loadScript(script, wrap);
            }
        }

        public boolean shouldTrySearch(SearchState state) {
            return state.library == null;
        }

        public boolean trySearch(SearchState state) throws RaiseException {
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
                return true;
            }
            state.library = new ScriptClassLibrary(script);
            return true;
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

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getName()).append(": ");
            sb.append("library=").append(library.toString());
            sb.append(", loadName=").append(loadName);
            sb.append(", suffixType=").append(suffixType.toString());
            sb.append(", searchFile=").append(searchFile);
            return sb.toString();
        }
    }

    protected boolean tryLoadingLibraryOrScript(Ruby runtime, SearchState state) {
        // attempt to load the found library
        try {
            state.library.load(runtime, false);
            return true;
        } catch (MainExitException mee) {
            // allow MainExitException to propagate out for exec and friends
            throw mee;
        } catch (Throwable e) {
            if(isJarfileLibrary(state, state.searchFile)) {
                return true;
            }
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

    // Using the BailoutSearch twice, once only for source files and once for state suffixes,
    // in order to adhere to Rubyspec
    protected final List<LoadSearcher> searchers = new ArrayList<LoadSearcher>();
    {
        searchers.add(new SourceBailoutSearcher());
        searchers.add(new NormalSearcher());
        searchers.add(new ClassLoaderSearcher());
        searchers.add(new BailoutSearcher());
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
            throw runtime.newLoadError("no such file to load -- " + file);
        }
    }

    protected void debugLogTry(String what, String msg) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            LOG.info( "LoadService: trying " + what + ": " + msg );
        }
    }

    protected void debugLogFound(String what, String msg) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            LOG.info( "LoadService: found " + what + ": " + msg );
        }
    }

    protected void debugLogFound( LoadServiceResource resource ) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            String resourceUrl;
            try {
                resourceUrl = resource.getURL().toString();
            } catch (IOException e) {
                resourceUrl = e.getMessage();
            }
            LOG.info( "LoadService: found: " + resourceUrl );
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
            if (runtime.getInstanceConfig().isCextEnabled()) {
                return new CExtension(resource);
            } else {
                throw runtime.newLoadError("C extensions are disabled, can't load `" + resource.getName() + "'");
            }
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
                    foundResource = new LoadServiceResource(file, getFileName(file, namePlusSuffix), absolute);
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

        RubyHash env = (RubyHash) runtime.getObject().getConstant("ENV");
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
            }

            // not found, don't bother with load path
            return foundResource;
        }

        // if it's a ~/ baseName use HOME logic
        if (baseName.startsWith("~/")) {
            foundResource = tryResourceFromHome(state, baseName, suffixType);

            if (foundResource != null) {
                state.loadName = resolveLoadName(foundResource, foundResource.getName());
            }

            // not found, don't bother with load path
            return foundResource;
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
                boolean looksLikeClasspathURL = loadPathLooksLikeClasspathURL(loadPathEntry);
                for (String suffix : suffixType.getSuffixes()) {
                    String namePlusSuffix = baseName + suffix;

                    if (looksLikeJarURL) {
                        foundResource = tryResourceFromJarURLWithLoadPath(namePlusSuffix, loadPathEntry);
                    } else if (looksLikeClasspathURL) {
                        foundResource = findFileInClasspath(loadPathEntry + "/" + namePlusSuffix);
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

        String[] urlParts = splitJarUrl(loadPathEntry);
        String jarFileName = urlParts[0];
        String entryPath = urlParts[1];

        JarFile current = getJarFile(jarFileName);
        if (current != null ) {
            String canonicalEntry = (entryPath.length() > 0 ? entryPath + "/" : "") + namePlusSuffix;
            debugLogTry("resourceFromJarURLWithLoadPath", current.getName() + "!/" + canonicalEntry);
            if (current.getJarEntry(canonicalEntry) != null) {
                try {
                    String resourceUrl = "jar:file:" + jarFileName + "!/" + canonicalEntry;
                    foundResource = new LoadServiceResource(new URL(resourceUrl), resourceUrl);
                    debugLogFound(foundResource);
                } catch (MalformedURLException e) {
                    throw runtime.newIOErrorFromException(e);
                }
            }
        }

        return foundResource;
    }

    public JarFile getJarFile(String jarFileName) {
        JarFile jarFile = jarFiles.get(jarFileName);
        if(null == jarFile) {
            try {
                jarFile = new JarFile(jarFileName);
                jarFiles.put(jarFileName, jarFile);
            } catch (ZipException ignored) {
                if (runtime.getInstanceConfig().isDebug()) {
                    LOG.info("ZipException trying to access " + jarFileName + ", stack trace follows:");
                    ignored.printStackTrace(runtime.getErr());
                }
            } catch (FileNotFoundException ignored) {
            } catch (IOException e) {
                throw runtime.newIOErrorFromException(e);
            }
        }
        return jarFile;
    }

    protected boolean loadPathLooksLikeJarURL(String loadPathEntry) {
        return loadPathEntry.startsWith("jar:") || loadPathEntry.endsWith(".jar") || (loadPathEntry.startsWith("file:") && loadPathEntry.indexOf("!") != -1);
    }

    protected boolean loadPathLooksLikeClasspathURL(String loadPathEntry) {
        return loadPathEntry.startsWith("classpath:");
    }
    
    private String[] splitJarUrl(String loadPathEntry) {
        int idx = loadPathEntry.indexOf("!");
        if (idx == -1) {
            return new String[]{loadPathEntry, ""};
        }

        String filename = loadPathEntry.substring(0, idx);
        String entry = idx + 2 < loadPathEntry.length() ? loadPathEntry.substring(idx + 2) : "";

        if(filename.startsWith("jar:")) {
            filename = filename.substring(4);
        }
        
        if(filename.startsWith("file:")) {
            filename = filename.substring(5);
        }
        
        return new String[]{filename, entry};
    }

    protected LoadServiceResource tryResourceFromLoadPath( String namePlusSuffix,String loadPathEntry) throws RaiseException {
        LoadServiceResource foundResource = null;

        try {
            if (!Ruby.isSecurityRestricted()) {
                String reportedPath = loadPathEntry + "/" + namePlusSuffix;
                boolean absolute = true;
                // we check length == 0 for 'load', which does not use load path
                if (!new File(reportedPath).isAbsolute()) {
                    absolute = false;
                    // prepend ./ if . is not already there, since we're loading based on CWD
                    if (reportedPath.charAt(0) != '.') {
                        reportedPath = "./" + reportedPath;
                    }
                    loadPathEntry = JRubyFile.create(runtime.getCurrentDirectory(), loadPathEntry).getAbsolutePath();
                }
                JRubyFile actualPath = JRubyFile.create(loadPathEntry, RubyFile.expandUserPath(runtime.getCurrentContext(), namePlusSuffix));
                if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
                    debugLogTry("resourceFromLoadPath", "'" + actualPath.toString() + "' " + actualPath.isFile() + " " + actualPath.canRead());
                }
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

            String entryName;
            if (name.startsWith(entry)) {
                entryName = name.substring(entry.length());
            } else {
                entryName = name;
            }

            // otherwise, try to load from classpath (Note: Jar resources always uses '/')
            LoadServiceResource foundResource = getClassPathResource(classLoader, entry + "/" + entryName);
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
                if (loc.getProtocol().equals("file") && new java.io.File(getPath(loc)).isDirectory()) {
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
        } else if(name.startsWith("file:") && name.indexOf("!/") != -1) {
            name = name.substring(name.indexOf("!/") + 2);
        }

        debugLogTry("fileInClasspath", name);
        URL loc = classLoader.getResource(name);

        if (loc != null) { // got it
            String path = "classpath:/" + name;
            // special case for typical jar:file URLs, but only if the name didn't have
            // the classpath scheme explicitly
            if (!isClasspathScheme &&
                    (loc.getProtocol().equals("jar") || loc.getProtocol().equals("file"))
                    && isRequireable(loc)) {
                path = getPath(loc);
            }
            LoadServiceResource foundResource = new LoadServiceResource(loc, path);
            debugLogFound(foundResource);
            return foundResource;
        }
        return null;
    }

    private String expandRelativeJarPath(String path) {
        return path.replaceAll("/[^/]+/\\.\\.|[^/]+/\\.\\./|\\./","").replace("^\\\\","/");
    }

    protected String resolveLoadName(LoadServiceResource foundResource, String previousPath) {
        return previousPath;
    }

    protected String getFileName(JRubyFile file, String namePlusSuffix) {
        String s = namePlusSuffix;
        if(!namePlusSuffix.startsWith("./")) {
            s = "./" + s;
        }
        return s;
    }
}
