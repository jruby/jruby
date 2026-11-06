# frozen_string_literal: true

# Preloaded with -r by ExcludesAudit while a test file loads under a filter that matches no tests. Once the
# file has loaded, it reports which of the excluded classes and methods in EXCLUDES_AUDIT_QUERY exist.
at_exit do
  ENV.fetch("EXCLUDES_AUDIT_QUERY", "").each_line(chomp: true) do |test|
    class_name, method = test.split("#", 2)
    klass = class_name.split("::").inject(Object) do |scope, name|
      break nil unless scope.const_defined?(name, false)
      scope.const_get(name, false)
    rescue NameError
      break nil
    end
    next unless klass.is_a?(Module)

    puts "EXCLUDES_AUDIT #{test} class"
    puts "EXCLUDES_AUDIT #{test} method" if klass.public_method_defined?(method)
  end
end
