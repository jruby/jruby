package org.jruby.util;

import com.martiansoftware.nailgun.NGContext;
import org.jruby.Main;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.executable.Script;

public class NailMain {
    public static final ClassCache<Script> classCache;

    static {
         classCache = new ClassCache<Script>(NailMain.class.getClassLoader(), RubyInstanceConfig.JIT_MAX_METHODS_LIMIT);
    }
    public static void nailMain(NGContext context) {
        NailMain main = new NailMain();
        int status = main.run(context);
        if (status != 0) {
            context.exit(status);
        }
        // force a full GC so objects aren't kept alive longer than they should
        System.gc();
    }

    public int run(NGContext context) {
        context.assertLoopbackClient();

        RubyInstanceConfig config = new RubyInstanceConfig();
        Main main = new Main(config);
        
        config.setCurrentDirectory(context.getWorkingDirectory());
        config.setEnvironment(context.getEnv());

        // reuse one cache of compiled bodies
        config.setClassCache(classCache);

        return main.run(context.getArgs()).getStatus();
    }
}