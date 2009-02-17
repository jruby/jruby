########################################################################
# tc_bind.rb
#
# Test case for the UnboundMethod#bind instance method.
########################################################################
require 'test/unit'

class UnboundFoo
   def test
      'test'
   end
end

class UnboundBar < UnboundFoo; end
class UnrelatedBaz; end

class TC_UnboundMethod_Bind_InstanceMethod < Test::Unit::TestCase
   def setup
      @meth = UnboundFoo.instance_method(:test)
      @foo  = UnboundFoo.new
      @bar  = UnboundBar.new
      @baz  = UnrelatedBaz.new
      @bm   = nil
   end

   def test_bind_basic
      assert_respond_to(@meth, :bind)
   end

   def test_bind
      assert_nothing_raised{ @bm = @meth.bind(@foo) }
      assert_nothing_raised{ @bm = @meth.bind(@bar) }
      assert_kind_of(Method, @bm)
   end

   def test_bind_callable
      assert_nothing_raised{ @bm = @meth.bind(@bar) }
      assert_equal('test', @bm.call)
      assert_equal('test', @bar.test)
   end

   def test_multiple_binds_to_same_instance
      assert_nothing_raised{ @bm = @meth.bind(@bar) }
      assert_nothing_raised{ @bm = @meth.bind(@bar) }
   end

   def test_bind_expected_failures
      assert_raise(TypeError){ @bm = @meth.bind(@baz) }
      assert_raise(TypeError){ @bm = @meth.bind(Fixnum) }
      assert_raise(TypeError){ @bm = @meth.bind(77) }
   end

   def teardown
      @bm.unbind if @bm
      @meth = nil
      @foo  = nil
      @bar  = nil
      @baz  = nil
   end
end
