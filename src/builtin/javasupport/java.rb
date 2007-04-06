module Java
 class << self
   def const_missing(sym)
      JavaUtilities.get_proxy_class "#{sym}"
   end

   def method_missing(sym, *args)
     if JavaUtilities.is_primitive_type(sym) 
       JavaUtilities.get_proxy_class sym.to_s
     elsif sym.to_s.downcase[0] == sym.to_s[0]
       Package.create_package sym, sym, Java
     else
       JavaUtilities.get_proxy_class "#{sym}"
     end
   end
 end

 class Package
   # this class should be a blank slate
   
   def initialize(name)
     @name = name
   end
   
   def _name; @name; end

   def singleton; class << self; self; end; end 

   def method_missing(sym, *args)
     if sym.to_s.downcase[0] == sym.to_s[0]
       self.class.create_package sym, "#{@name}.#{sym}", singleton
     else
       JavaUtilities.get_proxy_class "#{@name}.#{sym}"
     end
   end

   class << self
     def create_package(sym, package_name, cls)
       package = Java::Package.new package_name
       cls.send(:define_method, sym) { package }
       package
     end
   end
 end
end