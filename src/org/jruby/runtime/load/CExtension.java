package org.jruby.runtime.load;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.cext.ModuleLoader;

public class CExtension implements Library {
    private LoadServiceResource resource;

    public CExtension(LoadServiceResource resource) {
        this.resource = resource;
    }

    public void load(Ruby runtime, boolean wrap) throws IOException {
        String file = resource.getAbsolutePath();
        new ModuleLoader().load(runtime, file);
    }

}
