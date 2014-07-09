package org.jruby.runtime.load;

import org.jruby.RubyInstanceConfig;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

class DebugLog {
    private static final Logger LOG = LoggerFactory.getLogger("LoadService");

    private final String typeMessage;

    private DebugLog(String typeMessage) {
        this.typeMessage = typeMessage;
    }

    public void logTry(String path) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            LOG.info("trying " + typeMessage + ": " + path);
        }
    }

    public void logFound(String path) {
        if (RubyInstanceConfig.DEBUG_LOAD_SERVICE) {
            LOG.info("found " + typeMessage + ": " + path);
        }
    }

    public static final DebugLog Builtin = new DebugLog("builtinLib");
    public static final DebugLog JarExtension = new DebugLog("jarExtension");
    public static final DebugLog Resource = new DebugLog("fileResource");
}
