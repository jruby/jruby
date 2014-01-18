# -*- coding: utf-8 -*-
require 'test/unit'
require 'rbconfig'
require 'stringio'
require 'java'
require 'jruby'

class TestIO < Test::Unit::TestCase
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/
  SOLARIS = RbConfig::CONFIG['host_os'] =~ /solaris/

  def setup
    @to_close = []
    @file = "TestIO_tmp"
    @file2 = "Test2IO_tmp"
    @file3 = "Test3IO_tmp"
    if (WINDOWS)
      @devnull = 'NUL:'
    else
      @devnull = '/dev/null'
    end
    @stringio = StringIO.new 'abcde'
  end

  def teardown
    @to_close.each {|io| io.close rescue nil }
    File.unlink @file rescue nil
    File.unlink @file2 rescue nil
    File.unlink @file3 rescue nil
  end

  def test_puts_on_a_recursive_array
    # Puts a recursive array
    x = []
    x << 2 << x
    f = File.new(@file, "w")
    @to_close << f
    g = IO.new(f.fileno)
    @to_close << g
    g.puts x
    g.close

    f = File.new(@file, "r")
    @to_close << f
    g = IO.new(f.fileno)
    @to_close << g
    a = f.gets
    b = f.gets
    assert_equal("2\n", a)
    assert_equal("[...]\n", b)
  end

  def test_premature_close_raises_appropriate_errors
    ensure_files @file
    # In this case we will have f close (which will pull the rug
    # out from under g) and thus make g try the ops and fail
    f = File.open(@file)
    g = IO.new(f.fileno)
    @to_close << g
    f.close
    assert_raises(Errno::EBADF) { g.readchar }
    assert_raises(Errno::EBADF) { g.readline }
    assert_raises(Errno::EBADF) { g.gets }
    assert_raises(Errno::EBADF) { g.close }
    assert_raises(IOError) { g.getc }
    assert_raises(IOError) { g.readchar }
    assert_raises(IOError) { g.read }
    assert_raises(IOError) { g.sysread 1 }

    f = File.open(@file, "w")
    g = IO.new(f.fileno)
    @to_close << g
    f.close
    assert_nothing_raised { g.print "" }
    assert_nothing_raised { g.write "" }
    assert_nothing_raised { g.puts "" }
    assert_nothing_raised { g.putc 'c' }
    assert_raises(Errno::EBADF) { g.syswrite "" }
  end

  def test_ios_with_incompatible_flags
    ensure_files @file, @file2
    # Cannot open an IO which does not have compatible permission with
    # original IO
    f = File.new(@file2, "w")
    @to_close << f
    assert_raises(Errno::EINVAL) { g = IO.new(f.fileno, "r") }
    f.close

    f = File.new(@file, "r")
    @to_close << f
    assert_raises(Errno::EINVAL) { g = IO.new(f.fileno, "w") }
    f.close
  end

  def test_ios_with_compatible_flags
    ensure_files @file
    # However, you can open a second with less permissions
    f = File.new(@file, "r+")
    @to_close << f
    g = IO.new(f.fileno, "r")
    @to_close << g
    g.gets
    f.puts "HEH"
    assert_raises(IOError) { g.write "HOH" }
    assert_equal(f.fileno, g.fileno)
    f.close
  end

  def test_empty_write_does_not_complain
    # empty write...writes nothing and does not complain
    f = File.new(@file, "w")
    @to_close << f
    i = f.syswrite("")
    assert_equal(i, 0)
    i = f.syswrite("heh")
    assert_equal(i, 3)
    f.close
  end

  def test_enoent
    assert_raises(Errno::ENOENT) { File.foreach("nonexistent_file") {} }
  end

  def test_reopen
    ensure_files @file, @file2
    file = File.open(@file)
    @to_close << file
    file.gets
    file2 = File.open(@file2)
    @to_close << file2
    file2_fileno = file2.fileno;
    file2 = file2.reopen(file)
    assert_equal(file.pos, file2.pos)
    assert_equal(file2_fileno, file2.fileno);
    assert(file.fileno != file2.fileno);
    file2.close
    file.close

    # reopen of a filename after a close should succeed (JRUBY-1885)
    assert_nothing_raised { file.reopen(@file) }
  end

  def test_file_read
    ensure_files @file
    # test that read returns correct values
    f = File.open(@file)
    @to_close << f
    f.read # read all
    assert_equal("", f.read)
    assert_equal(nil, f.read(1))
    f.close
  end

  # MRI 1.8.5 and 1.8.6 permit nil buffers with reads.
  def test_file_read_with_nil_buffer
     ensure_files @file

     f = File.open(@file)
     @to_close << f
     assert_equal " ", f.read(1, nil)
  end

  def test_open
    ensure_files @file

    assert_raises(ArgumentError) { io = IO.open }

    f = File.open(@file)
    @to_close << f
    assert_raises(ArgumentError) { io = IO.open(f.fileno, "r", :gratuitous) }
    io = IO.open(f.fileno, "r")
    @to_close << io
    assert_equal(f.fileno, io.fileno)
    assert(!io.closed?)
    io.close
    assert(io.closed?)

    assert(!f.closed?)
    assert_raises(Errno::EBADF) { f.close }
  end

  def test_open_with_block
    ensure_files @file

    f = File.open(@file)
    @to_close << f
    IO.open(f.fileno, "r") do |io|
      assert_equal(f.fileno, io.fileno)
      assert(!io.closed?)
    end

    assert(!f.closed?)
    assert_raises(Errno::EBADF) { f.close }
  end

  unless WINDOWS # Windows doesn't take kindly to perm mode tests
    def test_sysopen
      ensure_files @file

      fno = IO::sysopen(@file, "r", 0124) # not creating, mode is ignored
      assert_instance_of(Fixnum, fno)
      assert_raises(Errno::EINVAL) { IO.open(fno, "w") } # not writable
      IO.open(fno, "r") do |io|
        assert_equal(fno, io.fileno)
        assert(!io.closed?)
      end
      assert_raises(Errno::EBADF) { IO.open(fno, "r") } # fd is closed
      File.open(@file) do |f|
        mode = (f.stat.mode & 0777) # only comparing lower 9 bits
        assert(mode > 0124)
      end

      File.delete(@file)
      fno = IO::sysopen(@file, "w", 0611) # creating, mode is enforced
      File.open(@file) do |f|
        mode = (f.stat.mode & 0777)
        assert_equal(0611, mode)
      end
    end
  end

  def test_delete
    ensure_files @file, @file2, @file3
    # Test deleting files
    assert(File.delete(@file, @file2, @file3))
  end

  def test_select
    ##### select #####
    assert_equal(nil, select(nil, nil, nil, 0))
    assert_raises(ArgumentError) { select(nil, nil, nil, -1) }
  end

  class FakeStream
    attr_accessor :data
    def initialize(stream, passthrough = false)
      @stream = stream
      @passthrough = passthrough
    end
    def write(content)
      @data = content
      @stream.write(content) if @passthrough
    end
  end

  def test_puts_and_warn_redirection
    require 'stringio'
    begin
      $stdout = StringIO.new
      $stderr = StringIO.new
      $stdout.print ":"
      $stderr.print ":"
      puts "hi"
      warn "hello"
      assert_equal ":hi\n", $stdout.string
      assert_equal ":hello\n", $stderr.string
    ensure
      $stderr = STDERR
      $stdout = STDOUT
    end
  end

  # JRUBY-1894
  def test_getc_255
    File.open(@file, "wb") do |file|
      file.putc(255)
    end
  end

  # JRUBY-2202
  def test_ungetc_empty_file
    File.open(@file, "w+") {}
    File.open(@file) do |file|
      assert_nil(file.getc)
      assert_equal(0, file.pos)
      file.ungetc(100)

      # The following line is an intentional regression tests,
      # it checks that JRuby doesn't break.
      assert_equal(0, file.pos)

      assert_equal("d", file.getc)
     end
  end

  # JRUBY-2202
  def test_ungetc_nonempty_file
    File.open(@file, "w+") { |file| file.puts("HELLO") }
    File.open(@file) do |file|
      assert_equal("H", file.getc)
      assert_equal(1, file.pos)
      file.ungetc(100)
      assert_equal(0, file.pos)
      assert_equal("d", file.getc)
      assert_equal(1, file.pos)
     end
  end

  # JRUBY-2202
  def test_ungetc_position_change
    File.open(@file, "w+") { |file| file.puts("HELLO") }

    # getc/ungetc the same char
    File.open(@file) do |f|
      f.ungetc(f.getc)
      assert_equal(0, f.pos)
      assert_equal("HELLO", f.read(5))
      assert_equal(5, f.pos)
    end

    # getc/ungetc different char
    File.open(@file) do |f|
      f.getc
      f.ungetc(100)
      assert_equal(0, f.pos)
      assert_equal("dELLO", f.read(5))
      assert_equal(5, f.pos)
     end
  end

  # JRUBY-2203
  # unget char should be discarded after position changing calls
  def test_unget_before_position_change
    File.open(@file, "wb+") { |file| file.puts("HELLO") }
    File.open(@file) do |f|
      f.read(3)
      f.ungetc(100)
      f.pos = 2
      assert_equal("LLO", f.read(3))

      f.ungetc(100)
      f.seek(2)
      assert_equal("LLO", f.read(3))

      f.ungetc(100)
      f.rewind
      assert_equal("HELLO", f.read(5))

      f.ungetc(100)
      f.seek(-3, IO::SEEK_END)
      assert_equal("LO", f.read(2))
    end
  end

  # JRUBY-1987
  def test_reopen_doesnt_close_same_handler
    f = File.open(@file, "w")
    @to_close << f
    x = IO.new(f.fileno)
    @to_close << x
    f.print "."
    y = x.dup
    @to_close << y
    x.reopen(y)
    f.print "."
    f.close
    out = File.read(@file)
    assert_equal "..", out
  end

  # JRUBY-1698
  def test_very_big_read
    # See JRUBY-1686: this caused OOM
    ensure_files @file
    f = File.open(@file)
    @to_close << f
    assert_nothing_raised { f.read(1000000000) }
  end

  # JRUBY-2023, multithreaded writes
  def test_multithreaded_writes
    f = File.open("__temp1", "w")
    @to_close << f
    threads = []
    100.times {
      threads << Thread.new { 100.times { f.print('.') } }
    }
    threads.each {|thread| thread.join}
    f.close
    assert File.size("__temp1") == 100*100
  ensure
    File.unlink("__temp1")
  end

  #JRUBY-2145
  def test_eof_on_dev_null
    File.open(@devnull, 'rb') { |f|
      assert(f.eof?)
    }
  end

  #JRUBY-2145
  def test_read_dev_null
    File.open(@devnull, 'rb') { |f|
      assert_equal("", f.read)
      assert_equal(nil, f.read(1))
      assert_equal([], f.readlines)
      assert_raise EOFError do
        f.readline
      end
    }
  end

  def test_read_ignores_blocks
    a = true
    File.read(@devnull) { a = false }
    assert(a)
  end

  if (WINDOWS)
    #JRUBY-2158
    def test_null_open_windows
      null_names = ['NUL', 'NUL:', 'nul', 'nul:']
      null_names.each { |name|
        File.open(name) { |f|
          assert_equal("", f.read)
          assert(f.eof?)
        }
        File.open(name, 'r+') { |f|
          assert_nil(f.puts("test"))
        }
      }
    end
  end

  def test_file_constants_included
    assert IO.include?(File::Constants)
    constants = ["APPEND", "BINARY", "CREAT", "EXCL", "FNM_CASEFOLD",
                   "FNM_DOTMATCH", "FNM_NOESCAPE", "FNM_PATHNAME", "FNM_SYSCASE",
                   "LOCK_EX", "LOCK_NB", "LOCK_SH", "LOCK_UN", "NOCTTY", "NONBLOCK",
                   "RDONLY", "RDWR", "SEEK_CUR", "SEEK_END", "SEEK_SET", "SYNC", "TRUNC",
                   "WRONLY"]
    constants = constants.map(&:to_sym)
    constants.each { |c| assert(IO.constants.include?(c), "#{c} is not included") }
  end

  #JRUBY-3012
  def test_io_reopen
    quiet_script = File.dirname(__FILE__) + '/quiet.rb'
    result = `#{ENV_JAVA['jruby.home']}/bin/jruby #{quiet_script}`.chomp
    assert_equal("foo", result)
  end

  # JRUBY-4152
  def test_tty_leak
    if $stdin.tty? # in Ant that might be false
      assert $stdin.tty?
      10_000.times {
        $stdin.tty?
      }
      assert $stdin.tty?
    end
  end

  # JRUBY-4821
  def test_clear_dollar_bang_after_open_block
    open(__FILE__) do |io|
      io.close
    end
    assert_nil $!
  end

  # JRUBY-4932
  #  def test_popen4_read_error
  #  p, o, i, e = IO.popen4(__FILE__)
  #  assert_raise(IOError) { i.read }
  #end

  def ensure_files(*files)
    files.each {|f| File.open(f, "w") {|g| g << " " } }
  end
  private :ensure_files
  
  # JRUBY-4908  ... Solaris is commented out for now until I can figure out why
  # ci will not run it properly.
  if !WINDOWS && !SOLARIS && false # temporarily disable
    def test_sh_used_appropriately
      # should not use sh
      p, o, i, e = IO.popen4("/bin/ps -a -f")
      assert_match p.to_s, i.read.lines.grep(/\/bin\/ps -a -f/).first
      
      # should use sh
      p, o, i, e = IO.popen4("/bin/ps -a -f | grep [/]bin/ps'")
      assert_no_match Regexp.new(p.to_s), i.read.lines.grep(/\/bin\/ps/).first
    end
  end
  
  # JRUBY-5114
  def test_autoclose_false_leaves_channels_open
    channel = java.io.FileInputStream.new(__FILE__).channel
    
    # sanity check
    io1 = channel.to_io(:autoclose => false)
    assert_equal "#", io1.sysread(1)
    io2 = channel.to_io(:autoclose => false)
    assert_equal " ", io2.sysread(1)
    
    # dereference and force GC a few times to finalize
    io1 = nil
    5.times { java.lang.System.gc }
    
    # io2 and original channel should still be open and usable
    assert_equal "-", io2.sysread(1)
    assert !io2.closed?
    
    assert channel.open?
  end

  def test_gets_no_args
    File.open(@file, 'w') { |f| f.write 'abcde' }

    File.open(@file) do |f|
      assert_equal 'abcde', f.gets
    end
  end

  def test_gets_separator
    File.open(@file, 'w') { |f| f.write 'abcde' }

    File.open(@file) do |f|
      assert_equal 'abc', f.gets('c')
    end
  end

  def test_stringio_gets_no_args
    assert_equal 'abcde', @stringio.gets
  end

  def test_stringio_gets_separator
    assert_equal 'abc', @stringio.gets('c')
  end

  # JRUBY-6137
  def test_rubyio_fileno_mapping_leak
    starting_count = JRuby.runtime.fileno_int_map_size
    io = org.jruby.RubyIO.new(JRuby.runtime, org.jruby.util.io.STDIO::ERR)
    open_io_count = JRuby.runtime.fileno_int_map_size
    assert_equal(starting_count + 1, open_io_count)
    io.close
    closed_io_count = JRuby.runtime.fileno_int_map_size
    assert_equal(starting_count, closed_io_count)
  end

  # JRUBY-1222
  def test_stringio_gets_utf8
    @stringio = StringIO.new("®\r\n®\r\n")
    assert_equal "®\r\n", @stringio.gets("\r\n")
    assert_equal "®\r\n", @stringio.gets("\r\n")
  end
end
