package org.jruby.embed;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import org.jruby.util.ClassesLoader;
import org.jruby.util.UriLikePathHelper;

/**
 * the IsolatedScriptingContainer does set GEM_HOME and GEM_PATH and JARS_HOME
 * in such a way that it uses only resources which can be reached with classloader.
 *
 * GEM_HOME is uri:classloader://META-INF/jruby.home/lib/ruby/gems/shared
 * GEM_PATH is uri:classloader://
 * JARS_HOME is uri:classloader://jars
 *
 * but whenever you want to set them via {@link #setEnvironment(Map)} this will be honored.
 *
 * it also comes with OSGi support which allows to add a bundle to LOAD_PATH or GEM_PATH.
 */
public class IsolatedScriptingContainer extends ScriptingContainer {

    private static final String URI_CLASSLOADER = "uri:classloader:/";

    public IsolatedScriptingContainer()
    {
        this(LocalContextScope.SINGLETON);
    }

    public IsolatedScriptingContainer( LocalContextScope scope,
                                       LocalVariableBehavior behavior )
    {
        this(scope, behavior, true);
    }

    public IsolatedScriptingContainer( LocalContextScope scope )
    {
        this(scope, LocalVariableBehavior.TRANSIENT);
    }

    public IsolatedScriptingContainer( LocalVariableBehavior behavior )
    {
        this(LocalContextScope.SINGLETON, behavior);
    }

    public IsolatedScriptingContainer( LocalContextScope scope,
                                       LocalVariableBehavior behavior,
                                       boolean lazy )
    {
        super(scope, behavior, lazy);

        List<String> loadPaths = new LinkedList<String>();
        loadPaths.add(URI_CLASSLOADER);
        setLoadPaths(loadPaths);

        // set the right jruby home
        UriLikePathHelper uriPath = new UriLikePathHelper(new ClassesLoader(getClassLoader()));
        URL url = uriPath.getResource("/.jrubydir");
        if (url != null){
            setCurrentDirectory( URI_CLASSLOADER );
        }

        // setup the isolated GEM_PATH, i.e. without $HOME/.gem/**
        setEnvironment(null);
    }

    @Override
    public void setEnvironment(Map environment) {
        if (environment == null || !environment.containsKey("GEM_PATH")
                || !environment.containsKey("GEM_HOME") || !environment.containsKey("JARS_HOME")) {
            Map<String,String> env = environment == null ? new HashMap<String,String>() : new HashMap<String,String>(environment);
            if (!env.containsKey("GEM_PATH")) env.put("GEM_PATH", URI_CLASSLOADER);
            if (!env.containsKey("GEM_HOME")) env.put("GEM_HOME", URI_CLASSLOADER);
            if (!env.containsKey("JARS_HOME")) env.put("JARS_HOME", URI_CLASSLOADER + "jars");
            super.setEnvironment(env);
        }
        else {
            super.setEnvironment(environment);
        }
    }
}
