require 'mspec/guards/version'

class BugGuard < VersionGuard
  def initialize(bug, version)
    @bug = bug
    @version = SpecVersion.new version, true
    self.parameters = [@bug, @version]
  end

  def match?
    return false if MSpec.mode? :no_ruby_bug
    standard? && ruby_version <= @version
  end
end

class Object
  def ruby_bug(bug, version)
    g = BugGuard.new bug, version
    g.name = :ruby_bug
    yield if g.yield? true
  ensure
    g.unregister
  end
end
