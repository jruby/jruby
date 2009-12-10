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
  def self.impl(*meths, &block)
    block = lambda {|*args| send(:method_missing, *args) } unless block

    interface = JavaUtilities.get_interface_module java_class
    Class.new(self) do
      include interface
      define_method(:method_missing) do |name, *args|
        return block.call(name, *args) if meths.empty? || meths.include?(name)
        super
      end
    end.new
  end
end
