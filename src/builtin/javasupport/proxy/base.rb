module JavaProxyMethods
  attr :java_object, true

  def java_class
    self.class.java_class
  end

  def ==(rhs)
    java_object == rhs
  end
  
  def to_s
    java_object.to_s
  end

  def eql?(rhs)
    self == rhs
  end
  
  def equal?(rhs)
    java_object.equal?(rhs)
  end
  
  def hash()
    java_object.hash()
  end
  
  def to_java_object
    java_object
  end

  def synchronized
    java_object.synchronized { yield }
  end
end

# JavaProxy is a base class for all Java Proxies.  A Java proxy is a high-level abstraction
# that wraps a low-level JavaObject with ruby methods capable of dispatching to the JavaObjects
# native java methods.  
class JavaProxy
  class << self
    attr :java_class, true
    # Carry the Java class as a class variable on the derived classes too, otherwise 
    # JavaProxy.java_class won't work.
    def inherited(subclass)
      subclass.java_class = self.java_class unless subclass.java_class
      super
    end
    
    def singleton_class
      class << self; self; end 
    end

    def [](*args)
      if args.length > 0
        # array creation should use this variant
        ArrayJavaProxyCreator.new(java_class,*args)      
      else
        # keep this variant for kind_of? testing
      JavaUtilities.get_proxy_class(java_class.array_class)
      end
    end
  end
  
  include JavaProxyMethods

end
