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