package org.jruby.runtime.load;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyFile;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.ir.IRScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService.SuffixType;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.URLResource;

class LibrarySearcher {
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

    private final LoadService loadService;
    private final Ruby runtime;
    private final Map<String, Library> builtinLibraries;

    public LibrarySearcher(LoadService loadService) {
        this.loadService = loadService;
        this.runtime = loadService.runtime;
        this.builtinLibraries = loadService.builtinLibraries;
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
            FoundLibrary library = findBuiltinLibrary(baseName, suffix);
            if (library == null) library = findResourceLibrary(baseName, suffix);

            if (library != null) {
                return library;
            }
        }

        return findServiceLibrary(baseName);
    }

    private FoundLibrary findBuiltinLibrary(String name, String suffix) {
        String namePlusSuffix = name + suffix;

        DebugLog.Builtin.logTry(namePlusSuffix);
        if (builtinLibraries.containsKey(namePlusSuffix)) {
            DebugLog.Builtin.logFound(namePlusSuffix);
            return new FoundLibrary(
                    builtinLibraries.get(namePlusSuffix),
                    namePlusSuffix);
        }
        return null;
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
            return findFileResource(baseName, suffix);
        }

        if (baseName.startsWith("../")) {
            // Path should be canonicalized in the findFileResource
            return findFileResource(baseName, suffix);
        }

        if (baseName.startsWith("~/")) {
            RubyHash env = (RubyHash) runtime.getObject().getConstant("ENV");
            RubyString env_home = runtime.newString("HOME");
            if (env.has_key_p(env_home).isFalse()) {
                return null;
            }
            String home = env.op_aref(runtime.getCurrentContext(), env_home).toString();
            String path = home + "/" + baseName.substring(2);

            return findFileResource(path, suffix);
        }

        // If path is considered absolute, bypass loadPath iteration and load as-is
        if (isAbsolute(baseName)) {
          return findFileResource(baseName, suffix);
        }

        // search the $LOAD_PATH
        try {
            for (IRubyObject loadPathEntry : loadService.loadPath.toJavaArray()) {
                FoundLibrary library = findFileResourceWithLoadPath(baseName, suffix, getPath(loadPathEntry));
                if (library != null) return library;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // inside a classloader the path "." is the place where to find the jruby kernel
        if (!runtime.getCurrentDirectory().startsWith(URLResource.URI_CLASSLOADER)) {

            // ruby does not load a relative path unless the current working directory is in $LOAD_PATH
            FoundLibrary library = findFileResourceWithLoadPath(baseName, suffix, ".");

            // we did not find the file on the $LOAD_PATH but in current directory so we need to treat it
            // as not found (the classloader search below will find it otherwise)
            if (library != null) return null;
        }

        // load the jruby kernel and all resource added to $CLASSPATH
        return findFileResourceWithLoadPath(baseName, suffix, URLResource.URI_CLASSLOADER);
    }

    // FIXME: to_path should not be called n times it should only be once and that means a cache which would
    // also reduce all this casting and/or string creates.
    // (mkristian) would it make sense to turn $LOAD_PATH into something like RubyClassPathVariable where we could cache
    // the Strings ?
    private String getPath(IRubyObject loadPathEntry) {
        return RubyFile.get_path(runtime.getCurrentContext(), loadPathEntry).asJavaString();
    }

    private FoundLibrary findFileResource(String searchName, String suffix) {
        return findFileResourceWithLoadPath(searchName, suffix, null);
    }

    private FoundLibrary findFileResourceWithLoadPath(String searchName, String suffix, String loadPath) {
        String fullPath = loadPath != null ? loadPath + "/" + searchName : searchName;
        String pathWithSuffix = fullPath + suffix;

        DebugLog.Resource.logTry(pathWithSuffix);
        FileResource resource = JRubyFile.createResourceAsFile(runtime, pathWithSuffix);
        if (resource.exists()) {
            DebugLog.Resource.logFound(pathWithSuffix);
            String scriptName = resolveScriptName(resource, pathWithSuffix);
            String loadName = resolveLoadName(resource, searchName + suffix);

            return new FoundLibrary(ResourceLibrary.create(searchName, scriptName, resource), loadName);
        }

        return null;
    }

    private static boolean isAbsolute(String path) {
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
        return new File(path).isAbsolute();
    }

    protected String resolveLoadName(FileResource resource, String ruby18path) {
        return resource.absolutePath();
    }

    protected String resolveScriptName(FileResource resource, String ruby18Path) {
        return resource.absolutePath();
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
                runtime.loadFile(scriptName, new LoadServiceResourceInputStream(ris), wrap);
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
}
