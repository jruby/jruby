
package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.LastError;
import org.jruby.Ruby;
import org.jruby.ext.ffi.AbstractInvoker;
import org.jruby.ext.ffi.NativeParam;
import org.jruby.ext.ffi.NativeType;
import org.jruby.ext.ffi.Platform;


public class JFFIProvider extends org.jruby.ext.ffi.FFIProvider {

    protected JFFIProvider(Ruby runtime) {
        super(runtime);
    }
    @Override
    public AbstractInvoker createInvoker(Ruby runtime, String libraryName, String functionName, NativeType returnType, NativeParam[] parameterTypes, String convention) {
        return new JFFIInvoker(runtime, 
                libraryName != null ? Platform.getPlatform().mapLibraryName(libraryName) : null,
                functionName, returnType, parameterTypes, convention);
    }

    @Override
    public int getLastError() {
        return LastError.getInstance().getError();
    }

    @Override
    public void setLastError(int error) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
