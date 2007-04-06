class JavaInterfaceExtender
  def initialize(java_class_name, &block)
    @java_class = Java::JavaClass.for_name(java_class_name)
    @block = block
  end
  
  def extend_proxy(proxy_class)
    proxy_class.class_eval &@block if @java_class.assignable_from? proxy_class.java_class
  end
end
class InterfaceJavaProxy < JavaProxy
  class << self  
    alias_method :new_proxy, :new

    def new(*args, &block)
      proxy = new_proxy(*args, &block)
      proxy.java_object = Java.new_proxy_instance(proxy.class.java_class) { |proxy2, method, *args|
        args.collect! { |arg| Java.java_to_ruby(arg) }
        Java.ruby_to_java(proxy.send(method.name, *args))
      }
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
    
  def new(*args, &block)
    @interfaces.freeze unless @interfaces.frozen?
    proxy = @creator.call(*args)
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