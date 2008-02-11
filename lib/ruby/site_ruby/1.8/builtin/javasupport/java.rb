module Java
 class << self
   def const_missing(sym)
     JavaUtilities.get_top_level_proxy_or_package sym
   end

   def method_missing(sym, *args)
     raise ArgumentError, "wrong number of arguments (#{args.length} for 0)" unless args.empty?
     JavaUtilities.get_top_level_proxy_or_package sym
   end
 end

end
 
module JavaPackageModuleTemplate  
  @@keep = /^(__|<|>|=)|^(class|const_missing|inspect|method_missing|to_s)$|(\?|!|=)$/
  class << self
  
    # blank-slate logic relocated from org.jruby.javasupport.Java
    instance_methods.each do |meth|
      unless meth =~ @@keep
        # keep aliased methods for those we'll undef, for use by IRB and other utilities
        unless method_defined?(method_alias = :"__#{meth}__")
          alias_method method_alias, meth
        end
        undef_method meth  
      end
    end
    
    def __block__(name)
      if (name_str = name.to_s) !~ @@keep 
        (class<<self;self;end).__send__(:undef_method, name) rescue nil
      end
    end

    def const_missing(const)
      JavaUtilities.get_proxy_class(@package_name + const.to_s)
    end
    private :const_missing
    
    def method_missing(sym, *args)
      raise ArgumentError, "wrong number of arguments (#{args.length} for 0)" unless args.empty?
      JavaUtilities.get_proxy_or_package_under_package self, sym
    end
    private :method_missing
    
  end
end
# pull in the default package
JavaUtilities.get_package_module("Default")
