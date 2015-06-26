package org.jruby.embed;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * the IsolatedScriptingContainer detects the whether it is used with
 * a Thread.currentThread.contextClassLoader (J2EE) or with the classloader
 * which loaded IsolatedScriptingContainer.class (OSGi case)
 * 
 * the setup of LOAD_PATH and GEM_PATH and JRUBY_HOME uses ONLY uri: or uri:classloader:
 * protocol paths. i.e. everything lives within one or more classloaders - no jars added from
 * jave.class.path or similar "magics"
 *
 * the root of the "main" classloader is add to LOAD_PATH and GEM_PATH.
 *
 * in the OSGi case see the OSGiIsolatedScriptingContainer
 * 
 * a typical setup for the ContextClassLoader case and OSGi case looks likes this:
 * <li>LOAD_PATH == [ "uri:classloader:/META-INF/jruby.home/lib/ruby/1.9/site_ruby", 
 *                    "uri:classloader:/META-INF/jruby.home/lib/ruby/shared",
 *                    "uri:classloader:/META-INF/jruby.home/lib/ruby/1.9",
 *                    "uri:classloader:" ]</li>
 * <li>Gem::Specification.dirs ==  [ "uri:classloader:/specifications", "uri:classloader:/META-INF/jruby.home/lib/ruby/gems/shared/specifications" ]
 * here very resource is loaded via <code>Thread.currentTHread.getContextClassLoader().getResourceAsStream(...)</code>
 * 
 * <code>new URL( uri ).openStream()</code>, i.e. <code>new URL(classloader.getResource().toString()).openStream()</code> has to work for
 * those classloaders. felix, knoplerfish and equinox OSGi framework do work.
 * 
 * NOTE: <code>Gem.path</code> is base for determine the <code>Gem::Specification.dirs</code> and <code>Gem::Specification.dirs</code> is
 * used to find gemspec files of the installed gems.
 */
public class IsolatedScriptingContainer extends ScriptingContainer {

    private static final String JRUBYDIR = "/.jrubydir";
    private static final String JRUBY_HOME = "/META-INF/jruby.home";
    
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

        // get the right classloader
        ClassLoader cl = this.getClass().getClassLoader();
        if (cl == null) cl = Thread.currentThread().getContextClassLoader();
        setClassLoader( cl );

        setLoadPaths( Arrays.asList( "uri:classloader:" ) );

        // set the right jruby home
        setHomeDirectory( "uri:classloader:" + JRUBY_HOME );

        // setup the isolated GEM_PATH, i.e. without $HOME/.gem/**
        runScriptlet("require 'rubygems/defaults/jruby';"
                + "Gem::Specification.reset;"
                + "Gem::Specification.add_dir 'uri:classloader:" + JRUBY_HOME + "/lib/ruby/gems/shared';"
                + "Gem::Specification.add_dir 'uri:classloader:';");

        // setup the isolated GEM_PATH, i.e. without $HOME/.gem/**
        setEnvironment(null);
    }

    @Override
    public void setEnvironment(Map environment) {
        if (environment == null || !environment.containsKey("GEM_PATH")
                || !environment.containsKey("GEM_HOME")|| !environment.containsKey("JARS_HOME")) {
            Map<String,String> env = environment == null ? new HashMap<String,String>() : new HashMap<String,String>(environment);
            if (!env.containsKey("GEM_PATH")) env.put("GEM_PATH", "uri:classloader://");
            if (!env.containsKey("GEM_HOME")) env.put("GEM_HOME", "uri:classloader://");
            if (!env.containsKey("JARS_HOME")) env.put("JARS_HOME", "uri:classloader://jars");
            super.setEnvironment(env);
        }
        else {
            super.setEnvironment(environment);
        }
    }

    public void addLoadPath( ClassLoader cl ) {
        addLoadPath( cl, JRUBYDIR );
    }

    public void addLoadPath( ClassLoader cl, String ref ) {
        addLoadPath(createUri(cl, ref));
    }

    protected void addLoadPath(String uri) {
        runScriptlet( "$LOAD_PATH << '" + uri + "' unless $LOAD_PATH.member?( '" + uri + "' )" );
    }

    public void addGemPath( ClassLoader cl ) {
        addGemPath( cl, "/specifications" + JRUBYDIR );
    }

    public void addGemPath( ClassLoader cl, String ref ) {
        addGemPath(createUri(cl, ref));
    }

    private String createUri(ClassLoader cl, String ref) {
        URL url = cl.getResource( ref );
        if ( url == null && ref.startsWith( "/" ) ) {
            url = cl.getResource( ref.substring( 1 ) );
        }
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }
        return "uri:" + url.toString().replaceFirst( ref + "$", "" );
    }

    protected void addGemPath(String uri) {
        runScriptlet( "Gem::Specification.add_dir '" + uri + "' unless Gem::Specification.dirs.member?( '" + uri + "' )" );
    }
}
