#
# This is the main Rubicon module, implemented as a module to
# protect the namespace a tad
#

RUBICON_VERSION = "V0.3.5"

require 'fileutils'
require 'set'
require 'test/unit'
require 'test/unit/ui/console/testrunner.rb'

Test::Unit.run = true
module Test
  module Unit
   class TestResult
     attr_accessor :failures, :errors
   end
  end
end

module Rubicon

  def self.handleTests(testClass)
    suite=nil
    selected_testmethods = []
    ARGV.each do |arg|
      case arg
      when "-n"
        # ignore "-n" option for compatibility with
        # vanilla Test::Unit::AutoRunner
      when "-m", "-M"
        require "rubicon_missing"
        $rubicon_missing_strict = (arg == "-M")
      else
        selected_testmethods << arg
      end
    end
    if selected_testmethods.empty?
      suite = testClass.suite
    else
      suite = Test::Unit::TestSuite.new
      selected_testmethods.each do |testmethod|
        catch(:invalid_test) do
          suite << testClass.new(testmethod)
        end
      end
    end
    Test::Unit.run = true
    testrunner = Test::Unit::UI::Console::TestRunner.new(suite)
    results = testrunner.start

    exit(results.error_count + results.failure_count > 0 ? 1 : 0)
  end

  # -------------------------------------------------------

  class TestCase < Test::Unit::TestCase

    # Ruby uses the functions stat(2) and chmod(2) to manipulate
    # file permissions. One Windows there is an implementation of these
    # functions that tries to "emulate" the behaviour on Unix.
    # But all you can really do on Windows is to turn the readonly flag
    # on/off. The MSDN documentation of _chmod says:
    #
    #   "If write permission is not given, the file is read-only.
    #    Note that all files are always readable; it is not possible
    #    to give write-only permission".
    #
    # So the _fstat_map method below reflects the way stat/chmod
    # works on Windows. 
    #
    def _fstat_map(mode)
      if $os <= WindowsNative
        mode |= 0444
        if (mode & 0200) != 0
          mode |= 0222
        end
      end
      mode
    end

    # In addition to the chmod/stat behaviour on Windows described above,
    # Ruby also adds a twist all by itself: the Ruby source code on
    # Windows masks the "mode" value returned from the system so that
    # 0666 becomes 0644 (the code is in win32\win32.c in the function
    # "rb_w32_stat").
    #
    # I don't know the reason for this.
    # But the _stat_map method below reflects the fact that this mapping
    # occurs in Ruby.
    #
    def _stat_map(mode)
      mode = _fstat_map(mode)
      if $os <= WindowsNative
        mode &= ~0022
      end
      mode
    end

    # Local routine to check that a set of bits, and only a set of bits,
    # is set!
    def checkBits(bits, num)
      0.upto(90)  { |n|
        expected = bits.include?(n) ? 1 : 0
        assert_equal(expected, num[n], "bit %d" % n)
      }
    end

    def truth_table(method, *result)
      for a in [ false, true ]
        expected = result.shift
        assert_equal(expected, method.call(a))
        assert_equal(expected, method.call(a ? self : nil))
      end
    end

    # 
    # Report we're skipping a test
    #
    def skipping(info, from=nil)
      unless from
        caller[0] =~ /`(.*)'/ #`
        from = $1
      end
      $stderr.print "S" # TODO: fix this to work better w/ test/unit
    end

    #
    # Don't interpret no testcases in file as an error
    #
    def test_default
    end

    #
    # Check a float for approximate equality
    #
    # TODO: nuke this
    def assert_flequal(exp, actual, msg=nil)
      assert_in_delta(exp, actual, exp == 0.0 ? 1e-7 : exp.abs/1e7, msg)
    end

    #
    # Skip a test if not super user
    #
    # TODO: shouldn't this test to see if you are root or not?
    # or... get rid of it, it is functionally useless.
    def super_user
      caller[0] =~ /`(.*)'/ #`
      skipping("not super user", $1)
    end

    #
    # Issue a system and abort on error
    #
    # TODO: this is only used in TestFile and TestDir to almost always do 
    #       touch or rm. - nuke it in favor of fileutils
    def sys(cmd)
      MsWin32.or_variant {
	assert(system(cmd), "command failed: #{cmd}")
      }
      MsWin32.dont {
	assert(system(cmd), cmd + ": #{$? >> 8}")
	assert_equal(0, $?, "cmd: #{$?}")
      }
    end

    #
    # Use our 'test_touch' utility to touch a file
    #
    # TODO: nuke this in favor of fileutils
    def touch(arg)
      sys("#{TEST_TOUCH} #{arg}")
    end

    #
    # And out checkstat utility to get the status
    #
    # TODO: nuke this in favor of fileutils
    def checkstat(arg)
      `#{CHECKSTAT} #{arg}`
    end

    #
    # Check that two arrays contain the same "bag" of elements.
    # A mathematical bag differs from a "set" by counting the
    # occurences of each element. So as a bag [1,2,1] differs from
    # [2,1] (but is equal to [1,1,2]).
    #
    # The method only relies on the == operator to match objects
    # from the two arrays. The elements of the arrays may contain
    # objects that are not "Comparable".
    # 
    def assert_bag_equal(expected, actual)
      # For each object in "actual" we remove an equal object
      # from "expected". If we can match objects pairwise from the
      # two arrays we have two equal "bags". The method Array#index
      # uses == internally. We operate on a copy of "expected" to
      # avoid destructively changing the argument.
      #
      expected_left = expected.dup
      actual.each do |x|
        if j = expected_left.index(x)
          expected_left.slice!(j)
        end
      end
      assert( expected.length == actual.length && expected_left.length == 0,
             "Expected: #{expected.inspect}, Actual: #{actual.inspect}")
    end

    #
    # Run a block in a sub process and return exit status
    #
    def runChild(&block)
      pid = fork 
      if pid.nil?
	block.call
        exit 0
      end
      Process.waitpid(pid, 0)
      return ($? >> 8) & 0xff
    end

    def setup
      super
    end

    def teardown
      if $os != MsWin32 && $os != JRuby
	begin
	  loop { Process.wait; $stderr.puts "\n\nCHILD REAPED\n\n" }
	rescue Errno::ECHILD
	end
      end
      super
    end
    #
    # Setup some files in a test directory.
    #
    def setupTestDir
      @start = Dir.getwd
      teardownTestDir
      begin
	Dir.mkdir("_test")
      rescue
        $stderr.puts "Cannot run a file or directory test: " + 
          "will destroy existing directory _test"
        exit(99)
      end
      File.open(File.join("_test", "_file1"), "w", 0644) {}
      File.open(File.join("_test", "_file2"), "w", 0644) {}
      @files = %w(. .. _file1 _file2)
    end
    
    def deldir(name)
      File.chmod(0755, name)
      Dir.foreach(name) do |f|
        next if f == '.' || f == '..'
        f = File.join(name, f)
        if File.lstat(f).directory?
          deldir(f) 
        else
          File.chmod(0644, f) rescue true
          File.delete(f)
        end 
      end
      Dir.rmdir(name)
    end

    def teardownTestDir
      Dir.chdir(@start)
      deldir("_test") if (File.exists?("_test"))
    end
    
  end

  # Record a particule failure, which is a location
  # and an error message. We simply ape the Test::Unit
  # TestFailure class.

  class Failure
    attr_accessor :at
    attr_accessor :err
    def Failure.from_real_failures(f)
      f.collect do |a_failure|
        my_f = Failure.new
        if(a_failure.respond_to?(:exception)) then
          my_f.at = a_failure.test_name
          my_f.err = a_failure.exception
        else
          my_f.at = a_failure.location
          my_f.err = a_failure.message
        end
        my_f
      end
    end
  end

  # Objects of this class get generated from the TestResult
  # passed back by Test::Unit. We don't use it's class for two reasons:
  # 1. We decouple better this way
  # 2. We can't serialize the Test::Unit class, as it contains IO objects CHECK THIS FOR ACCURACY!
  #

  class Results
    attr_reader :failure_size
    attr_reader :error_size
    attr_reader :run_tests
    attr_reader :run_asserts
    attr_reader :failures
    attr_reader :errors

    def initialize_from(test_result)
      @failure_size = test_result.failure_count
      @error_size   = test_result.error_count
      @run_tests    = test_result.run_count
      @run_asserts  = test_result.assertion_count
      @succeed      = test_result.passed?
      @failures     = Failure.from_real_failures(test_result.failures)
      @errors       = Failure.from_real_failures(test_result.errors)
      self
    end

    def succeed?
      @succeed
    end
  end

  # And here is where we gather the results of all the tests. This is
  # also the object exported to XML

  class ResultGatherer

    attr_reader   :results
    attr_accessor :name
    attr_reader   :config
    attr_reader   :date
    attr_reader   :rubicon_version
    attr_reader   :ruby_version
    attr_reader   :ruby_release_date
    attr_reader   :ruby_architecture

    attr_reader   :failure_count

    # Two sage initialization, so that Rubric doesn't create all the
    # internals when we unmarshal

    def initialize(name = '')
      @name    = ''
      @failure_count = 0
    end

    def setup
      @results = {}
      @config  = Config::CONFIG
      @date    = Time.now
      @rubicon_version = RUBICON_VERSION

      ver = `#$interpreter --version`
      # ruby 1.7.1 (2001-07-26) [i686-linux]  
      unless ver =~ /ruby (\d+\.\d+\.\d+)\s+\((.*?)\)\s+\[(.*?)\]/
        raise "Couldn't find version in '#{ver}'" 
      end
      @ruby_version      = $1
      @ruby_release_date = $2
      @ruby_architecture = $3
      self
    end

    def add(klass, result_set)
      @results[klass.name] = Results.new.initialize_from(result_set)
      @failure_count += result_set.error_count + result_set.failure_count
    end
    
  end

  # Run a set of tests in a file. This would be a TestSuite, but we
  # want to run each file separately, and to summarize the results
  # differently

  class BulkTestRunner

    def initialize(args, group_name)
      @groupName = group_name
      @files     = []
      @results   = ResultGatherer.new.setup
      @results.name   = group_name
      @op_class_file  = "ascii"

      # Look for a -op <class> argument, which controls
      # where our output goes

      if args.size > 1 and args[0] == "-op"
        args.shift
        @op_class_file = args.shift
      end

      @op_class_file = "result_" + @op_class_file
      require @op_class_file
    end

    def addFile(fileName)
      @files << fileName
    end

    def run
      Test::Unit.run = true

      @files.each do |file|
        require file
        className = File.basename(file)
        className.sub!(/\.rb$/, '')
        klass = eval className

        runner = Test::Unit::UI::Console::TestRunner.new(klass.suite)

        $stderr.print "\n", className, ": "

        @results.add(klass, runner.start)
      end

      reporter = ResultDisplay.new(@results)
      reporter.reportOn $stdout
      return @results.failure_count
    end

  end
end
