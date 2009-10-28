# -*- coding: utf-8 -*-
require 'test/unit'
require 'rbconfig'
require 'fileutils'
require 'tempfile'
require 'pathname'

class TestFile < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

  def setup
    @teardown_blocks = []
  end

  def teardown
    @teardown_blocks.each do |b|
      b.call
    end
  end

  def jruby_specific_test
    flunk("JRuby specific test") unless defined?(JRUBY_VERSION)
  end
  
  def test_file_separator_constants_defined
    assert(File::SEPARATOR)
    assert(File::PATH_SEPARATOR)
  end

  def test_basename
    assert_equal("", File.basename(""))
    assert_equal("a", File.basename("a"))
    assert_equal("b", File.basename("a/b"))
    assert_equal("c", File.basename("a/b/c"))
    
    assert_equal("b", File.basename("a/b", ""))
    assert_equal("b", File.basename("a/bc", "c"))
    assert_equal("b", File.basename("a/b.c", ".c"))
    assert_equal("b", File.basename("a/b.c", ".*"))
    assert_equal(".c", File.basename("a/.c", ".*"))
    
    
    assert_equal("a", File.basename("a/"))
    assert_equal("b", File.basename("a/b/"))
    assert_equal("/", File.basename("/"))
  end

  def test_expand_path_cross_platform
    assert_equal(Dir.pwd, File.expand_path(""))
    assert_equal(Dir.pwd, File.expand_path("."))
    assert_equal(Dir.pwd, File.expand_path(".", "."))
    assert_equal(Dir.pwd, File.expand_path("", "."))
    assert_equal(Dir.pwd, File.expand_path(".", ""))
    assert_equal(Dir.pwd, File.expand_path("", ""))

    assert_equal(File.join(Dir.pwd, "x/y/z/a/b"), File.expand_path("a/b", "x/y/z"))
    assert_equal(File.join(Dir.pwd, "bin"), File.expand_path("../../bin", "tmp/x"))
    
    # JRUBY-2143
    assert_nothing_raised {
      File.expand_path("../../bin", "/tmp/x")
      File.expand_path("/bin", "/tmp/x")
      File.expand_path("../../bin", "C:/tmp/x")
      File.expand_path("../bin", "C:/tmp/x")
    }
  end

  def test_expand_path_nil
    assert_raise(TypeError) { File.expand_path(nil) }
    assert_raise(TypeError) { File.expand_path(nil, "/") }
    assert_raise(TypeError) { File.expand_path(nil, nil) }
  end

  # JRUBY-1116: these are currently broken on windows
  # what are these testing anyway?!?!
  if WINDOWS
    def test_windows_basename
      assert_equal "", File.basename("c:")
      assert_equal "\\", File.basename("c:\\")
      assert_equal "abc", File.basename("c:abc")
    end

    # JRUBY-2052: this is important for fileutils
    def test_windows_dirname
      assert_equal("C:/", File.dirname("C:/"))
      assert_equal("C:\\", File.dirname("C:\\"))
      assert_equal("C:/", File.dirname("C:///////"))
      assert_equal("C:\\", File.dirname("C:\\\\\\\\"))
      assert_equal("C:/", File.dirname("C:///////blah"))
      assert_equal("C:\\", File.dirname("C:\\\\\\\\blah"))
      assert_equal("C:.", File.dirname("C:blah"))
      assert_equal "C:/", File.dirname("C:/temp/")
      assert_equal "c:\\", File.dirname('c:\\temp')
      assert_equal "C:.", File.dirname("C:")
      assert_equal "C:/temp", File.dirname("C:/temp/foobar.txt")
    end

    def test_expand_path_windows
      assert_equal("C:/", File.expand_path("C:/"))
      assert_equal("C:/dir", File.expand_path("C:/dir"))
      assert_equal("C:/dir", File.expand_path("C:/dir/two/../"))

      assert_equal("C:/dir/two", File.expand_path("C:/dir/two/", "D:/"))
      assert_equal("C:/", File.expand_path("C:/", nil))

      # JRUBY-2161
      assert_equal("C:/", File.expand_path("C:/dir/../"))
      assert_equal("C:/", File.expand_path("C:/.."))
      assert_equal("C:/", File.expand_path("C:/../../"))
      assert_equal("C:/", File.expand_path("..", "C:/"))
      assert_equal("C:/", File.expand_path("..", "C:"))
      assert_equal("C:/", File.expand_path("C:/dir/two/../../"))
      assert_equal("C:/", File.expand_path("C:/dir/two/../../../../../"))

      # JRUBY-546
      current_drive_letter = Dir.pwd[0..2]
      assert_equal(current_drive_letter, File.expand_path(".", "/"))
      assert_equal(current_drive_letter, File.expand_path("..", "/"))
      assert_equal(current_drive_letter, File.expand_path("/", "/"))
      assert_equal(current_drive_letter, File.expand_path("../..", "/"))
      assert_equal(current_drive_letter, File.expand_path("../..", "/dir/two"))
      assert_equal(current_drive_letter + "dir",
        File.expand_path("../..", "/dir/two/three"))
      assert_equal(current_drive_letter, File.expand_path("/../..", "/"))
      assert_equal(current_drive_letter + "hello", File.expand_path("hello", "/"))
      assert_equal(current_drive_letter, File.expand_path("hello/..", "/"))
      assert_equal(current_drive_letter, File.expand_path("hello/../../..", "/"))
      assert_equal(current_drive_letter + "three/four",
        File.expand_path("/three/four", "/dir/two"))
      assert_equal(current_drive_letter + "two", File.expand_path("/two", "/one"))
      assert_equal(current_drive_letter + "three/four",
        File.expand_path("/three/four", "/dir/two"))

      assert_equal("C:/two", File.expand_path("/two", "C:/one"))
      assert_equal("C:/two", File.expand_path("/two", "C:/one/.."))
      assert_equal("C:/", File.expand_path("/two/..", "C:/one/.."))
      assert_equal("C:/", File.expand_path("/two/..", "C:/one"))
      
      assert_equal("//two", File.expand_path("//two", "/one"))
      assert_equal("//two", File.expand_path("//two", "//one"))
      assert_equal("//two", File.expand_path("//two", "///one"))
      assert_equal("//two", File.expand_path("//two", "////one"))
      assert_equal("//", File.expand_path("//", "//one"))

      # Corner cases that fail with JRuby on Windows (but pass with MRI 1.8.6)
      # 
      ### assert_equal("///two", File.expand_path("///two", "/one"))
      ### assert_equal("///two", File.expand_path("///two/..", "/one"))
      ### assert_equal("////two", File.expand_path("////two", "/one"))
      ### assert_equal("////two", File.expand_path("////two", "//one"))
      ### assert_equal("//two", File.expand_path("//two/..", "/one"))
      ### assert_equal("////two", File.expand_path("////two/..", "/one"))
      #
      ### assert_equal("//bar/foo", File.expand_path("../foo", "//bar"))
      ### assert_equal("///bar/foo", File.expand_path("../foo", "///bar"))
      ### assert_equal("//one/two", File.expand_path("/two", "//one"))
    end

    def test_pathname_windows
      assert_equal(Pathname('foo.bar.rb').expand_path.relative_path_from(Pathname(Dir.pwd)), Pathname('foo.bar.rb'))
    end
  else
    def test_expand_path
      assert_equal("/bin", File.expand_path("../../bin", "/foo/bar"))
      assert_equal("/foo/bin", File.expand_path("../bin", "/foo/bar"))
      assert_equal("//abc/def/jkl/mno", File.expand_path("//abc//def/jkl//mno"))
      assert_equal("//abc/def/jkl/mno/foo", File.expand_path("foo", "//abc//def/jkl//mno"))
      begin
        File.expand_path("~nonexistent")
        assert(false)
      rescue ArgumentError => e
        assert(true)
      rescue Exception => e
        assert(false)
      end
 
      assert_equal("/bin", File.expand_path("../../bin", "/tmp/x"))
      assert_equal("/bin", File.expand_path("../../bin", "/tmp"))
      assert_equal("/bin", File.expand_path("../../bin", "/"))
      assert_equal("/bin", File.expand_path("../../../../../../../bin", "/"))
      assert_equal(File.join(Dir.pwd, "x/y/z/a/b"), File.expand_path("a/b", "x/y/z"))
      assert_equal(File.join(Dir.pwd, "bin"), File.expand_path("../../bin", "tmp/x"))
      assert_equal("/bin", File.expand_path("./../foo/./.././../bin", "/a/b"))

      # JRUBY-2160
      assert_equal("/dir1/subdir", File.expand_path("/dir1/subdir/stuff/../"))
      assert_equal("///fedora/stuff/blah/MORE", File.expand_path("///fedora///stuff/blah/again//..////MORE/"))
      assert_equal("/", File.expand_path("/dir1/../"))
      assert_equal("/", File.expand_path("/"))
      assert_equal("/", File.expand_path("/.."))
      assert_equal("/hello", File.expand_path("/hello/world/three/../../"))

      assert_equal("/dir/two", File.expand_path("", "/dir/two"))
      assert_equal("/dir", File.expand_path("..", "/dir/two"))

      assert_equal("/file/abs", File.expand_path("/file/abs", '/abs/dir/here'))

      assert_equal("/", File.expand_path("/", nil))
      
      assert_equal("//two", File.expand_path("//two", "//one"))
      assert_equal("/", File.expand_path("/two/..", "//one"))

      # Corner cases that fail with JRuby on Linux (but pass with MRI 1.8.6)
      #
      # assert_equal("", File.expand_path("//two/..", "//one"))
      # assert_equal("", File.expand_path("///two/..", "//one"))
      # assert_equal("/blah", File.expand_path("///two/../blah", "//one"))
    end

    def test_expand_path_with_file_prefix
      jruby_specific_test
      assert_equal("file:/foo/bar", File.expand_path("file:/foo/bar"))
      assert_equal("file:/bar", File.expand_path("file:/foo/../bar"))
    end

    def test_expand_path_corner_case
      # this would fail on MRI 1.8.6 (MRI returns "/foo").
      assert_equal("//foo", File.expand_path("../foo", "//bar"))
    end
  end # if windows

  def test_dirname
    assert_equal(".", File.dirname(""))
    assert_equal(".", File.dirname("."))
    assert_equal(".", File.dirname(".."))
    assert_equal(".", File.dirname("a"))
    assert_equal(".", File.dirname("./a"))
    assert_equal("./a", File.dirname("./a/b"))
    assert_equal("/", File.dirname("/"))
    assert_equal("/", File.dirname("/a"))
    assert_equal("/a", File.dirname("/a/b"))
    assert_equal("/a", File.dirname("/a/b/"))
    assert_equal("/", File.dirname("/"))
  end

  def test_extname
    assert_equal("", File.extname(""))
    assert_equal("", File.extname("abc"))
    assert_equal(".foo", File.extname("abc.foo"))
    assert_equal(".foo", File.extname("abc.bar.foo"))
    assert_equal("", File.extname("abc.bar/foo"))

    assert_equal("", File.extname(".bashrc"))
    assert_equal("", File.extname("."))
    assert_equal("", File.extname("/."))
    assert_equal("", File.extname(".."))
    assert_equal("", File.extname(".foo."))
    assert_equal("", File.extname("foo."))
  end

  def test_fnmatch
    assert_equal(true, File.fnmatch('cat', 'cat'))
    assert_equal(false, File.fnmatch('cat', 'category'))
    assert_equal(false, File.fnmatch('c{at,ub}s', 'cats'))
    assert_equal(false, File.fnmatch('c{at,ub}s', 'cubs'))
    assert_equal(false, File.fnmatch('c{at,ub}s', 'cat'))

    assert_equal(true, File.fnmatch('c?t', 'cat'))
    assert_equal(false, File.fnmatch('c\?t', 'cat'))
    assert_equal(true, File.fnmatch('c\?t', 'c?t'))
    assert_equal(false, File.fnmatch('c??t', 'cat'))
    assert_equal(true, File.fnmatch('c*', 'cats'));
    assert_equal(true, File.fnmatch('c*t', 'cat'))
    #assert_equal(true, File.fnmatch('c\at', 'cat')) # Doesn't work correctly on both Unix and Win32
    assert_equal(false, File.fnmatch('c\at', 'cat', File::FNM_NOESCAPE))
    assert_equal(true, File.fnmatch('a?b', 'a/b'))
    assert_equal(false, File.fnmatch('a?b', 'a/b', File::FNM_PATHNAME))
    assert_equal(false, File.fnmatch('a?b', 'a/b', File::FNM_PATHNAME))

    assert_equal(false, File.fnmatch('*', '.profile'))
    assert_equal(true, File.fnmatch('*', '.profile', File::FNM_DOTMATCH))
    assert_equal(true, File.fnmatch('*', 'dave/.profile'))
    assert_equal(true, File.fnmatch('*', 'dave/.profile', File::FNM_DOTMATCH))
    assert_equal(false, File.fnmatch('*', 'dave/.profile', File::FNM_PATHNAME))

    assert_equal(false, File.fnmatch("/.ht*",""))
    assert_equal(false, File.fnmatch("/*~",""))
    assert_equal(false, File.fnmatch("/.ht*","/"))
    assert_equal(false, File.fnmatch("/*~","/"))
    assert_equal(false, File.fnmatch("/.ht*",""))
    assert_equal(false, File.fnmatch("/*~",""))
    assert_equal(false, File.fnmatch("/.ht*","/stylesheets"))
    assert_equal(false, File.fnmatch("/*~","/stylesheets"))
    assert_equal(false, File.fnmatch("/.ht*",""))
    assert_equal(false, File.fnmatch("/*~",""))
    assert_equal(false, File.fnmatch("/.ht*","/favicon.ico"))
    assert_equal(false, File.fnmatch("/*~","/favicon.ico"))
    
    # JRUBY-1986, make sure that fnmatch is sharing aware
    assert_equal(true, File.fnmatch("foobar"[/foo(.*)/, 1], "bar"))
  end

  # JRUBY-2196
  def test_fnmatch_double_star
    assert(File.fnmatch('**/foo', 'a/b/c/foo', File::FNM_PATHNAME))
    assert(File.fnmatch('**/foo', '/foo', File::FNM_PATHNAME))
    assert(!File.fnmatch('**/foo', 'a/.b/c/foo', File::FNM_PATHNAME))
    assert(File.fnmatch('**/foo', 'a/.b/c/foo', File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(File.fnmatch('**/foo', '/root/foo', File::FNM_PATHNAME))
    assert(File.fnmatch('**/foo', 'c:/root/foo', File::FNM_PATHNAME))
    assert(File.fnmatch("lib/**/*.rb", "lib/a.rb", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(File.fnmatch("lib/**/*.rb", "lib/a/b.rb", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(File.fnmatch('**/b/**/*', 'c/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('c/**/b/**/*', 'c/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('c**/**/b/**/*', 'c/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('h**o/**/b/**/*', 'hello/a/b/c/t', File::FNM_PATHNAME))
    assert(!File.fnmatch('h**o/**/b/**', 'hello/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('**', 'hello', File::FNM_PATHNAME))
    assert(!File.fnmatch('**/', 'hello', File::FNM_PATHNAME))
    assert(File.fnmatch('**/*', 'hello', File::FNM_PATHNAME))
    assert(File.fnmatch("**/*", "one/two/three/four", File::FNM_PATHNAME))
    assert(!File.fnmatch("**", "one/two/three", File::FNM_PATHNAME))
    assert(!File.fnmatch("**/three", ".one/two/three", File::FNM_PATHNAME))
    assert(File.fnmatch("**/three", ".one/two/three", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(!File.fnmatch("*/**", "one/two/three", File::FNM_PATHNAME))
    assert(!File.fnmatch("*/**/", "one/two/three", File::FNM_PATHNAME))
    assert(File.fnmatch("*/**/*", "one/two/three", File::FNM_PATHNAME))
    assert(File.fnmatch("**/*", ".one/two/three/four", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(!File.fnmatch("**/.one/*", ".one/.two/.three/.four", File::FNM_PATHNAME | File::FNM_DOTMATCH))
  end

  # JRUBY-2199
  def test_fnmatch_bracket_pattern
    assert(!File.fnmatch('[a-z]', 'D'))
    assert(File.fnmatch('[a-z]', 'D', File::FNM_CASEFOLD))
    assert(!File.fnmatch('[a-zA-Y]', 'Z'))
    assert(File.fnmatch('[^a-zA-Y]', 'Z'))
    assert(File.fnmatch('[a-zA-Y]', 'Z', File::FNM_CASEFOLD))
    assert(File.fnmatch('[a-zA-Z]', 'Z'))
    assert(File.fnmatch('[a-zA-Z]', 'A'))
    assert(File.fnmatch('[a-zA-Z]', 'a'))
    assert(!File.fnmatch('[b-zA-Z]', 'a'))
    assert(File.fnmatch('[^b-zA-Z]', 'a'))
    assert(File.fnmatch('[b-zA-Z]', 'a', File::FNM_CASEFOLD))
  end

  def test_join
    [
      ["a", "b", "c", "d"],
      ["a"],
      [],
      ["a", "b", "..", "c"]
    ].each do |a|
      assert_equal(a.join(File::SEPARATOR), File.join(*a))
    end

    assert_equal("////heh////bar/heh", File.join("////heh////bar", "heh"))
    assert_equal("/heh/bar/heh", File.join("/heh/bar/", "heh"))
    assert_equal("/heh/bar/heh", File.join("/heh/bar/", "/heh"))
    assert_equal("/heh//bar/heh", File.join("/heh//bar/", "/heh"))
    assert_equal("/heh/bar/heh", File.join("/heh/", "/bar/", "/heh"))
    assert_equal("/HEH/BAR/FOO/HOH", File.join("/HEH", ["/BAR", "FOO"], "HOH"))
    assert_equal("/heh/bar", File.join("/heh//", "/bar"))
    assert_equal("/heh///bar", File.join("/heh", "///bar"))
  end

  def test_split
    assert_equal([".", ""], File.split(""))
    assert_equal([".", "."], File.split("."))
    assert_equal([".", ".."], File.split(".."))
    assert_equal(["/", "/"], File.split("/"))
    assert_equal([".", "a"], File.split("a"))
    assert_equal([".", "a"], File.split("a/"))
    assert_equal(["a", "b"], File.split("a/b"))
    assert_equal(["a/b", "c"], File.split("a/b/c"))
    assert_equal(["/", "a"], File.split("/a"))
    assert_equal(["/", "a"], File.split("/a/"))
    assert_equal(["/a", "b"], File.split("/a/b"))
    assert_equal(["/a/b", "c"], File.split("/a/b/c"))
    #assert_equal(["//", "a"], File.split("//a"))
    assert_equal(["../a/..", "b"], File.split("../a/../b/"))
  end

  def test_io_readlines
    # IO#readlines, IO::readlines, open, close, delete, ...
    assert_raises(Errno::ENOENT) { File.open("NO_SUCH_FILE_EVER") }
    f = open("testFile_tmp", "w")
    f.write("one\ntwo\nthree\n")
    f.close

    f = open("testFile_tmp")
    assert_equal(["one", "two", "three"],
               f.readlines.collect {|l| l.strip })
    f.close

    assert_equal(["one", "two", "three"],
               IO.readlines("testFile_tmp").collect {|l| l.strip })
    assert(File.delete("testFile_tmp"))
  end

  def test_mkdir
    begin
      Dir.mkdir("dir_tmp")
      assert(File.lstat("dir_tmp").directory?)
    ensure
      Dir.rmdir("dir_tmp")
    end
  end

  def test_file_query # - file?
    assert(File.file?('test/test_file.rb'))
    assert(! File.file?('test'))
  end

  def test_file_exist_query
    assert(File.exist?('test'))
  end

  def test_file_exist_in_jar_file
    jruby_specific_test

    assert(File.exist?("file:" + File.expand_path("test/dir with spaces/test_jar.jar") + "!/abc/foo.rb"))
    assert(File.exist?("file:" + File.expand_path("test/dir with spaces/test_jar.jar") + "!/inside_jar.rb"))
    assert(!File.exist?("file:" + File.expand_path("test/dir with spaces/test_jar.jar") + "!/inside_jar2.rb"))
    assert(!File.exist?("file:" + File.expand_path("test/dir with spaces/test_jar.jar") + "!/"))
  end

  def test_file_read_in_jar_file
    jruby_specific_test

    assert_equal("foobarx\n", File.read("file:" + File.expand_path("test/test_jar2.jar") + "!/test_value.rb"))

    assert_raises(Errno::ENOENT) do 
      File.read("file:" + File.expand_path("test/test_jar2.jar") + "!/inside_jar2.rb")
    end

    assert_raises(Errno::ENOENT) do 
      File.read("file:" + File.expand_path("test/test_jar3.jar") + "!/inside_jar2.rb")
    end

    val = ""
    open("file:" + File.expand_path("test/test_jar2.jar") + "!/test_value.rb") do |f|
      val = f.read
    end
    assert_equal "foobarx\n", val

    values = ""
    File.open("file:" + File.expand_path("test/test_jar2.jar") + "!/test_value.rb") do |f|
      f.each do |s|
        values << s
      end
    end

    assert_equal "foobarx\n", values
  end
  
  # JRUBY-2357
  def test_truncate_file_in_jar_file
    jruby_specific_test
    File.open("file:" + File.expand_path("test/test_jar2.jar") + "!/test_value.rb", "r+") do |f|
      assert_raise(Errno::EINVAL) { f.truncate(2) }
    end
  end

  # JRUBY-1886
  def test_file_truncated_after_changing_directory
    subdir = "./testDir_1"
    Dir.mkdir(subdir)
    Dir.chdir(subdir) { |dir|
      begin
        file = File.open("__dummy_file.txt", "wb") { |file|
          file.write("dummy text")
          file
        }
        assert_nothing_raised { File.truncate(file.path, 0) }
      ensure
        File.unlink(file.path)
      end
    }
  ensure
    Dir.rmdir(subdir)
  end

  def test_file_size_query
    assert(File.size?('build.xml'))
  end

  # JRUBY-2275
  def test_file_size_empty_file
    filename = "__empty_test__file"
    File.open(filename, "w+") { }
    assert_equal(nil, File.size?(filename))
    assert_equal(nil, FileTest.size?(filename))
    assert_equal(0, File.size(filename))
    assert_equal(0, FileTest.size(filename))
  ensure
    File.delete(filename)
  end

  # JRUBY-2524
  def test_filetest_exists_uri_prefixes
    assert(!FileTest.exists?("file:/"))
    assert(!FileTest.exists?("file:/!"))
  end
  
  # JRUBY-2524
  def test_file_stat_uri_prefixes
    assert_raise(Errno::ENOENT) do
      File.lstat("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.lstat("file:!")
    end
    
    assert_raise(Errno::ENOENT) do
      File.stat("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.stat("file:!")
    end
  end
  
  # JRUBY-2524
  def test_file_time_uri_prefixes
    assert_raise(Errno::ENOENT) do
      File.atime("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.atime("file:!")
    end
    
    assert_raise(Errno::ENOENT) do
      File.ctime("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.ctime("file:!") 
    end    
  end
  
  def test_file_open_utime
    filename = "__test__file"
    File.open(filename, "w") {|f| }
    time = Time.now - 3600
    File.utime(time, time, filename)
    # File mtime resolution may not be sub-second on all platforms (e.g., windows)
    # allow for some slop
    
    # FIXME, doesn't work on Windows: JRUBY-4047
    unless WINDOWS
      assert((time.to_i - File.atime(filename).to_i).abs < 5)
    end
    assert((time.to_i - File.mtime(filename).to_i).abs < 5)
    File.unlink(filename)
  end
  
  def test_file_utime_nil
    filename = '__test__file'
    File.open(filename, 'w') {|f| }
    time = File.mtime(filename)
    sleep 2
    File.utime(nil, nil, filename)
    # FIXME, doesn't work on Windows: JRUBY-4047
    unless WINDOWS
      assert((File.atime(filename).to_i - time.to_i) >= 2)
    end
    assert((File.mtime(filename).to_i - time.to_i) >= 2)
    File.unlink(filename)
  end
  
  def test_file_utime_bad_time_raises_typeerror
    args = [ [], {}, '4000' ]
    filename = '__test__file'
    File.open(filename, 'w') {|f| }
    args.each do |arg|
      assert_raises(TypeError) {  File.utime(arg, nil, filename) }
      assert_raises(TypeError) {  File.utime(nil, arg, filename) }
      assert_raises(TypeError) {  File.utime(arg, arg, filename) }
    end
    time = Time.now
    assert_raises(TypeError) {  File.utime(time, nil, filename) }
    assert_raises(TypeError) {  File.utime(nil, time, filename) }
    File.unlink(filename)
  end
  
  # JRUBY-1982 and JRUBY-1983
  def test_file_mtime_after_fileutils_touch
    filename = '__test__file'
    File.open(filename, 'w') {|f| }
    time = File.mtime(filename)
    
    FileUtils.touch(filename)
    # File mtime resolution may not be sub-second on all platforms (e.g., windows)
    # allow for some slop
    assert((time.to_i - File.mtime(filename).to_i).abs < 2)
  end

  def test_file_stat # File::Stat tests
    stat = File.stat('test');
    stat2 = File.stat('build.xml');
    assert(stat.directory?)
    assert(stat2.file?)
    assert_equal("directory", stat.ftype)
    assert_equal("file", stat2.ftype)
    assert(stat2.readable?)
  end

  if (WINDOWS)
    # JRUBY-2351
    def test_not_implemented_methods_on_windows
      # the goal here is to make sure that those "weird"
      # POSIX methods don't break JRuby, since there were
      # numerous regressions in this area
      begin 
        # TODO: See JRUBY-2818.
        File.readlink('build.xml')
      rescue NotImplementedError
      rescue Errno::EINVAL  # TODO: this exception is wrong (see bug above)
      end

      begin
        # TODO: See JRUBY-2817.
        File.chown(100, 100, 'build.xml')
      rescue NotImplementedError
      end

      assert_raise(NotImplementedError) { File.lchown(100, 100, 'build.xml') }
      assert_raise(NotImplementedError) { File.lchmod(0644, 'build.xml') }
    end
  end

  unless(WINDOWS) # no symlinks on Windows
    def test_file_symlink
      # Test File.symlink? if possible
      system("ln -s build.xml build.xml.link")
      if File.exist? "build.xml.link"
        assert(File.symlink?("build.xml.link"))
        # JRUBY-683 -  Test that symlinks don't effect Dir and File.expand_path
        assert_equal(['build.xml.link'], Dir['build.xml.link'])
        assert_equal(File.expand_path('.') + '/build.xml.link', File.expand_path('build.xml.link'))
        File.delete("build.xml.link")
      end
    end
  end

  def test_file_times
    # Note: atime, mtime, ctime are all implemented using modification time
    assert_nothing_raised {
      File.mtime("build.xml")
      File.atime("build.xml")
      File.ctime("build.xml")
      File.new("build.xml").mtime
      File.new("build.xml").atime
      File.new("build.xml").ctime
    }

    assert_raises(Errno::ENOENT) { File.mtime("NO_SUCH_FILE_EVER") }
  end

  def test_file_times_types
    # Note: atime, mtime, ctime are all implemented using modification time
    assert_instance_of Time, File.mtime("build.xml")
    assert_instance_of Time, File.atime("build.xml")
    assert_instance_of Time, File.ctime("build.xml")
    assert_instance_of Time, File.new("build.xml").mtime
    assert_instance_of Time, File.new("build.xml").atime
    assert_instance_of Time, File.new("build.xml").ctime
  end

  def test_more_constants
    jruby_specific_test
    
    # FIXME: Not sure how I feel about pulling in Java here
    if Java::java.lang.System.get_property("file.separator") == '/'
      assert_equal(nil, File::ALT_SEPARATOR)
    else
      assert_equal("\\", File::ALT_SEPARATOR)
    end
  end

  # JRUBY-2572
  def test_fnm_syscase_constant
    if (WINDOWS)
      assert_equal(File::FNM_CASEFOLD, File::FNM_SYSCASE)
      assert File.fnmatch?('cat', 'CAT', File::FNM_SYSCASE)
    else
      assert_not_equal(File::FNM_CASEFOLD, File::FNM_SYSCASE)
      assert !File.fnmatch?('cat', 'CAT', File::FNM_SYSCASE)
    end
  end

  def test_truncate
    # JRUBY-1025: negative int passed to truncate should raise EINVAL
    filename = "__truncate_test_file"
    assert_raises(Errno::EINVAL) {
      begin
        f = File.open(filename, 'w')
        f.truncate(-1)
      ensure
        f.close
      end
    }
    assert_raises(Errno::EINVAL) {
      File.truncate(filename, -1)
    }
  ensure
    File.delete(filename)
  end

  def test_file_utf8
    # name contains a German "umlaut", maybe this should be encoded as Unicode integer (\u00FC) somehow
    filename = 'jrüby'  
    f = File.new(filename, File::CREAT)
    begin
      assert_equal(nil, f.read(1))
      
      assert File.file?(filename)
      assert File.exist?(filename)
    ensure
      f.close
      File.delete(filename)
      
      assert !File.file?(filename)
      assert !File.exist?(filename)
    end
  end
  
  def test_file_create
    filename = '2nnever'
    f = File.new(filename, File::CREAT)
    begin
      assert_equal(nil, f.read(1))
    ensure
      f.close
      File.delete(filename)
    end

    f = File.new(filename, File::CREAT)
    begin
      assert_raises(IOError) { f << 'b' }
    ensure
      f.close
      File.delete(filename)
    end
  end

  # http://jira.codehaus.org/browse/JRUBY-1023
  def test_file_reuse_fileno
    fh = File.new(STDIN.fileno, 'r')
    assert_equal(STDIN.fileno, fh.fileno)
  end

  # http://jira.codehaus.org/browse/JRUBY-1231
  def test_file_directory_empty_name
    assert !File.directory?("")
    assert !FileTest.directory?("")
    assert_raises(Errno::ENOENT) { File::Stat.new("") }
  end
  
  def test_file_test
    assert(FileTest.file?('test/test_file.rb'))
    assert(! FileTest.file?('test'))
  end

  def test_flock
    filename = '__lock_test__'
    file = File.open(filename,'w')
    file.flock(File::LOCK_EX | File::LOCK_NB)
    assert_equal(0, file.flock(File::LOCK_UN | File::LOCK_NB))
    file.close
    File.delete(filename)
  end

  # JRUBY-1214
  def test_flock_exclusive_on_readonly
    filename = '__lock_test_2_'
    File.open(filename, "w+") { }
    File.open(filename, "r") do |file|
      begin
        assert_nothing_raised {
          file.flock(File::LOCK_EX)
        }
      ensure
        file.flock(File::LOCK_UN)
      end
    end
    File.delete(filename)
  end

  def test_truncate_doesnt_create_file
    name = "___foo_bar___"
    assert(!File.exists?(name))

    assert_raises(Errno::ENOENT) { File.truncate(name, 100) }

    assert(!File.exists?(name))
  end

  # JRUBY-2340
  def test_opening_readonly_file_for_write_raises_eacces
    filename = "__read_only__"

    begin
      File.open(filename, "w+", 0444) { }
      assert_raise(Errno::EACCES) { File.open(filename, "w") { } }
    ensure
      File.delete(filename)
    end
  end
  
  # JRUBY-2397
  unless(WINDOWS)
    def test_chown_accepts_nil_and_minus_one
      # chown
      assert_equal(1, File.chown(-1, -1, 'build.xml'))
      assert_equal(1, File.chown(nil, nil, 'build.xml'))
      # lchown
      assert_equal(1, File.lchown(-1, -1, 'build.xml'))
      assert_equal(1, File.lchown(nil, nil, 'build.xml'))

      File.open('build.xml') { |file|
        # chown
        assert_equal(0, file.chown(-1, -1))
	assert_equal(0, file.chown(nil, nil))
        # lchown
	# NOTE: hmm, it seems that MRI
	# doesn't have File#lchown method at all!
        assert_equal(0, file.lchown(-1, -1))
        assert_equal(0, file.lchown(nil, nil))
      }
    end
  end

  # JRUBY-2491
  def test_umask_noarg_does_not_zero
    mask = 0200
    orig_mask = File.umask(mask)

    arch = java.lang.System.getProperty('sun.arch.data.model')
    if (WINDOWS && arch == '64')
      # TODO: We have a bug on Windows with x64 JVM
      # See JRUBY-2819
      return
    end

    assert_equal(mask, File.umask)
    # Subsequent calls should still return the same umask, not zero
    assert_equal(mask, File.umask)
  ensure
    File.umask(orig_mask)
  end

  def test_allow_override_of_make_tmpname
    # mimics behavior of attachment_fu, which overrides private #make_tmpname
    Tempfile.class_eval do
      alias_method :save_make_tmpname, :make_tmpname
      def make_tmpname(basename, n)
        ext = nil
        sprintf("%s%d-%d%s", basename.to_s.gsub(/\.\w+$/) { |s| ext = s; '' }, $$, n, ext)
      end
    end

    @teardown_blocks << proc do
      Tempfile.class_eval { alias_method :make_tmpname, :save_make_tmpname }
    end

    begin
      t = Tempfile.new "tcttac.jpg", File.dirname(__FILE__)
      assert t.path =~ /\.jpg$/
    ensure
      t.close
    end
  end

  unless WINDOWS
    def test_mode_of_tempfile_is_600
      t = Tempfile.new "tcttac.jpg"
      assert_equal 0100600, File.stat(t.path).mode
    end
  end

  # See JRUBY-2694; we don't have 1.8.7 support yet
  def test_tempfile_with_suffix
    Tempfile.open(['prefix', 'suffix']) { |f|
      assert_match(/^prefix/, File.basename(f.path))
      assert_match(/suffix$/, f.path)
    }
  end

  def test_file_size
    size = File.size('build.xml')
    assert(size > 0)
    assert_equal(size, File.size(File.new('build.xml')))
  end

  # JRUBY-4073
  def test_file_sizes
    filename = '100_bytes.bin'
    begin
      File.open(filename, 'wb+') { |f|
        f.write('0' * 100)
      }
      assert_equal(100, File.size(filename))
      assert_equal(100, File.stat(filename).size)
      assert_equal(100, File.stat(filename).size?)
      assert_match(/\ssize=100,/, File.stat(filename).inspect)
      assert_equal(100, File::Stat.new(filename).size)
      assert_equal(100, File::Stat.new(filename).size?)
      assert_match(/\ssize=100,/, File::Stat.new(filename).inspect)
    ensure
      File.unlink(filename)
    end
  end

  # JRUBY-4149
  def test_open_file_sizes
    filename = '100_bytes.bin'
    begin
      File.open(filename, 'wb+') { |f|
        f.write('0' * 100)
        f.flush; f.flush

        assert_equal(100, f.stat.size)
        assert_equal(100, f.stat.size?)
        assert_match(/\ssize=100,/, f.stat.inspect)

        assert_equal(100, File.size(filename))
        assert_equal(100, File.stat(filename).size)
        assert_equal(100, File.stat(filename).size?)
        assert_match(/\ssize=100,/, File.stat(filename).inspect)

        assert_equal(100, File::Stat.new(filename).size)
        assert_equal(100, File::Stat.new(filename).size?)
        assert_match(/\ssize=100,/, File::Stat.new(filename).inspect)
      }
    ensure
      File.unlink(filename)
    end
  end

  # JRUBY-3634: File.read or File.open with a url to a file resource fails with StringIndexOutOfBounds exception
  def test_file_url
    path = File.expand_path(__FILE__)
    expect = File.read(__FILE__)[0..100]
    got = File.read("file:///#{path}")[0..100]

    assert_equal(expect, got)
  end

  def test_basename_unicode
    filename = 'dir/a ⼈〉〃〄⨶↖.pdf'
    assert_equal("a \342\274\210\343\200\211\343\200\203\343\200\204\342\250\266\342\206\226.pdf", File.basename(filename))
  end
end
