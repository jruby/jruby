package org.jruby.runtime.load;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyDir;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.ir.IRScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService.SuffixType;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.URLResource;

class LibrarySearcher {
    private final LoadService loadService;
    private final Ruby runtime;
    private final PathEntry cwdPathEntry;
    private final PathEntry classloaderPathEntry;
    private final PathEntry nullPathEntry;
    private final PathEntry homePathEntry;

    protected ExpandedLoadPath expandedLoadPath;

    public LibrarySearcher(LoadService loadService) {
        this.loadService = loadService;
        this.runtime = loadService.runtime;
        this.cwdPathEntry = new NormalPathEntry(runtime.newString("."));
        this.classloaderPathEntry = new NormalPathEntry(runtime.newString(URLResource.URI_CLASSLOADER));
        this.nullPathEntry = new NullPathEntry();
        this.homePathEntry = new HomePathEntry();
    }

    public List<LibrarySearcher.PathEntry> getExpandedLoadPath() {
        LibrarySearcher.ExpandedLoadPath expandedLoadPath = this.expandedLoadPath;

        if (expandedLoadPath == null || !expandedLoadPath.isCurrent()) {
            expandedLoadPath = this.expandedLoadPath = new ExpandedLoadPath(loadService.loadPath);
        }

        return expandedLoadPath.pathEntries;
    }

    // TODO(ratnikov): Kill this helper once we kill LoadService.SearchState
    public FoundLibrary findBySearchState(LoadService.SearchState state) {
        FoundLibrary lib = findLibrary(state.searchFile, state.suffixType);
        if (lib != null) {
            state.library = lib;
            state.setLoadName(lib.getLoadName());
        }
        return lib;
    }

    public FoundLibrary findLibrary(String baseName, SuffixType suffixType) {
        for (String suffix : suffixType.getSuffixes()) {
            FoundLibrary library = findResourceLibrary(baseName, suffix);

            if (library != null) {
                return library;
            }
        }

        return findServiceLibrary(baseName);
    }

    private FoundLibrary findServiceLibrary(String name) {
        DebugLog.JarExtension.logTry(name);
        Library extensionLibrary = ClassExtensionLibrary.tryFind(runtime, name);
        if (extensionLibrary != null) {
            DebugLog.JarExtension.logFound(name);
            return new FoundLibrary(extensionLibrary, name);
        } else {
            return null;
        }
    }

    private FoundLibrary findResourceLibrary(String baseName, String suffix) {
        if (baseName.startsWith("./")) {
            return nullPathEntry.findFile(baseName, suffix);
        }

        if (baseName.startsWith("../")) {
            // Path should be canonicalized in the findFileResource
            return nullPathEntry.findFile(baseName, suffix);
        }

        if (baseName.startsWith("~/")) {
            return homePathEntry.findFile(baseName.substring(2), suffix);
        }

        // If path is considered absolute, bypass loadPath iteration and load as-is
        if (isAbsolute(baseName)) {
            return nullPathEntry.findFile(baseName, suffix);
        }

        // search the $LOAD_PATH
        for (PathEntry loadPathEntry : getExpandedLoadPath()) {
            FoundLibrary library = loadPathEntry.findFile(baseName, suffix);
            if (library != null) return library;
        }

        // inside a classloader the path "." is the place where to find the jruby kernel
        if (!runtime.getCurrentDirectory().startsWith(URLResource.URI_CLASSLOADER)) {

            // ruby does not load a relative path unless the current working directory is in $LOAD_PATH
            FoundLibrary library = cwdPathEntry.findFile(baseName, suffix);

            // we did not find the file on the $LOAD_PATH but in current directory so we need to treat it
            // as not found (the classloader search below will find it otherwise)
            if (library != null) return null;
        }

        // load the jruby kernel and all resource added to $CLASSPATH
        return classloaderPathEntry.findFile(baseName, suffix);
    }

    private static boolean isAbsolute(String path) {
        return isURI(path) || new File(path).isAbsolute();
    }

