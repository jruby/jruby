
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.Library;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches all the low level library handles
 */
public class LibraryCache {
    private static final Map<String, WeakReference<Library>> cache
        = new ConcurrentHashMap<String, WeakReference<Library>>();
    private static final Library DEFAULT = new Library(null, Library.LAZY);
    public final static Library open(String name, int flags) {
        if (name == null) {
            return DEFAULT;
        }
        WeakReference<Library> ref = cache.get(name);
        Library lib;
        if (ref != null && (lib = ref.get()) != null) {
            return lib;
        }
        cache.put(name, new WeakReference<Library>(lib = new Library(name, flags)));
        return lib;
    }
}
