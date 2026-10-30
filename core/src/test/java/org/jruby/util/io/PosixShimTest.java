package org.jruby.util.io;

import jnr.constants.platform.Errno;
import org.jruby.Ruby;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class PosixShimTest {

    private static Ruby runtime;

    @BeforeClass
    public static void createRuntime() {
        runtime = Ruby.newInstance();
    }

    @AfterClass
    public static void tearDownRuntime() {
        runtime.tearDown();
    }

    /**
     * errno is stored as an ordinal, so that the threads that set it don't keep JRuby's class loader alive. It must
     * still give back the exact constant, including aliases like EWOULDBLOCK and constants this platform doesn't define.
     */
    @Test
    public void errnoKeepsTheExactConstant() {
        var shim = new PosixShim(runtime);

        for (Errno errno : Errno.values()) {
            shim.setErrno(errno);

            assertSame(errno, shim.getErrno());
        }
    }

    @Test
    public void errnoIsPerThread() throws InterruptedException {
        var shim = new PosixShim(runtime);
        shim.setErrno(Errno.EAGAIN);

        var otherThreadErrno = new AtomicReference<>(Errno.EBADF);
        var thread = new Thread(() -> otherThreadErrno.set(shim.getErrno()));
        thread.start();
        thread.join();

        assertNull(otherThreadErrno.get());
        assertSame(Errno.EAGAIN, shim.getErrno());
    }

    @Test
    public void settingNullClearsErrno() {
        var shim = new PosixShim(runtime);
        shim.setErrno(Errno.EAGAIN);

        shim.setErrno(null);

        assertNull(shim.getErrno());
    }
}
