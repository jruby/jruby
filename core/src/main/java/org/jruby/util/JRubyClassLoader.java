/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

/**
 * this classloader will be populated dynamically in the following ways:
 * <ul>
 * <li><code>JRuby.runtime.jruby_class_loader.add_url( java.net.URL.new( "file:my.jar" )</code></li>
 * <li><code>$CLASSPATH &lt;&lt; 'path/to/class/or/resources'</code></li>
 * <li><code>require 'some.jar'</code></li>
 * <li><code>load 'some.jar'</code></li>
 * </ul>
 *
 * so it is the classloader for ALL the jars used by gems. and this
 * classlaoder is "implicit" part of the $LOAD_PATH of the jruby runtime
 * all ruby resources inside any of the added jars will be found via
 * <code>require</code> and <code>load</code>
 */
public class JRubyClassLoader extends ClassDefiningJRubyClassLoader {
    static {
        registerAsParallelCapable();
    }

    private static final Logger LOG = LoggerFactory.getLogger(JRubyClassLoader.class);

    private Runnable unloader;

    private static volatile File tempDir;

    private final Map<String, URL> cachedJarPaths = new ConcurrentHashMap<>();

    public JRubyClassLoader(ClassLoader parent) {
        super(parent);
    }

    // Change visibility so others can see it
    @Override
    public void addURL(URL url) {
        // if we have such embedded jar within a jar, we copy it to temp file and use the
        // the temp file with the super URLClassLoader
        if (url.toString().contains( "!/" ) ||
            !(url.getProtocol().equals("file") || url.getProtocol().equals("http") || url.getProtocol().equals("https"))) {
            try {
                File f = File.createTempFile("jruby", new File(url.getFile()).getName(), getTempDir());

                try (FileOutputStream fileOut = new FileOutputStream(f);
                     InputStream urlIn = url.openStream()) {

                    OutputStream out = new BufferedOutputStream(fileOut);
                    InputStream in = new BufferedInputStream(urlIn);

                    int i = in.read();
                    while (i != -1) {
                        out.write(i);
                        i = in.read();
                    }
                    out.flush();
                    url = f.toURI().toURL();

                    cachedJarPaths.put(URLUtil.getPath(url), url);
                }
            } catch (IOException e) {
                throw new RuntimeException("BUG: we can not copy embedded jar to temp directory", e);
            }
        }
        super.addURL(url);
    }

    public static synchronized File getTempDir() {
        if (tempDir != null) return tempDir;

        tempDir = new File(tempDir(), tempDirName());
        if (tempDir.mkdirs()) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    for (File f : tempDir.listFiles()) {
                        try {
                            f.delete();
                        } catch (Exception ex) {
                            LOG.debug(ex);
                        }
                    }
                    try {
                        tempDir.delete();
                    } catch (Exception ex) {
                        LOG.info("failed to delete temp dir " + tempDir + " : " + ex);
                    }
                }
            });
        }
        return tempDir;
    }

    private static final String TEMP_DIR_PREFIX = "jruby-";
    private static String tempDirName;

    private static String tempDirName() {
        String dirName = tempDirName;
        if (dirName != null) return dirName;
        try {
            String processName = ManagementFactory.getRuntimeMXBean().getName();
            return tempDirName = TEMP_DIR_PREFIX + processName.split("@")[0]; // jruby-PID
        }
        catch (Throwable ex) {
            LOG.debug(ex); // e.g. java.lang.management classes not available (on Android)
            return tempDirName = TEMP_DIR_PREFIX + Integer.toHexString(System.identityHashCode(JRubyClassLoader.class));
        }
    }

    private static String tempDir() {
        // try JRuby-specific option first
        String jrubyTmpdir = Options.JI_NESTED_JAR_TMPDIR.load();

        if (jrubyTmpdir != null) {
            return jrubyTmpdir;
        }

        // fall back on Java tmpdir if available
        try {
            return System.getProperty("java.io.tmpdir");
        }
        catch (SecurityException ex) {
            LOG.warn("could not access 'java.io.tmpdir' will use working directory", ex);
        }

        // give up and just use current dir
        return "";
    }

    /**
     * Called when the parent runtime is torn down.
     */
    @Override
    public void close() {
        if (Options.JI_CLOSE_CLASSLOADER.load()) {
            // We no longer unconditionally close the classloader due to JDK issues with the open jar files it may be
            // holding references to.
            // See jruby/jruby#6218 and https://bugs.openjdk.java.net/browse/JDK-8246714
            try {
                super.close();
            } catch (Exception ex) {
                LOG.debug(ex);
            }
        }

        try {
            // A hack to allow unloading all JDBC Drivers loaded by this classloader.
            // See http://bugs.jruby.org/4226
            getJDBCDriverUnloader().run();
        } catch (NoClassDefFoundError ncdfe) {
            // if we can't access it, we can't clear it; ignore
        } catch (Exception ex) {
            LOG.debug(ex);
        }

        terminateJarIndexCacheEntries();
    }

    protected void terminateJarIndexCacheEntries() {
        cachedJarPaths.forEach((path, url) -> {
            // close the jar file associated with the connection, since this might be cached by JDK
            try {
                URLConnection connection = url.openConnection();

                if (connection instanceof JarURLConnection) {
                    JarFile jarFile = ((JarURLConnection) connection).getJarFile();

                    try {
                        jarFile.close();
                    } catch (IOException ioe) {
                        // ignore and proceed to delete the file
                    }
                }

                // Remove reference from jar cache
                JarResource.removeJarResource(path);

                // Delete temp jar on disk
                File jarFile = new File(path);
                jarFile.delete();
            } catch (Exception e) {
                // keep trying to clean up other temp jars
            }
        });
    }

    @Deprecated
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
            }
            catch (RuntimeException e) { throw e; }
            catch (Exception e) { throw new RuntimeException(e); }
        }
        return unloader;
    }
}
