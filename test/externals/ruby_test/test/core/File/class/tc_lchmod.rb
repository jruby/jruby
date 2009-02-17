#####################################################################
# tc_lchmod.rb
#
# Test case for the File.lchmod class method. Note that almost all
# of these tests are skipped on MS Windows, OS X, and SOLARIS.
#####################################################################
require 'test/unit'
require 'test/helper'

class TC_File_Lchmod_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   # TODO: Setup actual symbolic links
   def setup
      @file1 = File.expand_path(__FILE__)
      @file2 = base_file(__FILE__, 'tc_atime.rb')
      @file1_mode = File.stat(@file1).mode
      @file2_mode = File.stat(@file2).mode
   end

   def test_lchmod_basic
      assert_respond_to(File, :lchmod)
      unless WINDOWS || OSX || SOLARIS
         assert_nothing_raised{ File.lchmod(0644, @file1) }
         assert_nothing_raised{ File.lchmod(0644, @file1, @file2) }
         assert_kind_of(Fixnum, File.lchmod(0644, @file1))
      end
   end

   unless WINDOWS || OSX || SOLARIS
      def test_lchmod
         assert_nothing_raised{ File.lchmod(0644, @file1) }
         assert_equal(1, File.lchmod(0644, @file1))
         assert_equal('100644', File.stat(@file1).mode.to_s(8))

         assert_nothing_raised{ File.lchmod(0444, @file2) }
         assert_equal(1, File.lchmod(0444, @file2))
         assert_equal('100444', File.stat(@file2).mode.to_s(8))
      end

      def test_lchmod_multiple_files
         assert_nothing_raised{ File.lchmod(0444, @file1, @file2) }
         assert_equal(2, File.lchmod(0444, @file1, @file2))
         assert_equal('100444', File.stat(@file1).mode.to_s(8))
         assert_equal('100444', File.stat(@file2).mode.to_s(8))
      end

      def test_lchmod_edge_cases
         assert_nothing_raised{ File.lchmod(0444) } # Debatable
         assert_equal(0, File.lchmod(0444))
      end

      def test_lchmod_expected_errors
         assert_raises(ArgumentError){ File.lchmod }
         assert_raises(TypeError){ File.lchmod('0644') }
      end
   end

   def teardown
      unless WINDOWS || OSX || SOLARIS
         File.lchmod(@file1_mode, @file1)
         File.lchmod(@file2_mode, @file2)
      end
      @file1 = nil
      @file2 = nil
      @file1_mode = nil
      @file2_mode = nil
   end
end
