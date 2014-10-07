package org.jruby.embed;

import java.net.URL;
import java.util.Arrays;

/**
 * the setup of LOAD_PATH and GEM_PATH and JRUBY_HOME uses ONLY uri: or uri:classloader:
 * protocol paths. i.e. everything lives within one or more classloaders - no jars added from
 * jave.class.path or similar "magics"
 *
 * the root of the "main" classloader is added to LOAD_PATH and GEM_PATH.
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
 * NOTE: <code>Gem.path</code> is used to setup the <code>Gem::Specification.dirs</code> and <code>Gem::Specification.dirs</code> are
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
        runScriptlet("require 'rubygems/defaults/jruby';"
                + "Gem::Specification.reset;"
                + "Gem::Specification.add_dir 'uri:classloader:" + JRUBY_HOME + "/lib/ruby/gems/shared';"
                + "Gem::Specification.add_dir 'uri:classloader:';");
    }

    public void addLoadPath( ClassLoader cl ) {
        addLoadPath( cl, JRUBYDIR );
    }

    public void addLoadPath( ClassLoader cl, String ref ) {
        URL url = cl.getResource( ref );
        if ( url == null && ref.startsWith( "/" ) ) {
            url = cl.getResource( ref.substring( 1 ) );
        }
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }

        String uri = "uri:" + url.toString().replaceFirst( ref + "$", "" );
        runScriptlet( "$LOAD_PATH << '" + uri + "' unless $LOAD_PATH.member?( '" + uri + "' )" );
    }

    public void addGemPath( ClassLoader cl ) {
        addGemPath( cl, "/specifications" + JRUBYDIR );
    }

    public void addGemPath( ClassLoader cl, String ref ) {
        URL url = cl.getResource( ref );
        if ( url == null && ref.startsWith( "/" ) ) {
            url = cl.getResource( ref.substring( 1 ) );
        }
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }

        String uri = "uri:" + url.toString().replaceFirst( ref + "$", "" );
        runScriptlet( "Gem::Specification.add_dir '" + uri + "' unless Gem::Specification.dirs.member?( '" + uri + "' )" );
    }

}
