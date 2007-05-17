class ExceptionExtender
  def initialize
    @exception_class = Java::JavaClass.for_name('java.lang.Exception')
  end
  
  def extend_proxy(proxy_class)
    if @exception_class.assignable_from? proxy_class.java_class
      class << proxy_class
        def ===(rhs)
          (NativeException == rhs.class) && (java_class.assignable_from?(rhs.cause.java_class))
        end
      end
    end
  end
end

JavaUtilities.add_proxy_extender ExceptionExtender.new
