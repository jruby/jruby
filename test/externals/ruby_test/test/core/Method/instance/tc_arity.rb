########################################################################
# tc_arity.rb
#
# Test case for the Method#arity instance method.
########################################################################
require 'test/unit'

class MethodTest
   def one;                end
   def two(a);             end
   def three(*a);          end
   def four(a, b);         end
   def five(a, b, *c);     end
   def six(a, b, *c, &d);  end
end

class TC_Method_Arity_InstanceMethod < Test::Unit::TestCase
   def setup
      @one   = MethodTest.instance_method(:one)
      @two   = MethodTest.instance_method(:two)
      @three = MethodTest.instance_method(:three)
      @four  = MethodTest.instance_method(:four)
      @five  = MethodTest.instance_method(:five)
      @six   = MethodTest.instance_method(:six)
   end

   def test_arity_basic
      assert_respond_to(@one, :arity)
      assert_nothing_raised{ @one.arity }
      assert_kind_of(Fixnum, @one.arity)
   end

   def test_arity
      assert_equal(0, @one.arity)
      assert_equal(1, @two.arity)
      assert_equal(-1, @three.arity)
      assert_equal(2, @four.arity)
      assert_equal(-3, @five.arity)
      assert_equal(-3, @six.arity)
   end

   def teardown
      @one   = nil
      @two   = nil
      @three = nil
      @four  = nil
      @five  = nil
      @six   = nil
   end
end
