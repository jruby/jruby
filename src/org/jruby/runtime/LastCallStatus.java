package org.jruby.runtime;

import org.jruby.util.IdUtil;
import org.jruby.Ruby;

/**
 *
 * @author jpetersen
 * @version $Revision$
 */
public class LastCallStatus {
    private static final Object NORMAL = new Object();
    private static final Object PRIVATE = new Object();
    private static final Object PROTECTED = new Object();
    private static final Object VARIABLE = new Object();

    private final Ruby runtime;
    private Object status = NORMAL;

    public LastCallStatus(Ruby runtime) {
        this.runtime = runtime;
    }

    public void setNormal() {
        status = NORMAL;
    }

    public void setPrivate() {
        status = PRIVATE;
    }

    public void setProtected() {
        status = PROTECTED;
    }

    public void setVariable() {
        status = VARIABLE;
    }

    public Ruby getRuntime() {
        return runtime;
    }

    public String errorMessageFormat(String name) {
        String format = "Undefined method '%s' for %s%s%s";
        if (status == PRIVATE) {
            format = "private method '%s' called for %s%s%s";
        } else if (status == PROTECTED) {
            format = "protected method '%s' called for %s%s%s";
        } else if (status == VARIABLE) {
            if (IdUtil.isLocal(name)) {
                format = "Undefined local variable or method '%s' for %s%s%s";
            }
        }
        return format;
    }
}
