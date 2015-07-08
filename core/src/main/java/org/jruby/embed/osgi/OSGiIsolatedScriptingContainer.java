package org.jruby.embed.osgi;

import java.net.URL;

import org.jruby.embed.IsolatedScriptingContainer;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * adds some helper methods to add Bundle to the LOAD_PATH or GEM_PATH using the
 * IsolatedScriptingContainer as base.
 * 
 * <code>new URL( uri ).openStream()</code>, i.e. <code>new URL(classloader.getResource().toString()).openStream()</code> has to work for
 * those classloaders. felix, knoplerfish and equinox OSGi framework do work.
 */
public class OSGiIsolatedScriptingContainer extends IsolatedScriptingContainer {

    public OSGiIsolatedScriptingContainer()
    {
        this(LocalContextScope.SINGLETON);
    }

    public OSGiIsolatedScriptingContainer( LocalContextScope scope,
                                           LocalVariableBehavior behavior )
    {
        this(scope, behavior, true);
    }

    public OSGiIsolatedScriptingContainer( LocalContextScope scope )
    {
        this(scope, LocalVariableBehavior.TRANSIENT);
    }

    public OSGiIsolatedScriptingContainer( LocalVariableBehavior behavior )
    {
        this(LocalContextScope.SINGLETON, behavior);
    }

    public OSGiIsolatedScriptingContainer( LocalContextScope scope,
                                           LocalVariableBehavior behavior,
                                           boolean lazy )
    {
        super(scope, behavior, lazy);
    }

    private Bundle toBundle(String symbolicName) {
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        Bundle bundle = null;
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                bundle = b;
                break;
            }
        }
        if (bundle == null ) {
            throw new RuntimeException("unknown bundle: " + symbolicName);
        }
        return bundle;
    }

    private String createUri(Bundle cl, String ref) {
        URL url = cl.getResource( ref );
        if ( url == null && ref.startsWith( "/" ) ) {
            url = cl.getResource( ref.substring( 1 ) );
        }
        if ( url == null ) {
            throw new RuntimeException( "reference " + ref + " not found on classloader " + cl );
        }
        return "uri:" + url.toString().replaceFirst( ref + "$", "" );
    }
    /**
     * add the classloader from the given bundle to the LOAD_PATH
     * @param bundle
     */
    public void addBundleToLoadPath(Bundle bundle) {
        addLoadPath(createUri(bundle, "/.jrubydir"));
    }

    /**
     * add the classloader from the given bundle to the LOAD_PATH
     * using the bundle symbolic name
     * 
     * @param symbolicName
     */
    public void addBundleToLoadPath(String symbolicName) {
        addBundleToLoadPath(toBundle(symbolicName));
    }

    /**
     * add the classloader from the given bundle to the GEM_PATH
     * @param bundle
     */
    public void addBundleToGemPath(Bundle bundle) {
        addGemPath(createUri(bundle, "/specifications/.jrubydir"));
    }
 
    /**
     * add the classloader from the given bundle to the GEM_PATH
     * using the bundle symbolic name
     * 
     * @param symbolicName
     */
    public void addBundleToGemPath(String symbolicName) {
        addBundleToGemPath(toBundle(symbolicName));
    }
}
