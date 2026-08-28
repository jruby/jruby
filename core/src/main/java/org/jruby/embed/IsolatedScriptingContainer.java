package org.jruby.embed;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;

import org.jruby.util.ClassesLoader;
import org.jruby.util.UriLikePathHelper;

/**
 * The IsolatedScriptingContainer does set GEM_HOME and GEM_PATH and JARS_HOME
 * in such a way that it uses only resources which can be reached with classloader.
 *
 * GEM_HOME is uri:classloader://META-INF/jruby.home/lib/ruby/gems/shared
 * GEM_PATH is uri:classloader://
 * JARS_HOME is uri:classloader://jars
 *
 * But whenever you want to set them via {@link #setEnvironment(Map)} this will be honored.
 *
 * It also allows to add a classloaders to LOAD_PATH or GEM_PATH.
 *
 * This container also sets option classloader.delegate to false, i.e. the JRubyClassloader 
 * for each runtime will lookup classes first on itself before looking into the parent 
 * classloader.
 *
 * WARNING: this can give problems when joda-time is used inside the 
 *   JRubyClassloader or with current version of nokogiri (1.6.7.2) as it uses 
 *   (sun-)jdk classes which conflicts with classes nokogiri loaded into the 
 *   JRubyClassloader.
 *
 * With any classloader related problem the first thing is to try
 * <code>container.getProvider().getRubyInstanceConfig().setClassloaderDelegate(true);</code> to solve it.
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

	// give preference to jrubyClassloader over parent-classloader
	getProvider().getRubyInstanceConfig().setClassloaderDelegate(false);
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
