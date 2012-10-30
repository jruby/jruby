require 'mspec/guards/guard'

class UserGuard < SpecGuard
  def match?
    Process.euid != 0
  end
end

class Object
  def as_user
    g = UserGuard.new
    g.name = :as_user
    yield if g.yield?
  ensure
    g.unregister
  end
end
