# JavaProxy is a base class for all Java Proxies.  A Java proxy is a high-level abstraction
# that wraps a low-level JavaObject with ruby methods capable of dispatching to the JavaObjects
# native java methods.  
class JavaProxy
  class << self
    attr :java_class, true
    # Carry the Java class as a class variable on the derived classes too, otherwise 
    # JavaProxy.java_class won't work.
    def inherited(subclass)
      subclass.java_class = self.java_class unless subclass.java_class
      super
    end
    
    def singleton_class
      class << self; self; end 
    end

    def [](*args)
      if args.length > 0
        # array creation should use this variant
        ArrayJavaProxyCreator.new(java_class,*args)      
      else
        # keep this variant for kind_of? testing
      JavaUtilities.get_proxy_class(java_class.array_class)
    end
    end
    
    def setup
      unless java_class.array?
        setup_attributes
        setup_class_methods
        setup_constants
        setup_inner_classes
        setup_instance_methods
      end
    end
    
    def setup_attributes
      instance_methods = java_class.java_instance_methods.collect! {|m| m.name}
      java_class.fields.select {|field| field.public? && !field.static? }.each do |attr|
        name = attr.name
      
        # Do not add any constants that have the same name as an existing method
        next if instance_methods.detect {|m| m == name }

        class_eval do
          define_method(name) do |*args| Java.java_to_ruby(attr.value(@java_object)); end
        end

        next if attr.final?

        class_eval do
          define_method("#{name}=") do |*args|
            Java.java_to_ruby(attr.set_value(@java_object, Java.ruby_to_java(args.first)))
          end
        end
      end
    end

    def setup_class_methods
      java_class.java_class_methods.select { |m| m.public? }.group_by { |m| m.name 
      }.each do |name, methods|
        if methods.length == 1
          method = methods.first
          singleton_class.send(:define_method, name) do |*args|
            args.collect! { |v| Java.ruby_to_java(v) }
            Java.java_to_ruby(method.invoke_static(*args))
          end
        else
          singleton_class.send(:define_method, name) do |*args|
            args.collect! { |v| Java.ruby_to_java(v) }
            Java.java_to_ruby(JavaUtilities.matching_method(methods, args).invoke_static(*args))
          end
        end
        singleton_class.instance_eval do
          alias_method name.gsub(/([a-z])([A-Z])/, '\1_\2').downcase, name
        end
      end
    end
    
    def setup_constants
      fields = java_class.fields
      class_methods = java_class.java_class_methods.collect! { |m| m.name } 

      fields.each do |field|
        next unless field.public? && field.static?

        if field.final? && JavaUtilities.valid_constant_name?(field.name)
          const_set(field.name, Java.java_to_ruby(field.static_value)) unless const_defined?(field.name)
        else
          next if class_methods.detect {|m| m == field.name } 
          class_eval do
            singleton_class.send(:define_method, field.name) do |*args|
              Java.java_to_ruby(java_class.field(field.name).static_value)
            end
          end
        end
      end
    end

    def setup_inner_classes
      # the select block filters out anonymous inner classes ($1 and friends)
      # these have empty simple names, which don't really work as constant names
      java_class.declared_classes.select{|c| !c.simple_name.empty?}.each do |clazz|
        inner_class = Java::JavaClass.for_name(clazz.name)
        JavaUtilities.create_proxy_class(clazz.simple_name.intern, inner_class, self)
      end
    end
    
    def setup_instance_methods
      java_class.define_instance_methods_for_proxy(self)
    end
  end
  
  attr :java_object, true

  def java_class
    self.class.java_class
  end

  def ==(rhs)
    java_object == rhs
  end
  
  def to_s
    java_object.to_s
  end

  def eql?(rhs)
    self == rhs
  end
  
  def equal?(rhs)
    java_object.equal?(rhs)
  end
  
  def hash()
    java_object.hash()
  end
  
  def to_java_object
    java_object
  end

  def synchronized
    java_object.synchronized { yield }
  end
end