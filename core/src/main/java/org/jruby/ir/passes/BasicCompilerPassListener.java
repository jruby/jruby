package org.jruby.ir.passes;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class BasicCompilerPassListener implements CompilerPassListener {
    private static final Logger LOG = LoggerFactory.getLogger(BasicCompilerPassListener.class);

    private Map<CompilerPass, Long> times = new HashMap<CompilerPass, Long>();

    @Override
    public void alreadyExecuted(CompilerPass passClass, IRScope scope, Object data, boolean childScope) {
    }

    @Override
    public void startExecute(CompilerPass pass, IRScope scope, boolean childScope) {
        times.put(pass, new Long(System.currentTimeMillis()));
        LOG.info("Starting " + pass.getLabel() + " on scope " + scope);
    }

    @Override
    public void endExecute(CompilerPass pass, IRScope scope, Object data, boolean childScope) {
        Long startTime = times.get(pass);
        long timeTaken = startTime != null ? System.currentTimeMillis() - startTime.longValue() : -1;

        CFG c = scope.getCFG();

        if (c != null) {
            LOG.info("\nGraph:\n" + c.toStringGraph());
            LOG.info("\nInstructions[" + getScopeUUID(scope) + "," + scope.getClass().getSimpleName() + "," +
                    pass.getClass().getSimpleName() + "]:\n" + c.toStringInstrs() + "\n:Instructions");
        } else {
            LOG.info("\n  instrs:\n" + scope.toStringInstrs());
        }


        if (startTime > 0) {
            LOG.info("Finished " + pass.getLabel() + " on scope in " + timeTaken + "ms.");
        } else { // Not really sure we should allow same pass to be run twice in same pass order run...too defensive?
            LOG.info("Finished " + pass.getLabel() + " on scope " + scope);
        }
    }

    private String getScopeUUID(IRScope scope) {
        if (scope instanceof IRScriptBody || scope instanceof IRClosure) {
            return scope.getFile() + '#' + scope.getLine() + '#';
        }

        return scope.getFile() + '#' + scope.getLine() + '#' + scope.getId();
    }
}
