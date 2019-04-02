package org.jruby.runtime.load;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.regex.Matcher;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyDir;
import org.jruby.RubyFile;
import org.jruby.RubyString;
import org.jruby.ir.IRScope;
import org.jruby.platform.Platform;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService.SuffixType;
import org.jruby.util.ByteList;
import org.jruby.util.FileResource;
import org.jruby.util.JRubyFile;
import org.jruby.util.URLResource;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.IntList;
import org.jruby.util.collections.StringArraySet;

public class LibrarySearcher {
    private final LoadService loadService;
    private final Ruby runtime;
    private final PathEntry cwdPathEntry;
    private final PathEntry classloaderPathEntry;
    private final PathEntry nullPathEntry;
    private final PathEntry homePathEntry;

    protected ExpandedLoadPath expandedLoadPath;

    protected RubyArray loadedFeaturesSnapshot;
    private final Map<String, Feature> loadedFeaturesIndex = new ConcurrentHashMap<>(64);

    public LibrarySearcher(LoadService loadService) {
        this.loadService = loadService;
        this.runtime = loadService.runtime;
        this.cwdPathEntry = new NormalPathEntry(runtime.newString("."));
        this.classloaderPathEntry = new NormalPathEntry(runtime.newString(URLResource.URI_CLASSLOADER));
        this.nullPathEntry = new NullPathEntry();
        this.homePathEntry = new HomePathEntry();
        this.loadedFeaturesSnapshot = runtime.newArray();
    }

    public List<LibrarySearcher.PathEntry> getExpandedLoadPath() {
        LibrarySearcher.ExpandedLoadPath expandedLoadPath = this.expandedLoadPath;

        if (expandedLoadPath == null || !expandedLoadPath.isCurrent()) {
            expandedLoadPath = this.expandedLoadPath = new ExpandedLoadPath(loadService.loadPath);
        }

        return expandedLoadPath.pathEntries;
    }

    @Deprecated
    public FoundLibrary findBySearchState(LoadService.SearchState state) {
        FoundLibrary[] lib = {null};
        char found = findLibraryForRequire(state.searchFile, lib);
        if (found != 0) {
            state.searchFile = lib[0].searchName;
            state.library = lib[0];
            state.setLoadName(lib[0].getLoadName());
        }
        return lib[0];
    }

    // MRI: search_required
    public synchronized char findLibraryForRequire(String file, FoundLibrary[] path) {
        // check loaded features
        FoundLibrary tmp;
        int ext, ftptr;
        char type, ft = 0;
        String[] loading = {null};

        path[0] = null;

        ext = file.lastIndexOf('.');
        if (ext != -1 && file.indexOf('/', ext) == -1) {
            if (isSourceExt(file)) {
                if (isFeature(file, ext, true, false, loading) != 0) {
                    if (loading[0] != null) path[0] = new FoundLibrary(file, loading[0], Suffix.RUBY.constructLibrary(file, loading[0], JRubyFile.createResourceAsFile(runtime, loading[0])));
                    return 'r';
                }
                if ((tmp = findLibrary(file.substring(0, ext), SuffixType.Source)) != null) {
                    String tmpPath = tmp.loadName;
                    ext = tmpPath.lastIndexOf('.');
                    if (isFeature(tmpPath, ext, true, true, loading) == 0 || loading[0] != null)
                        path[0] = tmp;
                    return 'r';
                }
                return 0;
            } else if (isLibraryExt(file)) {
                if (isFeature(file, ext, true, false, loading) != 0) {
                    if (loading[0] != null) path[0] = new FoundLibrary(file, loading[0], Suffix.JAR.constructLibrary(file, loading[0], JRubyFile.createResourceAsFile(runtime, loading[0])));
                    return 's';
                }
                if ((tmp = findLibrary(file.substring(0, ext), SuffixType.Extension)) != null) {
                    ext = tmp.loadName.lastIndexOf('.');
                    if (isFeature(tmp.loadName, ext, false, true, loading) == 0 || loading[0] != null) {
                        path[0] = tmp;
                    }
                    return 's';
                }
            }
        } else if ((ft = isFeature(file, -1, false, false, loading)) == 'r') {
            if (loading[0] != null) path[0] = new FoundLibrary(file, loading[0], Suffix.RUBY.constructLibrary(file, loading[0], JRubyFile.createResourceAsFile(runtime, loading[0])));
            return 'r';
        }

        tmp = findLibrary(file, SuffixType.Both);

        // CRuby: above call is rb_find_file_ext_safe and returns 1 for .rb found or >1 for some ext extension.
        if (tmp == null) {
            if (ft != 0) { // from extensionless search above
                if (loading[0] != null) path[0] = tmp;
                return ft;
            }
            return isFeature(file, -1, false, true, null);
        }

        // File was found, do a final check to see if it was loaded already
        boolean rb = true;
        if (tmp.loadName.endsWith(".jar")) {
            if (ft != 0) { // from extensionless search above
                if (loading[0] != null) path[0] = tmp;
                return ft;
            }
            rb = false;
        }
        ext = tmp.loadName.lastIndexOf('.');
        if (isFeature(tmp.loadName, ext, rb, true, loading) != 0 && loading[0] == null) {
            // Found in features but not in currently-loading features
        } else {
            // Not found in loaded features but did show up in currently-loading
            path[0] = tmp;
        }

        // If it's not a jar, it's Ruby source
        return tmp.loadName.endsWith(".jar") ? 's' : 'r';
    }

