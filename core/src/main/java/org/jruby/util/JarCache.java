package org.jruby.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
  * Instances of JarCache provides jar index information.
  *
  * <p>
  *     Implementation is threadsafe.
  *
  *     Since loading index information is O(jar-entries) we cache the snapshot in a WeakHashMap.
  *     The implementation pays attention to lastModified timestamp of the jar and will invalidate
  *     the cache entry if jar has been updated since the snapshot calculation.
  * </p>
  */
class JarCache {
    static class JarIndex {
        private static final String ROOT_KEY = "";

        final Map<String, String[]> cachedDirEntries;
        final JarFile jar;
        final long snapshotCalculated;

        JarIndex(String jarPath) throws IOException {
            this.jar = new JarFile(jarPath);
            this.snapshotCalculated = new File(jarPath).lastModified();

            Map<String, Set<String>> mutableCache = new HashMap<String, Set<String>>();

            // Always have a root directory
            mutableCache.put(ROOT_KEY, new HashSet<String>());

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();

                int lastPathSep;
                while ((lastPathSep = path.lastIndexOf('/')) != -1) {
                    String dirPath = path.substring(0, lastPathSep); 

                    if (!mutableCache.containsKey(dirPath)) {
                        mutableCache.put(dirPath, new HashSet<String>());
                    }

                    String entryPath = path.substring(lastPathSep + 1);

                    // "" is not really a child path, even if we see foo/ entry
                    if (entryPath.length() > 0) {
                        mutableCache.get(dirPath).add(entryPath);
                    }

                    path = dirPath;
                }

                mutableCache.get(ROOT_KEY).add(path);
            }

            Map<String, String[]> cachedDirEntries = new HashMap<String, String[]>();
            for (Map.Entry<String, Set<String>> entry : mutableCache.entrySet()) {
                cachedDirEntries.put(entry.getKey(), entry.getValue().toArray(new String[0]));
            }

            this.cachedDirEntries = Collections.unmodifiableMap(cachedDirEntries);
        }

        public JarEntry getJarEntry(String entryPath) {
            return jar.getJarEntry(entryPath);
        }

        public void release() {
            try {
                jar.close();
            } catch (IOException ioe) { }
        }

        public boolean isValid() {
            return new File(jar.getName()).lastModified() <= snapshotCalculated;
        }
    }

    private final Map<String, JarIndex> indexCache = new WeakHashMap<String, JarIndex>();

    public JarIndex getIndex(String jarPath) {
        String cacheKey = jarPath;

        synchronized (indexCache) {
            JarIndex index = indexCache.get(cacheKey);

            // If the index is invalid (jar has changed since snapshot was loaded)
            // we can just treat it as a "new" index and cache the updated results.
            if (index != null && !index.isValid()) {
                index.release();
                index = null;
            }

            if (index == null) {
                try { 
                    index = new JarIndex(jarPath);
                    indexCache.put(cacheKey, index);
                } catch (IOException ioe) {
                    return null;
                }
            }

            return index;
        }
    }
}
