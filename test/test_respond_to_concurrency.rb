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
