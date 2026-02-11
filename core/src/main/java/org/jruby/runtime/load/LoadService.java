/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime.load;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyFile;
import org.jruby.RubyHash;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.RubyThread;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.Unrescuable;
import org.jruby.ext.rbconfig.RbConfigLibrary;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.collections.StringArraySet;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import static org.jruby.api.Access.loadService;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Create.*;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.Helpers.throwException;
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
 * @author jpetersen
 */
public class LoadService {
    static final Logger LOG = LoggerFactory.getLogger(LoadService.class);

    private final LoadTimer loadTimer;

    public enum SuffixType {
        Source(LibrarySearcher.Suffix.SOURCES),
        Extension(LibrarySearcher.Suffix.EXTENSIONS),
        Both(LibrarySearcher.Suffix.ALL),
        Neither(EnumSet.noneOf(LibrarySearcher.Suffix.class));

        public final EnumSet<LibrarySearcher.Suffix> suffixes;

        SuffixType(EnumSet suffixes) {
            this.suffixes = suffixes;
        }

        public EnumSet<LibrarySearcher.Suffix> getSuffixSet() {
            return suffixes;
        }

        @Deprecated(since = "9.3.0.0")
        public String[] getSuffixes() {
            return suffixes.stream()
                    .map((suffix) -> suffix.name())
                    .toArray(String[]::new);
        }

        @Deprecated(since = "9.3.0.0")
        public static final String[] sourceSuffixes = LibrarySearcher.Suffix.SOURCES.stream().map((suffix) -> suffix.name()).toArray(String[]::new);
        @Deprecated(since = "9.3.0.0")
        public static final String[] extensionSuffixes = LibrarySearcher.Suffix.EXTENSIONS.stream().map((suffix) -> suffix.name()).toArray(String[]::new);
    }

    @Deprecated(since = "10.0.3.0")
    protected static final Pattern sourcePattern = Pattern.compile("\\.(?:rb)$");
    @Deprecated(since = "10.0.3.0")
    protected static final Pattern extensionPattern = Pattern.compile("\\.(?:so|o|jar)$");

    protected RubyArray loadPath;
    protected StringArraySet loadedFeatures;

    @Deprecated(since = "10.0.3.0")
    protected final Map<String, JarFile> jarFiles = new HashMap<>(0);

    protected final Ruby runtime;
    protected LibrarySearcher librarySearcher;

    protected String mainScript;
    protected String mainScriptPath;

    public LoadService(Ruby runtime) {
        this.runtime = runtime;
        if (RubyInstanceConfig.DEBUG_LOAD_TIMINGS) {
            loadTimer = new TracingLoadTimer();
        } else {
            loadTimer = new LoadTimer();
        }
    }

    /**
     * Called to initialize the load path with a set of optional prepended
     * directories and then the standard set of dirs.
     *
     * This should only be called once, at load time, since it wipes out loaded
     * features.
     *
     * @param prependDirectories
     */
    public void init(List<String> prependDirectories) {
        var context = runtime.getCurrentContext();
        loadPath = newArray(context);
        loadPath.getMetaClass().defineMethods(context, LoadPathMethods.class);

        String jrubyHome = runtime.getJRubyHome();

        loadedFeatures = new StringArraySet(runtime);

        librarySearcher = new LibrarySearcher(this);

        // add all startup load paths to the list first
        addPaths(prependDirectories);

        // add $RUBYLIB paths
        RubyHash env = (RubyHash) objectClass(context).getConstant(context, "ENV");
        RubyString env_rubylib = newString(context, "RUBYLIB");

        if (env.has_key_p(context, env_rubylib).isTrue()) {
            String rubylib = env.op_aref(context, env_rubylib).toString();
            String[] paths = rubylib.split(File.pathSeparator);
            addPaths(paths);
        }

        // wrap in try/catch for security exceptions in an applet
        try {
            if (jrubyHome != null) {
                // siteDir has to come first, because rubygems insert paths after it
                // and we must to prefer Gems to rubyLibDir/rubySharedLibDir (same as MRI)
                // NOTE: this path *must* be added, whether it exists or not, because RubyGems
                // uses it to know where to insert gem paths into the load path. Removing it
                // causes all gem paths to be inserted at the beginning, overriding paths
                // added via -I.
                addPath(RbConfigLibrary.getSiteDir(runtime));

                // if vendorDirGeneral is different than siteDirGeneral,
                // add vendorDir, too
                // adding {vendor,site}{Lib,Arch}Dir dirs is not necessary,
                // since they should be the same as {vendor,site}Dir
                if (!RbConfigLibrary.isSiteVendorSame(runtime)) {
                    addPath(RbConfigLibrary.getVendorDir(runtime));
                }
                String rubygemsDir = RbConfigLibrary.getRubygemsDir(runtime);
                if (rubygemsDir != null) addPath(rubygemsDir);

                addPath(RbConfigLibrary.getRubyLibDir(runtime));
            }

        } catch(SecurityException ignore) {}
        addPaths(runtime.getInstanceConfig().getExtraLoadPaths());
    }

