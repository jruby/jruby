# Extensions to the standard Module package.
class Module
  def included_packages
    @included_packages ||= []
  end

  def included_packages_from_namespaces
    namespaces.map do |klass|
      klass.method(:included_packages).call rescue []
    end.map(&:to_a).reduce([], :+).uniq
  end

  def included_packages_from_ancestors
    ancestors.map do |klass|
      klass.method(:included_packages_from_namespaces).call rescue []
    end.map(&:to_a).reduce([], :+).uniq
  end

  def java_aliases
    @java_aliases ||= {}
  end

  def java_aliases_from_namespaces
    namespaces.map do |klass|
      klass.method(:java_aliases).call rescue {}
    end.reverse.reduce({}, :merge)
  end

  def java_aliases_from_ancestors
    ancestors.map do |klass|
      klass.method(:java_aliases_from_namespaces).call rescue {}
    end.reverse.reduce({}, :merge)
  end

  # Returns namespaces containing this module/class starting with self.
  # Example: `Outer::Inner::Shape.namespaces` returns:
  # => [Outer::Inner::Shape, Outer::Inner, Outer]
  def namespaces
    return [self] if name.nil?
    constants = name.split(/::/).map(&:to_sym)
    constants.reduce([Object]) do |output, constant|
      output += [output.last.const_get(constant)]
    end[1..-1].uniq.reverse
  end

  alias const_missing_without_jruby const_missing
  def const_missing(constant)
    all_included_packages = included_packages_from_ancestors
    return const_missing_without_jruby(constant) if all_included_packages.empty?
    real_name = java_aliases_from_namespaces[constant] || constant

    java_class = nil
    last_error = nil

    all_included_packages.each do |package|
      begin
        java_class = JavaUtilities.get_java_class("#{package}.#{real_name}")
      rescue NameError => e
        # we only rescue NameError, since other errors should bubble out
        last_error = e
      end
      break if java_class
    end

    if java_class
      return JavaUtilities.create_proxy_class(constant, java_class, self)
    else
      # try to chain to super's const_missing
      begin
        return const_missing_without_jruby(constant)
      rescue NameError => e
        # super didn't find anything either, raise our Java error
        raise NameError.new("#{constant} not found in packages #{all_included_packages.join(', ')}; last error: #{(last_error || e).message}")
      end
    end
  end

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
        raise ArgumentError.new "must use jvm-style name: #{import_class}" if import_class.include?('::')
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

      if !const_defined?(constant) || const_get(constant) != import_class
        const_set(constant, import_class)
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
    included_packages << package unless included_packages.include?(package)
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
    java_aliases[new_id] = old_id
  end
end
