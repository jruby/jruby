require 'test/unit'

class TestDir < Test::Unit::TestCase
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
  # FIXME: This should be moved to common location.
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
  
  def super_user
    caller[0] =~ /`(.*)'/ #`
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

  def setup
    setupTestDir
  end

  def teardown
    teardownTestDir
  end

  def test_s_AREF
    [
      [ %w( _test ),                     Dir["_test"] ],
      [ %w( _test/ ),                    Dir["_test/"] ],
      [ %w( _test/_file1 _test/_file2 ), Dir["_test/*"] ],
      [ %w( _test/_file1 _test/_file2 ), Dir["_test/_file*"] ],
      [ %w(  ),                          Dir["_test/frog*"] ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir["**/_file*"] ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir["_test/_file[0-9]*"] ],
      [ %w( ),                           Dir["_test/_file[a-z]*"] ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir["_test/_file{0,1,2,3}"] ],
      [ %w( ),                           Dir["_test/_file{4,5,6,7}"] ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir["**/_f*[il]l*"] ],    
      [ %w( _test/_file1 _test/_file2 ), Dir["**/_f*[il]e[0-9]"] ],
      [ %w( _test/_file1              ), Dir["**/_f*[il]e[01]"] ],
      [ %w( _test/_file1              ), Dir["**/_f*[il]e[01]*"] ],
      [ %w( _test/_file1              ), Dir["**/_f*[^ie]e[01]*"] ],
    ].each do |expected, got|
      assert_bag_equal(expected, got)
    end
  end

  # Helper method to test_s_chdir.
  # Sets/restores some of HOME / LOGDIR.
  # Yields to the caller with the changed settings.
  def with_home_logdir(vars_set)
    all_vars = ["HOME", "LOGDIR"]
    to_restore = {}
    begin
      for var in all_vars
        to_restore[var] = ENV[var]
        if vars_set.include?(var)
          ENV[var] = "subdir_#{var}"
        else
          ENV.delete(var)
        end
      end
      yield
    ensure
      for var in all_vars
        value = to_restore[var]
        if value
          ENV[var] = value
        else
          ENV.delete(var)
        end
      end
    end
  end

  def test_s_chdir
    start = Dir.getwd
    assert_raise(Errno::ENOENT)       { Dir.chdir "_wombat" }
    assert_equal(0,                         Dir.chdir("_test"))
    assert_equal(File.join(start, "_test"), Dir.getwd)
    assert_equal(0,                         Dir.chdir(".."))
    assert_equal(start,                     Dir.getwd)

    # chdir with block to existing dir.
    # Parameter to block same value as parameter to chdir.
    assert_equal(start, Dir.getwd)
    Dir.chdir("_test") do |dir_param|
      assert_equal(File.join(start, "_test"), Dir.getwd)
      assert_equal("_test", dir_param)
    end
    assert_equal(start, Dir.getwd)

    # chdir with block.
    # No parameter to chdir => meaning "home dir".
    # Parameter to block nil (because no parameter to chdir)
    # Check all combinations of HOME and LOGDIR by explicitly
    # setting/unsetting them.
    #
    home_logdir_tests = [
      [[],                 nil],
      [["HOME"],           "HOME"],
      [["LOGDIR"],         "LOGDIR"],
      [["HOME", "LOGDIR"], "HOME"],
    ]
    Dir.chdir("_test") do
      Dir.mkdir("subdir_HOME")
      Dir.mkdir("subdir_LOGDIR")
      for vars_set, res in home_logdir_tests
        with_home_logdir(vars_set) do
          assert_equal(File.join(start, "_test"), Dir.getwd)
          if res
            Dir.chdir() do |dir_param|
              assert_equal(File.join(start, "_test","subdir_#{res}"),
                           Dir.getwd)
              Version.less_than("1.8.2") do
                assert_equal(nil, dir_param)
              end
              Version.greater_or_equal("1.8.2") do
                assert_equal("subdir_#{res}", dir_param)
              end
            end
            assert_equal(File.join(start, "_test"), Dir.getwd)
          else
            # none of HOME and LOGDIR set
            assert_raise(ArgumentError) do
              Dir.chdir() do |dir_param|
                # not reached
                assert(false)
              end
            end
          end
        end
      end
    end

    # chdir with block return value of block
    ret_value = Dir.chdir("_test") do
      x = 111
      y = 222
      x + y
    end
    assert_equal( 333, ret_value )

    # chdir with block to non-existing dir
    assert_raise(Errno::ENOENT) do
      Dir.chdir("_wombat") do
        # never reached
      end
    end

