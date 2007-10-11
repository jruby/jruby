class JavaInterfaceExtender
  def initialize(java_class_name, &block)
    # don't really need @java_class here any more, keeping around
    # in case any users use this class directly
    @java_class = Java::JavaClass.for_name(java_class_name)
    @block = block
  end
  
  def extend_proxy(proxy_class)
    proxy_class.class_eval(&@block)
  end
end

class InterfaceJavaProxy < JavaProxy
  class << self  
    alias_method :new_proxy, :new

    def new(*outer_args, &block)
      proxy = allocate
      proxy.java_object = Java.new_proxy_instance(proxy.class.java_class) { |proxy2, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) }
        Java.ruby_to_java(proxy.send(method.name, *args))
      }
      proxy.send(:initialize,*outer_args,&block)
      proxy
    end

    def +(other)
      MultipleInterfaceJavaProxy.new(lambda{|*args| new_proxy(*args)}, self, other)
    end
    
    alias_method :old_eqq, :===
    
    def ===(other)
      if other.respond_to?(:java_object)
        other.java_object.java_class.interfaces.include?(self.java_class)
      else
        old_eqq(other)
      end
    end
  end
    
  def self.impl(*meths, &block)
    block = lambda {|*args| send(:method_missing, *args) } unless block

    Class.new(self) do
      define_method(:method_missing) do |name, *args|
        return block.call(name, *args) if meths.empty? || meths.include?(name)
        super
      end
    end.new
  end
end

# TODO: I think we can drop this now
class MultipleInterfaceJavaProxy
  attr_reader :interfaces
    
  def initialize(creator, *args)
    @creator = creator
    @interfaces = args.map{ |v| into_arr(v) }.flatten
  end

  def <<(other)
    @interfaces += into_arr(other)
  end

  def +(other)
    MultipleInterfaceJavaProxy.new @creator, *(@interfaces + into_arr(other))
  end
    
  def new(*outer_args, &block)
    @interfaces.freeze unless @interfaces.frozen?
    proxy = @creator.call(*outer_args)
    proxy.java_object = Java.new_proxy_instance(*@interfaces) { |proxy2, method, *args|
      args.collect! { |arg| Java.java_to_ruby(arg) }
      Java.ruby_to_java(proxy.__jsend!(method.name, *args))
    }
    proxy
  end

  def ===(other)
    if other.respond_to?(:java_object)
      (@interfaces - other.java_object.java_class.interfaces) == []
    else
      super
    end
  end

  private
  def into_arr(other)
    case other
      when MultipleInterfaceJavaProxy: other.interfaces
      else [other.java_class]
    end
  end
end

# template for Java interface modules, not used directly
module JavaInterfaceTemplate
 class << self
  attr :java_class

private # not intended to be called directly by users
  # TODO: this should be implemented in JavaClass.java, where we can
  # check for reserved Ruby names, conflicting methods, etc.
  def implement(clazz)
    @java_class.java_instance_methods.each do |meth|
      name = meth.name
      clazz.module_eval <<-EOM
        def #{name}(*args); end unless method_defined?(:#{name})
      EOM
    end
  end

public

  def append_features(clazz)
    if clazz.instance_of?(Class)
      java_class = @java_class
      clazz.module_eval do
        # not allowed for original (non-generated) Java classes
        # note: not allowing for any previously created class right now;
        # this restriction might be loosened later (post-1.0.0) for generated classes
        if (@java_class && !(class<<self;self;end).method_defined?(:java_proxy_class)) || @java_proxy_class
          raise ArgumentError.new("can't add Java interface to existing Java class!")
        end

        unless @java_interfaces
          @java_interfaces = [java_class]

          # setup new, etc unless this is a ConcreteJavaProxy subclass
          unless method_defined?(:__jcreate!)
            class << self
              alias_method :__jredef_new, :new
              private :__jredef_new

              def new(*args, &block)
                proxy = allocate
                proxy.__send__(:__jcreate!,*args,&block)
                proxy.__send__(:initialize,*args,&block)
                proxy
              end

              def java_interfaces
                @java_interfaces
              end
              private :java_interfaces

            end #self

            def __jcreate!(*ignored_args)
              interfaces = self.class.send(:java_interfaces)
              __jcreate_proxy!(interfaces, *ignored_args)
            end

            def __jcreate_meta!(*ignored_args)
              interfaces = (class << self; self; end).send(:java_interfaces)
              __jcreate_proxy!(interfaces, *ignored_args)
            end

            def __jcreate_proxy!(interfaces, *ignored_args)
              interfaces.freeze unless interfaces.frozen?
              self.java_object = Java.new_proxy_instance(*interfaces) do |proxy2, method, *args|
                args.collect! { |arg| Java.java_to_ruby(arg) }
                Java.ruby_to_java(self.__send__(method.name, *args))
              end
            end
            private :__jcreate!, :__jcreate_meta!, :__jcreate_proxy!

            include ::JavaProxyMethods

            def java_class
              java_object.java_class
            end

            alias_method :old_eqq, :===

            def ===(other)
              # TODO: WRONG - get interfaces from class
              if other.respond_to?(:java_object)
                (self.class.java_interfaces - other.java_object.java_class.interfaces) == []
              else
                old_eqq(other)
              end
            end
          end

            # setup implement
          unless method_defined?(:implement)
            class << self
              private
              def implement(ifc)
                ifc.send(:implement,self) if @java_interfaces && @java_interfaces.include?(ifc.java_class)
              end
              def implement_all
                @java_interfaces.each do |ifc| JavaUtilities.get_interface_module(ifc).send(:implement,self); end
              end
            end #self
          end
          
        else
          @java_interfaces << java_class unless @java_interfaces.frozen? || @java_interfaces.include?(java_class)
        end
      end    
    elsif clazz.instance_of?(Module)
      # assuming the user wants a collection of interfaces that can be
      # included together. make it so.
      ifc_mod = self
      clazz.module_eval do
        # not allowed for existing Java interface modules
        raise ArgumentError.new("can't add Java interface to existing Java interface!") if @java_class
      
        unless @java_interface_mods
          @java_interface_mods = [ifc_mod]
          class << self
            def append_features(clazz)
              @java_interface_mods.each do |ifc| ifc.append_features(clazz); end
              super
            end
          end #self
        else
          @java_interface_mods << ifc_mod unless @java_interface_mods.include?(ifc_mod)
        end
      end  
    else
      raise TypeError.new("illegal type for include: #{clazz}")    
    end
    super
  end #append_features
  
  def extended(obj)
     metaclass = class << obj; self; end
     interface_class = self
     metaclass.instance_eval { include interface_class }
   end

  # array creation/identity
  def [](*args)
    unless args.empty?
      # array creation should use this variant
      ArrayJavaProxyCreator.new(java_class,*args)      
    else
      # keep this variant for kind_of? testing
      JavaUtilities.get_proxy_class(java_class.array_class)
    end
  end
  
  # support old-style impl
  def impl(*args,&block)
    JavaUtilities.get_deprecated_interface_proxy(@java_class).impl(*args,&block)  
  end

  def new(*args,&block)
    JavaUtilities.get_deprecated_interface_proxy(@java_class).new(*args,&block)
  end
  
  def deprecated
    JavaUtilities.get_deprecated_interface_proxy(@java_class)  
  end

 end #self
end #JavaInterface
