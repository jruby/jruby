package org.jruby.ir.passes;

import java.util.HashMap;
import java.util.Map;
import org.jruby.ir.IRScope;

/**
 *
 */
public class BasicCompilerPassListener implements CompilerPassListener {
    private Map<CompilerPass, Long> times = new HashMap<CompilerPass, Long>();

    public void alreadyExecuted(CompilerPass passClass, IRScope scope, Object data) {
    }

    public void startExecute(CompilerPass pass, IRScope scope) {
        times.put(pass, new Long(System.currentTimeMillis()));
        System.out.println("Starting pass " + pass.getLabel());
    }

    public void endExecute(CompilerPass pass, IRScope scope, Object data) {
        Long startTime = times.get(pass);
        
        if (startTime != null) {
            long timeTaken = System.currentTimeMillis() - startTime.longValue();
            System.out.println("Finished pass " + pass.getLabel() + " in " + timeTaken + "ms.");
        } else { // Not really sure we should allow same pass to be run twice in same pass order run...too defensive?
            System.out.println("Finished pass " + pass.getLabel());
        }
    }
    
}
