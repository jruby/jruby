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
        if (name.equals("getpid")) {
            if (library.getName().equals("msvcrt")) {
               name = "_getpid";
            }
        }
        return name;
    }

}