    /**
     * Add additional directories to the load path.
     *
     * @param additionalDirectories a List of additional dirs to append to the load path
     */
    public void addPaths(List<String> additionalDirectories) {
        for (String dir : additionalDirectories) {
            addPath(dir);
        }
    }

    /**
     * Add additional directories to the load path.
     *
     * @param additionalDirectories an array of additional dirs to append to the load path
     */
    public void addPaths(String... additionalDirectories) {
        for (String dir : additionalDirectories) {
            addPath(dir);
        }
    }

    // MRI: rb_provide, roughly
    public void provide(String name) {
        librarySearcher.provideFeature(runtime.newString(name));
    }

    protected void addPath(String path) {
        // Empty paths do not need to be added
        if (path == null || path.isEmpty()) return;
        var loadPath = this.loadPath;
        synchronized(loadPath) {
            final RubyString pathToAdd = runtime.newString(path.replace('\\', '/'));
            var context = runtime.getCurrentContext();
            // Do not add duplicated paths
            if (loadPath.includes(context, pathToAdd)) return;
            loadPath.append(context, pathToAdd);
        }
    }

    public static class LoadPathMethods {
        @JRubyMethod
        public static IRubyObject resolve_feature_path(ThreadContext context, IRubyObject self, IRubyObject pathArg) {
            RubyString path = RubyFile.get_path(context, pathArg);
            LibrarySearcher.FoundLibrary[] libraryHolder = {null};
            char extension = loadService(context).searchForRequire(path.toString(), libraryHolder);

            if (extension == 0) return context.nil;

            RubySymbol ext = switch (extension) {
                case 'r' -> asSymbol(context, "rb");
                case 's' -> asSymbol(context, "so"); // FIXME: should this be so or jar?
                default -> asSymbol(context, "unknown");
            };

            RubyString name;
            if (libraryHolder[0] == null) {
                // FIXME: Our builtin libraries are returning 'r' is ext but has no loader.
                ext = asSymbol(context, "so"); // FIXME: should this be so or jar?
                name = path;
            } else {
                name = newString(context, libraryHolder[0].getLoadName());
            }

            return newArray(context, ext, name);
        }
    }

    private final ThreadLocal<RubyModule> wrapperSelf = new ThreadLocal<>();

    public RubyModule getWrapperSelf() {
        return wrapperSelf.get();
    }

    public void load(String file, IRubyObject wrapWith) {
        if (wrapWith == null || !wrapWith.isTrue()) {
            load(file, false);
            return;
        }

        if (wrapWith instanceof RubyModule) {
            wrapperSelf.set((RubyModule) wrapWith);
            try {
                load(file, true);
            } finally {
                wrapperSelf.remove();
            }
        } else {
            load(file, true);
        }
    }

