require 'mspec/guards/guard'

class Object
  # Helper for syntax-sensitive specs. The specs should be placed in a file in
  # the +versions+ subdirectory. For example, suppose language/method_spec.rb
  # contains specs whose syntax depends on the Ruby version. In the
  # language/method_spec.rb use the helper as follows:
  #
  #   language_version __FILE__, "method"
  #
  # Then add a file "language/versions/method_1.8.rb" for the specs that are
  # syntax-compatible with Ruby 1.8.x.
  #
  # The most version-specific file will be loaded. If the version is 1.8.6,
  # "method_1.8.6.rb" will be loaded if it exists, otherwise "method_1.8.rb"
  # will be loaded if it exists.
  def language_version(dir, name)
    path = File.dirname(File.expand_path(dir))

    [SpecGuard.ruby_version(:tiny), SpecGuard.ruby_version].each do |version|
      file = File.join path, "versions", "#{name}_#{version}.rb"
      if File.exists? file
        require file
        break
      end
    end

    nil
  end
end
