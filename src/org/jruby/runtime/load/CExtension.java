/*
 * Copyright (C) 2010 Tim Felgentreff
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jruby.runtime.load;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.jruby.Ruby;
import org.jruby.cext.ModuleLoader;

/**
 * This class wraps the {@link ModuleLoader} for loading c-extensions 
 * in JRuby.
 * TODO: Support loading from non-filesystem resources such as Jar files.
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
                        if (os != null) 
                            os.close();
                        is.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            file = dstFile.getAbsolutePath();
        }
        new ModuleLoader().load(runtime, file);
    }

}
