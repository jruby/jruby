require 'test/unit'

class TestIO < Test::Unit::TestCase
  def setup
    @file = "TestIO_tmp"
    @file2 = "Test2IO_tmp"
    @file3 = "Test3IO_tmp"
  end

  def teardown
    File.unlink @file rescue nil
    File.unlink @file2 rescue nil
    File.unlink @file3 rescue nil
  end

  def test_erroneous_io_usage
    assert_raises(ArgumentError) { IO.new }
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

  def test_seek_on_stdin_fails
    assert_raises(Errno::ESPIPE) { $stdin.seek(10) }
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
    f = File.open(@file)
    f.gets
    i = File.open(@file2)
    t = i.fileno;
    i = i.reopen(f)
    assert_equal(f.pos, i.pos)
    assert_equal(t, i.fileno);
    assert(f.fileno != i.fileno);
    i.close
    f.close
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

  def test_stream_redirection
    old_stderr = $stderr
    old_stdout = $stdout

    begin
      assert_nothing_raised {
        $stderr = FakeStream.new($stderr)
        $stdout = FakeStream.new($stdout)

        system("ruby -e '$stdout.write(42); $stderr.write(24)'")

        assert_equal("42", $stdout.data)
        assert_equal("24", $stderr.data)
      }
    ensure
      $stderr = old_stderr
      $stdout = old_stdout
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

  private
  def ensure_files(*files)
    files.each {|f| File.open(f, "w") {|g| g << " " } }
  end
end