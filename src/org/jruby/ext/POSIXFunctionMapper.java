/*
 * POSIXFunctionMapper.java
 */

package org.jruby.ext;

import java.lang.reflect.Method;
import com.sun.jna.FunctionMapper;
import com.sun.jna.NativeLibrary;

public class POSIXFunctionMapper implements FunctionMapper {

    public POSIXFunctionMapper() {}
  
    public String getFunctionName(NativeLibrary library, Method method) {
        String name = method.getName();
        if (library.getName().equals("msvcrt")) {
            // FIXME: We should either always _ name for msvcrt or get good list of _ methods
            if (name.equals("getpid") || name.equals("chmod")) {
                name = "_" + name;
            }
        }
        return name;
    }

}