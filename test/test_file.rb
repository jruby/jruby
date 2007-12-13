require 'test/unit'
require 'rbconfig'

class TestFile < Test::Unit::TestCase
  
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

  # JRUBY-1116: these are currently broken on windows
  # what are these testing anyway?!?!
  if Config::CONFIG['target_os'] =~ /Windows|mswin/
    def test_windows_basename
      assert_equal "", File.basename("c:")
      assert_equal "\\", File.basename("c:\\")
      assert_equal "abc", File.basename("c:abc")
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
      assert_equal("//foo", File.expand_path("../foo", "//bar"))
      assert_equal("/bin", File.expand_path("../../../../../../../bin", "/"))
      assert_equal(File.join(Dir.pwd, "x/y/z/a/b"), File.expand_path("a/b", "x/y/z"))
      assert_equal(File.join(Dir.pwd, "bin"), File.expand_path("../../bin", "tmp/x"))
      assert_equal("/bin", File.expand_path("./../foo/./.././../bin", "/a/b"))
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

  def test_file_size_query
    assert(File.size?('build.xml'))
  end

  def test_file_open_utime
    filename = "__test__file"
    File.open(filename, "w") {|f| }
    time = Time.now - 3600
    File.utime(time, time, filename)
    # File mtime resolution may not be sub-second on all platforms (e.g., windows)
    # allow for some slop
    assert((time.to_i - File.mtime(filename).to_i).abs < 5)
    File.unlink(filename)
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
    # FIXME: Not sure how I feel about pulling in Java here
    if Java::java.lang.System.get_property("file.separator") == '/'
      assert_equal(nil, File::ALT_SEPARATOR)
    else
      assert_equal("\\", File::ALT_SEPARATOR)
    end

    assert_equal(File::FNM_CASEFOLD, File::FNM_SYSCASE)
  end

  def test_truncate
    # JRUBY-1025: negative int passed to truncate should raise EINVAL
    filename = "__truncate_test_file"
    assert_raises(Errno::EINVAL) {
      File.open(filename, 'w').truncate(-1)
    }
    assert_raises(Errno::EINVAL) {
      File.truncate(filename, -1)
    }
    File.delete(filename)
  end

  def test_file_create
    filename = '2nnever'
    assert_equal(nil, File.new(filename, File::CREAT).read(1))
    File.delete(filename)

    assert_raises(IOError) { File.new(filename, File::CREAT) << 'b' }
    File.delete(filename)
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
  
  def test_truncate_doesnt_create_file
    name = "___foo_bar___"
    assert(!File.exists?(name))

    assert_raises(Errno::ENOENT) { File.truncate(name, 100) }

    assert(!File.exists?(name))
  end
end

