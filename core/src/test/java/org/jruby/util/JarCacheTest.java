package org.jruby.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.awaitility.Awaitility.waitAtMost;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class JarCacheTest {

    private JarCache jarCache ;

    @Before
    public void setUp() throws Exception {
        this.jarCache = new JarCache();
    }

    /**
     * Verifies that updating a JAR (i.e. change of last modification time) is recognized by the {@link JarCache} after at most 500ms.
     */
    @Test
    public void updateJar() {
        URL resource = Thread.currentThread().getContextClassLoader().getResource("foobar.jar");
        File jarFile = new File(resource.getFile());
        jarFile.setLastModified(System.currentTimeMillis() - MINUTES.toMillis(2));
        JarCache.JarIndex index = jarCache.getIndex(resource.getFile());
        assertNotNull(index);

        jarFile.setLastModified(System.currentTimeMillis() - MINUTES.toMillis(1));
        waitAtMost(750, MILLISECONDS).untilAsserted(() -> {
            JarCache.JarIndex updatedIndex = jarCache.getIndex(resource.getFile());
            assertNotSame(index, updatedIndex);
        });
    }

}