=begin How to do this with test/unit?
    MsWin32.only do
      a_win_abs_path = (ENV["SystemRoot"] || "C:/Program Files").dup
      a_win_abs_path.gsub!(/\\/, "/")

      assert_equal(0,                       Dir.chdir(a_win_abs_path));
      assert_equal(a_win_abs_path,          Dir.getwd)
    end
    MsWin32.dont do
      assert_equal(0,                       Dir.chdir("/"))
      assert_equal("/",                  Dir.getwd)
    end
=end
  end

  def test_s_chroot
    super_user
  end

  def test_s_delete
    generic_test_s_rmdir(:delete)
  end

  def test_s_entries
    assert_raise(Errno::ENOENT)      { Dir.entries "_wombat" } 
    assert_raise(Errno::ENOENT)      { Dir.entries "_test/file*" } 
    assert_bag_equal(@files, Dir.entries("_test"))
    assert_bag_equal(@files, Dir.entries("_test/."))
    assert_bag_equal(@files, Dir.entries("_test/../_test"))
  end

  def test_s_foreach
    got = []
    entry = nil
    assert_raise(Errno::ENOENT) { Dir.foreach("_wombat") {}}
    assert_nil(Dir.foreach("_test") { |f| got << f } )
    assert_bag_equal(@files, got)
  end

  def test_s_getwd
=begin How to do this with test/unit
    MsWin32.only do
      assert_equal(`cd`.chomp.gsub(/\\/, '/'), Dir.getwd)
    end
    MsWin32.dont do
      assert_equal(`pwd`.chomp, Dir.getwd)
    end
=end
  end

  def test_s_glob
    [
      [ %w( _test ),                     Dir.glob("_test") ],
      [ %w( _test/ ),                    Dir.glob("_test/") ],
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("_test/*") ],
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("_test/_file*") ],
      [ %w(  ),                          Dir.glob("_test/frog*") ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("**/_file*") ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("_test/_file[0-9]*") ],
      [ %w( ),                           Dir.glob("_test/_file[a-z]*") ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("_test/_file{0,1,2,3}") ],
      [ %w( ),                           Dir.glob("_test/_file{4,5,6,7}") ],
      
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("**/_f*[il]l*") ],
      [ %w( _test/_file1 _test/_file2 ), Dir.glob("**/_f*[il]e[0-9]") ],
      [ %w( _test/_file1              ), Dir.glob("**/_f*[il]e[01]") ],
      [ %w( _test/_file1              ), Dir.glob("**/_f*[il]e[01]*") ],
      [ %w( _test/_file1              ), Dir.glob("**/_f*[^ie]e[01]*") ],
    ].each do |expected, got|
      assert_bag_equal(expected, got)
    end
  end

  def test_s_mkdir
    assert_equal(0, Dir.chdir("_test"))
    assert_equal(0, Dir.mkdir("_lower1"))
    assert(File.stat("_lower1").directory?)
    assert_equal(0, Dir.chdir("_lower1"))
    assert_equal(0, Dir.chdir(".."))
    assert_equal(0, Dir.mkdir("_lower2", 0777))
    assert_equal(0, Dir.delete("_lower1"))
    assert_equal(0, Dir.delete("_lower2"))
  end

  def test_s_new
    assert_raise(ArgumentError) { Dir.new }
    assert_raise(ArgumentError) { Dir.new("a", "b") }
    assert_raise(Errno::ENOENT) { Dir.new("_wombat") }

    assert_equal(Dir, Dir.new(".").class)
  end

  def test_s_open
    assert_raise(ArgumentError) { Dir.open }
    assert_raise(ArgumentError) { Dir.open("a", "b") }
    assert_raise(Errno::ENOENT) { Dir.open("_wombat") }

    assert_equal(Dir, Dir.open(".").class)
    assert_nil(Dir.open(".") { |d| assert_equal(Dir, d.class) } )
  end

  def test_s_pwd
=begin How to do under test/unit?
    MsWin32.only do
      assert_equal(`cd`.chomp.gsub(/\\/, '/'), Dir.pwd)
    end
    MsWin32.dont do
      assert_equal(`pwd`.chomp, Dir.pwd)
    end
=end
  end

  def test_s_rmdir
    generic_test_s_rmdir(:rmdir)
  end

  def generic_test_s_rmdir(method)
    assert_raise(Errno::ENOENT, SystemCallError) do
      Dir.send(method, "_wombat")
    end

    exceptions_ENOTEMPTY = [Errno::ENOTEMPTY, SystemCallError]
