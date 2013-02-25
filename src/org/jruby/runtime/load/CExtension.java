/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010, Tim Felgentreff
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.runtime.load;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.cext.ModuleLoader;
import org.jruby.util.SafePropertyAccessor;

/**
 * This class wraps the {@link ModuleLoader} for loading c-extensions 
 * in JRuby. Resources in the native file-system are loaded directly,
 * extensions included in a Jar are extracted to java.io.tmpdir to allow
 * the System to load them into the process space.
 */
public class CExtension implements Library {
    private LoadServiceResource resource;

    public CExtension(LoadServiceResource resource) {
        this.resource = resource;
    }

    /**
     * Try loading the found c-extension. If the extension is contained in a Jar file,
     * it will be extracted to the java.io.tmpdir
     */
    public void load(Ruby runtime, boolean wrap) throws IOException {
        String file;
        if (!resource.getURL().getProtocol().equals("jar")) {
            file = resource.getAbsolutePath();
        } else {
            // The cext was wrapped in a jar
            InputStream is = resource.getInputStream();
            FileOutputStream os = null;
            File dstFile = new File(System.getProperty("java.io.tmpdir") + File.pathSeparator + 
                    resource.getName());
            if (!dstFile.exists()) { // File doesn't exist yet. Extract it.
                try {
                    dstFile.deleteOnExit();
                    os = new FileOutputStream(dstFile);
                    ReadableByteChannel srcChannel = Channels.newChannel(is);

                    for (long pos = 0; is.available() > 0; ) {
                        pos += os.getChannel().transferFrom(srcChannel, pos, Math.max(4096, is.available()));
                    }
                } catch (IOException ex) {
                    throw runtime.newLoadError("Error loading file -- " + resource.getName());
                } finally {
                    try {
                        if (os != null)  {
                            os.close();
                        }
                        is.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            file = dstFile.getAbsolutePath();
        }
        ModuleLoader.load(runtime, file);
        
        // set a "global" property to indicate we have loaded a C extension
        RubyInstanceConfig.setLoadedNativeExtensions(true);
    }

}
