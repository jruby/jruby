require 'java'
$CLASSPATH << File.expand_path('../../../test/target/test-classes', __FILE__)
require 'rspec'

RSpec.configure do |config|
  require File.expand_path('../../../test/jruby/test_helper', __FILE__)
  config.include TestHelper
  # config.extend TestHelper # so that TestHelper constants work
end

# Works like 'should include('str1', 'str2') but for arrays of
# strings. Reports closest matches using Levenshtein distance when a
# string is not found instead of dumping a huge inspect string.
RSpec::Matchers.define :have_strings_or_symbols do |*strings|
  match do |container|
    @included, @missing = [], []
    strings.map!(&:to_sym)
    strings.flatten.each do |s|
      if container.include?(s)
        @included << s
      else
        @missing << s
      end
    end
    @missing.empty?
  end

  failure_message do |container|
    "expected array of #{container.length} elements to include #{@missing.inspect}.\n" +
      "#{closest_match_message(@missing, container)}"
  end

  failure_message_when_negated do |container|
    "expected array of #{container.length} elements to not include #{@included.inspect}."
  end

  # from http://en.wikipedia.org/wiki/Levenshtein_distance
  def levenshtein(s, t)
    m, n = s.length, t.length
    d = Array.new(m) { Array.new(n) { 0 } }

    0.upto(m-1) do |i|
      d[i][0] = i
    end

    0.upto(n-1) do |j|
      d[0][j] = j
    end

    1.upto(n-1) do |j|
      1.upto(m-1) do |i|
        d[i][j] = if s[i] == t[j]
          d[i-1][j-1]
        else
          [d[i-1][j]   + 1,     # deletion
           d[i][j-1]   + 1,     # insertion
           d[i-1][j-1] + 1].min # substitution
        end
      end
    end

    d[m-1][n-1]
  end

  def closest_match_message(missing, container)
    missing.map do |m|
      groups = container.group_by {|x| levenshtein(m, x) }
      "  closest match for #{m.inspect}: #{groups[groups.keys.min].inspect}"
    end.join("\n")
  end
end

def with_stderr_captured
  stderr = $stderr; require 'stringio'
  begin
    $stderr = StringIO.new
    yield
    $stderr.string
  ensure
    $stderr = stderr
  end
end

def with_warn_captured
  warns = []
  oldwarn = nil

  Warning.singleton_class.class_eval do
    oldwarn = instance_method(:warn)
    define_method :warn do |message, category: nil|
      warns << [message, category]
    end
  end

  yield

  Warning.singleton_class.class_eval do
    define_method :warn, oldwarn
  end

  warns
end
