module Java
 class << self
   def const_missing(sym)
     begin
       JavaUtilities.get_proxy_class "#{sym}"
     rescue
       JavaUtilities.get_package_module "#{sym}"
     end
   end

   def method_missing(sym, *args)
     if JavaUtilities.is_primitive_type(sym) 
       JavaUtilities.get_proxy_class sym.to_s
     elsif sym.to_s.downcase[0] == sym.to_s[0]
       JavaUtilities.get_package_module_dot_format(sym)
     else
       JavaUtilities.get_proxy_class "#{sym}"
     end
   end
 end

end
 
module JavaPackageModuleTemplate  
  class << self

    def const_missing(const)
      JavaUtilities.get_proxy_class(@package_name + const.to_s)
    end
    private :const_missing
    
    def method_missing(sym, *args)
      if JavaUtilities.is_primitive_type(sym)
        raise ArgumentError.new("illegal package name component: #{sym}")
      elsif (s = sym.to_s[0,1]) == s.downcase
        JavaUtilities.get_package_module_dot_format(@package_name + sym.to_s)
      else
        c = JavaUtilities.get_proxy_class(@package_name + sym.to_s)
      end
    end
    private :method_missing
    
  end
end
# pull in the default package
JavaUtilities.get_package_module("Default")