    public LibrarySearcher.FoundLibrary findLibraryForLoad(String file) {
        // Determine suffix type and get base file name out
        String[] fileHolder = {file};
        SuffixType suffixType = getSuffixTypeForLoad(fileHolder);
        String baseName = fileHolder[0];

        return findLibrary(baseName, suffixType);
    }

    public LibrarySearcher.FoundLibrary findLibrary(String baseName, SuffixType suffixType) {
        for (Suffix suffix : suffixType.getSuffixSet()) {
            FoundLibrary library = findResourceLibrary(baseName, suffix);

            if (library != null) {
                return library;
            }
        }

        return findServiceLibrary(baseName);
    }

    public static SuffixType getSuffixTypeForRequire(String[] fileHolder) {
        SuffixType suffixType;
        String file = fileHolder[0];

        // FIXME: Does this matter? We pass this through various path-normalizing calls elsewhere
        if (Platform.IS_WINDOWS) {
            file = file.replace('\\', '/');
        }

        int lastDot = file.lastIndexOf('.');

        if (lastDot != -1 && file.indexOf('/', lastDot) == -1) {
            if (isSourceExt(file)) {
                // source extensions
                suffixType = SuffixType.Source;

                // trim extension to try other options
                fileHolder[0] = file.substring(0, lastDot);
            } else if (isLibraryExt(file)) {

                // extension extensions
                suffixType = SuffixType.Extension;

                // trim extension to try other options
                fileHolder[0] = file.substring(0, lastDot);
            } else if (file.endsWith(".class")) {
                // For JRUBY-6731, treat require 'foo.class' as no other filename than 'foo.class'.
                suffixType = SuffixType.Neither;

            } else {
                // unknown extension, fall back to search with extensions
                suffixType = SuffixType.Both;
            }
        } else {
            // try all extensions
            suffixType = SuffixType.Both;
        }

        return suffixType;
    }

    public static SuffixType getSuffixTypeForLoad(final String[] fileHolder) {
        SuffixType suffixType;
        String file = fileHolder[0];

        int lastDot = file.lastIndexOf('.');

        if (lastDot != -1 && file.indexOf('/', lastDot) == -1) {
            // if a source extension is specified, try all source extensions
            Matcher matcher;
            if (isSourceExt(file)) {
                // source extensions
                suffixType = SuffixType.Source;

                // trim extension to try other options
                fileHolder[0] = file.substring(0, lastDot);
            } else {
                // unknown extension or .so/.jar, fall back to exact search
                suffixType = SuffixType.Neither;
            }
        } else {
            // try only literal search
            suffixType = SuffixType.Neither;
        }

        return suffixType;
    }

