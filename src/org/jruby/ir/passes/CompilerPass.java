package org.jruby.ir.passes;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.ir.IRScope;

//FIXME: Names are not so great
/**
 * 
 * @author enebo
 */
public abstract class CompilerPass {
    public List<Class<? extends CompilerPass>> NO_DEPENDENCIES = new ArrayList<Class<? extends CompilerPass>>();
    
    public abstract String getLabel();
    
    // Should we run this pass on the current scope before running it on nested scopes?
    public abstract boolean isPreOrder();
    
    /**
     * Meat of an individual pass. run will call this after dependency
     * resolution.
     * @param scope is the scope to run this pass on
     * @param dependencyData is the data supplied to this pass to use to execute the pass
     */
    public abstract Object execute(IRScope scope, Object... dependencyData);
    
    public List<Class<? extends CompilerPass>> getDependencies() {
        return NO_DEPENDENCIES;
    }

    /**
     * If this pass has been previous run then return the data from that last run.
     * @returns data or null if it needs to be run
     */
    public Object previouslyRun(IRScope scope) {
        return null;
    }
    
    // Run the pass on the passed in scope!
    public Object run(IRScope scope) {
        // Make sure all dependencies are satisfied
        List<Class<? extends CompilerPass>> dependencies = getDependencies();
        Object data[] = new Object[dependencies.size()];
           
        for (int i = 0; i < data.length; i++) {
            data[i] = makeSureDependencyHasRunOnce(dependencies.get(i), scope);
        }

//        System.out.println("Executing Pass: " + getLabel());
        return execute(scope, data);
    }

    private Object makeSureDependencyHasRunOnce(Class<? extends CompilerPass> passClass, IRScope scope) {
        CompilerPass pass = createPassInstance(passClass);
        Object data = pass.previouslyRun(scope);
        
        return data == null ? pass.run(scope) : data;
    }

    private CompilerPass createPassInstance(Class<? extends CompilerPass> passClass) {
        try {
            return (CompilerPass) passClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException ex) {
            Logger.getLogger(CompilerPass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CompilerPass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CompilerPass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(CompilerPass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(CompilerPass.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(CompilerPass.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return null;
    }
}
