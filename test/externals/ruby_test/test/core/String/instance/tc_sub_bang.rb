###############################################################################
# tc_sub_bang.rb
#
# Test case for the String#sub! instance methods. The test case for the
# String#sub instance method can be found in the tc_sub.rb file.
###############################################################################
require 'test/unit'

class TC_String_SubBang_InstanceMethod < Test::Unit::TestCase
   def setup
      @basic = 'hello123'
      @path  = '/foo/bar'
   end

   def test_sub_basic
      assert_respond_to(@basic, :sub!)
      assert_nothing_raised{ @basic.sub!(/\w/, '*') }
      assert_kind_of(String, @basic.sub!(/\w/, '*'))
   end

   def test_sub_regex
      assert_equal('hellp123', @basic.sub!(/o/, 'p'))
      assert_equal('hellp123', @basic)

      assert_equal('h*llp123', @basic.sub!(/[aeiou]/, '*'))
      assert_equal('h*llp123', @basic)

      assert_equal('h*ll123', @basic.sub!(/[pqrs]/, ''))
      assert_equal('h*ll123', @basic)

      assert_nil(@basic.sub!(/x/, '*'))
      assert_equal('h*ll123', @basic)

      assert_equal('h*ll453', @basic.sub!(/\d\d/, '45'))
      assert_equal('h*ll453', @basic)
   end

   def test_sub_string
      assert_equal('/poo/bar', @path.sub!('f', 'p'))
      assert_equal('/poo/bar', @path)

      assert_equal('/poo\car', @path.sub!('/b', '\c'))
      assert_equal('/poo\car', @path)

      assert_equal('=blah=car', @path.sub!("/poo\\", '=blah='))
      assert_equal('=blah=car', @path)

      assert_nil(@path.sub!("xxx", "yyy"))
      assert_equal('=blah=car', @path)
   end

   def test_sub_with_backreferences
      assert_equal('h-e-llo123', @basic.sub!(/([aeiou])/, '-\1-'))
      assert_equal('h-e-llo123', @basic)

      assert_equal('h--llo123', @basic.sub!(/[aeiou]/, '\1'))
      assert_equal('h--llo123', @basic)
   end

   def test_sub_with_block
      assert_equal('hello123xxx', @basic.sub!(/\d+/){ |m| m += 'xxx' })
      assert_equal('hello123xxx', @basic)

      assert_nil(@basic.sub!(/y/){ |m| m += 'xxx' })
      assert_equal('hello123xxx', @basic)

      assert_equal('hello723xxx', @basic.sub!(/\d/, '7'){ |m| m += 'xxx' })
      assert_equal('hello723xxx', @basic)
   end

   def test_sub_with_tainted_replacement
      str = 'x'
      assert_equal(false, @basic.tainted?)
      assert_nothing_raised{ @basic.sub!('l', str) }
      assert_equal(false, @basic.tainted?)
      
      str.taint
      assert_nothing_raised{ @basic.sub!('l', str) }
      assert_equal(true, @basic.tainted?)
   end

   def test_sub_edge_cases
      assert_equal('123_hello123', @basic.sub!(//, '123_'))
      assert_equal('world', @basic.sub!(/.*/, 'world'))
   end

   def test_sub_expected_errors
      assert_raise(TypeError){ @basic.sub!(1, 2) }
      assert_raise(ArgumentError){ @basic.sub!(1, 2, 3) }
      assert_raise(ArgumentError){ @basic.sub! }
      assert_raise(ArgumentError){ @basic.sub!{ } }
   end

   def teardown
      @basic = nil
      @path  = nil
   end
end
