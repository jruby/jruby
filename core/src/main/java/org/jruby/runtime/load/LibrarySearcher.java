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
import org.jruby.util.collections.StringArraySet;
import org.jruby.util.func.TriFunction;

public class LibrarySearcher {
    public static final char EXTENSION_TYPE = 's';
    public static final char SOURCE_TYPE = 'r';
    public static final char UNKNOWN_TYPE = 'u';
    public static final char NOT_FOUND = 0;
    private final LoadService loadService;
    private final Ruby runtime;
    private final PathEntry cwdPathEntry;
    private final PathEntry classloaderPathEntry;
    private final PathEntry nullPathEntry;
    private final PathEntry homePathEntry;

    protected ExpandedLoadPath expandedLoadPath;

    protected RubyArray loadPath;
    protected RubyArray loadedFeaturesSnapshot;
    private final Map<StringWrapper, Feature> loadedFeaturesIndex = new ConcurrentHashMap<>(64);

    public LibrarySearcher(LoadService loadService) {
        Ruby runtime = loadService.runtime;

        this.runtime = runtime;
        this.loadPath = RubyArray.newArray(runtime);
        this.loadService = loadService;
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
                    return SOURCE_TYPE;
                }
                if ((tmp = findLibrary(file.substring(0, ext), SuffixType.Source)) != null) {
                    String tmpPath = tmp.loadName;
                    ext = tmpPath.lastIndexOf('.');
                    if (isFeature(tmpPath, ext, true, true, loading) == 0 || loading[0] != null)
                        path[0] = tmp;
                    return SOURCE_TYPE;
                }
                return 0;
            } else if (isLibraryExt(file)) {
                if (isFeature(file, ext, true, false, loading) != 0) {
                    if (loading[0] != null) path[0] = new FoundLibrary(file, loading[0], Suffix.JAR.constructLibrary(file, loading[0], JRubyFile.createResourceAsFile(runtime, loading[0])));
                    return EXTENSION_TYPE;
                }
                if ((tmp = findLibrary(file.substring(0, ext), SuffixType.Extension)) != null) {
                    ext = tmp.loadName.lastIndexOf('.');
                    if (isFeature(tmp.loadName, ext, false, true, loading) == 0 || loading[0] != null) {
                        path[0] = tmp;
                    }
                    return EXTENSION_TYPE;
                }
            }
        } else if ((ft = isFeature(file, -1, false, false, loading)) == SOURCE_TYPE) {
            if (loading[0] != null) path[0] = new FoundLibrary(file, loading[0], Suffix.RUBY.constructLibrary(file, loading[0], JRubyFile.createResourceAsFile(runtime, loading[0])));
            return SOURCE_TYPE;
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
        return tmp.loadName.endsWith(".jar") ? EXTENSION_TYPE : SOURCE_TYPE;
    }

    public LibrarySearcher.FoundLibrary findLibraryForLoad(String file) {
        if (file.endsWith(".rb")) {
            // source files need to check both .rb and .class so we use suffix logic
            return findLibrary(file.substring(0, file.length() - 3), SuffixType.Source);
        }

        // otherwise we try to load as-is
        FoundLibrary library = findResourceLibrary(file, f -> f, ResourceLibrary::create);

        if (library != null) {
            return library;
        }

        return findServiceLibrary(file);
    }

    public LibrarySearcher.FoundLibrary findLibrary(String baseName, SuffixType suffixType) {
        for (Suffix suffix : suffixType.getSuffixSet()) {
            FoundLibrary library = findResourceLibrary(baseName, suffix::forTarget, suffix.libraryFactory);

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

    private static class StringWrapper implements CharSequence {
        private String str;
        private int beg;
        private int len;
        private int hash;

        /**
         * Construct a wrapper with the given values.
         *
         * @param str
         * @param beg
         * @param len
         */
        StringWrapper(String str, int beg, int len) {
            this.str = str;
            this.beg = beg;
            this.len = len;
        }

        /**
         * Populate this wrapper with the given values.
         *
         * @param str
         * @param beg
         * @param len
         */
        void rewrap(String str, int beg, int len) {
            this.str = str;
            this.beg = beg;
            this.len = len;
            this.hash = 0;
        }

        /**
         * Clear this wrapper.
         */
        void clear() {
            str = null;
            beg = 0;
            len = 0;
            hash = 0;
        }

        StringWrapper dup() {
            return new StringWrapper(str, beg, len);
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public char charAt(int index) {
            return str.charAt(beg + index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return new StringWrapper(str, beg + start, end - start);
        }

        @Override
        public String toString() {
            return str.substring(beg, beg + len);
        }

        @Override
        public boolean equals(Object other) {
            String otherStr;
            String str;
            int otherBeg;
            int otherLen;

            int beg = this.beg;
            int len = this.len;

            if (other instanceof StringWrapper) {
                StringWrapper otherWrapper = (StringWrapper) other;

                otherLen = otherWrapper.len;

                if (len != otherLen) return false;

                otherBeg = otherWrapper.beg;
                str = this.str;
                otherStr = otherWrapper.str;
            } else if (other instanceof String) {
                otherStr = (String) other;

                otherLen = otherStr.length();

                if (len != otherLen) return false;

                otherBeg = 0;
                str = this.str;
            } else {
                return false;
            }

            if (str == otherStr && beg == otherBeg) return true;

            return str.regionMatches(beg, otherStr, otherBeg, otherLen);
        }

        @Override
        public int hashCode() {
            int h = hash;
            int len = this.len;
            if (h == 0 && len > 0) {
                String str = this.str;
                int beg = this.beg;
                for (int i = 0; i < len; i++) {
                    h = 31 * h + str.charAt(beg + i);
                }
                hash = h;
            }
            return h;
        }
    }

    public synchronized boolean featureAlreadyLoaded(String feature, String[] loading) {
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

    protected synchronized void provideFeature(RubyString name) {
        StringArraySet loadedFeatures = this.loadService.loadedFeatures;

        if (loadedFeatures.isFrozen()) {
            throw runtime.newRuntimeError("$LOADED_FEATURES is frozen; cannot append feature");
        }

        name.setFrozen(true);

        loadedFeatures.append(name);

        snapshotLoadedFeatures();

        addFeatureToIndex(name.toString(), name);
    }

    protected synchronized RubyArray snapshotLoadedFeatures() {
        return (RubyArray) loadedFeaturesSnapshot.replace(this.loadService.loadedFeatures);
    }

    protected synchronized void addFeatureToIndex(String name, IRubyObject featurePath) {
        int featureEnd = name.length();

        int ext, p;

        for (ext = featureEnd - 1; ext > 0; ext--)
            if (name.charAt(ext) == '.' || name.charAt(ext) == '/') break;

        if (name.charAt(ext) != '.') ext = -1;

        /* Now `ext` points to the only string matching %r{^\.[^./]*$} that is
           at the end of `feature`, or is NULL if there is no such string. */

        p = ext != -1 ? ext : featureEnd;
        while (true) {
            p--;

            // Walk back to nearest '/'
            while (p >= 0 && name.charAt(p) != '/') p--;

            if (p < 0) break;

            // Add partial feature to index
            addSingleFeatureToIndex(name, p + 1, featureEnd, featurePath);

            // Add partial feature without extension, if appropriate
            if (ext != -1) {
                addSingleFeatureToIndex(name, p + 1, ext, featurePath);
            }
        }

        // Add full feature to index
        addSingleFeatureToIndex(name, 0, name.length(), featurePath);

        // Add version without extension, if appropriate
        if (ext != -1) {
            addSingleFeatureToIndex(name, 0, ext, featurePath);
        }
    }

    class Feature {
        Feature(StringWrapper key, IRubyObject featurePath) {
            this.key = key;
            this.featurePaths = new ArrayList<>();

            featurePaths.add(featurePath);
        }

        synchronized void addFeaturePath(IRubyObject offset) {
            featurePaths.add(offset);
        }

        public synchronized void clear() {
            featurePaths.clear();
        }

        public synchronized char matches(String feature, Suffix suffix, int len, boolean rb, boolean expanded, boolean suffixGiven) {
            List<IRubyObject> featurePaths = this.featurePaths;
            for (int i = 0; i < featurePaths.size(); i++) {
                IRubyObject featurePath = featurePaths.get(i);

                RubyString loadedFeaturePath = featurePath.convertToString();

                if (loadedFeaturePath.length() < len) continue;

                String featureString = loadedFeaturePath.asJavaString();

                int expandedPathLength = 0;

                if (!featureString.regionMatches(0, feature, 0, len)) {
                    if (expanded) continue; // already given expanded path

                    List<LibrarySearcher.PathEntry> loadPath = getExpandedLoadPath();
                    String withPath = loadedFeatureWithPath(featureString, feature, suffix, loadPath);

                    if (withPath == null) continue; // no match with expanded path, try next

                    expanded = true;
                    expandedPathLength = withPath.length() + 1;
                }

                int e = expandedPathLength + len;
                if (e == featureString.length()) {
                    if (suffixGiven) continue;
                    return UNKNOWN_TYPE;
                }
                if (featureString.charAt(e) != '.') continue;
                if ((!rb || !suffixGiven) && isLibraryExt(featureString)) {
                    return EXTENSION_TYPE;
                }
                if ((rb || !suffixGiven) && isSourceExt(featureString)) {
                    return SOURCE_TYPE;
                }
            }

            return 0;
        }

        final StringWrapper key;
        private final List<IRubyObject> featurePaths;
    }

    private final ThreadLocal<StringWrapper> keyWrapper = ThreadLocal.withInitial(() -> new StringWrapper(null, 0, 0));
    private final ThreadLocal<StringWrapper> rangeWrapper = ThreadLocal.withInitial(() -> new StringWrapper(null, 0, 0));

    protected synchronized void addSingleFeatureToIndex(String key, int beg, int end, IRubyObject featurePath) {
        Map<StringWrapper, Feature> featuresIndex = this.loadedFeaturesIndex;

        withRangeWrapper(key, beg, end - beg, (wrapper) -> {
            Feature thisFeature = featuresIndex.get(wrapper);

            if (thisFeature == null) {
                StringWrapper clone = wrapper.dup();
                featuresIndex.put(clone, new Feature(clone, featurePath));
            } else {
                thisFeature.addFeaturePath(featurePath);
            }

            return thisFeature;
        });
    }

    private synchronized void releaseWrapper(StringWrapper wrapper) {
        wrapper.clear();
    }

    private synchronized StringWrapper keyWrapper(String key) {
        StringWrapper wrapper = keyWrapper.get();
        wrapper.rewrap(key, 0, key.length());
        return wrapper;
    }

    private synchronized StringWrapper rangeWrapper(String key, int beg, int len) {
        StringWrapper wrapper = rangeWrapper.get();
        wrapper.rewrap(key, beg, len);
        return wrapper;
    }

    private synchronized <R> R withKeyWrapper(String key, Function<StringWrapper, R> body) {
        StringWrapper wrapper = keyWrapper(key);
        R result = body.apply(wrapper);
        releaseWrapper(wrapper);
        return result;
    }

    private synchronized <R> R withRangeWrapper(String key, int beg, int len, Function<StringWrapper, R> body) {
        StringWrapper wrapper = rangeWrapper(key, beg, len);
        R result = body.apply(wrapper);
        releaseWrapper(wrapper);
        return result;
    }

    protected char isFeature(String feature, int ext, boolean rb, boolean expanded, String[] fn) {
        List<LibrarySearcher.PathEntry> loadPath = null;

        final LibrarySearcher.Suffix suffix;

        if (fn != null) fn[0] = null;

        boolean suffixGiven = ext != -1;

        final int len;
        if (suffixGiven) {
            len = ext;
            suffix = rb ? LibrarySearcher.Suffix.RUBY : LibrarySearcher.Suffix.JAR;
        } else {
            len = feature.length();
            suffix = null;
        }

        /*
        This caching logic assumes an index mapping all loaded paths and subpaths to potential loaded features. Each
        feature is added to the cache as a series of subpaths pointing at the original loaded feature entry. This
        allows subsequent requires of that path or similar subpaths (as in require_relative) to quickly check all
        previously added features for a match.

        The matches are calculated by taking the given feature path combined with load path entries and file suffixes
        and attempting to match it against any loaded features this path has been associated with.
         */
        Feature matchingFeature = getLoadedFeature(feature);
        if (matchingFeature != null) {
            char matches = matchingFeature.matches(feature, suffix, len, rb, expanded, suffixGiven);

            if (matches != 0) return matches;
        }

        // Check load locks to see if another thread is currently loading this file
        Map<String, LoadService.RequireLocks.RequireLock> loadingTable = this.loadService.requireLocks.pool;
        if (!expanded) {
            loadPath = lazyLoadPath(loadPath);
            for (Map.Entry<String, LoadService.RequireLocks.RequireLock> entry : loadingTable.entrySet()) {
                if (loadedFeatureWithPath(entry.getKey(), feature, suffix, loadPath) != null) {
                    return setLoadingAndReturn(feature, fn, suffixGiven, entry.getKey());
                }
            }
        }

        if (loadingTable.containsKey(feature)) {
            // FIXME: use key from the actual table?
            return setLoadingAndReturn(feature, fn, suffixGiven, feature);
        }

        if (suffixGiven && ext == feature.length()) return 0;
        String baseName = feature.substring(0, len);
        for (LibrarySearcher.Suffix suffix2 : Suffix.ALL_ARY) {
            String withExt = suffix2.forTarget(baseName);
            if (loadingTable.containsKey(withExt)) {
                if (fn != null) fn[0] = withExt;
                return suffix2 != LibrarySearcher.Suffix.RUBY ? EXTENSION_TYPE : SOURCE_TYPE;
            }
        }

        return NOT_FOUND;
    }

    private char setLoadingAndReturn(String feature, String[] fn, boolean suffixGiven, String key) {
        if (fn != null) fn[0] = key;
        if (!suffixGiven) return UNKNOWN_TYPE;
        return !isSourceExt(feature) ? EXTENSION_TYPE : SOURCE_TYPE;
    }

    private List<LibrarySearcher.PathEntry> lazyLoadPath(List<LibrarySearcher.PathEntry> loadPath) {
        return (loadPath == null) ? getExpandedLoadPath() : loadPath;
    }

    synchronized Map<StringWrapper, Feature> getLoadedFeaturesIndex() {
        StringArraySet loadedFeatures = this.loadService.loadedFeatures;

        // Compare to see if the snapshot still matches actual.
        if (!loadedFeaturesSnapshot.isSharedJavaArray(loadedFeatures)) {
            loadedFeaturesIndex.clear();

            Ruby runtime = this.runtime;

            // defensive copy for iteration; if original is modified we snapshot again below
            RubyArray features = snapshotLoadedFeatures();
            boolean modified = false;

            for (int i = 0; i < features.size(); i++) {
                IRubyObject entry = features.eltOk(i);
                RubyString asStr = runtime.freezeAndDedupString(entry.convertToString());
                if (asStr != entry) {
                    modified = true;
                    loadedFeatures.eltSetOk(i, asStr);
                }
                addFeatureToIndex(asStr.toString(), asStr);
            }

            if (modified) snapshotLoadedFeatures();
        }

        return loadedFeaturesIndex;
    }

    Feature getLoadedFeature(String feature) {
        return withKeyWrapper(feature, (wrapper) -> getLoadedFeaturesIndex().get(wrapper));
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
    static String loadedFeatureWithPath(String name, String feature,
                                 LibrarySearcher.Suffix suffix, List<LibrarySearcher.PathEntry> loadPath) {
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
                (suffix == LibrarySearcher.Suffix.JAR && !LibrarySearcher.isLibraryExt(name)) ||
                        (suffix == LibrarySearcher.Suffix.RUBY && !LibrarySearcher.isSourceExt(name))) {

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

    private FoundLibrary findResourceLibrary(String baseName, FilenameFactory pathMaker, LibraryFactory libraryMaker) {
        if (baseName.startsWith("./") || baseName.startsWith("../") || isAbsolute(baseName)) {
            // Path should be canonicalized in the findFileResource
            return nullPathEntry.findFile(baseName, pathMaker, libraryMaker);
        }

        if (baseName.startsWith("~/")) {
            return homePathEntry.findFile(baseName.substring(2), pathMaker, libraryMaker);
        }

        // search the $LOAD_PATH
        for (PathEntry loadPathEntry : getExpandedLoadPath()) {
            FoundLibrary library = loadPathEntry.findFile(baseName, pathMaker, libraryMaker);
            if (library != null) return library;
        }

        // inside a classloader the path "." is the place where to find the jruby kernel
        if (!runtime.getCurrentDirectory().startsWith(URLResource.URI_CLASSLOADER)) {

            // ruby does not load a relative path unless the current working directory is in $LOAD_PATH
            FoundLibrary library = cwdPathEntry.findFile(baseName, pathMaker, libraryMaker);

            // we did not find the file on the $LOAD_PATH but in current directory so we need to treat it
            // as not found (the classloader search below will find it otherwise)
            if (library != null) return null;
        }

        // load the jruby kernel and all resource added to $CLASSPATH
        return classloaderPathEntry.findFile(baseName, pathMaker, libraryMaker);
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
        static final Suffix[] ALL_ARY = ALL.stream().toArray(i -> new Suffix[i]);

        private final String extension;
        private final byte[] extensionBytes;
        private final LibraryFactory libraryFactory;

        Suffix(String extension, LibraryFactory libraryFactory) {
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
            // Fully buffers file, so does not need to be closed
            LoadServiceResourceInputStream ris = prepareInputStream(runtime);

            if (runtime.getInstanceConfig().getCompileMode().shouldPrecompileAll()) {
                runtime.compileAndLoadFile(scriptName, ris, wrap);
            } else {
                runtime.loadFile(scriptName, ris, wrap);
            }
        }

        private LoadServiceResourceInputStream prepareInputStream(Ruby runtime) {
            try (InputStream is = resource.inputStream()){
                return new LoadServiceResourceInputStream(is);
            } catch (IOException ioe) {
                throw runtime.newLoadError("failure to load file: " + ioe.getLocalizedMessage(), searchName);
            }
        }
    }

    static class ClassResourceLibrary extends ResourceLibrary {
        public ClassResourceLibrary(String searchName, String scriptName, FileResource resource) {
            super(searchName, scriptName, resource);
        }

        @Override
        public void load(Ruby runtime, boolean wrap) {
            try (InputStream ris = resource.inputStream()) {

                InputStream is = new BufferedInputStream(ris, 32768);
                IRScope script = CompiledScriptLoader.loadScriptFromFile(runtime, is, null, scriptName, false);

                // Depending on the side-effect of the load, which loads the class but does not turn it into a script.
                // I don't like it, but until we restructure the code a bit more, we'll need to quietly let it by here.
                if (script == null) return;

                script.setFileName(scriptName);
                runtime.loadScope(script, wrap);
            } catch(IOException e) {
                throw runtime.newLoadError("no such file to load -- " + searchName, searchName);
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
            return loadPathSnapshot.isSharedJavaArray(loadPath);
        }
    }

    interface LibraryFactory extends TriFunction<String, String, FileResource, Library> {}
    interface FilenameFactory extends Function<String, String> {}

    abstract class PathEntry {
        protected FoundLibrary findFile(String target, FilenameFactory pathMaker, LibraryFactory libraryMaker) {
            Ruby runtime = LibrarySearcher.this.runtime;

            FileResource fullPath = fullPath(target, pathMaker);

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

                        return new FoundLibrary(target, expandedAbsolute, libraryMaker.apply(target, expandedAbsolute, fullPath));
                    }
                }

                DebugLog.Resource.logFound(fullPath);

                String resolvedName = absolutePath;

                return new FoundLibrary(target, resolvedName, libraryMaker.apply(target, resolvedName, fullPath));
            }

            return null;
        }

        protected abstract String path();

        protected abstract FileResource fullPath(String searchName, Function<String, String> pathMaker);
    }

    class NormalPathEntry extends PathEntry {
        final IRubyObject path;
        final boolean cacheExpanded;
        FileResource expanded;

        NormalPathEntry(IRubyObject path) {
            this.path = path;
            this.cacheExpanded = isCachable(runtime, path);
        }

        protected String path() {
            return expandPathCached().path();
        }

        protected FileResource fullPath(String searchFile, Function<String, String> pathMaker) {
            FileResource loadPath = expandPathCached();

            String fullPath = loadPath.path() + "/"
                    + pathMaker.apply(searchFile);

            DebugLog.Resource.logTry(fullPath);

            return JRubyFile.createResourceAsFile(runtime, fullPath);
        }

        private FileResource expandPathCached() {
            if (cacheExpanded) {
                FileResource expanded = this.expanded;
                if (expanded != null) return expanded;

                expanded = this.expanded = expandPath();

                return expanded;
            }

            FileResource expanded = expandPath();

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

            if (!new File(pathAsString).isAbsolute()) return false;

            FileResource resource = JRubyFile.createResourceAsFile(runtime, pathAsString);

            return !resource.isFile();
        }
    }

    class HomePathEntry extends PathEntry {
        protected String path() {
            return resolveHome();
        }

        protected FileResource fullPath(String searchFile, Function<String, String> pathMaker) {
            String fullPath = resolveHome();

            if (fullPath == null) return null;

            DebugLog.Resource.logTry(fullPath);

            return JRubyFile.createResourceAsFile(runtime, fullPath + "/" + pathMaker.apply(searchFile));
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

        protected FileResource fullPath(String searchFile, Function<String, String> pathMaker) {
            return JRubyFile.createResourceAsFile(runtime, pathMaker.apply(searchFile));
        }
    }
}
