class Binding
  # :nodoc:
  def irb
    # if lazy RubyGems, force it to load here to work around GH-6506
    if defined?(::Gem) && JRuby::Util.retrieve_option("rubygems.lazy")
      require 'rubygems'
    end
    require 'irb'
    irb
  end

  # suppress redefinition warning
  alias irb irb # :nodoc:
end

module Kernel
  def pp(*objs)
    require 'pp'
    pp(*objs)
  end

  # suppress redefinition warning
  alias pp pp # :nodoc:

  private :pp
end