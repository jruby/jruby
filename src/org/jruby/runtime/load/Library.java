
package org.jruby.runtime.load;

import java.io.IOException;

import org.jruby.Ruby;

public interface Library {

    void load(Ruby runtime) throws IOException;
}
