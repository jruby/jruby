package org.jruby.ir.persistence;

import org.jruby.ir.IRScope;

public class IRPeristenceFacade {
    
    public static String persist(IRScope irScopeToPersist) {
        String instructionsString = irScopeToPersist.toStringInstrs();
        System.out.println(instructionsString);
        return instructionsString;
    }
}
