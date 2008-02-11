module JavaUtilities
  def JavaUtilities.extend_proxy(java_class_name, &block)
    java_class = Java::JavaClass.for_name(java_class_name)
    java_class.extend_proxy JavaInterfaceExtender.new(java_class_name, &block)
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
      self.java_object = JavaUtilities.matching_method(constructors, args).new_instance2(self, args)
    }
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

end
