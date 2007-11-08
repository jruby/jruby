module JavaArrayUtilities
  class ProxyRef
    def initialize(class_name)
      @class_name = class_name
    end
    def new(*args)
      proxy.new(*args).java_object
    end
    def proxy
      @proxy ||= JavaUtilities.get_proxy_class(@class_name)
    end
    def name
      @class_name    
    end
    def valueOf(*args)
      proxy.valueOf(*args).java_object
    end
  end
  Jboolean = ProxyRef.new('boolean')
  Jbyte = ProxyRef.new('byte')
  Jchar = ProxyRef.new('char')
  Jshort = ProxyRef.new('short')
  Jint = ProxyRef.new('int')
  Jlong = ProxyRef.new('long')
  Jfloat = ProxyRef.new('float')
  Jdouble = ProxyRef.new('double')
  JBoolean = ProxyRef.new('java.lang.Boolean')
  JByte = ProxyRef.new('java.lang.Byte')
  JCharacter = ProxyRef.new('java.lang.Character')
  JShort = ProxyRef.new('java.lang.Short')
  JInteger = ProxyRef.new('java.lang.Integer')
  JLong = ProxyRef.new('java.lang.Long')
  JFloat = ProxyRef.new('java.lang.Float')
  JDouble = ProxyRef.new('java.lang.Double')
  JBigDecimal = ProxyRef.new('java.math.BigDecimal')
  JBigInteger = ProxyRef.new('java.math.BigInteger')
  JObject = ProxyRef.new('java.lang.Object')
  JString = ProxyRef.new('java.lang.String')
  JDate = ProxyRef.new('java.util.Date')
  JClass = ProxyRef.new('java.lang.Class')
  System = ProxyRef.new('java.lang.System')
  JOFalse = Java.ruby_to_java(false)
  JOTrue = Java.ruby_to_java(true)
  JIntegerMin = -2147483648
  JIntegerMax = 2147483647
  JLongMin = -9223372036854775808
  JLongMax = 9223372036854775807
  SBoolean = 'java.lang.Boolean'.to_sym
  SByte = 'java.lang.Byte'.to_sym
  SCharacter = 'java.lang.Character'.to_sym
  SShort = 'java.lang.Short'.to_sym
  SInteger = 'java.lang.Integer'.to_sym
  SLong = 'java.lang.Long'.to_sym
  SFloat = 'java.lang.Float'.to_sym
  SDouble = 'java.lang.Double'.to_sym
  SBigDecimal = 'java.math.BigDecimal'.to_sym
  SBigInteger = 'java.math.BigInteger'.to_sym
  SObject = 'java.lang.Object'.to_sym
  SString = 'java.lang.String'.to_sym
  SDate = 'java.util.Date'.to_sym
  # *very* loose/eager conversion rules in place here, can tighten them
  # up if need be. -BD
  # the order of evaluation in the converters is important, want to 
  # check the most probable first, then dispense with any unidentified 
  # non-Ruby classes before calling to_i/to_f/to_s. -BD
  @converters = {
    :boolean => Proc.new {|val| 
      if val == false || val.nil?
        JOFalse
      elsif val == true
        JOTrue
      elsif val.kind_of?(Numeric)
        val.to_i == 0 ? JOFalse : JOTrue
      elsif val.kind_of?(String)
        JBoolean.new(val)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        val.to_i == 0 ? JOFalse : JOTrue
      else
        Java.ruby_to_java(val)
      end
    },
    :byte => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JByte.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JByte.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :char => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JCharacter.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JCharacter.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :short => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JShort.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JShort.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :int => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JInteger.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JInteger.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :long => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JLong.new(val.to_i)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JLong.new(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :float => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JFloat.new(val.to_f)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_f)
        JFloat.new(val.to_f)
      else
        Java.ruby_to_java(val)
      end
    },
    :double => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JDouble.new(val.to_f)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_f)
        JDouble.new(val.to_f)
      else
        Java.ruby_to_java(val)
      end
    },
    :decimal => Proc.new {|val| 
      if val.kind_of?(Numeric)
        JBigDecimal.valueOf(val)
      elsif val.kind_of?(String)
        JBigDecimal.new(val)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_f)
        JBigDecimal.valueOf(val.to_f)
      elsif val.respond_to?(:to_i)
        JBigDecimal.valueOf(val.to_i)
      else
        Java.ruby_to_java(val)
      end
    },
    :big_int => Proc.new {|val|
      if val.kind_of?(Integer)
        JBigInteger.new(val.to_s)
      elsif val.kind_of?(Numeric)
        JBigInteger.new(val.to_i.to_s)
      elsif val.kind_of?(String)
        JBigInteger.new(val)
      elsif val.kind_of?(JavaProxy)
        Java.ruby_to_java(val)
      elsif val.respond_to?(:to_i)
        JBigInteger.new(val.to_i.to_s)
      else
        Java.ruby_to_java(val)
      end
    },
    :object => Proc.new {|val|
      if val.kind_of?(Integer)
        if val >= JIntegerMin && val <= JIntegerMax
          JInteger.new(val)
        elsif val >= JLongMin && val <= JLongMax
          JLong.new(val)
        else
          JBigInteger.new(val.to_s)
        end
      elsif val.kind_of?(Float)
        JDouble.new(val)
      else
        Java.ruby_to_java(val)
      end
    },
    :string => Proc.new {|val|
      if val.kind_of?(String)
        JString.new(val)
      elsif val.kind_of?(JavaProxy)
        if val.respond_to?(:toString)
          JString.new(val.toString)
        else
          Java.ruby_to_java(val)
        end
      elsif val.respond_to?(:to_s)
        JString.new(val.to_s)
      else
        Java.ruby_to_java(val)
      end
    },
  }
  @converters['boolean'] = @converters['java.lang.Boolean'] = @converters[:Boolean] =
    @converters[SBoolean] = @converters[:boolean]
  @converters['byte'] = @converters['java.lang.Byte'] = @converters[:Byte] =
    @converters[SByte] = @converters[:byte]
  @converters['char'] = @converters['java.lang.Character'] = @converters[:Character] =
    @converters[SCharacter] = @converters[:Char] = @converters[:char]
  @converters['short'] = @converters['java.lang.Short'] = @converters[:Short] =
    @converters[SShort] = @converters[:short]
  @converters['int'] = @converters['java.lang.Integer'] = @converters[:Integer] =
    @converters[SInteger] = @converters[:Int] = @converters[:int]
  @converters['long'] = @converters['java.lang.Long'] = @converters[:Long] =
    @converters[SLong] = @converters[:long]
  @converters['float'] = @converters['java.lang.Float'] = @converters[:Float] =
    @converters[SFloat] = @converters[:float]
  @converters['double'] = @converters['java.lang.Double'] = @converters[:Double] =
    @converters[SDouble] = @converters[:double]
  @converters['java.math.BigDecimal'] = @converters[:BigDecimal] = @converters[:big_decimal]
    @converters[SBigDecimal] = @converters[:decimal]
  @converters['java.math.BigInteger'] = @converters[:BigInteger] = @converters[:big_integer]
    @converters[SBigInteger] = @converters[:big_int]
  @converters['java.lang.Object'] = @converters[:Object] =
    @converters[SObject] = @converters[:object]
  @converters['java.lang.String'] = @converters[:String] =
    @converters[SString] = @converters[:string]

  @default_converter = Proc.new {|val| Java.ruby_to_java(val) }
  
  @class_index = {
    :boolean => Jboolean,
    :byte => Jbyte,
    :char => Jchar,
    :short => Jshort,
    :int => Jint,
    :long => Jlong,
    :float => Jfloat,
    :double => Jdouble,
    :Boolean => JBoolean,
    SBoolean => JBoolean,
    :Byte => JByte,
    SByte => JByte,
    :Char => JCharacter,
    :Character => JCharacter,
    SCharacter => JCharacter,
    :Short => JShort,
    SShort => JShort,
    :Int => JInteger,
    :Integer => JInteger,
    SInteger => JInteger,
    :Long => JLong,
    SLong => JLong,
    :Float => JFloat,
    SFloat => JFloat,
    :Double => JDouble,
    SDouble => JDouble,
    :object => JObject,
    :Object => JObject,
    SObject => JObject,
    :string => JString,
    :String => JString,
    SString => JString,
    :decimal => JBigDecimal,
    :big_decimal => JBigDecimal,
    :BigDecimal => JBigDecimal,
    SBigDecimal => JBigDecimal,
    :big_int => JBigInteger,
    :big_integer => JBigInteger,
    :BigInteger => JBigInteger,
    SBigInteger => JBigInteger,
  }
 class << self
  def get_converter(component_type)
    converter = @converters[component_type.name]
    converter ? converter : @default_converter
  end
  
  def convert_to_type(component_type,value)
    get_converter(component_type).call(value)
  end

  # this can be expensive, as it must examine every element
  # of every 'dimension' of the Ruby array.  thinking about
  # moving this to Java.
  def dimensions(ruby_array,dims = [],index = 0)
    return [] unless ruby_array.kind_of?(::Array)
    dims << 0 while dims.length <= index 
    dims[index] = ruby_array.length if ruby_array.length > dims[index]
    ruby_array.each do |sub_array|
      next unless sub_array.kind_of?(::Array)
      dims = dimensions(sub_array,dims,index+1)
    end
    dims
  end
  
  def enable_extended_array_support
    return if [].respond_to?(:to_java)
    Array.module_eval {
      def to_java(*args,&block)
        JavaArrayUtilities.ruby_to_java(*(args.unshift(self)),&block)
      end
    }
  end

  def disable_extended_array_support
    return unless [].respond_to?(:to_java)
    Array.send(:remove_method,:to_java)
  end
 
  def fill_array(array,dimensions,fill_value)
    dims = dimensions.kind_of?(Array) ? dimensions : [dimensions]
    copy_ruby_to_java(dims,nil,array,nil,fill_value)
  end
  
