module Java
 class << self
   def const_missing(sym)
     result = JavaUtilities.get_top_level_proxy_or_package(sym)
     if const_defined? sym
       result
     else
       const_set(sym, result)
     end
   end

   def method_missing(sym, *args)
      raise ArgumentError, "Java package `java' does not have a method `#{sym}'" unless args.empty?
     JavaUtilities.get_top_level_proxy_or_package sym
   end
 end

end
 
