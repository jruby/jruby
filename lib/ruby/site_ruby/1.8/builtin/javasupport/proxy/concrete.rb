class ConcreteJavaProxy < JavaProxy
  class << self
    alias_method :new_proxy, :new

    def new(*args,&block)
      proxy = new_proxy(*args,&block)
      proxy.__jcreate!(*args) unless proxy.java_object
      proxy
    end
  end
  
  def initialize(*args, &block)
    __jcreate!(*args)
  end
end
