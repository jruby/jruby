package org.jruby.ir.passes;

import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
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

    static final Logger LOG = LoggerFactory.getLogger(CompilerPass.class);

    private static final List<Class<? extends CompilerPass>> NO_DEPENDENCIES = Collections.emptyList();

    /**
     * What is the user-friendly name of this compiler pass
     */
    public abstract String getLabel();

    /**
     * Shorter label
     */
    public String getShortLabel() {
        return getLabel();
    }

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
     * @param fic is the FullInterpreterContext to run this pass on
     * @param dependencyData is the data supplied to this pass to use to execute the pass
     */
    public abstract Object execute(FullInterpreterContext fic, Object... dependencyData);

    public List<Class<? extends CompilerPass>> getDependencies() {
        return NO_DEPENDENCIES;
    }

    /**
     * If this pass has been previous run, then return the data from that last run.
     * Specific scopes can override this behavior.
     *
     * @returns data or null if it needs to be run
     */
    public Object previouslyRun(FullInterpreterContext fic) {
        return fic.getExecutedPasses().contains(this) ? new Object() : null;
    }

    /**
     * The data that this pass is responsible for will get invalidated so that
     * if this pass is then executed it will generate new pass data. Note
     * that some data will destructively manipulate dependent compiler pass
     * data. In that case, the pass may wipe more than just it's data.
     * In that case, an execute() should still rebuild everything fine because
     * all compiler passes list their dependencies.
     *
     * @param fic is where the pass stores its data.
     * @returns true if invalidation succeeded, false otherwise.
     */
    public boolean invalidate(FullInterpreterContext fic) {
        // System.out.println("--- INVALIDATING " + this.getLabel() + " on scope: " + scope);
        fic.getExecutedPasses().remove(this);
        return true;
    }

    // Run the pass on the passed in scope!
    protected Object run(FullInterpreterContext fic, boolean force, boolean childScope) {
        // System.out.println("--- RUNNING " + this.getLabel() + " on scope: " + scope);
        Object prevData = null;
        if (!force && (prevData = previouslyRun(fic)) != null) {
            // System.out.println("--- RETURNING OLD RESULT ---");
            return prevData;
        }

        List<Class<? extends CompilerPass>> dependencies = getDependencies();
        Object data[] = new Object[dependencies.size()];

        for (int i = 0; i < data.length; i++) {
            data[i] = makeSureDependencyHasRunOnce(dependencies.get(i), fic, childScope);
        }

        for (CompilerPassListener listener: fic.getScope().getManager().getListeners()) {
            listener.startExecute(this, fic, childScope);
        }

        // Record this pass
        fic.getExecutedPasses().add(this);

        if (Options.IR_PRINT_OPT.load()) {
            LOG.info("Printing IR CFG before " + getLabel() + " for " + fic.getScope().getId() + ":\n" + fic.toStringInstrs());
        }

        Object passData = execute(fic, data);

        if (Options.IR_PRINT_OPT.load()) {
            LOG.info("Printing IR CFG after " + getLabel() + " for " + fic.getScope().getId() + ":\n" + fic.toStringInstrs());
        }

        for (CompilerPassListener listener: fic.getScope().getManager().getListeners()) {
            listener.endExecute(this, fic, passData, childScope);
        }

        return passData;
    }

    public Object run(FullInterpreterContext fic, boolean force) {
        return run(fic, force, false);
    }

    public Object run(FullInterpreterContext fic) {
        return run(fic, false, false);
    }

    private Object makeSureDependencyHasRunOnce(Class<? extends CompilerPass> passClass, FullInterpreterContext fic, boolean childScope) {
        CompilerPass pass = createPassInstance(passClass);
        Object data = pass.previouslyRun(fic);

        if (data == null) {
            data = pass.run(fic, false, childScope);
        } else {
            for (CompilerPassListener listener: fic.getScope().getManager().getListeners()) {
                listener.alreadyExecuted(pass, fic, data, childScope);
            }
        }
        return data;
    }

    public static CompilerPass createPassInstance(Class<? extends CompilerPass> passClass) {
        try {
            return passClass.getDeclaredConstructor().newInstance();
        }
        catch (NoSuchMethodException|IllegalAccessException|IllegalArgumentException ex) {
            LOG.error("failed to create compiler pass: '" + passClass.getName() + "'", ex);
        }
        catch (InstantiationException|InvocationTargetException|SecurityException ex) {
            LOG.error("failed to create compiler pass: '" + passClass.getName() + "'", ex);
        }
        return null;
    }

    public static CompilerPass createPassInstance(String passClassName) {
        final String className = "org.jruby.ir.passes." + passClassName;
        try {
            Class<? extends CompilerPass> clazz = (Class<? extends CompilerPass>) Class.forName(className);
            return createPassInstance(clazz);
        }
        catch (ClassNotFoundException ex) {
            LOG.warn("skipping unknown compiler pass name: '" + className + "'");
        }
        return null;
    }

    public static List<CompilerPass> getPassesFromString(String passList, String defaultPassList) {
        if (passList == null) passList = defaultPassList;

        final List<CompilerPass> passes;

        if ( ! passList.isEmpty() ) {
            List<String> split = StringSupport.split(passList, ',');
            passes = new ArrayList<>(split.size());
            for ( String passClassName : split ) {
                if ( ! passClassName.isEmpty() ) {
                    CompilerPass pass = createPassInstance(passClassName);
                    if ( pass != null ) passes.add(pass);
                }
            }
        }
        else {
            passes = new ArrayList<>(2);
        }

        return passes;
    }
}
