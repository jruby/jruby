class Object
  # include the class specified by +include_class+ into the current namespace,
  # using either its base name or by using a name returned from an optional block,
  # passing all specified classes in turn and providing the block package name
  # and base class name.
  # @deprecated use {Object#java_import}
  def include_class(include_class, &block)
    warn "#{__method__} is deprecated. Use java_import."
    java_import(include_class, &block)
  end

  # @deprecated
  def java_kind_of?(other) # TODO: this can go away now, but people may be using it
    return true if self.kind_of?(other)
    return false unless self.respond_to?(:java_class) && other.respond_to?(:java_class) &&
      other.kind_of?(Module) && !self.kind_of?(Module) 
    return other.java_class.assignable_from?(self.java_class)
  end

  # Import one or many Java classes as follows:
  #
  #   java_import java.lang.System
  #   java_import java.lang.System, java.lang.Thread
  #   java_import [java.lang.System, java.lang.Thread]
  #
  # @!visibility public
  def java_import(*import_classes)
    import_classes = import_classes.each_with_object([]) do |classes, flattened|
      if classes.is_a?(Array)
        flattened.push(*classes)
      else
        flattened.push(classes)
      end
    end

    import_classes.map do |import_class|
      case import_class
      when String
        cc = java.lang.Character
        valid_name = import_class.split(".").all? do |frag|
          cc.java_identifier_start? frag[0].ord and
          frag.each_char.all? {|c| cc.java_identifier_part? c.ord }
        end
        unless valid_name
          raise ArgumentError.new "not a valid Java identifier: #{import_class}"
        end
        # pull in the class
        raise ArgumentError.new "must use jvm-style name: #{import_class}" if import_class.include? "::"
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
        elsif java_class.canonical_name =~ /(.*)\.[^.]$/
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

  # @private
  def handle_different_imports(*args, &block)
    if args.first.respond_to?(:java_class)
      java_import(*args, &block)
    else
      other_import(*args, &block)
    end
  end
  
  unless respond_to?(:import)
    alias :import :java_import
  end
end