    public static boolean isSourceExt(String file) {
        return file.endsWith(".rb");
    }

    public static boolean isLibraryExt(String file) {
        return file.endsWith(".so") // Even if we don't support .so, some stdlib require .so directly.
                || file.endsWith(".o") // Matching MRI default extension search
                || file.endsWith(".jar");
    }

    public boolean featureAlreadyLoaded(String feature, String[] loading) {
        int ext = feature.lastIndexOf('.');

        if (feature.charAt(0) == '.' &&
                (feature.charAt(1) == '/' || feature.regionMatches(1, "./", 0, 2))) {
            feature = RubyFile.expand_path(runtime.getCurrentContext(), runtime.getFile(), runtime.newString(feature)).asJavaString();
        }
        if (ext != -1 && feature.indexOf('/', ext) == -1) {
            if (LibrarySearcher.isSourceExt(feature)) {
                if (isFeature(feature, ext, true, false, loading) != 0) return true;
                return false;
            }
            else if (LibrarySearcher.isLibraryExt(feature)) {
                if (isFeature(feature, ext, false, false, loading) != 0) return true;
                return false;
            }
        }
        if (isFeature(feature, -1, true, false, loading) != 0)
            return true;
        return false;
    }

    protected void provideFeature(RubyString name) {
        StringArraySet loadedFeatures = this.loadService.loadedFeatures;

        if (loadedFeatures.isFrozen()) {
            throw runtime.newRuntimeError("$LOADED_FEATURES is frozen; cannot append feature");
        }

        name.setFrozen(true);

        loadedFeatures.append(name);

        snapshotLoadedFeatures();
        addFeatureToIndex(name, loadedFeatures.size() - 1);
    }

    protected RubyArray snapshotLoadedFeatures() {
        return (RubyArray) loadedFeaturesSnapshot.replace(this.loadService.loadedFeatures);
    }

    protected void addFeatureToIndex(RubyString name, int offset) {
        ByteList featureByteList = name.getByteList();
        byte[] featureBytes = featureByteList.unsafeBytes();
        int featureBegin = featureByteList.begin();
        int featureEnd = featureBegin + featureByteList.length();

        int ext, p;

        for (ext = featureEnd - 1; ext > featureBegin; ext--)
            if (featureBytes[ext] == '.' || featureBytes[ext] == '/') break;

        if (featureBytes[ext] != '.') ext = -1;

        /* Now `ext` points to the only string matching %r{^\.[^./]*$} that is
           at the end of `feature`, or is NULL if there is no such string. */

        p = ext != -1 ? ext : featureEnd;
        while (true) {
            p--;
            while (p >= featureBegin && featureBytes[p] != '/')
                p--;
            if (p < featureBegin) break;
            /* Now *p == '/'.  We reach this point for every '/' in `feature`. */
            addSingleFeatureToIndex(new String(featureBytes, p + 1, featureEnd - p - 1), offset);

            if (ext != -1) {
                addSingleFeatureToIndex(new String(featureBytes, p + 1, ext - p - 1), offset);
            }
        }
        addSingleFeatureToIndex(new String(featureBytes, featureBegin, featureEnd - featureBegin), offset);
        if (ext != -1) {
            addSingleFeatureToIndex(new String(featureBytes, featureBegin, ext - featureBegin), offset);
        }
    }

    static class Feature {
        Feature(String key, int offset) {
            this.key = key;
            this.indices = new IntList(2);
            indices.add(offset);
        }

        void addOffset(int offset) {
            indices.add(offset);
        }

        public void clear() {
            indices.clear();
        }