    public void load(String file, boolean wrap) {
        long startTime = loadTimer.startLoad(file);
        int currentLine = runtime.getCurrentLine();
        try {
            if(!runtime.getProfile().allowLoad(file)) {
                throw loadFailed(runtime, file);
            }

            LibrarySearcher.FoundLibrary library = librarySearcher.findLibraryForLoad(file);

            // load() will do a last chance look in current working directory for the file (see load.c:rb_f_load()).
            if (library == null) {
                FileResource fileResource = JRubyFile.createResourceAsFile(runtime, file);

                if (!fileResource.exists()) throw loadFailed(runtime, file);

                library = new LibrarySearcher.FoundLibrary(file, file, LibrarySearcher.ResourceLibrary.create(file, file, fileResource));
            }

            try {
                library.load(runtime, wrap);
            } catch (IOException ex) {
                throw runtime.newIOErrorFromException(ex);
            }
        } finally {
            runtime.setCurrentLine(currentLine);
            loadTimer.endLoad(file, startTime);
        }
    }

    public void loadFromClassLoader(ClassLoader classLoader, String file, boolean wrap) {
        long startTime = loadTimer.startLoad("classloader:" + file);
        int currentLine = runtime.getCurrentLine();
        try {
            String[] fileHolder = {file};
            librarySearcher.getSuffixTypeForLoad(fileHolder);
            String baseName = fileHolder[0];

            LoadServiceResource resource = getClassPathResource(classLoader, file);

            if (resource == null) throw loadFailed(runtime, file);

            String loadName = resolveLoadName(resource, file);
            LibrarySearcher.FoundLibrary library =
                    new LibrarySearcher.FoundLibrary(baseName, loadName, createLibrary(baseName, loadName, resource));

            try {
                library.load(runtime, wrap);
            } catch (IOException e) {
                throw runtime.newIOErrorFromException(e);
            }
        } finally {
            runtime.setCurrentLine(currentLine);
            loadTimer.endLoad("classloader:" + file, startTime);
        }
    }

    public boolean require(String requireName) {
        return smartLoadInternal(requireName, true) == RequireState.LOADED;
    }

    public boolean autoloadRequire(RubyString requireName) {
        return runtime.getTopSelf().callMethod(runtime.getCurrentContext(), "require", requireName).isTrue();
    }

    private enum RequireState {
        LOADED, ALREADY_LOADED, CIRCULAR
    }

    final RequireLocks requireLocks = new RequireLocks();

    final class RequireLocks {
        final class RequireLock extends ReentrantLock {
            volatile boolean destroyed;
        }
        final ConcurrentHashMap<String, RequireLock> pool;
        // global lock for require must be fair
        //private final ReentrantLock globalLock;

        private RequireLocks() {
            this.pool = new ConcurrentHashMap<>(8, 0.75f, 2);
            //this.globalLock = new ReentrantLock(true);
        }

        /**
         * Get exclusive lock for the specified requireName. Acquire sync object
         * for the requireName from the pool, then try to lock it. NOTE: This
         * lock is not fair for now.
         *
         * @param path
         *            just a name for the lock.
         * @return If the sync object already locked by current thread, it just
         *         returns false without getting a lock. Otherwise true.
         */
        private RequireState lock(
                String path,
                boolean circularRequireWarning,
                RequireState defaultResult,
                Function<String, RequireState> ifLocked) {
            ThreadContext currentContext = runtime.getCurrentContext();
            RubyThread thread = currentContext.getThread();

            // loop until require fails for us or succeeds for another thread
            while (true) {
                RequireLock lock = pool.get(path);

                // Check if lock is already there
                if (lock == null) {
                    RequireLock newLock = new RequireLock();

                    // If lock is new, lock and return LOCKED
                    lock = pool.computeIfAbsent(path, (name) -> {
                        thread.lock(newLock);
                        return newLock;
                    });

                    RequireState state = null;
                    if (lock == newLock) {
                        // Lock is ours, run ifLocked and then clean up
                        try {
                            return state = executeAndClearLock(path, ifLocked, thread, lock);
                        } finally {
                            // failed load, remove our lock and let other threads fight it out
                            if (state == null) pool.remove(path);
                        }
                    }
                }

                if (lock.isHeldByCurrentThread()) {
                    // we hold the lock, which means we're re-locking for the same file; warn about this
                    if (circularRequireWarning && runtime.isVerbose()) {
                        warnCircularRequire(path);
                    }

                    return null;
                }

                // Other thread holds the lock, wait to acquire
                while (true) {
                    try {
                        thread.lockInterruptibly(lock);
                        break;
                    } catch (InterruptedException ie) {
                        currentContext.pollThreadEvents();
                    }
                }

                // Lock has been acquired, confirm other thread has completed and return default
                thread.unlock(lock);
                if (lock.destroyed) {
                    return defaultResult;
                }

                // Other thread failed to load, try again to lock and load
            }
        }

