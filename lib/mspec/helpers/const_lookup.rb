require 'mspec/utils/version'

module Kernel
  def const_lookup(c)
    extra_arg = SpecVersion.new(RUBY_VERSION) >= "1.9"

    names = c.split '::'
    names.shift if names.first.empty?

    names.inject(Object) do |m, n|
      defined = extra_arg ? m.const_defined?(n, false) : m.const_defined?(n)
      defined ? m.const_get(n) : m.const_missing(n)
    end
  end
end
