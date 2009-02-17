###############################################################################
# tc_sub.rb
#
# Test case for the String#sub instance methods. The test case for the
# String#sub! instance method can be found in the tc_sub_bang.rb file.
###############################################################################
require 'test/unit'

class TC_String_Sub_InstanceMethod < Test::Unit::TestCase
   def setup
      @basic = 'hello123'
      @path  = '/foo/bar'
   end

   def test_sub_basic
      assert_respond_to(@basic, :sub)
      assert_nothing_raised{ @basic.sub(/\w/, '*') }
      assert_kind_of(String, @basic.sub(/\w/, '*'))
   end

   def test_sub_regex
      assert_equal('hellp123', @basic.sub(/o/, 'p'))
      assert_equal('h*llo123', @basic.sub(/[aeiou]/, '*'))
      assert_equal('hllo123', @basic.sub(/[aeiou]/, ''))
      assert_equal('hello123', @basic.sub(/x/, '*'))
      assert_equal('hello453', @basic.sub(/\d\d/, '45'))
   end

   def test_sub_string
      assert_equal('/poo/bar', @path.sub('f', 'p'))
      assert_equal('/foo\car', @path.sub('/b', '\c'))
      assert_equal('=blah=bar', @path.sub('/foo/', '=blah='))
   end

   def test_sub_with_backreferences
      assert_equal('h-e-llo123', @basic.sub(/([aeiou])/, '-\1-'))
      assert_equal('hllo123', @basic.sub(/[aeiou]/, '\1'))
   end

   def test_sub_with_block
      assert_equal('hello123xxx', @basic.sub(/\d+/){ |m| m += 'xxx' })
      assert_equal('hello123', @basic.sub(/x/){ |m| m += 'xxx' })
      assert_equal('hello723', @basic.sub(/\d/, '7'){ |m| m += 'x' }) #ignored
   end

   def test_sub_with_tainted_replacement
      str = 'x'
      assert_equal(false, @basic.tainted?)
      assert_equal(false, @basic.sub('l', str).tainted?)
      
      str.taint
      assert_equal(true, @basic.sub!('l', str).tainted?)
   end

   def test_sub_edge_cases
      assert_equal('123_hello123', @basic.sub(//, '123_'))
      assert_equal('world', @basic.sub(/.*/, 'world'))
   end

   def test_sub_expected_errors
      assert_raise(TypeError){ @basic.sub(1, 2) }
      assert_raise(ArgumentError){ @basic.sub(1, 2, 3) }
      assert_raise(ArgumentError){ @basic.sub }
      assert_raise(ArgumentError){ @basic.sub{ } }
   end

   def teardown
      @basic = nil
      @path  = nil
   end
end
