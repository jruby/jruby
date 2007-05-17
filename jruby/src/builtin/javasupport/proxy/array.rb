# This class enables the syntax arr = MyClass[n][n]...[n].new
class ArrayJavaProxyCreator
  def initialize(java_class,*args)
    @java_class = java_class
    @dimensions = []
    extract_dimensions(args)
      end
  
  def [](*args)
    extract_dimensions(args)
    self
    end
  
  private
  def extract_dimensions(args)
    unless args.length > 0
      raise ArgumentError,"empty array dimensions specified"    
    end
    args.each do |arg|
      unless arg.kind_of?(Fixnum)
        raise ArgumentError,"array dimension length must be Fixnum"    
      end
      @dimensions << arg
    end  
    end
  public
  
  def new(fill_value=nil)
    array = @java_class.new_array(@dimensions)
    array_class = @java_class.array_class
    (@dimensions.length-1).times do
      array_class = array_class.array_class    
    end
    proxy_class = JavaUtilities.get_proxy_class(array_class)
    proxy = proxy_class.new(array)
    if fill_value
      converter = JavaArrayUtilities.get_converter(@java_class)
      JavaArrayUtilities.fill_array(proxy,@dimensions,converter.call(fill_value))
    end
    proxy  
  end
end

class ArrayJavaProxy < JavaProxy
  include Enumerable

  class << self  
    alias_method :new_proxy, :new

    # the 'size' variant should be phased out ASAP
    def new(java_array_or_size, fill_value=nil)
      proxy = new_proxy
      if java_array_or_size.kind_of?(Java::JavaArray)
        proxy.java_object = java_array_or_size
        return proxy      
      end
      puts " ** Warning: the 'ClassName[].new(size)' form is deprecated; use ClassName[size].new"
      # sort out the mess of the previous approach
      size = java_array_or_size
      component_type = proxy.java_class.component_type
      component_type = component_type.component_type while component_type.array?
      array = component_type.new_array(size)
      # get the right proxy class for the number of array dimensions
      array_class = component_type.array_class
      if size.kind_of?(Array)
        (size.length-1).times do
          array_class = array_class.array_class    
        end
      end
      proxy_class = JavaUtilities.get_proxy_class(array_class)
      proxy = proxy_class.new(array)
      if fill_value
        converter = JavaArrayUtilities.get_converter(component_type)
        JavaArrayUtilities.fill_array(proxy,size,converter.call(fill_value))
      end
      proxy
    end
    
  end

  def length()
    java_object.length
  end
  
  alias_method :size, :length
  
  def [](*args)
    if (args.length == 1 && args[0].kind_of?(Integer))
      Java.java_to_ruby(java_object[args[0]])
    else
      JavaArrayUtilities.get_range(self,*args)
    end
  end
  
  def at(index)
    length = java_object.length
    index = length + index if index < 0
    return Java.java_to_ruby(java_object[index]) if index >= 0 && index < length
    nil
  end
  
  def []=(index, value)
    #java_object[index] = Java.ruby_to_java(value)

    # I don't know if this (instance var) will cause any problems. If so,
    # I can call JavaArrayUtilities.convert_to_type on every call to []=, but
    # I'd rather keep the converter locally for better performance. -BD
    @converter ||= JavaArrayUtilities.get_converter(java_class.component_type)
    java_object[index] = @converter.call(value)
  end

  def +(other)
    JavaArrayUtilities.concatenate(self,other)  
  end

  def each()
    block = Proc.new
    for index in 0...java_object.length
      block.call(self[index])
    end
  end
  
  def to_a
    JavaArrayUtilities.java_to_ruby(self)
  end

  alias_method :to_ary, :to_a

end
