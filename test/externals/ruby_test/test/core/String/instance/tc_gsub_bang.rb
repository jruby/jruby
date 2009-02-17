###############################################################################
# tc_gsub_bang.rb
#
# Test case for the String#gsub! instance methods. The test case for the
# String#gsub instance method can be found in the tc_gsub.rb file.
###############################################################################
require 'test/unit'

class TC_String_GsubBang_InstanceMethod < Test::Unit::TestCase
   def setup
      @basic = 'hello123'
      @path  = '/foo/bar'
   end

   def test_gsub_basic
      assert_respond_to(@basic, :gsub!)
      assert_nothing_raised{ @basic.gsub!(/\w/, '*') }
      assert_equal(nil, @basic.gsub!(/\w/, '*'))
   end

   def test_gsub_regex
      assert_equal('hexxo123', @basic.gsub!(/l/, 'x'))
      assert_equal('hexxo123', @basic)

      assert_equal('h*xx*123', @basic.gsub!(/[aeiou]/, '*'))
      assert_equal('h*xx*123', @basic)

      assert_equal(nil, @basic.gsub!(/[pqrs]/, ''))
      assert_equal('h*xx*123', @basic)

      assert_nil(@basic.gsub!(/y/, '*'))
      assert_equal('h*xx*123', @basic)

      assert_equal('h*xx*453', @basic.gsub!(/\d\d/, '45'))
      assert_equal('h*xx*453', @basic)
   end

   def test_gsub_string
      assert_equal('/poo/bar', @path.gsub!('f', 'p'))
      assert_equal('/poo/bar', @path)

      assert_equal('/poo\car', @path.gsub!('/b', '\c'))
      assert_equal('/poo\car', @path)

      assert_equal('=blah=car', @path.gsub!("/poo\\", '=blah='))
      assert_equal('=blah=car', @path)

      assert_nil(@path.gsub!("xxx", "yyy"))
      assert_equal('=blah=car', @path)
   end

   def test_gsub_with_backreferences
      assert_equal('h-e-ll-o-123', @basic.gsub!(/([aeiou])/, '-\1-'))
      assert_equal('h-e-ll-o-123', @basic)

      assert_equal('h--ll--123', @basic.gsub!(/[aeiou]/, '\1'))
      assert_equal('h--ll--123', @basic)
   end

   def test_gsub_with_block
      assert_equal('hello123xxx', @basic.gsub!(/\d+/){ |m| m += 'xxx' })
      assert_equal('hello123xxx', @basic)

      assert_nil(@basic.gsub!(/y/){ |m| m += 'xxx' })
      assert_equal('hello123xxx', @basic)

      assert_equal('hello777xxx', @basic.gsub!(/\d/, '7'){ |m| m += 'xxx' })
      assert_equal('hello777xxx', @basic)
   end

   def test_gsub_with_tainted_replacement
      str = 'x'
      assert_equal(false, @basic.tainted?)
      assert_nothing_raised{ @basic.gsub!('l', str) }
      assert_equal(false, @basic.tainted?)
      
      @basic = 'hello123' # reset
      str.taint
      assert_nothing_raised{ @basic.gsub!('l', str) }
      assert_equal(true, @basic.tainted?)
   end

   def test_gsub_edge_cases
      assert_equal('xhxexlxlxox1x2x3x', @basic.gsub!(//, 'x'))
      assert_equal('worldworld', @basic.gsub!(/.*/, 'world'))
   end

   def test_gsub_expected_errors
      assert_raise(TypeError){ @basic.gsub!(1, 2) }
      assert_raise(ArgumentError){ @basic.gsub!(1, 2, 3) }
      assert_raise(ArgumentError){ @basic.gsub! }
      assert_raise(ArgumentError){ @basic.gsub!{ } }
   end

   def teardown
      @basic = nil
      @path  = nil
   end
end
