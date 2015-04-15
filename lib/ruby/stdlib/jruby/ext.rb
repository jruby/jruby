require 'java'
require 'jruby'

module JRuby
  MethodArgs2 = org.jruby.internal.runtime.methods.MethodArgs2
  IRMethodArgs = org.jruby.internal.runtime.methods.IRMethodArgs
  Helpers = org.jruby.runtime.Helpers
  Arity = org.jruby.runtime.Arity
  
  # Extensions only provides one feature right now: stealing methods from one
  # class/module and inserting them into another.
  module Extensions
    
    # Transplant the named method from the given type into self. If self is a
    # module/class, it will gain the method. If self is not a module/class, then
    # the self object's singleton class will be used.
    def steal_method(type, method_name)
      if self.kind_of? Module
        to_add = self
      else
        to_add = JRuby.reference0(self).singleton_class
      end
      
      method_name = method_name.to_str
      
      raise TypeError, "first argument must be a module/class" unless type.kind_of? Module
      
      method = JRuby.reference0(type).search_method(method_name)
      
      if !method || method.undefined?
        raise ArgumentError, "no such method `#{method_name}' on type #{type}"
      end
      
      JRuby.reference0(to_add).add_method(method)
      
      nil
    end
    module_function :steal_method
    
    # Transplant all named methods from the given type into self. See
    # JRuby::Extensions.steal_method
    def steal_methods(type, *method_names)
      for method_name in method_names do
        steal_method(type, method_name)
      end
    end
  end
  
  class ::Method
    def args
      self_r = JRuby.reference0(self)
      method = self_r.get_method
      args_ary = []
      
      case method
      when MethodArgs2
        return Helpers.parameter_list_to_parameters(JRuby.runtime, method.parameter_list, true)
      when IRMethodArgs
        a = method.parameter_list
        (0...(a.size)).step(2) do |i|
          args_ary << (a[i+1] == "" ? [a[i].to_sym] : [a[i].to_sym, a[i+1].to_sym])
        end
      else
        if method.arity == Arity::OPTIONAL
          args_ary << [:rest]
        end
      end
      
      args_ary
    end
  end
end