    private static boolean isURI(String path) {
        // jar: prefix doesn't mean anything anymore, but we might still encounter it
        if (path.startsWith("jar:")) {
            path = path.substring(4);
        }

        if (path.startsWith("file:")) {
            // We treat any paths with a file schema as absolute, because apparently some tests
            // explicitely depend on such behavior (test/test_load.rb). On other hand, maybe it's
            // not too bad, since otherwise joining LOAD_PATH logic would be more complicated if
            // it'd have to worry about schema.
            return true;
        }
        if (path.startsWith("uri:")) {
            // uri: are absolute
            return true;
        }
        if (path.startsWith("classpath:")) {
            // classpath URLS are always absolute
            return true;
        }

        return false;
    }

    static class FoundLibrary implements Library {
        private final Library delegate;
        private final String loadName;

        public FoundLibrary(Library delegate, String loadName) {
            this.delegate = delegate;
            this.loadName = loadName;
        }

        @Override
        public void load(Ruby runtime, boolean wrap) throws IOException {
            delegate.load(runtime, wrap);
        }

        public String getLoadName() {
            return loadName;
        }
    }

    static class ResourceLibrary implements Library {
        public static ResourceLibrary create(String searchName, String scriptName, FileResource resource) {
            String location = resource.absolutePath();

            if (location.endsWith(".class")) return new ClassResourceLibrary(searchName, scriptName, resource);
            if (location.endsWith(".jar")) return new JarResourceLibrary(searchName, scriptName, resource);

            return new ResourceLibrary(searchName, scriptName, resource); // just .rb?
        }

        protected final String searchName;
        protected final String scriptName;
        protected final FileResource resource;
        protected final String location;

        public ResourceLibrary(String searchName, String scriptName, FileResource resource) {
            this.searchName = searchName;
            this.scriptName = scriptName;
            this.location = resource.absolutePath();
            this.resource = resource;
        }

        @Override
        public void load(Ruby runtime, boolean wrap) {
            InputStream ris = null;
            try {
                ris = resource.inputStream();

                if (runtime.getInstanceConfig().getCompileMode().shouldPrecompileAll()) {
                    runtime.compileAndLoadFile(scriptName, ris, wrap);
                } else {
                    runtime.loadFile(scriptName, new LoadServiceResourceInputStream(ris), wrap);
                }
            } catch(IOException e) {
                throw runtime.newLoadError("no such file to load -- " + searchName, searchName);
            } finally {
                try {
                    if (ris != null) ris.close();
                } catch (IOException ioE) { /* At least we tried.... */}
            }
        }
    }

    static class ClassResourceLibrary extends ResourceLibrary {
        public ClassResourceLibrary(String searchName, String scriptName, FileResource resource) {
            super(searchName, scriptName, resource);
        }

        @Override
        public void load(Ruby runtime, boolean wrap) {
            InputStream is = null;
            try {
                is = new BufferedInputStream(resource.inputStream(), 32768);
                IRScope script = CompiledScriptLoader.loadScriptFromFile(runtime, is, null, scriptName, false);

                // Depending on the side-effect of the load, which loads the class but does not turn it into a script.
                // I don't like it, but until we restructure the code a bit more, we'll need to quietly let it by here.
                if (script == null) return;

                script.setFileName(scriptName);
                runtime.loadScope(script, wrap);
            } catch(IOException e) {
                throw runtime.newLoadError("no such file to load -- " + searchName, searchName);
            } finally {
                try {
                    if (is != null) is.close();
                } catch (IOException ioE) { /* At least we tried.... */ }
            }
        }
    }

    static class JarResourceLibrary extends ResourceLibrary {
        public JarResourceLibrary(String searchName, String scriptName, FileResource resource) {
            super(searchName, scriptName, resource);
        }

        @Override
        public void load(Ruby runtime, boolean wrap) {
            try {
                URL url;
                if (location.startsWith(URLResource.URI)) {
                    url = URLResource.getResourceURL(runtime, location);
                } else {
                    // convert file urls with !/ into jar urls so the classloader
                    // can handle them via protocol handler
                    File f = new File(location);
                    if (f.exists() || location.contains( "!")){
                        url = f.toURI().toURL();
                        if (location.contains( "!")) {
                            url = new URL( "jar:" + url );
                        }
                    } else {
                        url = new URL(location);
                    }
                }
                runtime.getJRubyClassLoader().addURL(url);
            }
            catch (MalformedURLException badUrl) {
                throw runtime.newIOErrorFromException(badUrl);
            }

            // If an associated Service library exists, load it as well
            ClassExtensionLibrary serviceExtension = ClassExtensionLibrary.tryFind(runtime, searchName);
            if (serviceExtension != null) {
                serviceExtension.load(runtime, wrap);
            }
        }
    }

