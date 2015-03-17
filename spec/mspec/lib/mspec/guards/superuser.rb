require 'mspec/guards/guard'

class SuperUserGuard < SpecGuard
  def match?
    Process.euid == 0
  end
end

class Object
  def as_superuser
    g = SuperUserGuard.new
    g.name = :as_superuser
    yield if g.yield?
  ensure
    g.unregister
  end
end
