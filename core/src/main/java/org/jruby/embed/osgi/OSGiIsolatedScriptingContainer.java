package org.jruby.embed.osgi;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.jruby.embed.IsolatedScriptingContainer;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;

import org.jruby.embed.osgi.internal.BundleWiringOSGiClassLoaderAdapter;
import org.jruby.util.Loader;
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

    @Deprecated(since = "9.0.1.0")
    private String createUri(Bundle cl, String ref) {
        URL url = cl.getResource(ref);
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
    @Deprecated(since = "9.0.1.0")
    public void addBundleToLoadPath(Bundle bundle) {
        addLoadPath(createUri(bundle, "/.jrubydir"));
    }

    /**
     * add the classloader from the given bundle to the LOAD_PATH
     * using the bundle symbolic name
     * 
     * @param symbolicName
     */
    @Deprecated(since = "9.0.1.0")
    public void addBundleToLoadPath(String symbolicName) {
        addBundleToLoadPath(toBundle(symbolicName));
    }

    /**
     * add the classloader from the given bundle to the LOAD_PATH and the GEM_PATH
     * using the bundle symbolic name
     * @param symbolicName
     */
    public void addBundle(String symbolicName) {
        addBundle(toBundle(symbolicName));
    }

    /**
     * add the classloader from the given bundle to the LOAD_PATH and the GEM_PATH
     * @param bundle
     */
    public void addBundle(Bundle bundle) {
        getProvider().getRubyInstanceConfig().addLoader(new BundleGetResources(bundle));
    }

    /**
     * add the classloader from the given bundle to the GEM_PATH
     * @param bundle
     */
    @Deprecated(since = "9.0.1.0")
    public void addBundleToGemPath(Bundle bundle) {
        addGemPath(createUri(bundle, "/specifications/.jrubydir"));
    }

    /**
     * add the classloader from the given bundle to the GEM_PATH
     * using the bundle symbolic name
     * 
     * @param symbolicName
     */
    @Deprecated(since = "9.0.1.0")
    public void addBundleToGemPath(String symbolicName) {
        addBundleToGemPath(toBundle(symbolicName));
    }

    static class BundleGetResources implements Loader
    {

        private final Bundle bundle;

        BundleGetResources(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        public URL getResource(String path) {
            return bundle.getResource(path);
        }

        @Override
        public Enumeration<URL> getResources(String path) throws IOException {
            return bundle.getResources(path);
        }

        @Override
        public Class<?> loadClass(final String name) throws ClassNotFoundException {
            return bundle.loadClass(name);
        }

        @Override
        public ClassLoader getClassLoader() {
            return new BundleWiringOSGiClassLoaderAdapter().getClassLoader(bundle);
        }
    }
}
