package org.jruby.embed;

import java.net.URL;

import org.jruby.util.cli.Options;

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
 * a typical setup for the ContextClassLoader case looks likes this:
 * <li>LOAD_PATH == [ "uri:classloader:/META-INF/jruby.home/lib/ruby/1.9/site_ruby", 
 *                    "uri:classloader:/META-INF/jruby.home/lib/ruby/shared",
 *                    "uri:classloader:/META-INF/jruby.home/lib/ruby/1.9",
 *                    "uri:classloader:" ]</li>
 * <li>Gem::Specification.dirs ==  [ "uri:classloader:", "uri:classloader:/META-INF/jruby.home/lib/ruby/gems/shared" ]
 * here very resource is loaded via <code>Thread.currentTHread.getContextClassLoader().getResourceAsStream(...)</code>
 * 
 * a typical setup for OSGi case (one bundle with everything):
 * <li>LOAD_PATH == [ "uri:bundle://16.0:1/META-INF/jruby.home/lib/ruby/1.9/site_ruby", 
 *                    "uri:bundle://16.0:1/META-INF/jruby.home/lib/ruby/shared",
 *                    "uri:bundle://16.0:1/META-INF/jruby.home/lib/ruby/1.9",
 *                    "uri:bundle://16.0:1" ]</li>
 * <li>Gem::Specification.dirs ==  [ "uri:bundle://16.0:1", "uri:bundle://16.0:1/META-INF/jruby.home/lib/ruby/gems/shared" ]
 * other OSGi frameworks use other uris like bundleresource:/16.fwk1661197821. here very resource is loaded via 
 * <code>new URL( uri )openStream()</code>, i.e. <code>new URL(classloader.getResource().toString()).openStream()</code> has to work for
 * those classloaders. felix and equinox OSGi framework do work.
 * 
 * NOTE: <code>Gem.path</code> is base for determine the <code>Gem::Specification.dirs</code> and <code>Gem::Specification.dirs</code> is
 * used to find gemspec files of the installed gems.
 */
public class IsolatedScriptingContainer extends ScriptingContainer {

    static {
        Options.ADD_JARS_TO_LOAD_PATH.force("false");
    }

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
        runScriptlet("Gem::Specification.reset;"
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