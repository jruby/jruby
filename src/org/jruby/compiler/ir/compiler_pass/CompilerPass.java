package org.jruby.compiler.ir.compiler_pass;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Tuple;

//FIXME: Names are not so great
/**
 * 
 * @author enebo
 */
public abstract class CompilerPass {
    public List<Tuple<Class<CompilerPass>, DependencyType>> NO_DEPENDENCIES = new ArrayList<Tuple<Class<CompilerPass>, DependencyType>>();
    
    public enum DependencyType {
        RETRIEVE, // If the pass has been previously run just return that
        RERUN     // Unconditionally re-run dependent pass
    }
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
    
    public List<Tuple<Class<CompilerPass>, DependencyType>> getDependencies() {
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
        List<Tuple<Class<CompilerPass>, DependencyType>> dependencies = getDependencies();
        Object data[] = new Object[dependencies.size()];
           
        for (int i = 0; i < data.length; i++) {
            Tuple<Class<CompilerPass>, DependencyType> dependency = dependencies.get(i);

            switch (dependency.b) { // type of dependency
                case RETRIEVE:
                    data[i] = makeSureDependencyHasRunOnce(dependency.a, scope);
                case RERUN:
                    data[i] = executeDependency(dependency.a, scope);
            }
        }

//        System.out.println("Executing Pass: " + getLabel());
        return execute(scope, data);
    }
    
    private Object makeSureDependencyHasRunOnce(Class<CompilerPass> passClass, IRScope scope) {
        CompilerPass pass = createPassInstance(passClass);
        Object data = pass.previouslyRun(scope);
        
        return data == null ? pass.run(scope) : data;
    }

    private Object executeDependency(Class<CompilerPass> passClass, IRScope scope) {
        return createPassInstance(passClass).run(scope);
    }
    
    private CompilerPass createPassInstance(Class<CompilerPass> passClass) {
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
