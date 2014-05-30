require 'mspec/utils/version'
require 'mspec/guards/guard'

class VersionGuard < SpecGuard
  def initialize(version)
    case version
    when String
      @version = SpecVersion.new version
    when Range
      a = SpecVersion.new version.first
      b = SpecVersion.new version.last
      @version = version.exclude_end? ? a...b : a..b
    end
    self.parameters = [version]
  end

  def ruby_version
    @ruby_version ||= SpecVersion.new self.class.ruby_version(:full)
  end

  def match?
    if Range === @version
      @version.include? ruby_version
    else
      ruby_version >= @version
    end
  end
end

class Object
  def ruby_version_is(*args)
    g = VersionGuard.new(*args)
    g.name = :ruby_version_is
    yield if g.yield?
  ensure
    g.unregister
  end
end
