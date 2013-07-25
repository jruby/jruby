package org.jruby.ir.passes;

import java.util.Iterator;

public interface CompilerPassScheduler extends Iterator<CompilerPass> {
   public boolean hasNext();
   public CompilerPass next(); 
}
