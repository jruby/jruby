package org.jruby.runtime.load;

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

    public static ILoadService createLoadService() {
        return new LoadService();
    }
}