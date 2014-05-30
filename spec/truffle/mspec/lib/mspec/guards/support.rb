require 'mspec/guards/guard'

class SupportedGuard < SpecGuard
  def match?
    if @args.include? :ruby
      raise Exception, "improper use of not_supported_on guard"
    end
    standard? or !implementation?(*@args)
  end
end

class Object
  def not_supported_on(*args)
    g = SupportedGuard.new(*args)
    g.name = :not_supported_on
    yield if g.yield?
  ensure
    g.unregister
  end
end
