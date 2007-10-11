class ConcreteJavaProxy < JavaProxy
  class << self
    alias_method :new_proxy, :new

    def new(*args,&block)
      proxy = new_proxy(*args,&block)
      proxy.__jcreate!(*args) unless proxy.java_object
      proxy
    end
    
  end
  
  def __jcreate!(*args)
    constructors = self.java_class.constructors
    raise NameError, "not instantiatable" if constructors.length == 0
    constructors = constructors.select {|c| c.arity == args.length }
    raise NameError, "wrong # of arguments for constructor" if constructors.empty?
    args.collect! { |v| Java.ruby_to_java(v) }
    self.java_object = JavaUtilities.matching_method(constructors, args).new_instance(*args)
  end
  
  def initialize(*args, &block)
    __jcreate!(*args)
  end
end
