/**
 * The profile package contains the api to implement an profiler for jruby.
 * In the sub-package builtin you can find the profiler implementation shipped with jruby.
 * To implement your own profiler implement the {@link org.jruby.runtime.profile.ProfilingService ProfilingService}
 * interface, add your implementation to the classpath an add the command line argument --profile.service my.impl.class
 *
 * Example Profiling Service Impl.
 *
 * <pre>
 *public class MyProfiler implements ProfilingService {
 *
 *  private final ConcurrentMap<Long, String> methods = new ConcurrentHashMap<Long, String>(1000);
 *
 *  public ProfileCollection newProfileCollection(ThreadContext threadContext) {
 *    return new MyCollection();
 *  }
 *
 *  public MethodEnhancer newMethodEnhancer(Ruby ruby) {
 *    return new MyMethodEnhancer();
 *  }
 *
 *  public ProfileReporter newProfileReporter(ThreadContext threadContext) {
 *    return new MyReporter();
 *  }
 *
 *  private class MyCollection implements ProfileCollection {
 *
 *    private long serial;
 *
 *    public void profileEnter(long method) {
 *      this.serial = method;
 *    }
 *
 *    public void profileExit(long method, long time) {
 *      String name = methods.get(serial);
 *      serial = method;
 *      System.out.println(name + ": " + (System.nanoTime() - time));
 *    }
 *  }
 *
 *  private class MyMethodEnhancer implements MethodEnhancer {
 *
 *    public DynamicMethod enhance(String name, DynamicMethod delegate) {
 *      if ( isMyApp( delegate ) ) {
 *        methods.putIfAbsent(delegate.getSerialNumber(), name);
 *        return new ProfilingDynamicMethod(delegate);
 *      } else {
 *        return delegate;
 *      }
 *    }
 *
 *    private boolean isMyApp( DynamicMethod method ) {
 *      //if( delegate.getRealMethod() instanceof PositionAware && ((PositionAware)delegate.getRealMethod()).getFile().contains(""))
 *      return method.getRealMethod().getImplementationClass().getId().startsWith("MyApp::");
 *    }
 *  }
 *
 *  private static class MyReporter implements ProfileReporter {
 *
 *    public void report(ProfileCollection profileCollection) {
 *      MyCollection collection = (MyCollection) profileCollection;
 *      // do nothing
 *    }
 *  }
 *
 *}
 * </pre>
 *
 * @author Andre Kullmann
 */
@Deprecated
package org.jruby.runtime.profile;