    class ExpandedLoadPath {
        final RubyArray loadPath;
        final RubyArray loadPathSnapshot;
        final List<PathEntry> pathEntries;

        ExpandedLoadPath(RubyArray loadPath) {
            this.loadPath = loadPath;

            RubyArray loadPathSnapshot = loadPath.aryDup();
            List<PathEntry> pathEntries = new ArrayList<>(loadPathSnapshot.size());

            for (int i = 0; i < loadPathSnapshot.size(); i++) {
                IRubyObject path = loadPathSnapshot.eltOk(i);

                pathEntries.add(new NormalPathEntry(path));
            }
            this.loadPathSnapshot = loadPathSnapshot;
            this.pathEntries = pathEntries;
        }

        boolean isCurrent() {
            return loadPath.toJavaArrayUnsafe() == loadPathSnapshot.toJavaArrayUnsafe();
        }
    }

    abstract class PathEntry {
        protected FoundLibrary findFile(String searchName, String suffix) {
            Ruby runtime = LibrarySearcher.this.runtime;

            FileResource fullPath = fullPath(searchName, suffix);

            // Can't determine a full path for this entry, return no result
            if (fullPath == null) {
                return null;
            }

            if (fullPath.exists()) {
                if (fullPath.absolutePath() != fullPath.canonicalPath()) {
                    FileResource expandedResource = JRubyFile.createResourceAsFile(runtime, fullPath.canonicalPath());

                    if (expandedResource.exists()){
                        String expandedAbsolute = expandedResource.absolutePath();

                        DebugLog.Resource.logFound(fullPath);

                        return new FoundLibrary(ResourceLibrary.create(searchName, expandedAbsolute, fullPath), expandedAbsolute);
                    }
                }

                DebugLog.Resource.logFound(fullPath);

                String scriptName = fullPath.absolutePath();
                String loadName = fullPath.absolutePath();

                return new FoundLibrary(ResourceLibrary.create(searchName, scriptName, fullPath), loadName);
            }

            return null;
        }

        protected abstract FileResource fullPath(String searchName, String suffix);
    }

    class NormalPathEntry extends PathEntry {
        final IRubyObject path;
        final boolean cacheExpanded;
        FileResource expanded;

        NormalPathEntry(IRubyObject path) {
            this.path = path;
            this.cacheExpanded = isCachable(runtime, path);
        }

        protected FileResource fullPath(String searchName, String suffix) {
            FileResource loadPath = expandPathCached();

            String fullPath = loadPath.path() + "/" + searchName + suffix;

            DebugLog.Resource.logTry(fullPath);

            return JRubyFile.createResourceAsFile(runtime, fullPath);
        }

        private FileResource expandPathCached() {
            if (cacheExpanded) {
                FileResource expanded = this.expanded;
                if (expanded != null) return expanded;

                return this.expanded = expandPath();
            }

            return expandPath();
        }

        private FileResource expandPath() {
            FileResource resource = JRubyFile.createResourceAsFile(runtime, Helpers.javaStringFromPath(runtime, path));

            return JRubyFile.createResourceAsFile(runtime, resource.canonicalPath());
        }

        private boolean isCachable(Ruby runtime, IRubyObject path) {
            if (!(path instanceof RubyString)) return false;

            String pathAsString = path.asJavaString();

            if (pathAsString.length() == 0) return false;

            if (isURI(pathAsString)) return false;

            FileResource resource = JRubyFile.createResourceAsFile(runtime, pathAsString);

            return resource.isDirectory() && resource.path() == resource.canonicalPath();
        }
    }

    class HomePathEntry extends PathEntry {
        protected FileResource fullPath(String searchName, String suffix) {
            Optional<String> home = RubyDir.getHomeFromEnv(runtime);

            // FIXME: Ick. See #5661
            if (!home.isPresent()) return null;

            String fullPath = home.get();

            DebugLog.Resource.logTry(fullPath);

            return JRubyFile.createResourceAsFile(runtime, fullPath + "/" + searchName + suffix);
        }
    }

    class NullPathEntry extends PathEntry {
        protected FileResource fullPath(String searchName, String suffix) {
            return JRubyFile.createResourceAsFile(runtime, searchName + suffix);
        }
    }
}
