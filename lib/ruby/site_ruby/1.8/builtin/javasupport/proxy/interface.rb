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
#    alias_method :new_proxy, :new
#
    def new(*outer_args, &block)
      proxy = allocate
      JavaUtilities.set_java_object(proxy, Java.new_proxy_instance(proxy.class.java_class) { |proxy2, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) }
        Java.ruby_to_java(proxy.send(method.name, *args))
      })
      proxy.send(:initialize,*outer_args,&block)
      proxy
    end

#    alias_method :old_eqq, :===
#    
#    def ===(other)
#      if other.respond_to?(:java_object)
#        other.java_object.java_class.interfaces.include?(self.java_class)
#      else
#        old_eqq(other)
#      end
#    end
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
      # initialize if it hasn't be
      @java_class ||= nil
      
      java_class = @java_class
      clazz.module_eval do
        # initialize thses if they haven't been
        @java_class ||= nil
        @java_proxy_class ||= nil
        
        # not allowed for original (non-generated) Java classes
        # note: not allowing for any previously created class right now;
        # this restriction might be loosened later (post-1.0.0) for generated classes
        if (@java_class && !(class<<self;self;end).method_defined?(:java_proxy_class)) || @java_proxy_class
          raise ArgumentError.new("can't add Java interface to existing Java class!")
        end

        @java_interfaces ||= nil
        unless @java_interfaces
          @java_interfaces = [java_class]

          # setup new, etc unless this is a ConcreteJavaProxy subclass
          unless method_defined?(:__jcreate!)
            
            # First we make modifications to the class, to adapt it to being
            # both a Ruby class and a proxy for a Java type
            
            class << self
              attr_reader :java_interfaces # list of interfaces we implement
              
              # We capture the original "new" and make it private
              alias_method :__jredef_new, :new
              private :__jredef_new

              # The replacement "new" allocates and inits the Ruby object as before, but
              # also instantiates our proxified Java object by calling __jcreate!
              def new(*args, &block)
                proxy = allocate
                proxy.__send__(:__jcreate!,*args,&block)
                proxy.__send__(:initialize,*args,&block)
                proxy
              end
            end #self

            # Next, we define a few private methods that we'll use to manipulate
            # the Java object contained within this Ruby object
            
            # jcreate instantiates the proxy object which implements all interfaces
            # and which is wrapped and implemented by this object
            def __jcreate!(*ignored_args)
              interfaces = self.class.send(:java_interfaces)
              __jcreate_proxy!(interfaces, *ignored_args)
            end

            # Used by our duck-typification of Proc into interface types, to allow
            # coercing a simple proc into an interface parameter.
            def __jcreate_meta!(*ignored_args)
              interfaces = (class << self; self; end).send(:java_interfaces)
              __jcreate_proxy!(interfaces, *ignored_args)
            end

            # jcreate_proxy2 is the optimized version using a generated proxy
            # impl that implements the interface directly and dispatches to the
            # methods directly
            def __jcreate_proxy!(interfaces, *ignored_args)
              interfaces.freeze unless interfaces.frozen?
              JavaUtilities.set_java_object(self, Java.new_proxy_instance2(self, interfaces))
            end
            private :__jcreate!, :__jcreate_meta!, :__jcreate_proxy!, :__jcreate_proxy!

            include ::JavaProxyMethods

            # If we hold a Java object, we need a java_class accessor
            def java_class
              java_object.java_class
            end

            # Because we implement Java interfaces now, we need a new === that's
            # aware of those additional "virtual" supertypes
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

          # Now we add an "implement" and "implement_all" methods to the class
          unless method_defined?(:implement)
            class << self
              private
              # implement is called to force this class to create stubs for all
              # methods in the given interface, so they'll show up in the list
              # of methods and be invocable without passing through method_missing
              def implement(ifc)
                # call implement on the interface if we intend to implement it
                ifc.send(:implement,self) if @java_interfaces && @java_interfaces.include?(ifc.java_class)
              end
              
              # implement all forces implementation of all interfaces we intend
              # for this class to implement
              def implement_all
                # iterate over interfaces, invoking implement on each
                @java_interfaces.each do |ifc| JavaUtilities.get_interface_module(ifc).send(:implement,self); end
              end
            end #self
          end
          
        else
          # we've already done the above priming logic, just add another interface
          # to the list of intentions unless we're past the point of no return or
          # already intend to implement the given interface
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
      
        # To turn a module into an "interface collection" we add a class instance
        # variable to hold the list of interfaces, and modify append_features
        # for this module to call append_features on each of those interfaces as
        # well
        unless @java_interface_mods
          @java_interface_mods = [ifc_mod]
          class << self
            def append_features(clazz)
              @java_interface_mods.each do |ifc| ifc.append_features(clazz); end
              super
            end
          end #self
        else
          # already set up append_features, just add the interface if we haven't already
          @java_interface_mods << ifc_mod unless @java_interface_mods.include?(ifc_mod)
        end
      end  
    else
      raise TypeError.new("illegal type for include: #{clazz}")    
    end
    super
  end #append_features
  
  # Old interface extension behavior; basicaly just performs the include logic
  # above. TODO: This should probably also force the jcreate_proxy call, since
  # we're making a commitment to implement only one interface.
  def extended(obj)
     metaclass = class << obj; self; end
     interface_class = self
     metaclass.instance_eval { include interface_class }
   end

  # array-of-interface-type creation/identity
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
