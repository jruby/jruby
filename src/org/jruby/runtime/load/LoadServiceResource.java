/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2005 Thomas E. Enebo <enebo@acm.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.load;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Simple struct to capture name seperate from URL.  URL and File have internal 
 * logic which does unexpected things when presenting the resource as a string. 
 */
public class LoadServiceResource {
    private final URL resource;
    private final File path;
    private final String name;
    private final boolean absolute;

    public LoadServiceResource(URL resource, String name) {
        this.resource = resource;
        this.path = null;
        this.name = name;
        this.absolute = false;
    }

    public LoadServiceResource(URL resource, String name, boolean absolute) {
        this.resource = resource;
        this.path = null;
        this.name = name;
        this.absolute = absolute;
    }
    
    public LoadServiceResource(File path, String name) {
        this.resource = null;
        this.path = path;
        this.name = name;
        this.absolute = false;
    }

    public LoadServiceResource(File path, String name, boolean absolute) {
        this.resource = null;
        this.path = path;
        this.name = name;
        this.absolute = absolute;
    }

    public InputStream getInputStream() throws IOException {
        if (resource != null) {
            return resource.openStream();
        }
        byte[] bytes = new byte[(int)path.length()];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        FileInputStream fis = new FileInputStream(path);
        FileChannel fc = fis.getChannel();
        fc.read(buffer);
        fis.close();
        return new ByteArrayInputStream(bytes);
    }

    public String getName() {
        return name;
    }

    public File getPath() {
        return path;
    }
    
    public URL getURL() throws IOException {
        if (resource != null) {
            return resource;
        } else {
            return path.toURI().toURL();
        }
    }

    public boolean isAbsolute() {
        return absolute;
    }
}
