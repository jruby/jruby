package org.jruby.runtime.profile;

import org.jruby.Ruby;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;

/**
 * @author Andre Kullmann
 */
public class TestProfilingService implements ProfilingService {

    public TestProfilingService(Ruby runtime) {}

    @Override
    public ProfileCollection newProfileCollection(ThreadContext context) {
        return null;
    }

    @Override
    public MethodEnhancer newMethodEnhancer(Ruby runtime) {
        return new MethodEnhancer() {
            @Override
            public DynamicMethod enhance(String id, DynamicMethod delegate) {
                return delegate;
            }
        };
    }

    @Override
    public ProfileReporter newProfileReporter(ThreadContext context) {
        return null;
    }

    @Override
    public void addProfiledMethod(String name, DynamicMethod method) {
    }
}
