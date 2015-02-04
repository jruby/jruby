module Java
  class << self
    def method_missing(sym, *args)
      raise ArgumentError, "Java package `java' does not have a method `#{sym}'" unless args.empty?
      JavaUtilities.get_top_level_proxy_or_package sym
    end
  end
end