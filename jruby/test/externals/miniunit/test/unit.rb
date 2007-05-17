#
# Totally minimal (hopefully) drop-in replacement for test/unit
#

# TODO: document minimal core methods needed for this to work

at_exit { Test::Unit.autotest }

module Test
  class Assertion < Exception; end

  class Unit
    VERSION = "1.0.0"

    def self.autotest
      ObjectSpace.each_object(Class) do |klass|
        next unless klass < Test::Unit::TestCase
        inst = klass.new
        klass.public_instance_methods(true).each do |meth|
          next unless meth.index("test") == 0
          begin
            inst.setup
            inst.send meth.intern
            inst.teardown
          rescue Exception => e
            # TODO: make output compatible with unit_diff
            print "\n", (Test::Assertion === e ? "Failure: " : "Error: ")
            puts "#{klass}.#{meth}: #{e}"
            puts e.backtrace
          end
        end
      end
    end

    class TestCase
      def setup; end
      def teardown; end

      def assert(test, msg="failed assertion (no message given)")
        raise Test::Assertion, msg unless test
      end

      def assert_equal(exp, act, msg=nil)
        assert exp == act, msg || "Expected #{act.inspect} to be equal to #{exp.inspect}"
      end

      def assert_in_delta(exp, act, delta, msg=nil)
        assert((exp.to_f - act.to_f).abs <= delta.to_f, msg || "Expected #{exp} to be within #{delta} of #{act}")
      end

      def assert_instance_of(cls, obj, msg=nil)
        assert cls === obj, msg || "Expected #{obj} to be a #{cls}"
      end

      def assert_kind_of(cls, obj, msg=nil)
        assert obj.kind_of?(cls), msg || "Expected #{obj.inspect} to be a kind of #{cls}"
      end

      def assert_match(exp, act, msg=nil)
        assert act =~ exp, msg || "Expected #{act.inspect} to match #{exp.inspect}"
      end

      def assert_nil(obj, msg=nil)
        assert obj.nil?, msg || "Expected #{obj.inspect} to be nil"
      end

      def assert_not_equal(exp, act, msg=nil)
        assert exp != act, msg || "Expected #{act.inspect} to not be equal to #{exp.inspect}"
       end

      def assert_not_nil(obj, msg=nil)
        assert ! obj.nil?, msg || "Expected #{obj.inspect} to not be nil"
      end

      def assert_not_same(exp, act, msg=nil)
        assert ! exp.equal?(act), msg || "Expected #{act.inspect} to not be the same as #{exp.inspect}"
      end

      def assert_raises(exp, msg=nil)
        begin
          yield
          assert false, "Expected #{exp} to be raised"
        rescue Exception => e
          assert exp === e, msg || "Expected #{exp} to be raised, but got #{e.class}"
          return e
        end
      end
      alias :assert_raise :assert_raises

      def assert_same(exp, act, msg=nil)
        assert exp.equal?(act), msg || "Expected #{act.inspect} to be the same as #{exp.inspect}"
      end

      def assert_operator(o1, op, o2, msg="")
        assert o1.__send__(op, o2), msg || "Expected #{o1}.#{op}(#{o2}) to be true"
      end

      def assert_nothing_raised; yield; end

    end # class TestCase
  end # class Unit
end # module Test
