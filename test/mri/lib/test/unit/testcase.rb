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

      class << self
        alias inherited_without_excludes inherited
        def inherited(sub_class)
          result = inherited_without_excludes(sub_class)

          if ENV["EXCLUDES"]
            begin
              exclude_src = File.read File.join(ENV["EXCLUDES"], sub_class.inspect.gsub("::", "/") + ".rb")
              excludes = {}
              sub_class.send :instance_variable_set, :@excludes, excludes

              sub_class.instance_eval do
                def exclude(name, reason)
                  @excludes[name] = reason
                end
              end

              sub_class.class_eval exclude_src
            rescue Errno::ENOENT
              # no excludes for this class
            end
          end

          result
        end
      end
    end
  end
end
