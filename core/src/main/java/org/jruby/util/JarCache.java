package org.jruby.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.jruby.RubyFile.canonicalize;

/**
 * Instances of JarCache provides jar index information.
 *
 * <p>
 * Implementation is threadsafe.
  *
 * Since loading index information is O(jar-entries) we cache the snapshot in a WeakHashMap.
 * The implementation pays attention to lastModified timestamp of the jar and will invalidate
 * the cache entry if jar has been updated since the snapshot calculation.
 * </p>
  *
 * ******************************************************************************************
 * DANGER DANGER DANGER DANGER DANGER DANGER DANGER DANGER DANGER DANGER DANGER DANGER DANGER
 * ******************************************************************************************
  *
 * The spec for this cache is disabled currently for #2655, because of last-modified time
 * oddities on CloudBees. Please be cautious modifying this code and make sure you run the
 * associated spec locally.
 */
class JarCache {

    /**
     * The timeout in milliseconds for caching the last modified timestamp of JAR files.
     */
    private static final long LAST_MODIFIED_EXPIRATION_TIME_MILLISECONDS = 500;

    static class JarIndex {
        private static final String ROOT_KEY = "";

        private final Map<String, String[]> cachedDirEntries;
        private final JarFile jar;
        private final long lastModified;
        private Long lastModifiedExpiration;

        JarIndex(String jarPath) throws IOException {
            this.jar = new JarFile(jarPath);
            this.lastModified = getLastModified(jarPath);

            Map<String, HashSet<String>> mutableCache = new HashMap<>();

            // Always have a root directory
            mutableCache.put(ROOT_KEY, new HashSet<>());

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();

                int lastPathSep;
                while ((lastPathSep = path.lastIndexOf('/')) != -1) {
                    String dirPath = path.substring(0, lastPathSep);

                    HashSet<String> paths = mutableCache.get(dirPath);
                    if (paths == null) {
                        mutableCache.put(dirPath, paths = new HashSet<>());
                    }

                    String entryPath = path.substring(lastPathSep + 1);

                    // "" is not really a child path, even if we see foo/ entry
                    if (entryPath.length() > 0) paths.add(entryPath);

                    path = dirPath;
                }

                mutableCache.get(ROOT_KEY).add(path);
            }

            Map<String, String[]> cachedDirEntries = new HashMap<>(mutableCache.size() + 8, 1);
            for (Map.Entry<String, HashSet<String>> entry : mutableCache.entrySet()) {
                Set<String> value = entry.getValue();
                cachedDirEntries.put(entry.getKey(), value.toArray(new String[value.size()]));
            }

            this.cachedDirEntries = Collections.unmodifiableMap(cachedDirEntries);
        }

        /**
         * Determines the last modification timestamp of a file.
         * <p>
         * NOTE: Getting the value is an expensive operation on Windows systems (see
         * {@link <a href="https://github.com/jruby/jruby/issues/6730}").
         * Therefore a cached value is used with a maximum lifetime of {@link #LAST_MODIFIED_EXPIRATION_TIME_MILLISECONDS} milliseconds.
         *
         * @param jarPath The path to the JAR file.
         * @return The last modification timestamp.
         */
        private long getLastModified(String jarPath) {
            long currentTimeMillis = System.currentTimeMillis();
            if (lastModifiedExpiration != null && currentTimeMillis < lastModifiedExpiration) {
                return lastModified;
            }
            this.lastModifiedExpiration = currentTimeMillis + LAST_MODIFIED_EXPIRATION_TIME_MILLISECONDS;
            return new File(jarPath).lastModified();
        }

        public JarEntry getJarEntry(String entryPath) {
            return jar.getJarEntry(canonicalJarPath(entryPath));
        }

        public String[] getDirEntries(String entryPath) {
            return cachedDirEntries.get(canonicalJarPath(entryPath));
        }

        public InputStream getInputStream(JarEntry entry) throws IOException, IllegalStateException {
            return jar.getInputStream(entry);
        }

        public void release() {
            try {
                jar.close();
            } catch (IOException ioe) {
            }
        }

        public boolean isValid() {
            return getLastModified(jar.getName()) <= lastModified;
        }

        private static String canonicalJarPath(String path) {
            String canonical = canonicalize(path);

            // When hitting root, canonicalize tends to add a slash (so "foo/../bar" becomes "/bar"),
            // which doesn't quite work with jar entry paths, since most jar paths tends to be
            // relative (e.g. foo.jar!foo/bar). So we fix it.
            if (canonical.startsWith("/") && !path.startsWith("/")) {
                canonical = canonical.substring(1);
            }

            return canonical;
        }
    }

    private static class SoftJarIndex extends SoftReference<JarIndex> {
        private final String key;

        public SoftJarIndex(String key, JarIndex index) {
            super(index);
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private final Map<String, SoftJarIndex> indexCache = new ConcurrentHashMap<>();
    private final ReferenceQueue<JarIndex> indexQueue = new ReferenceQueue<>();

    public JarIndex getIndex(String jarPath) {
        String cacheKey = jarPath;

        cleanup();

        SoftReference<JarIndex> indexRef = indexCache.get(cacheKey);
        JarIndex index = indexRef == null ? null : indexRef.get();

        // If the index is invalid (jar has changed since snapshot was loaded)
        // we can just treat it as a "new" index and cache the updated results.
        // The old index will be dereferenced once no longer in use and eventually get cleaned up.
        if (index != null && !index.isValid()) {
            index = null;
        }

        if (index == null) {
            try {
                index = new JarIndex(jarPath);
                indexCache.put(cacheKey, new SoftJarIndex(cacheKey, index));
            } catch (IOException ioe) {
                return null;
            } catch (AccessControlException ace) {
                // No permissions to index the given path, bail out
                return null;
            }
        }

        return index;
    }

    public void remove(String jarPath) {
        // remove but do not otherwise damage the index associated with this path, since it may still be in use
        indexCache.remove(jarPath);
    }

    // must be called under locked indexCache
    private void cleanup() {
        SoftJarIndex indexRef;
        while ((indexRef = (SoftJarIndex) indexQueue.poll()) != null) {
            indexCache.remove(indexRef.getKey());
        }
    }
}
