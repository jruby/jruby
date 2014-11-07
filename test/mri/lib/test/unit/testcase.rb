require 'test/unit/assertions'

module Test
  module Unit
    # remove silly TestCase class
    remove_const(:TestCase) if defined?(self::TestCase)

    class TestCase < MiniTest::Unit::TestCase # :nodoc: all
      include Assertions

      def on_parallel_worker?
        false
      end

      def run runner
        @options = runner.options
        super runner
      end

      def self.test_order
        :sorted
      end

      def self.method_added(name)
        return unless name.to_s.start_with?("test_")

        if @excludes && @excludes[name]
          remove_method name
          return false
        end

        @test_methods ||= {}
        if @test_methods[name]
          warn "test/unit warning: method #{ self }##{ name } is redefined"
        end
        @test_methods[name] = true
      end

      # Override include so that tests pulled out of modules can also be excluded
      def self.include(mod)
        result = super(mod)

        mod.public_instance_methods.each do |method|
          if @excludes && @excludes[method]
            undef_method method
          end
        end

        result
      end
    end
  end
end
