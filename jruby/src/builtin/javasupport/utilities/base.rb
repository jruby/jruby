module JavaUtilities
  def JavaUtilities.extend_proxy(java_class_name, &block)
	add_proxy_extender JavaInterfaceExtender.new(java_class_name, &block)
  end

  def JavaUtilities.setup_java_subclass(subclass, java_class)
    # add new class-variable to hold the JavaProxyClass instance
    subclass.module_eval do
      #want this var defined as a double-check in proxy generation
      @java_proxy_class = nil
      class << self
        attr :java_proxy_class, true
        def java_interfaces
          @java_interfaces.dup if @java_interfaces        
        end     
      end #self  
    end

    subclass.send(:define_method, "__jcreate!") {|*args|
      self.class.java_proxy_class ||= Java::JavaProxyClass.get_with_class(self.class)
      constructors = self.class.java_proxy_class.constructors.select {|c| c.arity == args.length }
      raise NameError.new("wrong # of arguments for constructor") if constructors.empty?
      args.collect! { |v| Java.ruby_to_java(v) }
      self.java_object = JavaUtilities.matching_method(constructors, args).new_instance(args) { |proxy, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) }
        result = __jsend!(method.name, *args)
        Java.ruby_to_java(result)
      } 
    }
  end

  def JavaUtilities.get_java_class(name)
    begin
      return Java::JavaClass.for_name(name)
    rescue NameError
      return nil
    end
  end
  
  def JavaUtilities.create_proxy_class(constant, java_class, mod)
    mod.const_set(constant.to_s, get_proxy_class(java_class))
  end
  
  def JavaUtilities.print_class(java_type, indent="")
     while (!java_type.nil? && java_type.name != "java.lang.Class")
        puts "#{indent}Name:  #{java_type.name}, access: #{ JavaUtilities.access(java_type) }  Interfaces: "
        java_type.interfaces.each { |i| print_class(i, "  #{indent}") }
        puts "#{indent}SuperClass: "
        print_class(java_type.superclass, "  #{indent}")
        java_type = java_type.superclass
     end
  end

  @primitives = {
    :boolean => true,
    :byte => true,
    :char => true,
    :short => true,
    :int => true,
    :long => true,
    :float => true,
    :double => true  
  }
  def JavaUtilities.is_primitive_type(sym)
    @primitives[sym]  
  end
end