        String key;
        IntList indices;
    }

    protected void addSingleFeatureToIndex(String key, int offset) {
        Map<String, Feature> featuresIndex = this.loadedFeaturesIndex;

        Feature thisFeature = featuresIndex.get(key);

        if (thisFeature == null) {
            featuresIndex.put(key, new Feature(key, offset));
        } else {
            thisFeature.addOffset(offset);
        }
    }

    protected char isFeature(String feature, int ext, boolean rb, boolean expanded, String[] fn) {
        StringArraySet features;

        Feature thisFeature = null;
        String featureString;
        String p;
        List<LibrarySearcher.PathEntry> loadPath = null;

        int f, e;
        final int len, elen;

        Map<String, ReentrantLock> loadingTable;
        Map<String, Feature> featuresIndex;
        final LibrarySearcher.Suffix type;

        if (fn != null) fn[0] = null;
        if (ext != -1) {
            elen = feature.length() - ext;
            len = feature.length() - elen;
            type = rb ? LibrarySearcher.Suffix.RUBY : LibrarySearcher.Suffix.JAR;
        }
        else {
            len = feature.length();
            elen = 0;
            type = null;
        }

        features = this.loadService.loadedFeatures;
        featuresIndex = getLoadedFeaturesIndex();

        thisFeature = featuresIndex.get(feature);

        /* We search `features` for an entry such that either
             "#{features[i]}" == "#{load_path[j]}/#{feature}#{e}"
           for some j, or
             "#{features[i]}" == "#{feature}#{e}"
           Here `e` is an "allowed" extension -- either empty or one
           of the extensions accepted by IS_RBEXT, IS_SOEXT, or
           IS_DLEXT.  Further, if `ext && rb` then `IS_RBEXT(e)`,
           and if `ext && !rb` then `IS_SOEXT(e) || IS_DLEXT(e)`.

           If `expanded`, then only the latter form (without load_path[j])
           is accepted.  Otherwise either form is accepted, *unless* `ext`
           is false and an otherwise-matching entry of the first form is
           preceded by an entry of the form
             "#{features[i2]}" == "#{load_path[j2]}/#{feature}#{e2}"
           where `e2` matches %r{^\.[^./]*$} but is not an allowed extension.
           After a "distractor" entry of this form, only entries of the
           form "#{feature}#{e}" are accepted.

           In `rb_provide_feature()` and `get_loaded_features_index()` we
           maintain an invariant that the array `this_feature_index` will
           point to every entry in `features` which has the form
             "#{prefix}#{feature}#{e}"
           where `e` is empty or matches %r{^\.[^./]*$}, and `prefix` is empty
           or ends in '/'.  This includes both match forms above, as well
           as any distractors, so we may ignore all other entries in `features`.
         */
        if (thisFeature != null) {
            for (int i = 0; i < thisFeature.indices.size(); i++) {
                int index = thisFeature.indices.get(i);
                RubyString featureRuby = features.eltOk(index).convertToString();
                f = 0;
                if (featureRuby.length() < len) continue;
                featureString = featureRuby.asJavaString();
                if (!featureString.regionMatches(0, feature, 0, len)) {
                    if (expanded) continue;
                    loadPath = lazyLoadPath(loadPath);
                    if ((p = loadedFeatureWithPath(featureString, feature, type, loadPath)) == null) {
                        continue;
                    }
                    expanded = true;
                    f += p.length() + 1;
                }
                if ((e = f + len) == featureString.length()) {
                    if (ext != -1) continue;
                    return 'u';
                }
                if (featureString.charAt(e) != '.') continue;
                if ((!rb || ext == -1) && (featureString.endsWith(".jar"))) {
                    return 's';
                }
                if ((rb || ext == -1) && (featureString.endsWith(".rb"))) {
                    return 'r';
                }
            }
        }

        // Check load locks to see if another thread is currently loading this file
        loadingTable = this.loadService.requireLocks.pool;
        if (!expanded) {
            loadPath = lazyLoadPath(loadPath);
            for (Map.Entry<String, ReentrantLock> entry : loadingTable.entrySet()) {
                if (loadedFeatureWithPath(entry.getKey(), feature, type, loadPath) != null) {
                    if (fn != null) fn[0] = entry.getKey();
                    if (ext == -1) return 'u';
                    return !feature.endsWith(".rb") ? 's' : 'r';
                }
            }
        }

        if (loadingTable.containsKey(feature)) {
            // FIXME: use key from the actual table?
            if (fn != null) fn[0] = feature;
            if (ext == -1) return 'u';
            return !feature.endsWith(".rb") ? 's' : 'r';
        }

        if (ext != -1 && ext == feature.length()) return 0;
        // FIXME: using Iterator
        String baseName = feature.substring(0, len);
        for (LibrarySearcher.Suffix suffix : Suffix.ALL) {
            String withExt = suffix.forTarget(baseName);
            if (loadingTable.containsKey(withExt)) {
                if (fn != null) fn[0] = withExt;
                return suffix != LibrarySearcher.Suffix.RUBY ? 's' : 'r';
            }
        }

        return 0;
    }

