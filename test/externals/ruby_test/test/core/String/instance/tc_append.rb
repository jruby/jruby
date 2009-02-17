###############################################################################
# tc_append.rb
#
# Test case for the String#<< instance method and the String#concat alias.
###############################################################################
require 'test/unit'

class TC_String_Append_InstanceMethod < Test::Unit::TestCase
   class StringAppender
      def to_str
         '123'
      end
   end

   def setup
      @string = 'hello'
      @append = StringAppender.new
   end

   def test_append_basic
      assert_respond_to(@string, :<<)
      assert_nothing_raised{ @string << 'world' }
      assert_kind_of(String, @string << 'abc')
   end

   def test_concat_alias_basic
      assert_respond_to(@string, :concat)
      assert_nothing_raised{ @string.concat('world') }
      assert_kind_of(String, @string.concat('abc'))
   end

   def test_append_string
      assert_equal('helloabc', @string << 'abc')
      assert_equal('helloabc', @string) # receiver modified
      assert_equal('helloabc ', @string << ' ')
   end

   def test_concat_alias_string
      assert_equal('helloabc', @string.concat('abc'))
      assert_equal('helloabc', @string) # receiver modified
      assert_equal('helloabc ', @string.concat(' '))
   end

   def test_append_string_chained
      assert_equal('helloabcdef', @string << 'abc' << 'def' )
   end

   def test_append_string_edge_cases
      assert_equal('hello', @string << '')
      assert_equal('hellotrue', @string << 'true')
      assert_equal('hellotrue0', @string << '0')
   end

   def test_concat_alias_edge_cases
      assert_equal('hello', @string.concat(''))
      assert_equal('hellotrue', @string.concat('true'))
      assert_equal('hellotrue0', @string.concat('0'))
   end

   def test_append_calls_to_str_method
      assert_equal('hello123', @string << @append)
   end

   def test_append_fixnum
      assert_equal('hello!', @string << 33)
      assert_equal("hello!\000", @string << 0)
   end

   def test_concat_alias_fixnum
      assert_equal('hello!', @string.concat(33))
      assert_equal("hello!\000", @string.concat(0))
   end

   def test_append_expected_errors
      assert_raise(ArgumentError){ @string.send(:<<) }
      assert_raise(ArgumentError){ @string.send(:<<, 1, 2) }
      assert_raise(TypeError){ @string << nil }
      assert_raise(TypeError){ @string << true }
      assert_raise(TypeError){ @string << false }
      assert_raise(TypeError){ @string << 256 } # 0 to 255 only
      assert_raise(TypeError){ @string << -1 } # 0 to 255 only
   end

   def test_concat_alias_expected_errors
      assert_raise(ArgumentError){ @string.concat }
      assert_raise(ArgumentError){ @string.concat(1,2) }
      assert_raise(TypeError){ @string.concat(nil) }
      assert_raise(TypeError){ @string.concat(true) }
      assert_raise(TypeError){ @string.concat(false) }
      assert_raise(TypeError){ @string.concat(256) } # 0 to 255 only
      assert_raise(TypeError){ @string.concat(-1) } # 0 to 255 only
   end

   def teardown
      @string = nil
      @append = nil
   end
end
