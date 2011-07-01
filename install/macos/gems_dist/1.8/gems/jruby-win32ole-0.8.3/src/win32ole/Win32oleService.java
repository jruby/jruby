package win32ole;

import org.racob.com.LibraryLoader;
import java.io.IOException;import java.lang.ref.WeakReference;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.ext.win32ole.RubyWIN32OLE;
import org.jruby.runtime.load.BasicLibraryService;

public class Win32oleService implements BasicLibraryService {
    static WeakReference<RubyClass> win32oleClass;

    public boolean basicLoad(Ruby runtime) throws IOException {
        LibraryLoader.loadLibrary();
        RubyClass object = runtime.getObject();
        RubyClass win32ole = runtime.defineClass("WIN32OLE", object,
                RubyWIN32OLE.WIN32OLE_ALLOCATOR);

        win32ole.defineAnnotatedMethods(RubyWIN32OLE.class);

        win32oleClass = new WeakReference<RubyClass>(win32ole);

        return true;
    }

    public static RubyClass getMetaClass() {
        return win32oleClass.get();
    }
}