    private List<LibrarySearcher.PathEntry> lazyLoadPath(List<LibrarySearcher.PathEntry> loadPath) {
        return (loadPath == null) ? getExpandedLoadPath() : loadPath;
    }

    Map<String, Feature> getLoadedFeaturesIndex() {
        StringArraySet loadedFeatures = this.loadService.loadedFeatures;
        if (loadedFeaturesSnapshot.toJavaArrayUnsafe() != loadedFeatures.toJavaArrayUnsafe()) {
            /* The sharing was broken; something (other than us in rb_provide_feature())
               modified loaded_features.  Rebuild the index. */
            loadedFeaturesIndex.forEach((name, feature) -> feature.clear());
            RubyArray features = snapshotLoadedFeatures();
            for (int i = 0; i < features.size(); i++) {
                IRubyObject entry;
                entry = features.eltOk(i);
                RubyString asStr = runtime.freezeAndDedupString(entry.convertToString());
                if (asStr != entry)
                    features.eltSetOk(i, asStr);
                addFeatureToIndex(asStr, i);
            }
        }
        return loadedFeaturesIndex;
    }

    /* This searches `load_path` for a value such that
         name == "#{load_path[i]}/#{feature}"
       if `feature` is a suffix of `name`, or otherwise
         name == "#{load_path[i]}/#{feature}#{ext}"
       for an acceptable string `ext`.  It returns
       `load_path[i].to_str` if found, else 0.

       If type is 's', then `ext` is acceptable only if IS_DLEXT(ext);
       if 'r', then only if IS_RBEXT(ext); otherwise `ext` may be absent
       or have any value matching `%r{^\.[^./]*$}`.
    */
    String loadedFeatureWithPath(String name, String feature,
                                 LibrarySearcher.Suffix type, List<LibrarySearcher.PathEntry> loadPath) {
        final int nameLength = name.length();
        final int featureLength = feature.length();

        int plen;

        if (nameLength <= featureLength) return null;
        if (feature.indexOf('.') != -1 && name.endsWith(feature)) {
            plen = nameLength - featureLength;
        }
        else {
            int e;
            for (e = nameLength - 1; e >= 0 && name.charAt(e) != '.' && name.charAt(e) != '/'; --e);
            if (name.charAt(e) != '.' ||
                    e < featureLength ||
                    !name.regionMatches(e - featureLength, feature, 0, featureLength))
                return null;
            plen = e - featureLength;
        }
        if (plen > 0 && name.charAt(plen-1) != '/') {
            return null;
        }
        if (
                (type == LibrarySearcher.Suffix.JAR && !LibrarySearcher.isLibraryExt(name.substring(plen+featureLength))) ||
                        (type == LibrarySearcher.Suffix.RUBY && !LibrarySearcher.isSourceExt(name.substring(plen+featureLength)))) {

            return null;
        }

        /* Now name == "#{prefix}/#{feature}#{ext}" where ext is acceptable
           (possibly empty) and prefix is some string of length plen. */

        if (plen > 0) --plen;	/* exclude '.' */
        for (int i = 0; i < loadPath.size(); ++i) {
            LibrarySearcher.PathEntry pathEntry = loadPath.get(i);
            String path = pathEntry.path();

            int n = path.length();

            if (n != plen) continue;
            if (n > 0 && !name.startsWith(path)) {
                continue;
            }

            return path;
        }
        return null;
    }

