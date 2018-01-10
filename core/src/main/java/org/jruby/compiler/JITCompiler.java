/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006-2008 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.compiler;


import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JITCompiler implements JITCompilerMBean {
    private static final Logger LOG = LoggerFactory.getLogger(JITCompiler.class);

    public static final String RUBY_JIT_PREFIX = "rubyjit";

    final JITCounts counts = new JITCounts();
    private final ExecutorService executor;

    final Ruby runtime;
    final RubyInstanceConfig config;

    public JITCompiler(Ruby runtime) {
        this.runtime = runtime;
        this.config = runtime.getInstanceConfig();

        this.executor = new ThreadPoolExecutor(
                0, // don't start threads until needed
                2, // two max
                60, // stop then if no jitting for 60 seconds
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DaemonThreadFactory("Ruby-" + runtime.getRuntimeNumber() + "-JIT", Thread.MIN_PRIORITY));
    }

    public long getSuccessCount() {
        return counts.successCount.get();
    }

    public long getCompileCount() {
        return counts.compiledCount.get();
    }

    public long getFailCount() {
        return counts.failCount.get();
    }

    public long getCompileTime() {
        return counts.compileTime.get() / 1000;
    }

    public long getAbandonCount() {
        return counts.abandonCount.get();
    }

    public long getCodeSize() {
        return counts.codeSize.get();
    }

    public long getCodeAverageSize() {
        return counts.codeAverageSize.get();
    }

    public long getCompileTimeAverage() {
        return counts.compileTimeAverage.get() / 1000;
    }

    public long getCodeLargestSize() {
        return counts.codeLargestSize.get();
    }

    public long getIRSize() {
        return counts.irSize.get();
    }

    public long getIRAverageSize() {
        return counts.irAverageSize.get();
    }

    public long getIRLargestSize() {
        return counts.irLargestSize.get();
    }

    public String[] getFrameAwareMethods() {
        String[] frameAwareMethods = MethodIndex.FRAME_AWARE_METHODS.toArray(new String[0]);
        Arrays.sort(frameAwareMethods);
        return frameAwareMethods;
    }

    public String[] getScopeAwareMethods() {
        String[] scopeAwareMethods = MethodIndex.SCOPE_AWARE_METHODS.toArray(new String[0]);
        Arrays.sort(scopeAwareMethods);
        return scopeAwareMethods;
    }

    public void tearDown() {
        try {
            executor.shutdown();
        } catch (SecurityException se) {
            // ignore, can't shut down executor
        }
    }

    public Runnable getTaskFor(ThreadContext context, Compilable method) {
        if (method instanceof MixedModeIRMethod) {
            return new MethodJITTask(this, (MixedModeIRMethod) method, method.getClassName(context));
        } else if (method instanceof MixedModeIRBlockBody) {
            return new BlockJITTask(this, (MixedModeIRBlockBody) method, method.getClassName(context));
        }

        return new FullBuildTask(this, method);
    }

    public void buildThresholdReached(ThreadContext context, final Compilable method) {
        final RubyInstanceConfig config = context.runtime.getInstanceConfig();

        // Disable any other jit tasks from entering queue
        method.setCallCount(-1);

        Runnable jitTask = getTaskFor(context, method);

        try {
            if (config.getJitBackground() && config.getJitThreshold() > 0) {
                try {
                    executor.submit(jitTask);
                } catch (RejectedExecutionException ree) {
                    // failed to submit, just run it directly
                    jitTask.run();
                }
            } else {
                // Because are non-asynchonously build if the JIT threshold happens to be 0 we will have no ic yet.
                method.ensureInstrsReady();
                // just run directly
                jitTask.run();
            }
        } catch (Exception e) {
            throw new NotCompilableException(e);
        }
    }

    static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup().in(Ruby.class);

    public static String getHashForString(String str) {
        return getHashForBytes(RubyEncoding.encodeUTF8(str));
    }

    public static String getHashForBytes(byte[] bytes) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte aByte : sha1.digest()) {
                builder.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            return builder.toString().toUpperCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    static void log(RubyModule implementationClass, String file, int line, String name, String message, String... reason) {
        boolean isBlock = implementationClass == null;
        String className = isBlock ? "<block>" : implementationClass.getBaseName();
        if (className == null) className = "<anon class>";

        StringBuilder builder = new StringBuilder(32);
        builder.append(message).append(": ").append(className)
               .append(' ').append(name == null ? "" : name)
               .append(" at ").append(file).append(':').append(line);

        if (reason.length > 0) {
            builder.append(" because of: \"");
            for (String aReason : reason) builder.append(aReason);
            builder.append('"');
        }

        LOG.info(builder.toString());
    }
}
