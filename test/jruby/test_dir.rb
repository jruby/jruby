# coding: utf-8
require 'test/unit'
require 'test/jruby/test_helper'
require 'rbconfig'

class TestDir < Test::Unit::TestCase
  include TestHelper
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

  def setup
    @save_dir = Dir.pwd
    1.upto(5) do |i|
      Dir["testDir_#{i}/*"].each do |f|
        File.unlink f rescue nil
      end
      Dir.delete("testDir_#{i}") rescue nil
    end
  end

  def teardown
    Dir.chdir(@save_dir)
    setup
  end

  # JRUBY-2519
  def test_dir_instance_should_not_cache_dir_contents
    
    require 'fileutils'
    require 'tmpdir'
    
    testdir = File.join(Dir.tmpdir, Process.pid.to_s)
    FileUtils.mkdir_p testdir
    
    FileUtils.touch File.join(testdir, 'fileA.txt')
    dir = Dir.new(testdir)
    FileUtils.touch File.join(testdir, 'fileB.txt')
    dir.rewind # does nothing

    assert_equal 'fileA.txt', dir.find {|item| item == 'fileA.txt' }
    assert_equal 'fileB.txt', dir.find {|item| item == 'fileB.txt' }
  end  
  
  def test_pwd_and_getwd_equivalent
    assert_equal(Dir.pwd, Dir.getwd)
  end

  def test_dir_enumerable
    Dir.mkdir("./testDir_1")

    d = Dir.new("./testDir_1")
    assert(d.kind_of?(Enumerable))
    assert_equal(['.', '..'], d.entries)
  end

  def test_dir_entries
    Dir.mkdir("./testDir_1")
    (1..2).each {|i|
      File.open("./testDir_1/file" + i.to_s, "w") {|f|
        f.write("hello")
      }
    }

    assert_equal(['.', '..', "file1", "file2"], Dir.entries('./testDir_1').sort)
    assert_equal(['.', '..', "file1", "file2"], Dir.new('./testDir_1').entries.sort)
    Dir.chdir("./testDir_1")
    assert_equal(['.', '..', "file1", "file2"], Dir.entries('.').sort)
    Dir.chdir("..")

    files = []
    Dir.foreach('./testDir_1') {|f| files << f }
    assert_equal(['.', '..', "file1", "file2"], files.sort)
  end

  def test_bogus_glob
    # Test unescaped special char that is meant to be used with another 
    # (i.e. bogus glob pattern)
    assert_equal([], Dir.glob("{"))
  end

  def test_glob_empty_string
    assert_equal([], Dir.glob(''))
    assert_equal([], Dir[''])
  end

  def test_glob_double_star
    # Test that glob expansion of ** works ok with non-patterns as path 
    # elements. This used to throw NPE.
    Dir.mkdir("testDir_2")
    open("testDir_2/testDir_tmp1", "w").close
    Dir.glob('./testDir_2/**/testDir_tmp1').each {|f| assert File.exist?(f) }
  end

  def test_glob_with_blocks
    Dir.mkdir("testDir_3")
    open("testDir_3/testDir_tmp1", "w").close
    vals = []
    glob_val = Dir.glob('./testDir_3/**/*tmp1'){|f| vals << f}
    assert_equal(true, glob_val.nil?)
    assert_equal(1, vals.size)
    assert_equal(true, File.exists?(vals[0])) unless vals.empty?
  end

  def test_dir_dot_does_not_throw_exception
    # just makes sure this doesn't throw a Java exception
    Dir['.']
  end

  # JRUBY-2717
  def test_more_than_two_arguments_to_aref_does_not_throw_exception
    Dir['.','.','.','.']
  end

  def test_glob_on_shared_string
    Dir["blahtest/test_argf.rb"[4..-1]]
  end

  # http://jira.codehaus.org/browse/JRUBY-300
  def test_chdir_and_pwd
    java_test_classes = File.expand_path(File.dirname(__FILE__) + '/../target/test-classes')
    java_test_classes = java_test_classes + ":" + File.expand_path(File.dirname(__FILE__) + '/../core/target/test-classes')
    Dir.mkdir("testDir_4")
    Dir.chdir("testDir_4") do
      pwd = `#{RUBY} -e "puts Dir.pwd"`
      pwd.gsub! '\\', '/'
      assert_equal("testDir_4", pwd.split("/")[-1].strip)

      if (ENV_JAVA['jruby.home'] and not 
          ENV_JAVA['jruby.home'].match( /!\// ) and not
          ENV_JAVA['jruby.home'].match( /:\// ))
        pwd = `#{ENV_JAVA['jruby.home']}/bin/jruby -e "puts Dir.pwd"`
        pwd.gsub! '\\', '/'
        assert_equal("testDir_4", pwd.split("/")[-1].strip)
      end

#      FIXME: does not pass in 2.0 mode
#      pwd = `java -cp "#{java_test_classes}" org.jruby.util.Pwd`
#      pwd.gsub! '\\', '/'
#      assert_equal("testDir_4", pwd.split("/")[-1].strip)
    end
#    FIXME: does not pass in 2.0 mode
#    Dir.chdir("testDir_4")
#    pwd = `java -cp "#{java_test_classes}" org.jruby.util.Pwd`
#    pwd.gsub! '\\', '/'
#    assert_equal("testDir_4", pwd.split("/")[-1].strip)
  end

  def test_glob_inside_jar_file
    jar_file = jar_file_with_spaces.sub(/.*!/, 'uri:classloader:')

    ["#{jar_file}/abc", "#{jar_file}/inside_jar.rb", "#{jar_file}/second_jar.rb"].each do |f|
      assert $__glob_value.include?(f), "#{f} not found in #{$__glob_value.inspect}"
    end
    ["#{jar_file}/abc", "#{jar_file}/abc/foo.rb", "#{jar_file}/inside_jar.rb", "#{jar_file}/second_jar.rb"].each do |f|
      assert $__glob_value2.include?(f)
    end
    assert_equal ["#{jar_file}/abc"], Dir["#{jar_file}/abc"]
  end

  # JRUBY-5155
  def test_glob_with_magic_inside_jar_file
    jar_file = jar_file_with_spaces

    aref = Dir["#{jar_file}/[a-z]*_jar.rb"]
    glob = Dir.glob("#{jar_file}/[a-z]*_jar.rb")

    [aref, glob].each do |collect|
      ["#{jar_file}/inside_jar.rb", "#{jar_file}/second_jar.rb"].each do |f|
        assert collect.include?(f)
      end
      assert !collect.include?("#{jar_file}/abc/foo.rb")
    end
  end

  def test_foreach_works_in_jar_file
    jar_file = File.expand_path('../jar_with_relative_require1.jar', __FILE__)
    jar_path = "file:#{jar_file}!/test"
    dir = Dir.new(jar_path)
    assert dir.entries.include?('require_relative1.rb'), "#{jar_path} does not contain require_relative1.rb: #{dir.entries.inspect}"
    entries = []
    dir.each {|d| entries << d}
    assert entries.include?('require_relative1.rb'), "#{jar_path} does not contain require_relative1.rb: #{entries.inspect}"
    entries = []
    Dir.foreach(jar_path) {|d| entries << d}
    assert entries.include?('require_relative1.rb'), "#{jar_path} does not contain require_relative1.rb: #{entries.inspect}"

    entries_by_enum = Dir.foreach(jar_path).to_a
    assert entries_by_enum.include?('require_relative1.rb'), "#{jar_path} does not contain require_relative1.rb: #{entries_by_enum.inspect}"

    root_jar_path = "file:#{jar_file}!"
    root_entries_by_enum = Dir.foreach(root_jar_path).to_a
    assert root_entries_by_enum.include?('test'), "#{root_jar_path} does not contain 'test' directory: #{root_entries_by_enum.inspect}"

    root_jar_path_with_slash = "file:#{jar_file}!/"
    root_entries_with_slash_by_enum = Dir.foreach(root_jar_path_with_slash).to_a
    assert root_entries_with_slash_by_enum.include?('test'), "#{root_jar_path_with_slash} does not contain 'test' directory: #{root_entries_with_slash_by_enum.inspect}"

    root_jar_path_with_jar_prefix = "jar:file:#{jar_file}!"
    root_entries_with_jar_prefix_by_enum = Dir.foreach(root_jar_path_with_jar_prefix).to_a
    assert root_entries_with_jar_prefix_by_enum.include?('test'), "#{root_jar_path_with_jar_prefix} does not contain 'test' directory: #{root_entries_with_jar_prefix_by_enum.inspect}"
  end

  def jar_file_with_spaces
    require 'test/jruby/dir with spaces/test_jar.jar'
    require 'inside_jar'

    "file:" + File.join(File.dirname(__FILE__), "dir with spaces", "test_jar.jar") + "!"
  end

  # JRUBY-4177
  # FIXME: Excluded due to JRUBY-4082
  def xxx_test_mktmpdir
    require 'tmpdir'
    assert_nothing_raised do
      Dir.mktmpdir('xx') {}
    end
  end

  # JRUBY-4983
  def test_entries_unicode
    utf8_dir = "testDir_1/glk\u00a9"
    
    Dir.mkdir("./testDir_1")
    Dir.mkdir(utf8_dir)

    assert_nothing_raised { Dir.entries(utf8_dir) }

    require 'fileutils'
    assert_nothing_raised do
      FileUtils.cp_r(utf8_dir, "./testDir_1/target")
      FileUtils.rm_r(utf8_dir)
    end
  ensure
    Dir.unlink "./testDir_1/target" rescue nil
    Dir.unlink utf8_dir rescue nil
  end

  # JRUBY-5286
  def test_pwd_tainted
    assert Dir.pwd.tainted?
  end

  if WINDOWS
    def test_chdir_slash_windows
      @orig_pwd = Dir.pwd
      def restore_cwd
        Dir.chdir(@orig_pwd)
      end
      slashes = ['/', '\\']
      slashes.each { |slash|
        current_drive_letter = Dir.pwd[0..2]
        Dir.chdir(slash)
        assert_equal(current_drive_letter, Dir.pwd, "slash - #{slash}")
        restore_cwd

        letters = ['C:/', 'D:/', 'E:/', 'F:/', 'C:\\', 'D:\\', 'E:\\']
        letters.each { |letter|
          next unless File.exists?(letter)
          Dir.chdir(letter)
          pwd = Dir.pwd
          Dir.chdir(slash)
          slash_pwd = Dir.pwd
          assert_equal(pwd, slash_pwd, "slash - #{slash}")
          restore_cwd
        }
      }
    ensure
      Dir.chdir(@orig_pwd)
    end

    def test_chdir_exceptions_windows
      orig_pwd = Dir.pwd
      assert_raise(Errno::EINVAL) {
        Dir.chdir('//') # '//' is not a valid thing on Windows 
      }
      assert_raise(Errno::ENOENT) {
        Dir.chdir('//blah-blah-blah') # doesn't exist
      }
      assert_raise(Errno::EINVAL) {
        Dir.chdir('\\\\') # '\\\\' is not a valid thing on Windows
      }
      assert_raise(Errno::ENOENT) {
        Dir.chdir('\\\\blah-blah-blah') # doesn't exist
      }
      assert_raise(Errno::ENOENT) {
        Dir.chdir('///') # doesn't exist
      }
      assert_raise(Errno::ENOENT) {
        Dir.chdir('\\\\\\') # doesn't exist
      }
    ensure
      Dir.chdir(orig_pwd)
    end
    
    def test_new_windows
      slashes = ['/', '\\']
      slashes.each { |slash|
        current_drive_letter = Dir.pwd[0..2]
        slash_dir = Dir.new(slash)

        slash_entries = []
        slash_dir.each { |file|
          slash_entries << file
        }

        drive_root_entries = Dir.entries(current_drive_letter).sort
        slash_entries.sort!
        assert_equal(drive_root_entries, slash_entries, "slash - #{slash}")
      }
    end
    
    def test_new_with_drive_letter
      current_drive_letter = Dir.pwd[0..2]

      # Check that 'C:' == 'C:/' == 'C:\\'
      assert_equal(
        Dir.new(current_drive_letter + "/").entries,
        Dir.new(current_drive_letter).entries)
      assert_equal(
        Dir.new(current_drive_letter + "\\").entries,
        Dir.new(current_drive_letter).entries)
    end
    
    def test_entries_with_drive_letter
      current_drive_letter = Dir.pwd[0..2]

      # Check that 'C:' == 'C:/' == 'C:\\'
      assert_equal(
        Dir.entries(current_drive_letter + "/"),
        Dir.entries(current_drive_letter))
      assert_equal(
        Dir.entries(current_drive_letter + "\\"),
        Dir.entries(current_drive_letter))
    end
    
    def test_open_windows
      slashes = ['/', '\\']
      slashes.each { |slash|
        current_drive_letter = Dir.pwd[0..2]
        slash_dir = Dir.open(slash)

        slash_entries = []
        slash_dir.each { |file|
          slash_entries << file
        }

        drive_root_entries = Dir.entries(current_drive_letter).sort
        slash_entries.sort!
        assert_equal(drive_root_entries, slash_entries, "slash - #{slash}")
      }
    end
    
    def test_dir_new_exceptions_windows
      assert_raise(Errno::ENOENT) {
        Dir.new('')
      }
      assert_raise(Errno::EINVAL) {
        Dir.new('//') # '//' is not a valid thing on Windows 
      }
      assert_raise(Errno::ENOENT) {
        Dir.new('//blah-blah-blah') # doesn't exist
      }
      assert_raise(Errno::EINVAL) {
        Dir.new('\\\\') # '\\\\' is not a valid thing on Windows
      }
      assert_raise(Errno::ENOENT) {
        Dir.new('\\\\blah-blah-blah') # doesn't exist
      }
      assert_raise(Errno::ENOENT) {
        Dir.new('///') # doesn't exist
      }
      assert_raise(Errno::ENOENT) {
        Dir.new('\\\\\\') # doesn't exist
      }
    end
    
    def test_entries_windows
      slashes = ['/', '\\']
      slashes.each { |slash|
        current_drive_letter = Dir.pwd[0..2]
        drive_root_entries = Dir.entries(current_drive_letter).sort
        slash_entries = Dir.entries(slash).sort
        assert_equal(drive_root_entries, slash_entries, "slash - #{slash}")
      }
    end

    def test_entries_exceptions_windows
      assert_raise(Errno::ENOENT) {
        Dir.entries('')
      }
      assert_raise(Errno::EINVAL) {
        Dir.entries('//') # '//' is not a valid thing on Windows 
      }
      assert_raise(Errno::ENOENT) {
        Dir.entries('//blah-blah-blah') # doesn't exist
      }
      assert_raise(Errno::EINVAL) {
        Dir.entries('\\\\') # '\\\\' is not a valid thing on Windows
      }
      assert_raise(Errno::ENOENT) {
        Dir.entries('\\\\blah-blah-blah') # doesn't exist
      }
      assert_raise(Errno::ENOENT) {
        Dir.entries('///') # doesn't exist
      }
      assert_raise(Errno::ENOENT) {
        Dir.entries('\\\\\\') # doesn't exist
      }
    end

    def test_glob_windows
      current_drive_letter = Dir.pwd[0..2]

      slash_entries = Dir.glob( "/*").sort.map { |e|
        # remove slash
        e[1..-1]
      }
      drive_root_entries = Dir.glob(current_drive_letter + "*").sort.map { |e|
        # remove drive letter
        e[3..-1]
      }
      assert_equal(drive_root_entries, slash_entries)
    end

    def test_path_windows
      assert_equal(Dir.new('/').path, '/')
      assert_equal(Dir.new('\\').path, '\\')

      current_drive_letter = Dir.pwd[0, 2]
      assert_equal(Dir.new(current_drive_letter).path, current_drive_letter)
      assert_equal(
        Dir.new(current_drive_letter + "/").path,
        current_drive_letter + "/")
      assert_equal(
        Dir.new(current_drive_letter + "\\").path,
        current_drive_letter + "\\")
      assert_equal(
        Dir.new(current_drive_letter + '/blah/..').path,
        current_drive_letter + '/blah/..')
    end

    def test_drive_letter_dirname_leaves_trailing_slash
      assert_equal "C:/", File.dirname('C:/Temp')
      assert_equal "c:\\", File.dirname('c:\\temp')
    end

    def test_pathname_realpath_works_with_drive_letters
      require 'pathname'
      win_dir = nil
      if FileTest.exist?('C:/windows')
        win_dir = "windows" 
      elsif FileTest.exist?('C:/winnt')
        win_dir = "winnt" 
      end
        
      if (win_dir != nil)
        Pathname.new("C:\\#{win_dir}").realpath.to_s
        Pathname.new("C:\\#{win_dir}\\..\\#{win_dir}").realpath.to_s
      end
    end
  else
    # http://jira.codehaus.org/browse/JRUBY-1375
    def test_mkdir_on_protected_directory_fails
      Dir.mkdir("testDir_5") unless File.exists?("testDir_5")
      File.chmod(0400, 'testDir_5')
      assert_raises(Errno::EACCES) do
        Dir.mkdir("testDir_5/another_dir")
      end
    end
  end
end
