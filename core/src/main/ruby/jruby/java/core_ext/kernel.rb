# Create convenience methods for top-level java packages so we do not need to prefix
# with 'Java::'.  We undef these methods within Package in case we run into 'com.foo.com'.
[:java, :javax, :javafx, :com, :org].each do |meth|
  Kernel.module_eval <<-EOM
    def #{meth}
      JavaUtilities.get_package_module_dot_format('#{meth}')    
    end
  EOM
end
