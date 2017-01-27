require 'mspec/runner/mspec'
require 'mspec/runner/actions/tally'
require 'mspec/utils/ruby_name'

class SpecGuard
  def self.report
    @report ||= Hash.new { |h,k| h[k] = [] }
  end

  def self.clear
    @report = nil
  end

  def self.finish
    report.keys.sort.each do |key|
      desc = report[key]
      size = desc.size
      spec = size == 1 ? "spec" : "specs"
      print "\n\n#{size} #{spec} omitted by guard: #{key}:\n"
      desc.each { |description| print "\n", description; }
    end

    print "\n\n"
  end

  def self.guards
    @guards ||= []
  end

  def self.clear_guards
    @guards = []
  end

  @@ruby_version_override = nil

  def self.ruby_version_override=(version)
    @@ruby_version_override = version
  end

  def self.ruby_version_override
    @@ruby_version_override
  end

  # Returns a partial Ruby version string based on +which+.
  # For example, if RUBY_VERSION = 8.2.3:
  #
  #  :major  => "8"
  #  :minor  => "8.2"
  #  :tiny   => "8.2.3"
  #  :teeny  => "8.2.3"
  #  :full   => "8.2.3"
  def self.ruby_version(which = :minor)
    case which
    when :major
      n = 1
    when :minor
      n = 2
    when :tiny, :teeny, :full
      n = 3
    end

    version = ruby_version_override || RUBY_VERSION
    version.split('.')[0,n].join('.')
  end

  attr_accessor :name, :parameters

  def initialize(*args)
    self.parameters = @args = args
  end

  def yield?(invert=false)
    return true if MSpec.mode? :unguarded

    allow = match? ^ invert

    if not allow and reporting?
      MSpec.guard
      MSpec.register :finish, SpecGuard
      MSpec.register :add,    self
      return true
    elsif MSpec.mode? :verify
      return true
    end

    allow
  end

  def ===(other)
    true
  end

  def reporting?
    MSpec.mode?(:report) or
      (MSpec.mode?(:report_on) and SpecGuard.guards.include?(name))
  end

  def report_key
    "#{name} #{parameters.join(", ")}"
  end

  def record(description)
    SpecGuard.report[report_key] << description
  end

  def add(example)
    record example.description
    MSpec.retrieve(:formatter).tally.counter.guards!
  end

  def unregister
    MSpec.unguard
    MSpec.unregister :add, self
  end

  def implementation?(*args)
    args.any? do |name|
      !!case name
      when :rubinius
        RUBY_NAME =~ /^rbx/
      when :ruby
        RUBY_NAME =~ /^ruby/ && !defined?(::Truffle)
      when :jruby
        RUBY_NAME =~ /^jruby/
      when :truffleruby
        RUBY_NAME =~ /^ruby/ && defined?(::Truffle)
      when :ironruby
        RUBY_NAME =~ /^ironruby/
      when :macruby
        RUBY_NAME =~ /^macruby/
      when :maglev
        RUBY_NAME =~ /^maglev/
      when :topaz
        RUBY_NAME =~ /^topaz/
      when :opal
        RUBY_NAME =~ /^opal/
      else
        false
      end
    end
  end

  def standard?
    implementation? :ruby
  end

  def windows?(sym, key)
    sym == :windows && !key.match(/(mswin|mingw)/).nil?
  end

  def platform?(*args)
    args.any? do |platform|
      if platform != :java && RUBY_PLATFORM.match('java') && os?(platform)
        true
      else
        RUBY_PLATFORM.match(platform.to_s) || windows?(platform, RUBY_PLATFORM)
      end
    end
  end

  def wordsize?(size)
    size == 8 * 1.size
  end

  HOST_OS = begin
    require 'rbconfig'
    RbConfig::CONFIG['host_os'] || RUBY_PLATFORM
  rescue LoadError
    RUBY_PLATFORM
  end.downcase

  def os?(*oses)
    oses.any? do |os|
      HOST_OS.match(os.to_s) || windows?(os, HOST_OS)
    end
  end

  def match?
    implementation?(*@args) or platform?(*@args)
  end
end
