require 'mspec/guards/guard'

class ExtensionsGuard < SpecGuard
  def match?
    if @args.include? :ruby
      raise Exception, "improper use of extended_on guard"
    end
    !standard? and implementation?(*@args)
  end
end

class Object
  def extended_on(*args)
    g = ExtensionsGuard.new(*args)
    g.name = :extended_on
    yield if g.yield?
  ensure
    g.unregister
  end
end
