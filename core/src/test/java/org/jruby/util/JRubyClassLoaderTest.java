package org.jruby.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class JRubyClassLoaderTest {

    private static final String NESTED_JAR_TMPDIR = "jruby.ji.nested.jar.tmpdir";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * A jar nested in another jar is copied to the temp dir, and close() deletes the copy when the runtime is torn
     * down.
     */
    @Test
    public void closeDeletesCopiedJars() throws Exception {
        var loader = new JRubyClassLoader(getClass().getClassLoader());
        loader.addURL(nestedJar());
        var copy = new File(loader.getURLs()[0].toURI());

        assertEquals(JRubyClassLoader.getTempDir().getAbsoluteFile(), copy.getParentFile());
        assertTrue("nested jar was not copied", copy.exists());

        loader.close();

        assertFalse("copied jar still exists after close()", copy.exists());
    }

    /**
     * Copying a nested jar creates the temp dir, which must not register anything that keeps the class loader that
     * loaded JRuby alive, like a shutdown hook whose class is loaded by it. Here that class loader is a disposable
     * one, like the class loader of an embedded JRuby, with its own temp dir so that it's the one creating it.
     */
    @Test
    public void tempDirDoesNotKeepTheClassLoaderAlive() throws Exception {
        WeakReference<ClassLoader> loader = copyAndCloseInDisposableLoader(nestedJar());
        forceGC(loader);

        assertNull("class loader was not collected after copying a nested jar and close()", loader.get());
    }

    /**
     * A copy close() can't delete, as on Windows while the jar is still open, is deleted when the JVM exits, and so
     * is the temp dir. A child JVM copies a nested jar and calls close() while the temp dir is read-only.
     */
    @Test
    public void leftoversAreDeletedAtExit() throws Exception {
        assumeTrue("needs POSIX file permissions", FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        File tmpdir = folder.newFolder();

        var command = new ArrayList<String>();
        command.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        command.addAll(List.of("-cp", System.getProperty("java.class.path"), "-D" + NESTED_JAR_TMPDIR + "=" + tmpdir));
        command.addAll(List.of(CloseWithReadOnlyTempDir.class.getName(), nestedJar().toString()));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(output, 0, process.waitFor());
        assumeTrue("close() deleted the copy in a read-only dir, e.g. as root", output.contains("kept"));
        assertArrayEquals("left in the temp dir after exit", new String[0], tmpdir.list());
    }

    public static class CloseWithReadOnlyTempDir {
        public static void main(String[] args) throws Exception {
            var loader = new JRubyClassLoader(CloseWithReadOnlyTempDir.class.getClassLoader());
            loader.addURL(URI.create(args[0]).toURL());
            var copy = new File(loader.getURLs()[0].toURI());
            Path tempDir = JRubyClassLoader.getTempDir().toPath();

            Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("r-xr-xr-x"));
            try {
                loader.close();
            } finally {
                Files.setPosixFilePermissions(tempDir, PosixFilePermissions.fromString("rwx------"));
            }
            System.out.println(copy.exists() ? "kept" : "deleted");
        }
    }

    private WeakReference<ClassLoader> copyAndCloseInDisposableLoader(URL nestedJar) throws Exception {
        List<URL> classpath = new ArrayList<>();
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
            classpath.add(new File(path).toURI().toURL());
        }
        String tmpdir = System.setProperty(NESTED_JAR_TMPDIR, folder.newFolder().getPath());
        try (var loader = new URLClassLoader(classpath.toArray(new URL[0]), ClassLoader.getPlatformClassLoader())) {
            Class<?> jrubyClassLoader = loader.loadClass(JRubyClassLoader.class.getName());
            URLClassLoader copier = (URLClassLoader) jrubyClassLoader.getConstructor(ClassLoader.class).newInstance(loader);
            jrubyClassLoader.getMethod("addURL", URL.class).invoke(copier, nestedJar);
            copier.close();

            return new WeakReference<>(loader);
        } finally {
            if (tmpdir == null) System.clearProperty(NESTED_JAR_TMPDIR);
            else System.setProperty(NESTED_JAR_TMPDIR, tmpdir);
        }
    }

    // a jar:file:...!/inner.jar URL to a jar nested in another jar
    private URL nestedJar() throws IOException {
        var inner = new ByteArrayOutputStream();
        try (var jar = new JarOutputStream(inner)) {
            jar.putNextEntry(new JarEntry("hello.txt"));
            jar.write("hello".getBytes(StandardCharsets.UTF_8));
        }
        File outer = folder.newFile("outer.jar");
        try (var jar = new JarOutputStream(new FileOutputStream(outer))) {
            jar.putNextEntry(new JarEntry("inner.jar"));
            jar.write(inner.toByteArray());
        }
        return URI.create("jar:" + outer.toURI() + "!/inner.jar").toURL();
    }

    private static void forceGC(WeakReference<?> ref) throws InterruptedException {
        for (int i = 0; i < 10 && ref.get() != null; i++) {
            System.gc();
            Thread.sleep(100);
        }
    }
}
