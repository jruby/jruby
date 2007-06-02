require 'test/unit'
require 'stat'
require 'socket'

# Fixme: needs platform-specific stuff dealt with
class TestFile < Test::Unit::TestCase

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

    @file = File.join("_test", "_touched")

    touch("-a -t 122512341999 #@file")
    @aTime = Time.local(1999, 12, 25, 12, 34, 00)

    touch("-m -t 010112341997 #@file")
    @mTime = Time.local(1997,  1,  1, 12, 34, 00)
  end

  def teardown
    File.delete @file if File.exist?(@file)
    teardownTestDir
  end

  Windows.dont do    # FAT file systems only store mtime
    def test_s_atime
      assert_equal(@aTime, File.atime(@file))
    end
  end

  def test_s_basename
    assert_equal("_touched", File.basename(@file))
    assert_equal("tmp", File.basename(File.join("/tmp")))
    assert_equal("b",   File.basename(File.join(*%w( g f d s a b))))
    assert_equal("tmp", File.basename("/tmp", ".*"))
    assert_equal("tmp", File.basename("/tmp", ".c"))
    assert_equal("tmp", File.basename("/tmp.c", ".c"))
    assert_equal("tmp", File.basename("/tmp.c", ".*"))
    assert_equal("tmp.o", File.basename("/tmp.o", ".c"))
    Version.greater_or_equal("1.8.0") do
      assert_equal("tmp", File.basename(File.join("/tmp/")))
      assert_equal("/",   File.basename("/"))
      assert_equal("/",   File.basename("//"))
      assert_equal("base", File.basename("dir///base", ".*"))
      assert_equal("base", File.basename("dir///base", ".c"))
      assert_equal("base", File.basename("dir///base.c", ".c"))
      assert_equal("base", File.basename("dir///base.c", ".*"))
      assert_equal("base.o", File.basename("dir///base.o", ".c"))
      assert_equal("base", File.basename("dir///base///"))
      assert_equal("base", File.basename("dir//base/", ".*"))
      assert_equal("base", File.basename("dir//base/", ".c"))
      assert_equal("base", File.basename("dir//base.c/", ".c"))
      assert_equal("base", File.basename("dir//base.c/", ".*"))
      assert_equal("base.o", File.basename("dir//base.o/", ".c"))
    end
    Version.less_than("1.8.0") do
      assert_equal("", File.basename(File.join("/tmp/")))
      assert_equal("",   File.basename("/"))
    end

    Version.greater_or_equal("1.7.2") do
      unless File::ALT_SEPARATOR.nil?
        assert_equal("base", File.basename("dir" + File::ALT_SEPARATOR + "base")) 
      end
    end
  end

  def test_s_chmod
    assert_raise(Errno::ENOENT) { File.chmod(0, "_gumby") }
    assert_equal(0, File.chmod(0))
    Dir.chdir("_test")
    begin
      assert_equal(1,         File.chmod(0, "_file1"))
      assert_equal(2,         File.chmod(0, "_file1", "_file2"))

      assert_equal(_stat_map(0),     File.stat("_file1").mode & 0777)
      assert_equal(1,                File.chmod(0400, "_file1"))
      assert_equal(_stat_map(0400),  File.stat("_file1").mode & 0777)
      assert_equal(1,                File.chmod(0644, "_file1"))
      assert_equal(_stat_map(0644),  File.stat("_file1").mode & 0777)
    ensure
      Dir.chdir("..")
    end
  end

  def test_s_chown
    super_user
  end

  def test_s_ctime
    sys("touch  #@file")
    ctime = RubiconStat::ctime(@file)
    @cTime = Time.at(ctime)

    assert_equal(@cTime, File.ctime(@file))
  end

  def test_s_delete
    Dir.chdir("_test")
    assert_equal(0, File.delete)
    assert_raise(Errno::ENOENT) { File.delete("gumby") }
    assert_equal(2, File.delete("_file1", "_file2"))
  end

  def test_s_dirname
    assert_equal("/",         File.dirname(File.join("/tmp")))
    assert_equal("g/f/d/s/a", File.dirname(File.join(*%w( g f d s a b))))
    assert_equal("/",         File.dirname("/"))

    Version.greater_or_equal("1.8.0") do
      assert_equal("/",       File.dirname(File.join("/tmp/")))
    end
    Version.less_than("1.8.0") do
      assert_equal("/tmp",    File.dirname(File.join("/tmp/")))
    end

    Version.greater_or_equal("1.7.2") do
      unless File::ALT_SEPARATOR.nil? 
        assert_equal("dir", File.dirname("dir" + File::ALT_SEPARATOR + "base")) 
      end
    end
  end

  def test_s_expand_path
    if $os == MsWin32
      base = `cd`.chomp.tr '\\', '/'
      tmpdir = "c:/tmp"
      rootdir = "c:/"
    else
      base = `pwd`.chomp
      tmpdir = "/tmp"
      rootdir = "/"
    end

    assert_equal(base,                 File.expand_path(''))
    assert_equal(File.join(base, 'a'), File.expand_path('a'))
    assert_equal(File.join(base, 'a'), File.expand_path('a', nil)) # V0.1.1

    # Because of Ruby-Talk:18512
    assert_equal(File.join(base, 'a.'),    File.expand_path('a.')) 
    assert_equal(File.join(base, '.a'),    File.expand_path('.a')) 
    assert_equal(File.join(base, 'a..'),   File.expand_path('a..')) 
    assert_equal(File.join(base, '..a'),   File.expand_path('..a')) 
    assert_equal(File.join(base, 'a../b'), File.expand_path('a../b')) 

    b1 = File.join(base.split(File::SEPARATOR)[0..-2])
    assert_equal(b1, File.expand_path('..'))

    assert_equal("#{tmpdir}",   File.expand_path("", "#{tmpdir}"))
    assert_equal("#{tmpdir}/a", File.expand_path("a", "#{tmpdir}"))
    assert_equal("#{tmpdir}/a", File.expand_path("../a", "#{tmpdir}/xxx"))
    assert_equal("#{rootdir}",  File.expand_path(".", "#{rootdir}"))

    home = ENV['HOME']
    if (home)
      assert_equal(home, File.expand_path('~'))
      assert_equal(home, File.expand_path('~', '/tmp/gumby/ddd'))
      assert_equal(File.join(home, 'a'),
                         File.expand_path('~/a', '/tmp/gumby/ddd'))
    else
      skipping("$HOME not set")
    end

    begin
      File.open("/etc/passwd") do |pw|
	users = pw.readlines
	line = ''
	line = users.pop while users.nitems > 0 and (line.length == 0 || /^\+:/ =~ line)
	if line.length > 0 
	  line = line.split(':')
	  name, home = line[0], line[-2]
	  assert_equal(home, File.expand_path("~#{name}"))
	  assert_equal(home, File.expand_path("~#{name}", "/tmp/gumby"))
	  assert_equal(File.join(home, 'a'),
		       File.expand_path("~#{name}/a", "/tmp/gumby"))
	end
      end
    rescue Errno::ENOENT
      skipping("~user")
    end
  end

  def test_extname
    assert_equal(".c",    File.extname("alpha.c"))
    assert_equal("",      File.extname("beta"))
    assert_equal("",      File.extname(".gamma"))
    assert_equal(".",     File.extname("delta."))

    assert_equal(".c",    File.extname("some/dir/alpha.c"))
    assert_equal("",      File.extname("some/dir/beta"))
    assert_equal("",      File.extname("some/dir/.gamma"))
    assert_equal(".",     File.extname("some/dir/delta."))

    assert_equal(".e",    File.extname("a.b.c.d.e"))
  end

  def generic_test_s_fnmatch(method)
    assert_equal(true,  File.send(method, 'cat',       'cat'))
    assert_equal(false, File.send(method, 'cat',       'category'))
    assert_equal(false, File.send(method, 'c{at,ub}s', 'cats'))
    assert_equal(false, File.send(method, 'c{at,ub}s', 'cubs'))
    assert_equal(false, File.send(method, 'c{at,ub}s', 'cat'))

    assert_equal(true,  File.send(method, 'c?t',    'cat'))
    assert_equal(false, File.send(method, 'c\?t',   'cat'))
    assert_equal(false, File.send(method, 'c??t',   'cat'))
    assert_equal(true,  File.send(method, 'c*',     'cats'))
    assert_equal(true,  File.send(method, '*s',     'cats'))
    assert_equal(false, File.send(method, '*x*',    'cats'))

    assert_equal(false, File.send(method, 'c/**/x', 'c/a/b/c/t'))
    assert_equal(true,  File.send(method, 'c/**/t', 'c/a/b/c/t'))
    assert_equal(true,  File.send(method, 'c/*/t',  'c/a/b/c/t'))
    assert_equal(false, File.send(method, 'c/*/t',  'c/a/b/c/t',
                                  File::FNM_PATHNAME))

    assert_equal(true,  File.send(method, 'c*t',    'cat'))
    assert_equal(true,  File.send(method, 'c\at',   'cat'))
    assert_equal(false, File.send(method, 'c\at',   'cat', File::FNM_NOESCAPE))
    assert_equal(true,  File.send(method, 'a?b',    'a/b'))
    assert_equal(false, File.send(method, 'a?b',    'a/b', File::FNM_PATHNAME))

    assert_equal(false, File.send(method, '*', '.profile'))
    assert_equal(true,  File.send(method, '*', '.profile', File::FNM_DOTMATCH))
    assert_equal(true,  File.send(method, '*', 'dave/.profile'))
    assert_equal(true,  File.send(method, '*', 'dave/.profile',
                                  File::FNM_DOTMATCH))
    assert_equal(false, File.send(method, '*', 'dave/.profile',
                                  File::FNM_PATHNAME))
    assert_equal(false, File.send(method, '*/*', 'dave/.profile',
                                  File::FNM_PATHNAME))
    strict = File::FNM_PATHNAME | File::FNM_DOTMATCH
    assert_equal(true,  File.send(method, '*/*', 'dave/.profile', strict))

    assert_equal(false, File.send(method, 'cat', 'CAt'))
    assert_equal(true,  File.send(method, 'cat', 'CAt', File::FNM_CASEFOLD))

    assert_equal(true,  File.send(method, 'c[au]t', 'cat'))
    assert_equal(true,  File.send(method, 'c[au]t', 'cut'))
    assert_equal(false, File.send(method, 'c[au]t', 'cot'))

    assert_equal(false, File.send(method, 'c[^au]t', 'cat'))
    assert_equal(false, File.send(method, 'c[^au]t', 'cut'))
    assert_equal(true,  File.send(method, 'c[^au]t', 'cot'))

    for ch in "a".."z"
      inside = ch >= "h" && ch <= "p" || ch == "x"
      str = "c#{ch}t"
      comment = "matching #{str.inspect}"
      assert_equal(inside,  File.send(method, 'c[h-px]t', str), comment)
      assert_equal(!inside, File.send(method, 'c[^h-px]t', str), comment)
    end
  end

  def test_s_fnmatch
    generic_test_s_fnmatch(:fnmatch)
  end

  def test_s_fnmatch?
    generic_test_s_fnmatch(:fnmatch?)
  end

  def test_s_ftype
    Dir.chdir("_test")
    sock = nil

    MsWin32.dont do
      sock = UNIXServer.open("_sock")
      File.symlink("_file1", "_file3") # may fail
    end

    begin
      tests = {
        "../_test" => "directory",
        "_file1"   => "file",
      }

      Windows.dont do
	begin
	  tests[File.expand_path(File.readlink("/dev/tty"), "/dev")] =
	    "characterSpecial"
	rescue Errno::EINVAL
	  tests["/dev/tty"] = "characterSpecial"
	end
      end

      MsWin32.dont do
        tests["_file3"] = "link"
	tests["_sock"]  = "socket"
      end

      Linux.only do
        tests["/dev/"+`readlink /dev/fd0 || echo fd0`.chomp] = "blockSpecial"
	system("mkfifo _fifo") # may fail
	tests["_fifo"] = "fifo"
      end

      tests.each { |file, type|
        if File.exists?(file)
          assert_equal(type, File.ftype(file), file.dup)
        else
          skipping("#{type} not supported")
        end
      }
    ensure
      sock.close if sock 
    end
  end

  def test_s_join

    [
      %w( a b c d ),
      %w( a ),
      %w( ),
      %w( a b .. c )
    ].each do |a|
      assert_equal(a.join(File::SEPARATOR), File.join(*a))
    end
  end

  def test_s_link
    Dir.chdir("_test")
    begin
      assert_equal(0, File.link("_file1", "_file3"))
      
      assert(File.exists?("_file3"))
      Windows.dont do
	assert_equal(2, File.stat("_file1").nlink)
	assert_equal(2, File.stat("_file3").nlink)
	assert(File.stat("_file1").ino == File.stat("_file3").ino)
      end
    ensure
      Dir.chdir("..")
    end
  end

  MsWin32.dont do
    def test_s_lstat
      
      Dir.chdir("_test")
      File.symlink("_file1", "_file3") # may fail
      
      assert_equal(0, File.stat("_file3").size)
      assert(0 < File.lstat("_file3").size)
      
      assert_equal(0, File.stat("_file1").size)
      assert_equal(0,  File.lstat("_file1").size)
    end
  end

  def test_s_mtime
    assert_equal(@mTime, File.mtime(@file))
  end

  # REFACTOR: this test is duplicated in TestFile
  def test_s_open
    file1 = "_test/_file1"

    assert_raise(Errno::ENOENT) { File.open("_gumby") }

    # test block/non block forms
    
    f = File.open(file1)
    begin
      assert_equal(File, f.class)
    ensure
      f.close
    end

    assert_nil(File.open(file1) { |f| assert_equal(File, f.class)})

    # test modes

    modes = [
      %w( r w r+ w+ a a+ ),
      [ File::RDONLY, 
        File::WRONLY | File::CREAT,
        File::RDWR,
        File::RDWR   + File::TRUNC + File::CREAT,
        File::WRONLY + File::APPEND + File::CREAT,
        File::RDWR   + File::APPEND + File::CREAT
        ]]

    for modeset in modes
      sys("rm -f #{file1}")
      sys("touch #{file1}")

      mode = modeset.shift      # "r"

      # file: empty
      File.open(file1, mode) { |f| 
        assert_nil(f.gets)
        assert_raise(IOError) { f.puts "wombat" }
      }

      mode = modeset.shift      # "w"

      # file: empty
      File.open(file1, mode) { |f| 
        assert_nil(f.puts("wombat"))
        assert_raise(IOError) { f.gets }
      }

      mode = modeset.shift      # "r+"

      # file: wombat
      File.open(file1, mode) { |f| 
        assert_equal("wombat\n", f.gets)
        assert_nil(f.puts("koala"))
        f.rewind
        assert_equal("wombat\n", f.gets)
        assert_equal("koala\n", f.gets)
      }

      mode = modeset.shift      # "w+"

      # file: wombat/koala
      File.open(file1, mode) { |f| 
        assert_nil(f.gets)
        assert_nil(f.puts("koala"))
        f.rewind
        assert_equal("koala\n", f.gets)
      }

      mode = modeset.shift      # "a"

      # file: koala
      File.open(file1, mode) { |f| 
        assert_nil(f.puts("wombat"))
        assert_raise(IOError) { f.gets }
      }
      
      mode = modeset.shift      # "a+"

      # file: koala/wombat
      File.open(file1, mode) { |f| 
        assert_nil(f.puts("wallaby"))
        f.rewind
        assert_equal("koala\n", f.gets)
        assert_equal("wombat\n", f.gets)
        assert_equal("wallaby\n", f.gets)
      }

    end

    # Now try creating files

    filen = "_test/_filen"

    File.open(filen, "w") {}
    begin
      assert(File.exists?(filen))
    ensure
      File.delete(filen)
    end
    
    File.open(filen, File::CREAT, 0444) {}
    begin
      assert(File.exists?(filen))
      Cygwin.known_problem do
        assert_equal(0444 & ~File.umask, File.stat(filen).mode & 0777)
      end
    ensure
      WindowsNative.or_variant do
        # to be able to delete the file on Windows
        File.chmod(0666, filen)
      end
      File.delete(filen)
    end
  end

  def test_s_readlink
    MsWin32.dont do 
      Dir.chdir("_test")
      File.symlink("_file1", "_file3") # may fail
      assert_equal("_file1", File.readlink("_file3"))
      assert_raise(Errno::EINVAL) { File.readlink("_file1") }
    end
  end

  def test_s_rename
    Dir.chdir("_test")
    assert_raise(Errno::ENOENT) { File.rename("gumby", "pokey") }
    assert_equal(0, File.rename("_file1", "_renamed"))
    assert(!File.exists?("_file1"))
    assert(File.exists?("_renamed"))

  end

  def test_s_size
    file = "_test/_file1"
    assert_raise(Errno::ENOENT) { File.size("gumby") }
    assert_equal(0, File.size(file))
    File.open(file, "w") { |f| f.puts "123456789" }
    if $os == MsWin32
      assert_equal(11, File.size(file))
    else
      assert_equal(10, File.size(file))
    end
  end

  def test_s_split
    %w{ "/", "/tmp", "/tmp/a", "/tmp/a/b", "/tmp/a/b/", "/tmp//a",
        "/tmp//"
    }.each { |file|
      assert_equal( [ File.dirname(file), File.basename(file) ],
                     File.split(file), file )
    }
  end

  # Stat is pretty much tested elsewhere, so we're minimal here
  def test_s_stat
    assert_instance_of(File::Stat, File.stat("."))
  end


  def test_s_symlink
    MsWin32.dont do 
      Dir.chdir("_test")
      File.symlink("_file1", "_file3") # may fail
      assert(File.symlink?("_file3"))
      assert(!File.symlink?("_file1"))
    end
  end

  def test_s_truncate
    file = "_test/_file1"
    File.open(file, "w") { |f| f.puts "123456789" }
    if $os <= MsWin32
      assert_equal(11, File.size(file))
    else
      assert_equal(10, File.size(file))
    end
    File.truncate(file, 5)
    assert_equal(5, File.size(file))
    File.open(file, "r") { |f|
      assert_equal("12345", f.read(99))
      assert(f.eof?)
    }
  end

  MsWin32.dont do
    def myUmask
      Integer(`sh -c umask`.chomp)
    end

    def test_s_umask
      orig = myUmask
      assert_equal(myUmask, File.umask)
      assert_equal(myUmask, File.umask(0404))
      assert_equal(0404, File.umask(orig))
    end
  end

  
  def test_s_unlink
    Dir.chdir("_test")
    assert_equal(0, File.unlink)
    assert_raise(Errno::ENOENT) { File.unlink("gumby") }
    assert_equal(2, File.unlink("_file1", "_file2"))
  end

  def test_s_utime
    Dir.chdir("_test")
    begin
      [ 
	[ Time.at(18000),             Time.at(53423) ],
	[ Time.at(Time.now.to_i), Time.at(54321) ],
	[ Time.at(121314),        Time.now.to_i ]   # observe the last instance is an integer!
      ].each { |aTime, mTime|
	File.utime(aTime, mTime, "_file1", "_file2")
	
	for file in [ "_file1", "_file2" ]
	  assert_equal(aTime.to_i, File.stat(file).atime.to_i)
	  assert_equal(mTime.to_i, File.stat(file).mtime.to_i)
	end
      }
    ensure
      Dir.chdir("..")
    end
  end

  # Instance methods

  Windows.dont do   # FAT filesystems don't store this properly
    def test_atime
      File.open(@file) { |f| assert_equal(@aTime, f.atime) }
    end
  end

  def test_chmod
    Dir.chdir("_test")
    File.open("_file1") { |f|
      assert_equal(0,                     f.chmod(0))
      assert_equal(_fstat_map(0),    f.stat.mode & 0777)
      assert_equal(0,                     f.chmod(0400))
      assert_equal(_fstat_map(0400), f.stat.mode & 0777)
      assert_equal(0,                     f.chmod(0644))
      assert_equal(_fstat_map(0644), f.stat.mode & 0777)
    }
  end

  def test_chown
    super_user
  end

  def test_ctime
    sys("touch  #@file")
    ctime = RubiconStat::ctime(@file)
    @cTime = Time.at(ctime)

    File.open(@file) { |f| assert_equal(@cTime, f.ctime) }
  end

  def test_flock
    MsWin32.dont do

      Dir.chdir("_test")
      
      # parent forks, then waits for a SIGUSR1 from child. Child locks file
      # and signals parent, then sleeps
      # When parent gets signal, confirms file si locked, kills child,
      # and confirms its unlocked
      
      pid = fork
      if pid
	File.open("_file1", "w") { |f|
	  trap("USR1") {
	    assert_equal(false, f.flock(File::LOCK_EX | File::LOCK_NB))
	    Process.kill "KILL", pid
	    Process.waitpid(pid, 0)
	    assert_equal(0, f.flock(File::LOCK_EX | File::LOCK_NB))
	    return
	  }
	  sleep 10
	  fail("Never got signalled")
	}
      else
	File.open("_file1", "w") { |f|
	  assert_equal(0, f.flock(File::LOCK_EX))
	  sleep 1
	  Process.kill "USR1", Process.ppid
	  sleep 10
	  fail "Parent never killed us"
	}
      end
    end
  end

  def test_lstat
    MsWin32.dont do
      Dir.chdir("_test")

      begin
	File.symlink("_file1", "_file3") # may fail
	f1 = File.open("_file1")
	begin
	  f3 = File.open("_file3")
	  
	  assert_equal(0, f3.stat.size)
	  assert(0 < f3.lstat.size)
	  
	  assert_equal(0, f1.stat.size)
	  assert_equal(0, f1.lstat.size)
	f3.close
	ensure
	  f1.close
	end
      ensure
	Dir.chdir("..")
      end
    end
  end

  def test_mtime
    File.open(@file) { |f| assert_equal(@mTime, f.mtime) }
  end

  def test_path
    File.open(@file) { |f| assert_equal(@file, f.path) }
  end

  def test_truncate
    file = "_test/_file1"
    File.open(file, "w") { |f|
      f.syswrite "123456789" 
      f.truncate(5)
    }
    assert_equal(5, File.size(file))
    File.open(file, "r") { |f|
      assert_equal("12345", f.read(99))
      assert(f.eof?)
    }
  end


end