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

  if WINDOWS
    def test_drive_letter_dirname_leaves_trailing_slash
      assert_equal "C:/", File.dirname('C:/Temp')
      assert_equal "c:\\", File.dirname('c:\temp')
    end

    def test_pathname_realpath_works_with_drive_letters
      require 'pathname'
      assert_nothing_raised do
        Pathname.new('C:\windows').realpath.to_s
        Pathname.new('C:\windows\..\windows').realpath.to_s
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