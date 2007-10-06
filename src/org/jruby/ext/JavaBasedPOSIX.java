/*
 * SpawnedProcessPOSIX.java
 * 
 * Created on Sep 1, 2007, 5:58:12 PM
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ext;

import java.io.File;
import java.io.IOException;
import org.jruby.util.Chmod;

/**
 *
 * @author headius
 */
public class JavaBasedPOSIX implements POSIX {
    public int chmod(String filename, int mode) {
        return Chmod.chmod(new File(filename), Integer.toOctalString(mode));
    }

    public int chown(String filename, int user, int group) {
        int chownResult = -1;
        int chgrpResult = -1;
        try {
            if (user != -1) {
                Process chown = Runtime.getRuntime().exec("chown " + user + " " + filename);
                chownResult = chown.waitFor();
            }
            if (group != -1) {
                Process chgrp = Runtime.getRuntime().exec("chgrp " + user + " " + filename);
                chgrpResult = chgrp.waitFor();
            }
        } catch (IOException ioe) {
            // FIXME: ignore?
        } catch (InterruptedException ie) {
            // FIXME: ignore?
        }
        if (chownResult != -1 && chgrpResult != -1) return 0;
        return -1;
    }

    public int getpid() {
      return Runtime.getRuntime().hashCode();
    }

}