private
  def get_class(class_name)
    ref =  @class_index[class_name.to_sym]
    ref ? ref.proxy : JavaUtilities.get_proxy_class(class_name.to_s)
  end
public
  
  def ruby_to_java(*args,&block)
    return JObject.proxy[].new(0) if args.length == 0
    ruby_array = args[0]
    unless ruby_array.kind_of?(::Array) || ruby_array.nil?
      raise ArgumentError,"invalid arg[0] passed to to_java (#{args[0]})"    
    end
    dims = nil
    fill_value = nil
    index = 1
    if index < args.length
      arg = args[index]
      # the (optional) first arg is class/name. if omitted,
      # defaults to java.lang.Object
      if arg.kind_of?(Class) && arg.respond_to?(:java_class)
        cls = arg
        cls_name = arg.java_class.name
        index += 1
      elsif arg.kind_of?(String) || arg.kind_of?(Symbol)
        cls = get_class(arg)
        unless cls
          raise ArgumentError,"invalid class name (#{arg}) specified for to_java"      
        end
        cls_name = arg
        index += 1
      else
        cls = JObject.proxy
        cls_name = SObject
      end
    else
      cls = JObject.proxy
      cls_name = SObject
    end
    if block
      converter = block
    elsif converter = @converters[cls_name]
    else
      converter = @default_converter
    end
    # the (optional) next arg(s) is dimensions. may be
    # specified as dim1,dim2,...,dimn, or [dim1,dim2,...,dimn]
    # the array version is required if you want to pass a
    # fill value after it
    if index < args.length
      arg = args[index]
      if arg.kind_of?(Fixnum)
        dims = [arg]
        index += 1
        while index < args.length && args[index].kind_of?(Fixnum)
          dims << args[index]
          index += 1        
        end
      elsif arg.kind_of?(::Array)
        dims = arg
        index += 1
        fill_value = converter.call(args[index]) if index < args.length
      elsif arg.nil?
        dims = dimensions(ruby_array) if ruby_array
        index += 1
        fill_value = converter.call(args[index]) if index < args.length
      end
    else
      dims = dimensions(ruby_array) if ruby_array
    end
    dims = [0] unless dims
    java_array = new_array(cls.java_class,*dims)
    if ruby_array
      copy_ruby_to_java(dims,ruby_array,java_array,converter,fill_value)          
    elsif fill_value
      copy_ruby_to_java(dims,nil,java_array,converter,fill_value)
    end
    java_array
  end