        private RequireState executeAndClearLock(String path, Function<String, RequireState> ifLocked, RubyThread thread, RequireLock lock) {
            boolean destroyLock = false;
            try {
                RequireState state = ifLocked.apply(path);

                // successful load, destroy lock
                destroyLock = true;

                return state;

            // NOTE: raising errors (LoadError included) should not be considered a completed load: `destroyLock == false`
            } finally {
                if (destroyLock) {
                    lock.destroyed = true;
                    pool.remove(path);
                }
                thread.unlock(lock);
            }
        }
    }

    protected void warnCircularRequire(String requireName) {
        var context = runtime.getCurrentContext();
        StringBuilder sb = new StringBuilder("loading in progress, circular require considered harmful - " + requireName);
        sb.append("\n");

        context.renderCurrentBacktrace(sb);
        warn(context, sb.toString());
    }

    private RequireState smartLoadInternal(String file, boolean circularRequireWarning) {
        checkEmptyLoad(file);

        if (!runtime.getProfile().allowRequire(file)) {
            throw loadFailed(runtime, file);
        }

        LibrarySearcher.FoundLibrary[] libraryHolder = {null};
        char found = searchForRequire(file, libraryHolder);

        if (found == 0) {
            throw loadFailed(runtime, file);
        }

        LibrarySearcher.FoundLibrary library = libraryHolder[0];

        if (library == null) {
            return RequireState.ALREADY_LOADED;
        } else {
            String loadName = library.getLoadName();
            return requireLocks.lock(loadName, circularRequireWarning, RequireState.ALREADY_LOADED, (name) -> {
                // double-check features in case it was added and lock removed before we saw either
                if (librarySearcher.getLoadedFeature(name) != null) {
                    return RequireState.ALREADY_LOADED;
                }

                if (name.length() == 0) {
                    // logic for load_lock returning a blank string, apparently for autoload func?
                    provide(name);
                    return RequireState.LOADED;
                } else {
                    // numbers from loadTimer does not include lock waiting time.
                    long startTime = loadTimer.startLoad(name);
                    try {
                        tryLoadingLibraryOrScript(runtime, library, library.getSearchName());
                        provide(name);
                        return RequireState.LOADED;
                    } finally {
                        loadTimer.endLoad(name, startTime);
                    }
                }
            });
        }
    }

    private static class LoadTimer {
        public long startLoad(String file) { return 0L; }
        public void endLoad(String file, long startTime) {}
    }

    private static final class TracingLoadTimer extends LoadTimer {
        private final AtomicInteger indent = new AtomicInteger(0);

        private StringBuilder getIndentString() {
            final int i = indent.get();
            StringBuilder buf = new StringBuilder(i * 2);
            for (int j = 0; j < i; j++) {
                buf.append(' ').append(' ');
            }
            return buf;
        }

        @Override
        public long startLoad(String file) {
            indent.incrementAndGet();
            LOG.info( "{}-> {}", getIndentString(), file );
            return System.currentTimeMillis();
        }

        @Override
        public void endLoad(String file, long startTime) {
            LOG.info( "{}<- {} - {}ms", getIndentString(), file, (System.currentTimeMillis() - startTime) );
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

            Object libObject = classLoader.loadClass(className).getConstructor().newInstance();
            if (libObject instanceof Library) {
                Library library = (Library)libObject;
                library.load(runtime, false);
            } else if (libObject instanceof BasicLibraryService) {
                BasicLibraryService service = (BasicLibraryService)libObject;
                service.basicLoad(runtime);
            } else {
                // invalid type of library, raise error
                throw runtime.newLoadError("library '" + libraryName + "' is not of type Library or BasicLibraryService", libraryName);
            }
        } catch (RaiseException re) {
            throw re;
        } catch (Throwable e) {
            debugLoadException(runtime, e);
            throw runtime.newLoadError("library '" + libraryName + "' could not be loaded: " + e, libraryName);
        }
    }

