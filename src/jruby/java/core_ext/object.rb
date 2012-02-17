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
    warn "#{__method__} is deprecated. Use java_import."
    java_import(include_class, &block)
  end
  
  # TODO: this can go away now, but people may be using it
  def java_kind_of?(other)
    return true if self.kind_of?(other)
    return false unless self.respond_to?(:java_class) && other.respond_to?(:java_class) &&
      other.kind_of?(Module) && !self.kind_of?(Module) 
    return other.java_class.assignable_from?(self.java_class)
  end

  def java_import(*import_classes)
    import_classes.flatten!

    import_classes.map do |import_class|
      case import_class
      when String
        # pull in the class
        import_class = JavaUtilities.get_proxy_class(import_class)
      when Module
        if import_class.respond_to? "java_class"
          # ok, it's a proxy
        else
          raise ArgumentError.new "not a Java class or interface: #{import_class}"
        end
      else
        raise ArgumentError.new "invalid Java class or interface: #{import_class}"
      end

      java_class = import_class.java_class
      class_name = java_class.simple_name

      if block_given?
        package = java_class.package

        # package can be nil if it's default or no package was defined by the classloader
        if package
          package_name = package.name
        elsif java_class.full_name =~ /(.*)\.[^.]$/
          package_name = $1
        else
          package_name = ""
        end

        constant = yield(package_name, class_name)
      else
        constant = class_name

        # Inner classes are separated with $, get last element
        if constant =~ /\$([^$])$/
          constant = $1
        end
      end

      unless constant =~ /^[A-Z].*/
        raise ArgumentError.new "cannot import class `" + java_class.name + "' as `" + constant + "'"
      end

      # JRUBY-3453: Make import not complain if Java already has already imported the specific Java class
      # If no constant is defined, or the constant is not already set to the java_import, assign it
      eval_str = "if !defined?(#{constant}) || #{constant} != import_class; #{constant} = import_class; end"
      if Module === self
        class_eval(eval_str, __FILE__, __LINE__)
      else
        eval(eval_str, binding, __FILE__, __LINE__)
      end

      import_class
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
