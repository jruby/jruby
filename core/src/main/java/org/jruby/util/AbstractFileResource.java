package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;
import jnr.constants.platform.Errno;

abstract class AbstractFileResource implements FileResource {

    @Override
    public boolean canExecute() {
        return false;
    }

    public int errno() {
        return Errno.ENOENT.intValue();
    }

    @Override
    public InputStream inputStream() throws ResourceException {
        if (!exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }
        if (isDirectory()) {
            throw new ResourceException.FileIsDirectory(absolutePath());
        }
        try {
            return openInputStream();
        }
        catch (IOException e) {
            throw new ResourceException.IOError(e);
        }
    }

    abstract InputStream openInputStream() throws IOException;

}
