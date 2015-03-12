/*
 **** BEGIN LICENSE BLOCK *****
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

package org.jruby.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public class JRubyClassLoader extends ClassDefiningJRubyClassLoader {

    private static final Logger LOG = LoggerFactory.getLogger("JRubyClassLoader");

    private final Map<URL,Set<String>> jarIndexes = new LinkedHashMap<URL,Set<String>>();

    private Runnable unloader;

    public JRubyClassLoader(ClassLoader parent) {
        super(parent);
    }

    public void addURLNoIndex(URL url) {
        // if we have such embedded jar within a jar, we copy it to temp file and use the
        // the temp file with the super URLClassLoader
        if ( url.toString().contains( "!/" )) {
            InputStream in = null;
            OutputStream out = null;
            try
            {
                File f = File.createTempFile( "jruby", new File(url.getFile()).getName());
                f.deleteOnExit();
                out = new BufferedOutputStream( new FileOutputStream( f ) );
                in = new BufferedInputStream( url.openStream() );
                int i = in.read();
                while( i != -1 ) {
                    out.write( i );
                    i = in.read();
                }
                out.close();
                in.close();
                url = f.toURI().toURL();
            }
            catch (IOException e)
            {
                throw new RuntimeException("BUG: we can not copy embedded jar to temp directory", e);
            }
            finally {
                // make sure we close everything
                if ( out != null ) {
                    try
                    {
                        out.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
                if ( in != null ) {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e)
                    {
                    }
                }
            }
        }
        super.addURL( url );
    }

    // Change visibility so others can see it
    @Override
    public void addURL(URL url) {
        super.addURL(url);
        indexJarContents(url);
    }

    /**
     * Called when the parent runtime is torn down.
     */
    public void tearDown(boolean debug) {
        try {
            // A hack to allow unloading all JDBC Drivers loaded by this classloader.
            // See http://bugs.jruby.org/4226
            getJDBCDriverUnloader().run();
        } catch (Exception e) {
            if (debug) {
                LOG.debug(e);
            }
        }
    }

    public synchronized Runnable getJDBCDriverUnloader() {
        if (unloader == null) {
            try {
                InputStream unloaderStream = getClass().getResourceAsStream("/org/jruby/util/JDBCDriverUnloader.class");
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = unloaderStream.read(buf)) != -1) {
                    baos.write(buf, 0, bytesRead);
                }

                Class<?> unloaderClass = defineClass("org.jruby.util.JDBCDriverUnloader", baos.toByteArray());
                unloader = (Runnable) unloaderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return unloader;
    }

    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            return super.findClass(className);
        } catch (ClassNotFoundException ex) {
            String resourceName = className.replace('.', '/').concat(".class");

            URL classUrl = null;
            synchronized (jarIndexes) {
                for (URL jarUrl : jarIndexes.keySet()) {
                    if (jarIndexes.get(jarUrl).contains(resourceName)) {
                        try {
                            classUrl = CompoundJarURLStreamHandler.createUrl(jarUrl, resourceName);
                            break;
                        } catch (IOException e) {
                            // keep going to next URL
                        }
                    }
                }
            }

            if (classUrl != null) {
                try {
                    InputStream input = classUrl.openStream();
                    try {
                        byte[] buffer = new byte[4096];
                        ByteArrayOutputStream output = new ByteArrayOutputStream();

                        for (int count = input.read(buffer); count > 0; count = input.read(buffer)) {
                            output.write(buffer, 0, count);
                        }

                        byte[] data = output.toByteArray();
                        return defineClass(className, data, 0, data.length);
                    } finally {
                        close(input);
                    }
                } catch (IOException e) {
                    // just fall-through to the re-throw below
                }
            }

            throw ex;
        }
    }

    @Override
    public URL findResource(String resourceName) {
        URL result = super.findResource(resourceName);

        if (result == null) {
            synchronized (jarIndexes) {
                for (URL jarUrl : jarIndexes.keySet()) {
                    if (jarIndexes.get(jarUrl).contains(resourceName)) {
                        try {
                            return CompoundJarURLStreamHandler.createUrl(jarUrl, resourceName);
                        } catch (IOException e) {
                            // keep going
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public Enumeration<URL> findResources(String resourceName) throws IOException {
        final List<URL> embeddedUrls = new ArrayList<URL>();

        synchronized (jarIndexes) {
            for (URL jarUrl : jarIndexes.keySet()) {
                if (jarIndexes.get(jarUrl).contains(resourceName)) {
                    try {
                        embeddedUrls.add(CompoundJarURLStreamHandler.createUrl(jarUrl, resourceName));
                    } catch (IOException e) {
                        // keep going
                    }
                }
            }
        }

        if (embeddedUrls.isEmpty()) {
            return super.findResources(resourceName);
        } else {
            final Enumeration<URL> originalResult = super.findResources(resourceName);

            return new Enumeration<URL>() {
                private Iterator<URL> extendedResult;

                public URL nextElement() {
                    if (extendedResult == null) {
                        return originalResult.nextElement();
                    } else {
                        return extendedResult.next();
                    }
                }

                public boolean hasMoreElements() {
                    if (extendedResult == null) {
                        boolean result = originalResult.hasMoreElements();

                        if (!result) {
                            // original result is consumed, switching to result
                            // from embedded jars processing.
                            extendedResult = embeddedUrls.iterator();
                            result = extendedResult.hasNext();
                        }
                        return result;
                    } else {
                        return extendedResult.hasNext();
                    }
                }
            };
        }
    }

    private void indexJarContents(URL jarUrl) {
        String proto = jarUrl.getProtocol();
        // we only need to index jar: and compoundjar: URLs
        // 1st-level jar files with file: URLs are handled by the JDK
        if (proto.equals("jar") || proto.equals(CompoundJarURLStreamHandler.PROTOCOL)) {
            synchronized (jarIndexes) {
                Set<String> entries = new HashSet<String>();
                jarIndexes.put(jarUrl, entries);

                try {
                    InputStream baseInputStream = jarUrl.openStream();
                    try {
                        JarInputStream baseJar = new JarInputStream(baseInputStream);
                        for (JarEntry entry = baseJar.getNextJarEntry(); entry != null; entry = baseJar.getNextJarEntry()) {
                            entries.add(entry.getName());
                        }
                    } finally {
                        close(baseInputStream);
                    }
                } catch (IOException ex) {
                    // can't read the stream, keep going
                }
            }
        }
    }

    private static void close(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException ignore) {
            }
        }
    }
}
