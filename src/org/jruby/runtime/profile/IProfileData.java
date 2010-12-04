package org.jruby.runtime.profile;

import java.io.PrintStream;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;

public interface IProfileData {

    /**
     * Begin profiling a new method, aggregating the current time diff in the previous
     * method's profile slot.
     *
     * @param nextMethod the serial number of the next method to profile
     * @return the serial number of the previous method being profiled
     */
	public int profileEnter(int nextMethod);

    /**
     * Fall back to previously profiled method after current method has returned.
     *
     * @param nextMethod the serial number of the next method to profile
     * @param startTime the nanotime when this invocation began
     * @return the serial number of the previous method being profiled
     */
	public int profileExit(int nextMethod, long startTime);

	/**
     * Print the profile data for a given thread (context).
     *
     * @param context the thread (context) for which to dump profile data
     */
	public void printProfile(ThreadContext context, String[] profiledNames, DynamicMethod[] profiledMethods, PrintStream out);
}