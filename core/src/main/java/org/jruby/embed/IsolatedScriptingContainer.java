package org.jruby.embed;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;

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
 * in the OSGi case there are helper methods to add ClassLoaders to the LOAD_PATH or GEM_PATH
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

        setLoadPaths( Arrays.asList( "uri:classloader:" ) );

        // setup the isolated GEM_PATH, i.e. without $HOME/.gem/**
        setEnvironment(null);
    }

    @Override
    public void setEnvironment(Map environment) {
        if (environment == null || !environment.containsKey("GEM_PATH")) {
            Map env = environment == null ? new HashMap() : new HashMap(environment);
            env.put("GEM_PATH", "");
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

    public void addBundleToLoadPath( Bundle cl ) {
        addBundleToLoadPath( cl, JRUBYDIR );
    }

    public void addBundleToLoadPath( Bundle cl, String ref ) {
        addLoadPath(createUriFromBundle(cl, ref));
    }

    private String createUriFromBundle( Bundle cl, String ref) {
        URL url = cl.getResource( ref );
        if ( url == null && ref.startsWith( "/" ) ) {
            url = cl.getResource( ref.substring( 1 ) );
        }
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on bundle " + cl );
        }
        return "uri:" + url.toString().replaceFirst( ref + "$", "" );
    }

    private void addLoadPath(String uri) {
        runScriptlet( "$LOAD_PATH << '" + uri + "' unless $LOAD_PATH.member?( '" + uri + "' )" );
    }

    public void addBundleToGemPath( Bundle cl ) {
        addBundleToGemPath( cl, "/specifications" + JRUBYDIR );
    }

    public void addBundleToGemPath( Bundle cl, String ref ) {
        addGemPath(createUriFromBundle(cl, ref));
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

    private void addGemPath(String uri) {
        runScriptlet( "require 'rubygems/defaults/jruby';Gem::Specification.add_dir '" + uri + "' unless Gem::Specification.dirs.member?( '" + uri + "' )" );
    }
}
