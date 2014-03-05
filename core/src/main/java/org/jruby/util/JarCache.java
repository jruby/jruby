package org.jruby.util;

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


class JarCache {
  static class JarIndex {
    private static final String ROOT_KEY = "";
    final Map<String, String[]> cachedDirEntries;

    JarIndex(JarFile jar) {
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
  }

  private final Map<String, JarIndex> indexCache = new WeakHashMap<String, JarIndex>();

  public JarIndex getIndex(JarFile jar) {
    String cacheKey = jar.getName();

    synchronized (indexCache) {
      if (!indexCache.containsKey(cacheKey)) {
        indexCache.put(cacheKey, new JarIndex(jar));
      }

      return indexCache.get(cacheKey);
    }
  }
}
