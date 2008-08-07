module JavaArrayUtilities
  JClass = Java::java.lang.Class
  System = Java::java.lang.System
  
  class << self
  
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
      System.arraycopy(java_array,first,subarray,0,len)
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
      System.arraycopy(java_array,0,new_array,0,java_array.length)
      if arr2.kind_of?(ArrayJavaProxy) &&
          really_assignable(component_type,arr2.java_class.component_type)
        System.arraycopy(arr2,0,new_array,java_array.length,arr2.length)
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
      return JClass.forName(to_type.name).isAssignableFrom(JClass.forName(from_type.name))
    end
    public
  
  end #self
end #JavaArrayUtilities