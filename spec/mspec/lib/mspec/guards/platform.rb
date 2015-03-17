require 'mspec/guards/guard'

class PlatformGuard < SpecGuard
  def self.windows?
    PlatformGuard.new(:os => :windows).match?
  end

  def self.opal?
    PlatformGuard.new(:opal)
  end

  def initialize(*args)
    if args.last.is_a?(Hash)
      @options, @platforms = args.last, args[0..-2]
    else
      @options, @platforms = {}, args
    end
    self.parameters = args
  end

  def match?
    match = @platforms.empty? ? true : platform?(*@platforms)
    @options.each do |key, value|
      case key
      when :os
        match &&= os?(*value)
      when :wordsize
        match &&= wordsize? value
      end
    end
    match
  end
end

class Object
  def platform_is(*args)
    g = PlatformGuard.new(*args)
    g.name = :platform_is
    yield if g.yield?
  ensure
    g.unregister
  end

  def platform_is_not(*args)
    g = PlatformGuard.new(*args)
    g.name = :platform_is_not
    yield if g.yield? true
  ensure
    g.unregister
  end
end
