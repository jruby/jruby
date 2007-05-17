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
       Package.create_package sym, sym, Java
     else
       JavaUtilities.get_proxy_class "#{sym}"
     end
   end
 end

 class Package
   # this class should be a blank slate
   keep_names = /^(__|class|inspect|object_id|to_s|equal|respond_to)/
   instance_methods.each do |m|
     undef_method m unless m =~ keep_names
   end
       
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
       cls.__send__(:define_method, sym) { package }
       package
     end
   end
 end
end

module JavaPackageModuleTemplate
  class << self
    attr :package_name      

    def const_missing(const)
      JavaUtilities.get_proxy_class(package_name + const.to_s)
    end
  end
end
# pull in the default package
JavaUtilities.get_package_module("Default")