=begin Another platform-specific test?
    if $os <= Solaris
      # On Solaris, the system call rmdir(2) returns EEXIST if
      # a directory is not empty. The man page says:
      #
      #    EEXIST
      #        The directory contains entries other  than  those  for
      #        "." and "..".
      #
      # I don't know why Solaris differs from for example
      # Linux and FreeBSD here, but it seems to be a fact.
      #
      exceptions_ENOTEMPTY << Errno::EEXIST
    end
=end
    assert_raise(*exceptions_ENOTEMPTY) { Dir.send(method, "_test") } 
    
    # I'm not sure what this is supposed to test, but it fails in MRI
    deldir("_test")
    assert_equal(0, Dir.send(method, "_test"))
    assert_raise(Errno::ENOENT, SystemCallError)    { Dir.send(method, "_test") } 
  end

  def test_s_unlink
    generic_test_s_rmdir(:unlink)
  end

  def test_close
    d = Dir.new(".")
    d.read
    assert_nil(d.close)
    assert_raise(IOError) { d.read }
  end

  def test_each
    got = []
    d = Dir.new("_test")
    assert_equal(d, d.each { |f| got << f })
    assert_bag_equal(@files, got)
    d.close
  end

  def test_path
    # returns the parameter passed in "constructor"
    assert_equal(".",      Dir.open(".").path)
    assert_equal("..",     Dir.open("..").path)
    assert_equal(Dir.pwd,  Dir.open(Dir.pwd).path)

    # calling after close gives error
    d = Dir.open("..")
    assert_equal("..", d.path)
    d.close
    assert_raise(IOError) do
      d.path
    end
  end

  def test_pos
    generic_test_seek_and_tell(:pos=, :pos)
  end

  def test_pos=
    generic_test_seek_and_tell(:pos=, :pos)
  end

  def test_read
    d = Dir.new("_test")
    got = []
    entry = nil
    got << entry while entry = d.read
    assert_bag_equal(@files, got)
    d.close
  end

  def test_rewind
    d = Dir.new("_test")
    entry = nil
    got = []
    got << entry while entry = d.read
    assert_bag_equal(@files, got)
    d.rewind
    got = []
    got << entry while entry = d.read
    assert_bag_equal(@files, got)
    d.close
  end

  def test_seek
    generic_test_seek_and_tell(:seek, :tell)
  end

  def test_tell
    generic_test_seek_and_tell(:seek, :tell)
  end

  # Dir#seek & Dir#tell are so closely connected, so it makes sense to test
  # them together. And Dir#pos= and Dir#pos are *almost* aliases for
  # seek/tell, so we test them here too.
  #
  def generic_test_seek_and_tell(seek_method, tell_method)
    d = Dir.new("_test")
    i1    = d.send(tell_method)
    name1 = d.read

    i2    = d.send(tell_method)
    name2 = d.read

    i3    = d.send(tell_method)
    name3 = d.read

    assert_instance_of(Fixnum, i1)
    assert_instance_of(Fixnum, i2)
    assert_instance_of(Fixnum, i3)
    assert_not_equal(i1, i2)
    assert_not_equal(i1, i3)
    assert_not_equal(i3, i2)
    assert_not_equal(name1, name2)
    assert_not_equal(name1, name3)
    assert_not_equal(name3, name2)

    # go back to second entry
    res = d.send(seek_method, i2)

    case seek_method
    when :seek
      assert_equal(d, res)
    when :pos=
        assert_equal(i2, res)
    else
      raise "Internal rubicon error: unknown seek method: #{seek_method}"
    end

    i2b    = d.send(tell_method)
    name2b = d.read

    i3b    = d.send(tell_method)
    name3b = d.read

    # we should have re-read two entries
    assert_equal(name2, name2b)
    assert_equal(name3, name3b)

    # Note that the following assertions can NOT be made.  The
    # telldir(3) and seekdir(3) does not guarantee a 1-1 mapping
    # between "logical position" in the directory and a given
    # "position number". After alternating calls to telldir(3) and
    # seekdir(3), several "position numbers" may refer to the same
    # "logical position" in the directory. This behaviour has been
    # observed on FreeBSD.
    #
    #    not valid:     assert_equal(i2, i2b)
    #    not valid:     assert_equal(i3, i3b)
    #

    d.close
  end

  def test_00_improper_close
    teardownTestDir
    Dir.mkdir("_test")
    d = Dir.new("_test")
    Dir.rmdir("_test")
    begin
      Dir.mkdir("_test")
    rescue
      raise Test::Unit::AssertionFailedError
    ensure
      d.close
    end
  end
  
end