private
  def copy_ruby_to_java(dims,ruby_array,java_array,converter,fill_value)
    if dims.length > 1
      shift_dims = dims[1...dims.length]
      for i in 0...dims[0]
        if ruby_array.kind_of?(::Array)
          ruby_param = ruby_array[i]
        else
          ruby_param = ruby_array # fill with value when no array        
        end
        copy_ruby_to_java(shift_dims,ruby_param,java_array[i],converter,fill_value)
      end
    else
      copy_data(ruby_array,java_array,converter,fill_value)
    end
    java_array 
  end
  
private
  def copy_data(ruby_array,java_array,converter,fill_value)
    if ruby_array.kind_of?(::Array)
      rlen = ruby_array.length
    else
      rlen = 0
      # in irregularly-formed Ruby arrays, values that appear where
      # a subarray is expected get propagated. not sure if this is
      # the best behavior, will see what users say
      fill_value = converter.call(ruby_array) if ruby_array    
    end
    java_object = java_array.java_object
    jlen = java_array.length
    i = 0
    while i < rlen && i < jlen
      java_object[i] = converter.call(ruby_array[i])
      i += 1
    end
    if i < jlen && fill_value
      java_object.fill(i,jlen,fill_value)
    end
    java_array
  end
public

  def java_to_ruby(java_array)
    unless java_array.kind_of?(ArrayJavaProxy)
      raise ArgumentError,"not a Java array: #{java_array}"
    end
    length = java_array.length
    ruby_array = Array.new(length)
    if length > 0
      if java_array[0].kind_of?ArrayJavaProxy
        length.times do |i|
          ruby_array[i] = java_to_ruby(java_array[i])      
        end
      else
        length.times do |i|
          ruby_array[i] = java_array[i];      
        end
      end
    end
    ruby_array
  end
  
  def get_range(java_array,*args)
    unless java_array.kind_of?(ArrayJavaProxy)
      raise ArgumentError,"not a Java array: #{java_array}"
    end
    length = java_array.length
    component_type = java_array.java_class.component_type
    if args.length == 1 && args[0].kind_of?(Range) &&
         args[0].first.kind_of?(Integer) && args[0].last.kind_of?(Integer)
      first = args[0].first >= 0 ? args[0].first : length + args[0].first
      last = args[0].last >= 0 ? args[0].last : length + args[0].last
      len = last - first
      len += 1 unless args[0].exclude_end?
      return new_array(component_type,0) if len <= 0
    elsif args.length == 2 && args[0].kind_of?(Integer) && args[1].kind_of?(Integer)
      return nil if args[1] < 0
      first = args[0] >= 0 ? args[0] : length + args[0]
      len = args[1];
      return nil if len < 0
    else
      raise ArgumentError,"[index] not Integer, two Integers, or Range: #{args}"
    end
    return nil if first > length
    return new_array(component_type,0) if first == length
    len = length - first if first + len > length
    subarray = new_array(component_type,len)
    System.proxy.arraycopy(java_array,first,subarray,0,len)
    subarray
  end
  
  def concatenate(java_array,arr2) 
    unless java_array.kind_of?(ArrayJavaProxy)
      raise ArgumentError,"not a Java array: #{java_array}"
    end
    unless arr2.kind_of?(ArrayJavaProxy) || arr2.kind_of?(Array)
      raise ArgumentError,"not an Array / Java array: #{arr2}"
    end
    length = java_array.length + arr2.length
    component_type = java_array.java_class.component_type
    new_array = new_array(component_type,length)
    System.proxy.arraycopy(java_array,0,new_array,0,java_array.length)
    if arr2.kind_of?(ArrayJavaProxy) &&
        really_assignable(component_type,arr2.java_class.component_type)
      System.proxy.arraycopy(arr2,0,new_array,java_array.length,arr2.length)
    else
      # use the conversion proc for the target array type
      offset = java_array.length
      0.upto(arr2.length - 1) do |i|
        new_array[offset] = arr2[i]
        offset += 1
      end
    end
    new_array
  end

  private
  def new_array(type,*dims)
    ArrayJavaProxyCreator.new(type,*dims).new
  end
  def really_assignable(to_type,from_type)
    return true if to_type == from_type
    return false if to_type.primitive? || from_type.primitive?
    return JClass.proxy.forName(to_type.name).isAssignableFrom(JClass.proxy.forName(from_type.name))
  end
  public
  
 end #self
end #JavaArrayUtilities