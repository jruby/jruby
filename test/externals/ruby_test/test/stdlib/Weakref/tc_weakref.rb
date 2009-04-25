########################################################################
# tc_weakref.rb
#
# Test suite for the Weakref library.
#
# TODO: FIX! I think Test::Unit is messing things up, because similar
# standalone code works as expected.
########################################################################
require 'test/unit'
require 'weakref'
require 'jruby'

class TC_WeakRef < Test::Unit::TestCase
   def setup
      @ref = nil
      @str = 'hello'
      GC.enable
   end

   def test_weakref_constructor
      assert_respond_to(WeakRef, :new)
      assert_nothing_raised{ @ref = WeakRef.new(@str) }
      assert_kind_of(WeakRef, @ref)
   end

   # TODO: Figure out why last test fails
   def test_weakref
      assert_nothing_raised{ @ref = WeakRef.new(@str) }
      assert_equal('hello', @ref)

      assert_nothing_raised{ JRuby.gc }
      assert_equal('hello', @ref)

      assert_nothing_raised{ @str = nil }
      assert_equal('hello', @ref)

      assert_nothing_raised{ JRuby.gc }
      assert_raise(WeakRef::RefError){ @str = @ref * 3 }
   end

   def test_weakref_is_alive_basic
      assert_nothing_raised{ @ref = WeakRef.new(@str) }
      assert_respond_to(@ref, :weakref_alive?)
   end

   # TODO: Figure out why last test fails
   def test_weakref_is_alive
      assert_nothing_raised{ @ref = WeakRef.new(@str) }
      assert_equal(true, @ref.weakref_alive?)

      assert_nothing_raised{ JRuby.gc }
      assert_equal(true, @ref.weakref_alive?)

      assert_nothing_raised{ @str = nil }
      assert_equal(true, @ref.weakref_alive?)

      assert_nothing_raised{ JRuby.gc }
      assert_equal(false, @ref.weakref_alive?)
   end

   def teardown
      @str = nil
      @ref = nil
   end
end
