/*
 * Chmod.java
 *
 * Created on April 2, 2007, 8:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jruby.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *
 * @author headius
 */
public class Chmod {
    private static final boolean CHMOD_API_AVAILABLE;
    private static final Method setWritable;
    private static final Method setReadable;
    private static final Method setExecutable;
    private static final Method canExecute;
    
    static {
        boolean apiAvailable = false;
        Method setWritableVar = null;
        Method setReadableVar = null;
        Method setExecutableVar = null;
        Method canExecuteVar = null;
        try {
            setWritableVar = File.class.getMethod("setWritable", new Class[] {Boolean.TYPE, Boolean.TYPE});
            setReadableVar = File.class.getMethod("setReadable", new Class[] {Boolean.TYPE, Boolean.TYPE});
            setExecutableVar = File.class.getMethod("setExecutable", new Class[] {Boolean.TYPE, Boolean.TYPE});
            canExecuteVar = File.class.getMethod("canExecute", new Class[] {});
            apiAvailable = true;
        } catch (Exception e) {
            // failed to load methods, no chmod API available
        }
        setWritable = setWritableVar;
        setReadable = setReadableVar;
        setExecutable = setExecutableVar;
        canExecute = canExecuteVar;
        CHMOD_API_AVAILABLE = apiAvailable;
    }
    
    public static boolean chmod(File file, String mode) {
        if (CHMOD_API_AVAILABLE) {
            // fast version
            char other = mode.charAt(mode.length() - 1);
            char group = mode.charAt(mode.length() - 2);
            char user = mode.charAt(mode.length() - 3);
            char setuidgid = mode.charAt(mode.length() - 3);
            
            // group and setuid/gid are ignored, no way to do them fast. Should we fall back on slow?
            if (!setPermissions(file, other, false)) return false;
            if (!setPermissions(file, user, true)) return false;
        } else {
            // slow version
            try {
                Process chmod = Runtime.getRuntime().exec("chmod " + mode + " " + file.getName());
                chmod.waitFor();
                return chmod.exitValue() == 0;
            } catch (IOException ioe) {
                // FIXME: ignore?
            } catch (InterruptedException ie) {
                // FIXME: ignore?
            }
        }
        return false;
    }
    
    private static boolean setPermissions(File file, char permChar, boolean userOnly) {
        int permValue = Character.digit(permChar, 8);
        
        try {
            if ((permValue & 1) != 0) {
                setExecutable.invoke(file, new Object[] {Boolean.TRUE, Boolean.valueOf(userOnly)});
            } else {
                setExecutable.invoke(file, new Object[] {Boolean.FALSE, Boolean.valueOf(userOnly)});
            }
            
            
            if ((permValue & 2) != 0) {
                setReadable.invoke(file, new Object[] {Boolean.TRUE, Boolean.valueOf(userOnly)});
            } else {
                setReadable.invoke(file, new Object[] {Boolean.FALSE, Boolean.valueOf(userOnly)});
            }
            
            if ((permValue & 4) != 0) {
                setWritable.invoke(file, new Object[] {Boolean.TRUE, Boolean.valueOf(userOnly)});
            } else {
                setWritable.invoke(file, new Object[] {Boolean.FALSE, Boolean.valueOf(userOnly)});
            }
            
            return true;
        } catch (IllegalAccessException iae) {
            // ignore, return false below
        } catch (InvocationTargetException ite) {
            // ignore, return false below
        }
        
        return false;
    }
}
