package org.jruby.runtime;

public class RubyOptions {
    private JavaSupport javaSupport = new JavaSupport();
    
    public JavaSupport getJavaSupport() {
        return javaSupport;
    }
    
    public class JavaSupport {
		private boolean rubyNames = true;
		private boolean javaNames = true;
		private boolean rubyModules = true;
		
		public boolean isRubyNames() {
		    return rubyNames;
		}
		
		public boolean isJavaNames() {
		    return javaNames;
		}
		
		public boolean isRubyModules() {
		    return rubyModules;
		}
		
		public void setRubyNames(boolean rubyNames) {
		    this.rubyNames = rubyNames;
		}
		
		public void setJavaNames(boolean javaNames) {
		    this.javaNames = javaNames;
		}
		
		public void setRubyModules(boolean rubyModules) {
		    this.rubyModules = rubyModules;
		}
    }
}