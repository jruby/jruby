
package org.jruby.runtime.load;

import org.jruby.Ruby;
import org.jruby.exceptions.IOError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class ExternalScript implements Library {
    private final URL url;
    private final String name;

    public ExternalScript(URL url, String name) {
        this.url = url;
        this.name = name;
    }

    public void load(Ruby runtime) {
        try {
            Reader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            runtime.loadScript(name, reader, false);
            reader.close();
        } catch (IOException ioe) {
            throw IOError.fromException(runtime, ioe);
        }
    }
}