    private static void debugLoadException(final Ruby runtime, final Throwable ex) {
        if (runtime.isDebug()) ex.printStackTrace(runtime.getErr());
    }

    public IRubyObject getLoadPath() {
        return loadPath;
    }

    public IRubyObject getLoadedFeatures() {
        return loadedFeatures;
    }

    protected boolean isJarfileLibrary(Library library, final String file) {
        return library instanceof JarredScript && file.endsWith(".jar");
    }

    protected boolean tryLoadingLibraryOrScript(Ruby runtime, Library library, String searchFile) {
        // attempt to load the found library
        try {
            library.load(runtime, false);
            return true;
        }
        catch (RaiseException ex) {
            if ( ex instanceof Unrescuable || !isJarfileLibrary(library, searchFile) ) throw ex;

            return true;
        }
        catch (JumpException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw runtime.newIOErrorFromException(ex);
        }
        catch (Throwable ex) {
            if ( ex instanceof Unrescuable || !isJarfileLibrary(library, searchFile) ) throwException(ex);

            return true;
        }
    }

    protected void checkEmptyLoad(String file) throws RaiseException {
        if (file.isEmpty()) {
            throw loadFailed(runtime, file);
        }
    }

    protected final void debugLogTry(String what, String msg) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            LOG.info( "trying {}: {}", what, msg );
        }
    }

    protected final void debugLogFound( LoadServiceResource resource ) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            String resourceUrl;
            try {
                resourceUrl = resource.getURL().toString();
            } catch (IOException e) {
                resourceUrl = e.getMessage();
            }
            LOG.info( "found: {}", resourceUrl );
        }
    }

    protected final void debugLogFound(String what, String msg) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            LOG.info( "found {}: {}", what, msg );
        }
    }

    /**
     * Replaces findLibraryBySearchState but split off for require. Needed for OSGiLoadService to override.
     */
    public char searchForRequire(String file, LibrarySearcher.FoundLibrary[] path) {
        return librarySearcher.findLibraryForRequire(file, path);
    }

    /**
     * Replaces findLibraryBySearchState but split off for load. Needed for OSGiLoadService to override.
     */
    protected LibrarySearcher.FoundLibrary searchForLoad(String file) {
        return librarySearcher.findLibraryForLoad(file);
    }

    public boolean featureAlreadyLoaded(String feature) {
        return librarySearcher.featureAlreadyLoaded(feature, null);
    }

    public boolean featureAlreadyLoaded(String feature, String[] loading) {
        return librarySearcher.featureAlreadyLoaded(feature, loading);
    }

    protected LibrarySearcher.FoundLibrary findLibraryWithClassloaders(String baseName, SuffixType suffixType) {
        for (LibrarySearcher.Suffix suffix : suffixType.getSuffixSet()) {
            String file = baseName + suffix;
            LoadServiceResource resource = findFileInClasspath(file);
            if (resource != null) {
                String loadName = resolveLoadName(resource, file);
                // FIXME: use suffix to construct library rather than scanning extensions again
                return new LibrarySearcher.FoundLibrary(baseName, loadName, createLibrary(baseName, loadName, resource));
            }
        }
        return null;
    }

    protected Library createLibrary(String baseName, String loadName, LoadServiceResource resource) {
        if (resource == null) {
            return null;
        }
        String file = resource.getName();
        String location = loadName;
        if (file.endsWith(".so") || file.endsWith(".dll") || file.endsWith(".bundle")) {
            throw runtime.newLoadError("C extensions are not supported, can't load '" + resource.getName() + "'", resource.getName());
        } else if (file.endsWith(".jar")) {
            return new JarredScript(resource, baseName);
        } else if (file.endsWith(".class")) {
            return new JavaCompiledScript(resource);
        } else {
            return new ExternalScript(resource, location);
        }
    }

    // MRI: load_failed
    static RaiseException loadFailed(Ruby runtime, String name) {
        return runtime.newLoadError("cannot load such file -- " + name, name);
    }

    protected String getLoadPathEntry(IRubyObject entry) {
        return RubyFile.get_path(runtime.getCurrentContext(), entry).asJavaString();
    }

    /**
     * this method uses the appropriate lookup strategy to find a file.
     * It is used by Kernel#require.
     *
     * <p>MRI: rb_find_file</p>
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
            // TODO this is really inefficient, and potentially a problem every time anyone require's something.
            // we should try to make LoadPath a special array object.
            String entry = getLoadPathEntry(loadPath.eltInternal(i));

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
    protected static boolean isRequireable(URL loc) {
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

    private static final Pattern URI_PATTERN = Pattern.compile("([a-z]+?://.*)$");
    public LoadServiceResource getClassPathResource(ClassLoader classLoader, String name) {
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

        URL loc;
        Matcher m = URI_PATTERN.matcher( name );
        final String pn;
        if (m.matches()) {
            debugLogTry("fileInClassloader", m.group( 1 ) );
            try
            {
                loc = new URL( m.group( 1 ).replaceAll("([^:])//", "$1/") );
                loc.openStream();
            }
            catch (IOException e)
            {
                loc = null;
            }
        }
        else {
            debugLogTry("fileInClasspath", name);
            try
            {
                loc = classLoader.getResource(name);
            }
            // some classloaders can throw IllegalArgumentException here
            //  	at org.apache.felix.framework.BundleWiringImpl$BundleClassLoader.getResource(BundleWiringImpl.java:2419) ~[org.apache.felix.framework-4.2.1.jar:na]
            //  	at java.lang.ClassLoader.getResource(ClassLoader.java:1142) ~[na:1.7.0_65]
            catch (IllegalArgumentException e)
            {
                loc = null;
            }
        }

        if (loc != null) { // got it
            String path = classpathFilenameFromURL(name, loc, isClasspathScheme);
            LoadServiceResource foundResource = new LoadServiceResource(loc, path);
            debugLogFound(foundResource);
            return foundResource;
        }
        return null;
    }

    /**
     * Given a URL to a classloader resource, build an appropriate load string.
     *
     * @param name the original filename requested
     * @param loc the URL to the resource
     * @param isClasspathScheme whether we're using the classpath: sceheme
     * @return
     */
    public static String classpathFilenameFromURL(String name, URL loc, boolean isClasspathScheme) {
        String path = "classpath:/" + name;
        // special case for typical jar:file URLs, but only if the name didn't have
        // the classpath scheme explicitly
        if (!isClasspathScheme &&
                (loc.getProtocol().equals("jar") || loc.getProtocol().equals("file"))
                && isRequireable(loc)) {
            path = getPath(loc);
            // On windows file: urls converted to names will return /C:/foo from
            // getPath versus C:/foo.  Since getPath is used in a million places
            // putting the newFile.getPath broke some file with-in Jar loading.
            // So I moved it to only this site.
            if (Platform.IS_WINDOWS && loc.getProtocol().equals("file")) {
                path = new File(path).getPath();
            }
        }
        return path;
    }

    protected String resolveLoadName(LoadServiceResource foundResource, String previousPath) {
        String path = foundResource.getAbsolutePath();
        if (Platform.IS_WINDOWS) path = path.replace('\\', '/');
        return path;
    }

    public String getMainScript() {
        return mainScript;
    }

    public String getMainScriptPath() {
        return mainScriptPath;
    }

    public void setMainScript(String filename, String cwd) {
        this.mainScript = filename;
        File mainFile = new File(filename);

        if (!mainFile.isAbsolute()) {
            mainFile = new File(cwd, filename);
        }

        if (mainFile.exists()) {
            this.mainScriptPath = mainFile.getAbsolutePath();
        } else {
            this.mainScriptPath = filename;
        }
    }

    public String getPathForLocation(String filename) {
        if (filename.equals(mainScript)) {
            return mainScriptPath;
        }

        return filename;
    }

    public void tearDown() {
        if (loadedFeatures.isFrozen()) {
            loadedFeatures = new StringArraySet(runtime);
        } else {
            loadedFeatures.clear();
        }
        librarySearcher.tearDown();
    }
}
