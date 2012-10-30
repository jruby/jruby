require 'mspec/guards/guard'

class QuarantineGuard < SpecGuard
  def match?
    false
  end
end

class Object
  def quarantine!
    g = QuarantineGuard.new
    g.name = :quarantine!
    yield if g.yield?
  ensure
    g.unregister
  end
end
