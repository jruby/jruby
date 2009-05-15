package org.jruby.util;

import com.martiansoftware.nailgun.NGContext;
import org.jruby.Main;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.MainExitException;
import org.jruby.exceptions.ThreadKill;

public class NailMain {
    public static void nailMain(NGContext context) {
        NailMain main = new NailMain();
        int status = main.run(context);
        if (status != 0) {
            context.exit(status);
        }
    }

    public int run(NGContext context) {
        context.assertLoopbackClient();

        // FIXME: This is almost entirely duplicated from Main.java
        RubyInstanceConfig config = new RubyInstanceConfig();
        Main main = new Main(config);
        
        try {
            // populate commandline with NG-provided stuff
            config.processArguments(context.getArgs());
            config.setCurrentDirectory(context.getWorkingDirectory());
            config.setEnvironment(context.getEnv());

            return main.run();
        } catch (MainExitException mee) {
            if (!mee.isAborted()) {
                config.getOutput().println(mee.getMessage());
                if (mee.isUsageError()) {
                    main.printUsage();
                }
            }
            return mee.getStatus();
        } catch (OutOfMemoryError oome) {
            // produce a nicer error since Rubyists aren't used to seeing this
            System.gc();

            String memoryMax = SafePropertyAccessor.getProperty("jruby.memory.max");
            String message = "";
            if (memoryMax != null) {
                message = " of " + memoryMax;
            }
            config.getError().println("Error: Your application used more memory than the safety cap" + message + ".");
            config.getError().println("Specify -J-Xmx####m to increase it (#### = cap size in MB).");

            if (config.getVerbose()) {
                config.getError().println("Exception trace follows:");
                oome.printStackTrace();
            } else {
                config.getError().println("Specify -w for full OutOfMemoryError stack trace");
            }
            return 1;
        } catch (StackOverflowError soe) {
            // produce a nicer error since Rubyists aren't used to seeing this
            System.gc();

            String stackMax = SafePropertyAccessor.getProperty("jruby.stack.max");
            String message = "";
            if (stackMax != null) {
                message = " of " + stackMax;
            }
            config.getError().println("Error: Your application used more stack memory than the safety cap" + message + ".");
            config.getError().println("Specify -J-Xss####k to increase it (#### = cap size in KB).");

            if (config.getVerbose()) {
                config.getError().println("Exception trace follows:");
                soe.printStackTrace();
            } else {
                config.getError().println("Specify -w for full StackOverflowError stack trace");
            }
            return 1;
        } catch (UnsupportedClassVersionError ucve) {
            config.getError().println("Error: Some library (perhaps JRuby) was built with a later JVM version.");
            config.getError().println("Please use libraries built with the version you intend to use or an earlier one.");

            if (config.getVerbose()) {
                config.getError().println("Exception trace follows:");
                ucve.printStackTrace();
            } else {
                config.getError().println("Specify -w for full UnsupportedClassVersionError stack trace");
            }
            return 1;
        } catch (ThreadKill kill) {
            return 0;
        }
    }
}