require 'mspec/utils/deprecate'
require 'mspec/utils/version'
require 'mspec/guards/guard'

class VersionGuard < SpecGuard
  def initialize(version)
    case version
    when String
      @version = SpecVersion.new version
    when Range
      a = SpecVersion.new version.begin
      b = SpecVersion.new version.end
      unless version.exclude_end?
        MSpec.deprecate "ruby_version_is with an inclusive range", 'an exclusive range ("2.1"..."2.3")'
      end
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
    begin
      g.name = :ruby_version_is
      yield if g.yield?
    ensure
      g.unregister
    end
  end
end
