########################################################################
# tc_glob.rb
#
# Test case for the Dir.glob class method.
########################################################################
require 'test/unit'
require 'test/helper'
require 'fileutils'

class TC_Dir_Glob_Class < Test::Unit::TestCase
   include Test::Helper
   
   # Helper method to get just the basename of the filename. Results
   # are also sorted, since ordering is not guaranteed.
   #
   def base(files)
      files.map{ |f| File.basename(f) }.sort
   end

   def setup
      @foo_files = %w/a.c a.cpp b.c b.h g.rb d/
      @bar_files = %w/a.c a2.cpp a3.h a4.rb/
      @dot_files = %w/.a.p .abc .p a a.p/

      FileUtils.mkdir_p('foo/bar/baz')
      FileUtils.mkdir_p('dot')
	  
      Dir.chdir('foo'){ @foo_files.each{ |f| FileUtils.touch(f) } }
      Dir.chdir('foo/bar'){ @bar_files.each{ |f| FileUtils.touch(f) } }
      Dir.chdir('dot'){ @dot_files.each{ |f| FileUtils.touch(f) } }

      unless WINDOWS
         @baz_files = %w/a* a? a** a^/
         Dir.chdir('foo/bar/baz'){ @baz_files.each{ |f| FileUtils.touch(f) } }
      end
   end
   
   def test_glob_dotmatch
      assert_equal(%w/. .. .a.p .abc .p a a.p/, base(Dir.glob('dot/*', File::FNM_DOTMATCH)))
      assert_equal(%w/.a.p .p a.p/, base(Dir.glob('dot/*p', File::FNM_DOTMATCH)))
   end
   
   def test_glob_casefold
      assert_equal(%w/a.c a.cpp/, base(Dir.glob('foo/A*', File::FNM_CASEFOLD)))
      assert_equal(%w/a.c a.cpp/, base(Dir.glob('foo/a*', File::FNM_CASEFOLD)))
      assert_equal(%w/a a.p/, base(Dir.glob('dot/A*', File::FNM_CASEFOLD)))
      assert_equal([], base(Dir.glob('dot/P*', File::FNM_CASEFOLD)))
   end   

   unless WINDOWS
      def test_glob_escape
         assert_equal(%w/a*/, base(Dir.glob('foo/bar/baz/a\*')))
         assert_equal(%w/a**/, base(Dir.glob('foo/bar/baz/a\*\*')))
         assert_equal(%w/a* a**/, base(Dir.glob('foo/bar/baz/a\**')))
         assert_equal(%w/a?/, base(Dir.glob('foo/bar/baz/a\?')))
         assert_equal(%w/a^/, base(Dir.glob('foo/bar/baz/a\^')))
         assert_equal(%w/a^/, base(Dir.glob('foo/bar/baz/a[\^]')))
      end
   end

   def test_glob_pattern
      assert_equal(%w/a.c a.cpp b.c b.h bar d g.rb/, base(Dir.glob('foo/{*}')))
      assert_equal(%w/g.rb/, base(Dir.glob('foo/{*.rb}')))
      assert_equal(%w/a.cpp g.rb/, base(Dir.glob('foo/*.{rb,cpp}')))
      assert_equal(%w/a.cpp g.rb/, base(Dir.glob('foo/*.{rb,cp}*')))
      assert_equal([], base(Dir.glob('foo/*.{}')))
   end

   def test_glob_char_list
      assert_equal(%w/d/, base(Dir.glob('foo/[a-d]')))
      assert_equal(%w/a.c a.cpp/, base(Dir.glob('foo/[a]*')))
      assert_equal(%w/a.c a.cpp b.c b.h bar d/, base(Dir.glob('foo/[a-d]*')))
      assert_equal(%w/d g.rb/, base(Dir.glob('foo/[^a-b]*')))
      if WINDOWS
         assert_equal(%w/a.c a.cpp b.c b.h bar d g.rb/, base(Dir.glob('foo/[A-Z]*')))
      else
         assert_equal([], base(Dir.glob('foo/[A-Z]*')))
      end
   end

   def test_glob_char_list_edge_cases
      assert_equal([], Dir.glob('foo/[]'))
      assert_equal(['d'], base(Dir.glob('foo/[^]')))
   end

   def test_glob_question_mark
      assert_equal(%w/a.c/, base(Dir.glob('foo/a.?')))
      assert_equal(%w/a.cpp/, base(Dir.glob('foo/a.c?p')))
      assert_equal(%w/a.c b.c b.h bar/, base(Dir.glob('foo/???')))
      assert_equal(%w/a.c b.c b.h/, base(Dir.glob('foo/?.?')))
   end

   def test_glob_basic
      assert_respond_to(Dir, :glob)
      assert_nothing_raised{ Dir.glob("*") }
   end

   def test_glob_valid_metacharacters
      assert_nothing_raised{ Dir.glob("**") }
      assert_nothing_raised{ Dir.glob("foo.*") }
      assert_nothing_raised{ Dir.glob("foo.?") }
      assert_nothing_raised{ Dir.glob("*.[^r]*") }
      assert_nothing_raised{ Dir.glob("*.[a-z][a-z]") }
      assert_nothing_raised{ Dir.glob("*.{rb,h}") }
      assert_nothing_raised{ Dir.glob("*.\t") }
   end

   def test_glob_star
      assert_equal(%w/a.c a.cpp b.c b.h bar d g.rb/, base(Dir.glob('foo/*')))
      assert_equal(%w/a.c a.cpp b.c b.h bar d g.rb/, base(Dir.glob('foo/****')))
      assert_equal(%w/a.c b.c/, base(Dir.glob('foo/*.c')))
      assert_equal(%w/a.c a.cpp/, base(Dir.glob('foo/a*')))
      assert_equal(%w/a.c a.cpp/, base(Dir.glob('foo/a*c*')))
      assert_equal(%w/a.cpp/, base(Dir.glob('foo/a*p*')))
      assert_equal(%w/a a.p/, base(Dir.glob('dot/*')))
      assert_equal([], Dir.glob('x*'))
   end

   def test_glob_double_star
      assert_equal(%w/a.c a.cpp b.c b.h bar d g.rb/, base(Dir.glob('foo/**')))
      # Fails
      #assert_equal(%w/a.c a.c b.c/, base(Dir.glob('**/*.c')))
      assert_equal(%w/a.c a.c b.c/, base(Dir.glob('foo/**/*.c')))
      if WINDOWS
         assert_equal(%w/a.c a.c a.cpp a2.cpp a3.h a4.rb/, base(Dir.glob('foo/**/a*')))
      else
         assert_equal(%w/a* a** a.c a.c a.cpp a2.cpp a3.h a4.rb a? a^/, base(Dir.glob('foo/**/a*')))
      end
      # Fails
#      assert_equal([], Dir.glob('**/x*'))
   end

   def test_glob_flags
      assert_nothing_raised{ Dir.glob("*", File::FNM_DOTMATCH) }
      assert_nothing_raised{ Dir.glob("*", File::FNM_NOESCAPE) }
      assert_nothing_raised{ Dir.glob("*", File::FNM_PATHNAME) }
      assert_nothing_raised{ Dir.glob("*", File::FNM_CASEFOLD) }
   end

   def test_glob_expected_errors
      assert_raises(TypeError){ Dir.glob("*", "*") }
   end

   def teardown
      @foo_files = nil
      @bar_files = nil
      FileUtils.rm_rf('foo')
      FileUtils.rm_rf('dot')
   end
end
