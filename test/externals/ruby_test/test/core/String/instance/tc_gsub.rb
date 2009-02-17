###############################################################################
# tc_gsub.rb
#
# Test case for the String#gsub instance methods. The test case for the
# String#gsub! instance method can be found in the tc_gsub_bang.rb file.
###############################################################################
require 'test/unit'

class TC_String_Gsub_InstanceMethod < Test::Unit::TestCase
   def setup
      @basic = 'hello123'
      @path  = '/foo/bar'
   end

   def test_gsub_basic
      assert_respond_to(@basic, :gsub)
      assert_nothing_raised{ @basic.gsub(/\w/, '*') }
      assert_kind_of(String, @basic.gsub(/\w/, '*'))
   end

   def test_gsub_regex
      assert_equal('hexxo123', @basic.gsub(/l/, 'x'))
      assert_equal('h*ll*123', @basic.gsub(/[aeiou]/, '*'))
      assert_equal('hll123', @basic.gsub(/[aeiou]/, ''))
      assert_equal('hello123', @basic.gsub(/x/, '*'))
      assert_equal('hello444', @basic.gsub(/\d/, '4'))
   end

   def test_gsub_string
      assert_equal('\foo\bar', @path.gsub('/', '\\'))
      assert_equal('/foo\car', @path.gsub('/b', '\c'))
      assert_equal('=blah=bar', @path.gsub('/foo/', '=blah='))
   end

   def test_gsub_with_backreferences
      assert_equal('h-e-ll-o-123', @basic.gsub(/([aeiou])/, '-\1-'))
      assert_equal('hll123', @basic.gsub(/[aeiou]/, '\1'))
   end

   def test_gsub_with_block
      assert_equal('hello123xxx', @basic.gsub(/\d+/){ |m| m += 'xxx' })
      assert_equal('hello123', @basic.gsub(/x/){ |m| m += 'xxx' })
      assert_equal('hello777', @basic.gsub(/\d/, '7'){ |m| m += 'x' }) #ignored
   end

   def test_gsub_with_tainted_replacement
      str = 'x'
      assert_equal(false, @basic.tainted?)
      assert_equal(false, @basic.gsub('l', str).tainted?)
      
      str.taint
      assert_equal(true, @basic.gsub!('l', str).tainted?)
   end

   def test_gsub_edge_cases
      assert_equal('xhxexlxlxox1x2x3x', @basic.gsub(//, 'x'))
      assert_equal('worldworld', @basic.gsub(/.*/, 'world'))
   end

   def test_gsub_expected_errors
      assert_raise(TypeError){ @basic.gsub(1, 2) }
      assert_raise(ArgumentError){ @basic.gsub(1, 2, 3) }
      assert_raise(ArgumentError){ @basic.gsub }
      assert_raise(ArgumentError){ @basic.gsub{ } }
   end

   def teardown
      @basic = nil
      @path  = nil
   end
end
