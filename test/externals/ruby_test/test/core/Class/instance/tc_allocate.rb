#####################################################################
# tc_allocate.rb
#
# Test case for the Class#allocate instance method.
#####################################################################
require 'test/unit'

class TC_Class_Allocate_InstanceMethod < Test::Unit::TestCase
   class Foo
      attr_accessor :a, :b, :c

      def self.create(*args)
         obj = allocate
         obj.send(:initialize, *args)
         obj
      end

      def initialize(a, b, c)
         @a, @b, @c = a, b, c
      end
   end
   
   def setup
      @foo = nil
   end

   def test_allocate_basic
      assert_respond_to(Foo, :allocate)
      assert_nothing_raised{ Foo.allocate }
   end

   def test_allocate
      assert_kind_of(Foo, Foo.allocate)
   end

   # Create a Foo object using our custom 'create' method
   #
   def test_create
      assert_nothing_raised{ @foo = Foo.create(1,2,3) }
      assert_kind_of(Foo, @foo)

      assert_equal(1, @foo.a)
      assert_equal(2, @foo.b)
      assert_equal(3, @foo.c)
   end

   def teardown
      @foo = nil
   end
end
