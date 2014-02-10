package org.jruby.ir.operands;

/**
 *  When we optimize full local variables to be temporary ones we like to keep the name
 * of what we renamed them as.  This is just enough wrapper for us to maintain a nice
 * debug string.
 */
public class TemporaryLocalReplacementVariable extends TemporaryLocalVariable {
    private final String oldName;

    public TemporaryLocalReplacementVariable(String oldName, int offset) {
        super(offset);

        this.oldName = oldName;
    }

    @Override
    public String getPrefix() {
        return "%t_" + oldName + "_";
    }
}
