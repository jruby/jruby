module JavaPackageModuleTemplate
  class << self
    def const_missing(const)
      JavaUtilities.get_proxy_class(@package_name + const.to_s)
    end
    private :const_missing

    def const_get(const, inherit=true)
      JavaUtilities.get_proxy_class(@package_name + const.to_s)
    end

    def method_missing(sym, *args)
      Kernel.raise ArgumentError, "Java package `#{package_name}' does not have a method `#{sym}'" unless args.empty?
      JavaUtilities.get_proxy_or_package_under_package self, sym
    end
    private :method_missing

    def name
      name_pkg = JavaUtilities.get_proxy_or_package_under_package self, :name
      name_pkg.nil? ? super : name_pkg
    end

    def package_name
      @package_name[0..-2] # strip off trailing .
    end
  end
end
# pull in the default package
JavaUtilities.get_package_module("Default")
