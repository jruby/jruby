module JavaPackageModuleTemplate
  class << self
    def const_missing(const)
      JavaUtilities.get_proxy_class(@package_name + const.to_s)
    end
    private :const_missing

    def const_get(const)
      JavaUtilities.get_proxy_class(@package_name + const.to_s)
    end

    def method_missing(sym, *args)
      Kernel.raise ArgumentError, "Java package `#{package_name}' does not have a method `#{sym}'" unless args.empty?
      JavaUtilities.get_proxy_or_package_under_package self, sym
    end
    private :method_missing

    def package_name
      # strip off trailing .
      @package_name[0..-2]
    end
  end
end
# pull in the default package
JavaUtilities.get_package_module("Default")
