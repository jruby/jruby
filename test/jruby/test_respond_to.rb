require 'test/unit'

# JRUBY-4548
class TestRespondToConcurrency < Test::Unit::TestCase
  module Hashable
    def to_h
      hash = {}
      self.class.hashable_methods.each do |method_entry|
        val = self.__send__(method_entry[:method_name])
        #Respond to returns true sometimes and false other times
        hash[method_entry[:key]] = (val.respond_to?(:to_h) ? val.to_h : val ) if val
        #is_a?(Hashable) always seems to work
        #hash[method_entry[:key]] = (val.is_a?(Hashable) ? val.to_h : val ) if val
      end
      hash
    end

    def self.included(klass)
      klass.extend(ClassMethods)
    end

    module ClassMethods
      attr_reader :hashable_methods
      def method_added(method_name)
        @hashable_methods ||= []
        @hashable_methods << {:method_name => method_name, :key => method_name.to_s } 
      end
    end
  end

  class Foo
    include Hashable
    def bar
      @bar ||= Bar.new
    end

    def dofoo
      return 1
    end
  end

  class Bar
    include Hashable
    def dobar
      return 1
    end
  end

  EXPECTED = {"bar"=>{"dobar"=>1}, "dofoo"=>1}

  def test_respond_to_heavy_concurrency
    assert_nothing_raised do
      threads = (0..100).map do
        Thread.new do
          1000.times do
            actual = Foo.new.to_h
            assert_equal EXPECTED, actual, "respond_to? concurrency produced an invalid result"
          end
        end
      end

      threads.each { |t| t.join }
    end
  end
end

# JRUBY-4954
class TestRespondToCallSite < Test::Unit::TestCase
  class CallSite
    @obj = Object.new
    def self.respond_to?(name, include_priv = false)
      @obj.respond_to?(name, include_priv)
    end
  end

  def test_respond_to_call_site_caches_visibility_check
    assert !CallSite.respond_to?(:method_missing)
    assert CallSite.respond_to?(:method_missing, true)
  end
end

# regression from r91031746 broke m_m invocation when attempting respond_to?
class TestRespondToViaMethodMissing < Test::Unit::TestCase
  class ABasicObject #:nodoc:
    instance_methods.each do |m|
      undef_method(m) if m.to_s !~ /(?:^__|^nil\?$|^send$|^object_id$)/
    end
    
    attr_accessor :respond_to_called

    def method_missing(name, *args)
      if name == :respond_to? && args[0] == :to_str
        @respond_to_called = true
        true
      elsif name == :==
        true
      else
        super
      end
    end
  end

  def test_respond_to_check_can_trigger_method_missing
    obj = ABasicObject.new
    assert_nothing_raised do
      assert "string" == obj
    end
  end
end

class TestRespondToMissingFastPath < Test::Unit::TestCase
  class Duration
    def initialize
      @value = 10
    end

    def respond_to_missing?(method, include_private=false)
      @value.respond_to?(method, include_private)
    end

    def method_missing(method, *args, &block)
      @value.send(method, *args, &block)
    end
  end

  def test_respond_to_doesnt_fastpath_if_respond_to_missing_exists
    obj = Duration.new
    assert(10 * obj == 100)
  end
end