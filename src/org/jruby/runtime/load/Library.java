
package org.jruby.runtime.load;

import org.jruby.Ruby;

public interface Library {

    void load(Ruby runtime);
}
