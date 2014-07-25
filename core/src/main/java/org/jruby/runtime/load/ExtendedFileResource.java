package org.jruby.runtime.load;

import java.net.URL;

import org.jruby.util.FileResource;

public interface ExtendedFileResource extends FileResource {
    URL getURL();
}