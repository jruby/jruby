require 'test/unit'
require 'rbconfig'

class TestDir < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

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
    Dir.glob('./**/testDir_tmp1').each {|f| assert File.exist?(f) }
  end

  def test_glob_with_blocks
    Dir.mkdir("testDir_3")
    open("testDir_3/testDir_tmp1", "w").close
    vals = []
    glob_val = Dir.glob('**/*tmp1'){|f| vals << f}
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
    java_test_classes = File.expand_path(File.dirname(__FILE__) + '/../build/classes/test')
    java_test_classes = File.expand_path(File.dirname(__FILE__) + '/..') unless File.exist?(java_test_classes)
    Dir.mkdir("testDir_4")
    Dir.chdir("testDir_4") do
      pwd = `ruby -e "puts Dir.pwd"`
      pwd.gsub! '\\', '/'
      assert_equal("testDir_4", pwd.split("/")[-1].strip)

      pwd = `jruby -e "puts Dir.pwd"`
      pwd.gsub! '\\', '/'
      assert_equal("testDir_4", pwd.split("/")[-1].strip)

      pwd = `java -cp "#{java_test_classes}" org.jruby.util.Pwd`
      pwd.gsub! '\\', '/'
      assert_equal("testDir_4", pwd.split("/")[-1].strip)
    end
    Dir.chdir("testDir_4")
    pwd = `java -cp "#{java_test_classes}" org.jruby.util.Pwd`
    pwd.gsub! '\\', '/'
    assert_equal("testDir_4", pwd.split("/")[-1].strip)
  end

  def test_glob_inside_jar_file
    require 'test/dir with spaces/test_jar.jar'
    require 'inside_jar'

    prefix = WINDOWS ? "file:/" : "file:"
    
    first = File.expand_path(File.join(File.dirname(__FILE__), '..'))
    
    jar_file = prefix + File.join(first, "test", "dir with spaces", "test_jar.jar") + "!"

    ["#{jar_file}/abc", "#{jar_file}/inside_jar.rb", "#{jar_file}/second_jar.rb"].each do |f|
      assert $__glob_value.include?(f)
    end
    ["#{jar_file}/abc", "#{jar_file}/abc/foo.rb", "#{jar_file}/inside_jar.rb", "#{jar_file}/second_jar.rb"].each do |f|
      assert $__glob_value2.include?(f)
    end
    assert_equal ["#{jar_file}/abc"], Dir["#{jar_file}/abc"]
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
      assert_raises(SystemCallError) do 
        Dir.mkdir("testDir_5/another_dir")
      end
    end
  end
end
