package org.jruby.ir.passes;

import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.representations.CFG;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class BasicCompilerPassListener implements CompilerPassListener {
    private static final Logger LOG = LoggerFactory.getLogger(BasicCompilerPassListener.class);

    private final Map<CompilerPass, Long> times = new HashMap<>();

    @Override
    public void alreadyExecuted(CompilerPass passClass, FullInterpreterContext fic, Object data, boolean childScope) {
    }

    @Override
    public void startExecute(CompilerPass pass, FullInterpreterContext fic, boolean childScope) {
        times.put(pass, Long.valueOf(System.currentTimeMillis()));
        LOG.info("Starting " + pass.getLabel() + " on scope " + fic.getScope());
    }

    @Override
    public void endExecute(CompilerPass pass, FullInterpreterContext fic, Object data, boolean childScope) {
        Long startTime = times.get(pass);
        long timeTaken = startTime != null ? System.currentTimeMillis() - startTime.longValue() : -1;

        CFG c = requireNonNull(fic.getCFG());

        LOG.info("\nGraph:\n" + c.toStringGraph());
        LOG.info("\nInstructions[" + getScopeUUID(fic.getScope()) + "," + fic.getScope().getClass().getSimpleName() + "," +
                pass.getClass().getSimpleName() + "]:\n" + c.toStringInstrs() + "\n:Instructions");

        if (startTime > 0) {
            LOG.info("Finished " + pass.getLabel() + " on scope in " + timeTaken + "ms.");
        } else { // Not really sure we should allow same pass to be run twice in same pass order run...too defensive?
            LOG.info("Finished " + pass.getLabel() + " on scope " + fic.getScope());
        }
    }

    private String getScopeUUID(IRScope scope) {
        if (scope instanceof IRScriptBody || scope instanceof IRClosure) {
            return scope.getFile() + '#' + scope.getLine() + '#';
        }

        return scope.getFile() + '#' + scope.getLine() + '#' + scope.getId();
    }
}