    private FoundLibrary findServiceLibrary(String name) {
        DebugLog.JarExtension.logTry(name);
        Library extensionLibrary = ClassExtensionLibrary.tryFind(runtime, name);
        if (extensionLibrary != null) {
            DebugLog.JarExtension.logFound(name);
            return new FoundLibrary(name, name, extensionLibrary);
        } else {
            return null;
        }
    }

    private FoundLibrary findResourceLibrary(String baseName, Suffix suffix) {
        if (baseName.startsWith("./") || baseName.startsWith("../") || isAbsolute(baseName)) {
            // Path should be canonicalized in the findFileResource
            return nullPathEntry.findFile(baseName, suffix);
        }

        if (baseName.startsWith("~/")) {
            return homePathEntry.findFile(baseName.substring(2), suffix);
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

    enum Suffix {
        RUBY(".rb", ResourceLibrary::new),
        CLASS(".class", ClassResourceLibrary::new),
        JAR(".jar", JarResourceLibrary::new);

        // If .class is enabled, we still search for .rb first
        static final EnumSet<Suffix> SOURCES =
                Options.AOT_LOADCLASSES.load() ?
                        EnumSet.of(RUBY, CLASS) :
                        EnumSet.of(RUBY);
        static final EnumSet<Suffix> EXTENSIONS = EnumSet.of(JAR);
        static final EnumSet<Suffix> ALL =
                Options.AOT_LOADCLASSES.load() ?
                        EnumSet.of(RUBY, CLASS, JAR) :
                        EnumSet.of(RUBY, JAR);

        private final String extension;
        private final byte[] extensionBytes;
        private final TriFunction<String, String, FileResource, Library> libraryFactory;

        Suffix(String extension, TriFunction<String, String, FileResource, Library> libraryFactory) {
            this.extension = extension;
            this.extensionBytes = extension.getBytes();
            this.libraryFactory = libraryFactory;
        }

        public Library constructLibrary(String target, String name, FileResource fullPath) {
            return libraryFactory.apply(target, name, fullPath);
        }

        public String forTarget(String targetName) {
            return targetName + extension;
        }

        public ByteList forTarget(ByteList targetName) {
            ByteList dup = targetName.shallowDup();
            dup.append(extensionBytes);
            return dup;
        }

        public static Suffix forString(String withSuffix) {
            int last = withSuffix.lastIndexOf('.');

            if (last > -1) {
                switch (withSuffix.substring(last)) {
                    case ".rb":
                        return RUBY;
                    case ".jar":
                        return JAR;
                    case ".class":
                        return CLASS;
                }
            }

            throw new RuntimeException("invalid suffix in LoadService (missing '.'?): " + withSuffix);
        }
    }

    public static class FoundLibrary implements Library {
        private final Library delegate;
        private final String searchName;
        private final String loadName;

        public FoundLibrary(String searchName, String loadName, Library delegate) {
            this.searchName = searchName;
            this.loadName = loadName;
            this.delegate = delegate;
        }

        @Override
        public void load(Ruby runtime, boolean wrap) throws IOException {
            delegate.load(runtime, wrap);
        }

        public String getLoadName() {
            return loadName;
        }

        public String getSearchName() {
            return searchName;
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
                ris = resource.openInputStream();

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
                is = new BufferedInputStream(resource.openInputStream(), 32768);
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

    interface TriFunction<T, U, V, R> {
        default <W> TriFunction<T, U, V, W> andThen(Function<? super R,? extends W> after) {
            return (t, u, v) -> after.apply(apply(t, u, v));
        }
        R apply(T t, U u, V v);
    }

    abstract class PathEntry {
        protected FoundLibrary findFile(String target, Suffix suffix) {
            Ruby runtime = LibrarySearcher.this.runtime;

            FileResource fullPath = fullPath(target, suffix);

            // Can't determine a full path for this entry, return no result
            if (fullPath == null) {
                return null;
            }

            if (fullPath.exists()) {
                String canonicalPath = fullPath.canonicalPath();
                String absolutePath = fullPath.absolutePath();
                if (!absolutePath.equals(canonicalPath)) {
                    FileResource expandedResource = JRubyFile.createResourceAsFile(runtime, canonicalPath);

                    if (expandedResource.exists()){
                        String expandedAbsolute = expandedResource.absolutePath();

                        DebugLog.Resource.logFound(fullPath);

                        return new FoundLibrary(target, expandedAbsolute, suffix.constructLibrary(target, expandedAbsolute, fullPath));
                    }
                }

                DebugLog.Resource.logFound(fullPath);

                String resolvedName = absolutePath;

                return new FoundLibrary(target, resolvedName, suffix.constructLibrary(target, resolvedName, fullPath));
            }

            return null;
        }

        protected abstract String path();

        protected abstract FileResource fullPath(String searchName, Suffix suffix);
    }

    class NormalPathEntry extends PathEntry {
        final IRubyObject path;
        final boolean cacheExpanded;
        FileResource expanded;
        ByteList bytes;

        NormalPathEntry(IRubyObject path) {
            this.path = path;
            this.cacheExpanded = isCachable(runtime, path);
        }

        protected String path() {
            return expandPathCached().path();
        }

        protected ByteList pathBytes() {
            expandPathCached();

            return bytes;
        }

        protected FileResource fullPath(String searchFile, Suffix suffix) {
            FileResource loadPath = expandPathCached();

            String fullPath = loadPath.path() + "/"
                    + (suffix == null ? searchFile : suffix.forTarget(searchFile));

            DebugLog.Resource.logTry(fullPath);

            return JRubyFile.createResourceAsFile(runtime, fullPath);
        }

        private FileResource expandPathCached() {
            if (cacheExpanded) {
                FileResource expanded = this.expanded;
                if (expanded != null) return expanded;

                expanded = this.expanded = expandPath();
                this.bytes = new ByteList(ByteList.plain(expanded.path()));

                return expanded;
            }

            FileResource expanded = expandPath();
            bytes = null;

            return expanded;
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
        protected String path() {
            return resolveHome();
        }

        protected ByteList pathBytes() {
            return new ByteList(ByteList.plain(path()));
        }

        protected FileResource fullPath(String searchFile, Suffix suffix) {
            String fullPath = resolveHome();

            if (fullPath == null) return null;

            DebugLog.Resource.logTry(fullPath);

            return JRubyFile.createResourceAsFile(runtime, fullPath + "/" + suffix.forTarget(searchFile));
        }

        private String resolveHome() {
            Optional<String> home = RubyDir.getHomeFromEnv(runtime);

            // FIXME: Ick. See #5661
            if (!home.isPresent()) return null;

            String fullPath = home.get();
            return fullPath;
        }
    }

    class NullPathEntry extends PathEntry {
        protected String path() {
            return "";
        }

        protected ByteList pathBytes() {
            return ByteList.EMPTY_BYTELIST;
        }

        protected FileResource fullPath(String searchFile, Suffix suffix) {
            return JRubyFile.createResourceAsFile(runtime, suffix.forTarget(searchFile));
        }
    }
}
