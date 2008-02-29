require 'test/unit'
require 'rbconfig'

class TestIO < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/
  def setup
    @file = "TestIO_tmp"
    @file2 = "Test2IO_tmp"
    @file3 = "Test3IO_tmp"
    if (WINDOWS)
      @devnull = 'NUL:'
    else
      @devnull = '/dev/null'
    end
  end

  def teardown
    File.unlink @file rescue nil
    File.unlink @file2 rescue nil
    File.unlink @file3 rescue nil
  end

  def test_erroneous_io_usage
    assert_raises(ArgumentError) { IO.new }
    # commented out until JRUBY-1048 is completed
    #assert_raises(StandardError) { IO.new(123) }
    assert_raises(TypeError) { IO.new "FROGGER" }
    assert_raises(TypeError) { IO.foreach 3 }
  end

  def test_gets_delimiting
    f = File.new(@file3, "w")
    f.print("A\n\n\nB\n")
    f.close
    f = File.new(@file3, "r")
    f.gets("\n\n")
    b = f.gets("\n\n")
    f.gets("\n\n")
    f.close
    assert(b == "\nB\n", "gets of non-paragraph \"\\n\\n\" failed")
  end

  def test_two_ios_with_same_filenos
    # Two ios with same fileno, but different objects.
    f = File.new(@file, "w")
    f.puts("heh")
    g = IO.new(f.fileno)
    assert_equal(f.fileno, g.fileno)
    assert_raises(IOError) { g.gets }
    g.close
    assert_raises(IOError) { g.puts }

    f = File.new(@file, "r")
    g = IO.new(f.fileno)
    assert_equal(f.fileno, g.fileno)
    assert_raises(IOError) { g.puts }
    # If g closes then g knows that it was once a valid descriptor.
    # So it throws an IOError.
    g.close
    assert_raises(IOError) { g.gets }
  end

  def test_puts_on_a_recursive_array
    # Puts a recursive array
    x = []
    x << 2 << x
    f = File.new(@file, "w")
    g = IO.new(f.fileno)
    g.puts x
    g.close

    f = File.new(@file, "r")
    g = IO.new(f.fileno)
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
    assert_raises(Errno::EINVAL) { g = IO.new(f.fileno, "r") }
    f.close

    f = File.new(@file, "r")
    assert_raises(Errno::EINVAL) { g = IO.new(f.fileno, "w") }
    f.close
  end

  def test_ios_with_compatible_flags
    ensure_files @file
    # However, you can open a second with less permissions
    f = File.new(@file, "r+")
    g = IO.new(f.fileno, "r")
    g.gets
    f.puts "HEH"
    assert_raises(IOError) { g.write "HOH" }
    assert_equal(f.fileno, g.fileno)
    f.close
  end

  def test_seek
    ensure_files @file
    f = File.new(@file)
    assert_raises(Errno::EINVAL) { f.seek(-1) }
    # Advance one + single arg seek
    f.seek(1)
    assert_equal(f.pos, 1)
    f.close
  end

  def test_empty_write_does_not_complain
    # empty write...writes nothing and does not complain
    f = File.new(@file, "w")
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
    file.gets
    file2 = File.open(@file2)
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

  def test_file_puts_gets_readline
    f = File.open(@file, "w")
    f.puts("line1");
    f.puts("line2");
    f.puts("line3");
    f.close

    f = File.open(@file)
    assert_equal(f.gets(), $_)
    assert_equal(f.readline(), $_)
    f.close
  end

  def test_file_read
    ensure_files @file
    # test that read returns correct values
    f = File.open(@file)
    f.read # read all
    assert_equal("", f.read)
    assert_equal(nil, f.read(1))
    f.close
  end
  
  # MRI 1.8.5 and 1.8.6 permit nil buffers with reads.
  def test_file_read_with_nil_buffer
     ensure_files @file
     
     f = File.open(@file)
     assert_equal " ", f.read(1, nil) 
  end
    
  def test_open
    ensure_files @file

    assert_raises(ArgumentError) { io = IO.open }

    f = File.open(@file)
    assert_raises(ArgumentError) { io = IO.open(f.fileno, "r", :gratuitous) }
    io = IO.open(f.fileno, "r")
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
    IO.open(f.fileno, "r") do |io|
      assert_equal(f.fileno, io.fileno)
      assert(!io.closed?)
    end

    assert(!f.closed?)
    assert_raises(Errno::EBADF) { f.close }
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
     File.open(@file, "rb") do |file|
       assert_equal(255, file.getc)
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

      assert_equal(100, file.getc)
     end
  end

  # JRUBY-2202
  def test_ungetc_nonempty_file
    File.open(@file, "w+") { |file| file.puts("HELLO") }
    File.open(@file) do |file|
      assert_equal(72, file.getc)
      assert_equal(1, file.pos)
      file.ungetc(100)
      assert_equal(0, file.pos)
      assert_equal(100, file.getc)
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
    File.open(@file, "w+") { |file| file.puts("HELLO") }
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
    f = File.open("__temp1", "w")
    x = IO.new(f.fileno)
    f.print "."
    y = x.dup
    x.reopen(y)
    f.print "."
    f.close
    out = File.read("__temp1")
    assert_equal "..", out
  ensure
    File.unlink("__temp1") rescue nil
  end
  
  # JRUBY-1698
  def test_very_big_read
    # See JRUBY-1686: this caused OOM
    ensure_files @file
    f = File.open(@file)
    assert_nothing_raised { f.read(1000000000) }
  end
  
  # JRUBY-2023, multithreaded writes
  def test_multithreaded_writes
    f = File.open("__temp1", "w")
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

  #JRUBY-2145
  def test_copy_dev_null
    require 'fileutils'
    begin
      FileUtils.cp(@devnull, 'somefile')
      assert(File.exists?('somefile'))
      assert_equal(0, File.size('somefile'))
    ensure
      File.delete('somefile') rescue nil
    end
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

  private
  def ensure_files(*files)
    files.each {|f| File.open(f, "w") {|g| g << " " } }
  end
end
