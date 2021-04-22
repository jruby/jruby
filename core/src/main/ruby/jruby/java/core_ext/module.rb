# Extensions to the standard Module package.
class Module
  private

  # Import one or many Java classes as follows:
  #
  #   java_import java.lang.System
  #   java_import java.lang.System, java.lang.Thread
  #   java_import [java.lang.System, java.lang.Thread]
  #
  def java_import(*import_classes)
    import_classes.flatten!

    import_classes.map do |import_class|
      case import_class
      when String
        unless JavaUtilities.valid_java_identifier?(import_class)
          raise ArgumentError.new "not a valid Java identifier: #{import_class}"
        end
        raise ArgumentError.new "must use Java style name: #{import_class}" if import_class.include?('::')
        import_class = JavaUtilities.get_proxy_class(import_class)
      when Java::JavaPackage
        raise ArgumentError.new "java_import does not work for Java packages (try include_package instead)"
      when Module
        unless import_class.respond_to? :java_class
          raise ArgumentError.new "not a Java class or interface: #{import_class}"
        end
      else
        raise ArgumentError.new "invalid Java class or interface: #{import_class} (of type #{import_class.class})"
      end

      java_class = import_class.java_class
      class_name = java_class.simple_name

      if block_given?
        package = java_class.package

        # package can be nil if it's default or no package was defined by the classloader
        if package
          package_name = package.name
        elsif m = java_class.canonical_name.match(/(.*)\.[^.]$/)
          package_name = m[1]
        else
          package_name = ""
        end

        constant = yield(package_name, class_name)
      else
        constant = class_name

        # Inner classes are separated with $, get last element
        if m = constant.match(/\$([^$])$/)
          constant = m[1]
        end
      end

      begin
        if !const_defined?(constant) || const_get(constant) != import_class
          const_set(constant, import_class)
        end
      rescue NameError => e
        ex = e.exception("cannot import Java class #{java_class.name.inspect} as `#{constant}' : #{e.message}")
        ex.set_backtrace e.backtrace
        raise ex
      end

      import_class
    end
  end

  # Includes a Java package into this class/module. The Java classes in the
  # package will become available in this class/module, unless a constant
  # with the same name as a Java class is already defined.
  def include_package(package)
  end if false

  # Imports the package specified by +package_name+, first by trying to scan JAR resources
  # for the file in question, and failing that by adding a const_missing hook
  # to try that package when constants are missing.
  def import(package_name, &block)
    if package_name.respond_to?(:java_class) || (String === package_name && package_name.split(/\./).last =~ /^[A-Z]/)
      return java_import(package_name, &block)
    end
    include_package(package_name, &block)
  end

end
