package org.jruby.util;

import org.jruby.Ruby;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

public abstract class JarResource implements FileResource {
    private static Pattern PREFIX_MATCH = Pattern.compile("^(?:jar:)?(?:file:)?(.*)$");

    public static JarResource create(String pathname) {
        Matcher matcher = PREFIX_MATCH.matcher(pathname);
        String sanitized = matcher.matches() ? matcher.group(1) : pathname;

        int bang = sanitized.indexOf('!');
        if (bang < 0) {
            return null;
        }

        String jarName = sanitized.substring(0, bang);
        JarFile jar = Ruby.getGlobalRuntime().getCurrentContext().runtime.getLoadService().getJarFile(jarName);
        if (jar == null) return null;

        String slashPath = sanitized.substring(bang + 1);
        if (!slashPath.startsWith("/")) {
            slashPath = "/" + slashPath;
        }

        // TODO: Do we really need to support both test.jar!foo/bar.rb and test.jar!/foo/bar.rb cases?
        JarResource resource = createJarResource(jar, slashPath);

        if (resource == null) {
            resource = createJarResource(jar, slashPath.substring(1));
        }

        return resource;
    }

    private static JarResource createJarResource(JarFile jar, String path) {
        // Try as directory first, file second, because if test.jar contains:
        //
        // test/
        // test/foo.rb
        //
        // file:test.jar!test should be a directory and not a file.
        JarResource resource = JarDirectoryResource.create(jar, path);
        if (resource == null) {
            resource = JarFileResource.create(jar, path);
        }
        return resource;
    }

    protected final JarFile jar;

    protected JarResource(JarFile jar) {
        this.jar = jar;
    }

    @Override
    public String absolutePath() {
        return jar.getName() + "!" + entryName();
    }

    @Override
    public boolean exists() {
        // If a jar resource got created, then it always corresponds to some kind of resource
        return true;
    }

    @Override
    public boolean canRead() {
        // Can always read from a jar
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean isSymLink() {
        // Jar archives shouldn't contain symbolic links, or it would break portability. `jar`
        // command behavior seems to comform to that (it unwraps syumbolic links when creating a jar
        // and replaces symbolic links with regular file when extracting from a zip that contains
        // symbolic links). Also see:
        // http://www.linuxquestions.org/questions/linux-general-1/how-to-create-jar-files-with-symbolic-links-639381/
        return false;
    }

    abstract protected String entryName();
}
