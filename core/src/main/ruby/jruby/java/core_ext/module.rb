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

  ##
  # Includes a Java package into this class/module. The Java classes in the
  # package will become available in this class/module, unless a constant
  # with the same name as a Java class is already defined.
  #
  def include_package(package)
    package = package.package_name if package.respond_to?(:package_name)

    if defined? @included_packages
      @included_packages << package
      return
    end

    @included_packages = [ package ]
    @java_aliases ||= {}

    def self.const_missing(constant)
      real_name = @java_aliases[constant] || constant

      java_class = nil
      last_error = nil

      @included_packages.each do |package|
          begin
            java_class = JavaUtilities.get_java_class("#{package}.#{real_name}")
          rescue NameError
            # we only rescue NameError, since other errors should bubble out
            last_error = $!
          end
          break if java_class
      end

      if java_class
        return JavaUtilities.create_proxy_class(constant, java_class, self)
      else
        # try to chain to super's const_missing
        begin
          return super
        rescue NameError
          # super didn't find anything either, raise our Java error
          raise NameError.new("#{constant} not found in packages #{@included_packages.join(', ')}; last error: #{last_error.message}")
        end
      end
    end
    nil
  end

  # Imports the package specified by +package_name+, first by trying to scan JAR resources
  # for the file in question, and failing that by adding a const_missing hook
  # to try that package when constants are missing.
  def import(package_name, &block)
    if package_name.respond_to?(:java_class) || (String === package_name && package_name.split(/\./).last =~ /^[A-Z]/)
      return java_import(package_name, &block)
    end
    include_package(package_name, &block)
  end

  def java_alias(new_id, old_id)
    (@java_aliases ||= {})[new_id] = old_id
  end

end
