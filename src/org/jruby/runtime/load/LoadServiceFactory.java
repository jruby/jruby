package org.jruby.runtime.load;

import org.jruby.Ruby;
import org.jruby.RbConfig;
import org.jruby.util.BuiltinScript;
import org.jruby.internal.runtime.load.LoadService;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public final class LoadServiceFactory {

    /**
     * Constructor for LoadServiceFactory is private. It isn't possible
     * to create an instance of LoadServiceFactory.
     */
    private LoadServiceFactory() {
        super();
    }

    public static ILoadService createLoadService(Ruby runtime) {
        ILoadService result = new LoadService(runtime);

        result.registerBuiltin("java", new BuiltinScript("javasupport"));
        result.registerBuiltin("rbconfig.rb", new RbConfig());

        return result;
    }
}