package org.jruby.ir.passes;

import org.jruby.ir.IRScope;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * A mechanism for executing code against an IRScope or transforming the
 * IRScopes dependent data.  A Compiler pass may or may not affect the state
 * of an IRScope (possibly including any child IRScopes) and it may
 * or may not depend on other compiler passes to execute first.
 *
 * For dependencies between compiler passes, getDependencies will return
 * a list of all dependent passes.  Those passes in turn will end up having
 * their own dependencies.  The order of execution is depth-first, but if the
 * pass recognizes that the data it depends on already exists, then it does
 * not run the pass.  It will just return the existing data.  If you want to
 * guarantee (re)execution, then you should call invalidate().
 */
public abstract class CompilerPass {
    public static List<Class<? extends CompilerPass>> NO_DEPENDENCIES = new ArrayList<Class<? extends CompilerPass>>();

    private List<CompilerPassListener> listeners = new ArrayList<CompilerPassListener>();

    /**
     * What is the user-friendly name of this compiler pass
     */
    public abstract String getLabel();

    @Override
    public int hashCode() {
        return getLabel().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return (other != null) && (other instanceof CompilerPass) && (getLabel() == ((CompilerPass)other).getLabel());
    }

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
     * If this pass has been previous run, then return the data from that last run.
     * Specific scopes can override this behavior.
     *
     * @returns data or null if it needs to be run
     */
    public Object previouslyRun(IRScope scope) {
        return scope.getExecutedPasses().contains(this) ? new Object() : null;
    }

    /**
     * The data that this pass is responsible for will get invalidated so that
     * if this pass is then executed it will generate new pass data. Note
     * that some data will destructively manipulate dependent compiler pass
     * data. In that case, the pass may wipe more than just it's data.
     * In that case, an execute() should still rebuild everything fine because
     * all compiler passes list their dependencies.
     *
     * @param scope is where the pass stores its data.
     * @returns true if invalidation succeeded, false otherwise.
     */
    public boolean invalidate(IRScope scope) {
        // System.out.println("--- INVALIDATING " + this.getLabel() + " on scope: " + scope);
        scope.getExecutedPasses().remove(this);
        return true;
    }

    // Run the pass on the passed in scope!
    protected Object run(IRScope scope, boolean force, boolean childScope) {
        // System.out.println("--- RUNNING " + this.getLabel() + " on scope: " + scope);
        Object prevData = null;
        if (!force && (prevData = previouslyRun(scope)) != null) {
            // System.out.println("--- RETURNING OLD RESULT ---");
            return prevData;
        }

        List<Class<? extends CompilerPass>> dependencies = getDependencies();
        Object data[] = new Object[dependencies.size()];

        for (int i = 0; i < data.length; i++) {
            data[i] = makeSureDependencyHasRunOnce(dependencies.get(i), scope, childScope);
        }

        for (CompilerPassListener listener: scope.getManager().getListeners()) {
            listener.startExecute(this, scope, childScope);
        }

        // Record this pass
        scope.getExecutedPasses().add(this);

        Object passData = execute(scope, data);

        for (CompilerPassListener listener: scope.getManager().getListeners()) {
            listener.endExecute(this, scope, passData, childScope);
        }

        return passData;
    }

    public Object run(IRScope scope, boolean force) {
        return run(scope, force, false);
    }

    public Object run(IRScope scope) {
        return run(scope, false, false);
    }

    private Object makeSureDependencyHasRunOnce(Class<? extends CompilerPass> passClass, IRScope scope, boolean childScope) {
        CompilerPass pass = createPassInstance(passClass);
        Object data = pass.previouslyRun(scope);

        if (data == null) {
            data = pass.run(scope, false, childScope);
        } else {
            for (CompilerPassListener listener: scope.getManager().getListeners()) {
                listener.alreadyExecuted(pass, scope, data, childScope);
            }
        }
        return data;
    }

    public static CompilerPass createPassInstance(Class<? extends CompilerPass> passClass) {
        try {
            return (CompilerPass) passClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException ex) {
            LoggerFactory.getLogger(CompilerPass.class).error(ex);
        } catch (IllegalAccessException ex) {
            LoggerFactory.getLogger(CompilerPass.class).error(ex);
        } catch (IllegalArgumentException ex) {
            LoggerFactory.getLogger(CompilerPass.class).error(ex);
        } catch (InvocationTargetException ex) {
            LoggerFactory.getLogger(CompilerPass.class).error(ex);
        } catch (NoSuchMethodException ex) {
            LoggerFactory.getLogger(CompilerPass.class).error(ex);
        } catch (SecurityException ex) {
            LoggerFactory.getLogger(CompilerPass.class).error(ex);
        }

        return null;
    }

    public static CompilerPass createPassInstance(String passClassName) {
        try {
            String clazzName = "org.jruby.ir.passes." + passClassName;
            Class<? extends CompilerPass> clazz =
                    (Class<? extends CompilerPass>) Class.forName(clazzName);
            return createPassInstance(clazz);
        } catch (ClassNotFoundException ex) {
            // FIXME: Do this in a nice way even if only for test code
            System.out.println("No such pass: " + ex);
            System.exit(-1);
        }

        return null;
    }

    public static List<CompilerPass> getPassesFromString(String passList, String defaultPassList) {
        if (passList == null) passList = defaultPassList;

        List<CompilerPass> passes = new ArrayList<CompilerPass>();

        if (!passList.equals("")) {
            for (String passClassName : passList.split(",")) {
                passes.add(createPassInstance(passClassName));
            }
        }

        return passes;
    }
}
