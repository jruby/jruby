class Object
  # Prevent methods added to Object from being added to the 
  # blank-slate JavaPackageModuleTemplate
  class << self
    alias_method :java_package_method_added, :method_added

    def method_added(name)
      # If someone added a new method_added since we aliased original, then 
      # lets defer to that.  Otherwise run one we aliased.
      if self.class.superclass.instance_method(:method_added) != method(:java_package_method_added)
        result = super 
      else
        result = java_package_method_added(name)
      end
      JavaPackageModuleTemplate.__block__(name) if self == Object
      result
    end
    private :method_added
  end

  # include the class specified by +include_class+ into the current namespace,
  # using either its base name or by using a name returned from an optional block,
  # passing all specified classes in turn and providing the block package name
  # and base class name.
  def include_class(include_class, &block)
    java_import(include_class, &block)
  end
  
  # TODO: this can go away now, but people may be using it
  def java_kind_of?(other)
    return true if self.kind_of?(other)
    return false unless self.respond_to?(:java_class) && other.respond_to?(:java_class) &&
      other.kind_of?(Module) && !self.kind_of?(Module) 
    return other.java_class.assignable_from?(self.java_class)
  end

  def java_import(import_class)
    case import_class
    when Array
      import_class.each do |arg|
        java_import(arg)
      end
      return
    when String
      # pull in the class
      import_class = JavaUtilities.get_proxy_class(import_class);
    when Module
      if import_class.respond_to? "java_class"
        # ok, it's a proxy
      else
        raise ArgumentError.new "Not a Java class or interface: #{import_class}"
      end
    else
      raise ArgumentError.new "Invalid java class/interface: #{import_class}"
    end

    full_name = import_class.java_class.name
    package = import_class.java_class.package
    # package can be nil if it's default or no package was defined by the classloader
    if package
      package_name = package.name
    else
      dot_index = full_name.rindex('.')
      if dot_index
        package_name = full_name[0...full_name.rindex('.')]
      else
        # class in default package
        package_name = ""
      end
    end
    
    if package_name.length > 0
      class_name = full_name[(package_name.length + 1)..-1]
    else
      class_name = full_name
    end

    if block_given?
      constant = yield(package_name, class_name)
    else
      constant = class_name

      # Inner classes are separated with $
      if constant =~ /\$/
        constant = constant.split(/\$/).last
      end

      if constant[0,1].upcase != constant[0,1]
        raise ArgumentError.new "cannot import class `" + class_name + "' as `" + constant + "'"
      end
    end

    # JRUBY-3453: Make import not complain if Java already has already imported the specific Java class
    # If no constant is defined, or the constant is not already set to the include_class, assign it
    eval_str = "if !defined?(#{constant}) || #{constant} != import_class; #{constant} = import_class; end"
    if (Module === self)
      return class_eval(eval_str, __FILE__, __LINE__)
    else
      return eval(eval_str, binding, __FILE__, __LINE__)
    end
  end
  
  private :java_import

  def handle_different_imports(*args, &block)
    if args.first.respond_to?(:java_class)
      java_import(*args, &block)
    else
      other_import(*args, &block)
    end
  end
  
  if respond_to?(:import)
    alias :other_import :import
    alias :import :handle_different_imports
  else
    alias :import :java_import
    
    class << self
      alias_method :method_added_without_import_checking, :method_added
      
      def method_added(name)
        if name.to_sym == :import && defined?(@adding) && !@adding
          @adding = true
          alias_method :other_import, :import
          alias_method :import, :handle_different_imports
          @adding = false
        end
        method_added_without_import_checking(name)
      end
    end
  end
end
