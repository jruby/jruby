require 'test/unit'
require 'fileutils'

# FIXME: This needs platform-specific stuff changed
class TestIO < Test::Unit::TestCase
  WIN32 = PLATFORM =~ /mswin/

  SAMPLE = "08: This is a line\n"

  LINE_LENGTH = WIN32 ? SAMPLE.length + 1 : SAMPLE.length
  
  def setup_test_dir
    Dir.mkdir "_test"
  end
  
  def teardown_test_dir
    FileUtils.rm_rf "_test"
  end

  def setup
    setup_test_dir
    @file  = "_test/_10lines"
    @file1 = "_test/_99lines"

    File.open(@file, "w") do |f|
      10.times { |i| f.printf "%02d: This is a line\n", i }
    end
    File.open(@file1, "w") do |f|
      99.times { |i| f.printf "Line %02d\n", i }
    end
  end

  def teardown
    teardown_test_dir
  end

  unless WIN32
    def stdin_copy_pipe
      IO.popen("#$interpreter -e '$stdout.sync=true;while gets;puts $_;end'", "r+")
    end
  end

  # ---------------------------------------------------------------

  def test_s_foreach
    assert_raise(Errno::ENOENT) { File.foreach("gumby") {} }
  # No longer a valid test in 1.8.7
=begin
    assert_raise(LocalJumpError) { File.foreach(@file) }
=end
    
    count = 0
    IO.foreach(@file) do |line|
      num = line[0..1].to_i
      assert_equal(count, num)
      count += 1
    end
    assert_equal(10, count)

    count = 0
    IO.foreach(@file, nil) do |file|
      file.split(/\n/).each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
    end
    assert_equal(10, count)

    count = 0
    IO.foreach(@file, ' ') do |thing|
      count += 1
    end
    assert_equal(41, count)
  end

  def test_s_new
    f = File.open(@file)
    io = IO.new(f.fileno, "r")
    begin
      count = 0
      io.each { count += 1 }
      assert_equal(10, count)
    ensure
      io.close
      begin
        f.close
      rescue Exception
      end
    end

    f = File.open(@file)
    io = IO.new(f.fileno, "r")
    
    begin
      f.close
      assert_raise(Errno::EBADF) { io.gets }
    ensure
      assert_raise(Errno::EBADF) { io.close }
      begin
        f.close
      rescue Exception
      end
    end

    f = File.open(@file, "r")
    f.sysread(3*LINE_LENGTH)
    io = IO.new(f.fileno, "r")
    begin
      assert_equal(3*LINE_LENGTH, io.tell)
      
      count = 0
      io.each { count += 1 }
      assert_equal(7, count)
    ensure
      io.close
      begin
        f.close
      rescue Exception
      end
    end
  end

  def test_s_pipe
    p = IO.pipe
    begin
      assert_equal(2, p.size)
      r, w = *p
      assert_instance_of(IO, r)
      assert_instance_of(IO, w)
      
      w.puts "Hello World"
      assert_equal("Hello World\n", r.gets)
    ensure
      r.close
      w.close
    end
  end


    # TODO: fails on 1.8.6
=begin
  def test_s_popen
    if WIN32
      cmd = "type"
      fname = @file.tr '/', '\\'
    else
      cmd = "cat"
      fname = @file
    end


    # READ

    IO.popen("#{cmd} #{fname}") do |p|
      count = 0
      p.each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(10, count)
    end

    # READ with block
    res = IO.popen("#{cmd} #{fname}") do |p|
      count = 0
      p.each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(10, count)
    end
    assert_nil(res)


    # WRITE
    IO.popen("#$interpreter -e 'puts readlines' >#{fname}", "w") do |p|
      5.times { |i| p.printf "Line %d\n", i }
    end

    count = 0
    IO.foreach(@file) do |line|
      num = line.chomp[-1,1].to_i
      assert_equal(count, num)
      count += 1
    end
    assert_equal(5, count)
    
    unless WIN32
      # Spawn an interpreter
      parent = $$
      p = IO.popen("-")
      if p
	begin
	  assert_equal(parent, $$)
	  assert_equal("Hello\n", p.gets)
	ensure
	  p.close
	end
      else
	assert_equal(parent, Process.ppid)
	puts "Hello"
	exit
      end
    end
  end
=end

  # disabled due to JRUBY-1895
  def XXXtest_s_popen_spawn
    unless WIN32
      # Spawn an interpreter - WRITE
      parent = $$
      pipe = IO.popen("-", "w")
      
      if pipe
	begin
	  assert_equal(parent, $$)
	  pipe.puts "12"
	  Process.wait pipe.pid
	  assert_equal(12, $?>>8)
	ensure
	  pipe.close
	end
      else
	buff = $stdin.gets
	exit buff.to_i
      end
      
      # Spawn an interpreter - READWRITE
      parent = $$
      p = IO.popen("-", "w+")
      
      if p
	begin
	  assert_equal(parent, $$)
	  p.puts "Hello\n"
	  assert_equal("Goodbye\n", p.gets)
	  Process.wait
	ensure
	  p.close
	end
      else
	puts "Goodbye" if $stdin.gets == "Hello\n"
	exit
      end
    end
  end    

  def test_s_readlines
    assert_raise(Errno::ENOENT) { IO.readlines('gumby') }

    lines = IO.readlines(@file)
    assert_equal(10, lines.size)

    lines = IO.readlines(@file, nil)
    assert_equal(1, lines.size)
    assert_equal(SAMPLE.length*10, lines[0].size)
  end

  # disabled since std streams are not selectable under Java.
  def XXXtest_s_select
    assert_nil(select(nil, nil, nil, 0))
    assert_raise(ArgumentError) { IO.select(nil, nil, nil, -1) }
    
    File.open(@file) do |file|
      res = IO.select([file], [$stdout, $stderr], [file,$stdout,$stderr], 1)
      assert_equal( 3, res.length )
      assert_equal( [file],             res[0] )
      assert_equal( [$stdout, $stderr], res[1] )

      # TODO: not sure how to handle this
=begin
      case
      when $os == Solaris || $os == MacOS
        # select is documented to work this way on Solaris.
        # From the select(3C) man page:
        #
        #     File descriptors associated with regular files always
        #     select true for ready to read, ready to write, and error
        #     conditions.
        #
        # MacOS seem to work the same way.
        #
        assert_equal( [file], res[2] )

      when $os == 
        # seems to work like this on Windows, but I have not found any
        # documentation supporting it.
        #
        assert_equal( [file, $stdout, $stderr], res[2])

      else
        # The "normal" case for Linux, FreeBSD, etc.
        #
        assert_equal( [], res[2] )

      end
=end
    end
    
#     read, write = *IO.pipe
#     read.fcntl(F_SETFL, File::NONBLOCK)
  
#     assert_nil(select([read], nil,  [read], .1))
#     write.puts "Hello"
#     assert_equal([[read],[],[]], select([read], nil,  [read], .1))
#     read.gets
#     assert_nil(select([read], nil,  [read], .1))
#     write.close
#     assert_equal([[read],[],[]], select([read], nil,  [read], .1))
#     assert_nil(read.gets)
#     read.close
  end

  class Dummy
    def to_s
      "dummy"
    end
  end

  def test_LSHIFT # '<<'
    file = File.open(@file, "w")
    io = IO.new(file.fileno, "w")
    io << 1 << "\n" << Dummy.new << "\n" << "cat\n"
    io.close
    assert_raise(Errno::EBADF) { file.close }
    expected = [ "1\n", "dummy\n", "cat\n"]
    IO.foreach(@file) do |line|
      assert_equal(expected.shift, line)
    end
    assert_equal([], expected)
  end

  def test_binmode
    # TODO: needs impl
  end

  def test_clone
    # check file position shared
    file = File.open(@file, "r")
    io = []
    io[0] = IO.new(file.fileno, "r")
    begin
      io[1] = io[0].clone
      begin
        count = 0
        io[count & 1].each do |line|
          num = line[0..1].to_i
          assert_equal(count, num)
          count += 1
        end
        assert_equal(10, count)
      ensure
        io[1].close
      end
    ensure
      io[0].close
    end
    assert_raise(Errno::EBADF) { file.close }
  end

  def test_close
    read, write = *IO.pipe
    begin
      read.close
      assert_raise(IOError) { read.gets }
    ensure
      begin
        read.close
      rescue Exception
      end
      write.close
    end
  end

    # TODO: fails on 1.8.6
=begin
  def test_close_read
    unless WIN32
      pipe = stdin_copy_pipe
      begin
	pipe.puts "Hello"
	assert_equal("Hello\n", pipe.gets)
	pipe.close_read
	assert_raise(IOError) { pipe.gets }
      ensure
	pipe.close_write
      end
    end
  end
=end

    # TODO: fails on 1.8.6
=begin
  def test_close_write
    unless WIN32
      pipe = stdin_copy_pipe
      
      pipe.puts "Hello"
      assert_equal("Hello\n", pipe.gets)
      pipe.close_write
      assert_raise(IOError) { pipe.puts "Hello" }
      pipe.close
    end
  end
=end

  def test_closed?
    f = File.open(@file)
    assert(!f.closed?)
    f.close
    assert(f.closed?)

    # TODO: stdin_copy_pipe produces a warning on 1.8.6
=begin
    unless WIN32
      pipe = stdin_copy_pipe
      assert(!pipe.closed?)
      pipe.close_read
      assert(!pipe.closed?)
      pipe.close_write
      assert(pipe.closed?)
    end
=end
  end

  def test_each
  #  count = 0
  # # File.open(@file) do |file|
  #    file.each do |line|
  #      num = line[0..1].to_i
   #     assert_equal(count, num)
    #    count += 1
  #    end
  #  #  assert_equal(10, count)
 #   end

  #  count = 0
  #  File.open(@file) do |file|
  #    file.each(nil) do |contents|
  #      contents.split(/\n/).each do |line|
  #        num = line[0..1].to_i
  #        assert_equal(count, num)
  #        count += 1
  ##      end
   #   end
   # end
   # assert_equal(10, count)

    count = 0
    File.open(@file) do |file|
      file.each(' ') do |thing|
        count += 1
      end
    end
    assert_equal(41, count)
  end

  def test_each_byte
    count = 0
    data = 
      "00: This is a line\n" +
      "01: This is a line\n" +
      "02: This is a line\n" +
      "03: This is a line\n" +
      "04: This is a line\n" +
      "05: This is a line\n" +
      "06: This is a line\n" +
      "07: This is a line\n" +
      "08: This is a line\n" +
      "09: This is a line\n" 

    File.open(@file) do |file|
      file.each_byte do |b|
        assert_equal(data[count], b)
        count += 1
      end
    end
    assert_equal(SAMPLE.length*10, count)
  end

  def test_each_line
    count = 0
    File.open(@file) do |file|
      file.each_line do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(10, count)
    end

    count = 0
    File.open(@file) do |file|
      file.each_line(nil) do |contents|
        contents.split(/\n/).each do |line|
          num = line[0..1].to_i
          assert_equal(count, num)
          count += 1
        end
      end
    end
    assert_equal(10, count)

    count = 0
    File.open(@file) do |file|
      file.each_line(' ') do |thing|
        count += 1
      end
    end
    assert_equal(41, count)
  end

  def test_eof
    File.open(@file) do |file|
      10.times do
        assert(!file.eof)
        assert(!file.eof?)
        file.gets
      end
      assert(file.eof)
      assert(file.eof?)
    end
  end

  def test_fcntl
    # TODO: needs impl
  end

  def test_fileno
    assert_equal(0, $stdin.fileno)
    assert_equal(1, $stdout.fileno)
    assert_equal(2, $stderr.fileno)
  end

  def test_flush
    unless WIN32
      read, write = IO.pipe
      write.sync = false
      write.print "hello"
      assert_nil(select([read], nil,  [read], 0.1))
      write.flush
      assert_equal([[read],[],[]], select([read], nil,  [read], 0.1))
      read.close
      write.close
    end
  end

  def test_getc
    count = 0
    data = 
      "00: This is a line\n" +
      "01: This is a line\n" +
      "02: This is a line\n" +
      "03: This is a line\n" +
      "04: This is a line\n" +
      "05: This is a line\n" +
      "06: This is a line\n" +
      "07: This is a line\n" +
      "08: This is a line\n" +
      "09: This is a line\n" 
    
    File.open(@file) do |file|
      while (ch = file.getc)
        assert_equal(data[count], ch)
        count += 1
      end
      assert_equal(nil, file.getc)
    end
    assert_equal(SAMPLE.length*10, count)
  end

  def test_gets
    count = 0
    File.open(@file) do |file|
      while (line = file.gets)
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_equal(nil, file.gets)
      assert_equal(10, count)
    end

    count = 0
    File.open(@file) do |file|
      while (contents = file.gets(nil))
        contents.split(/\n/).each do |line|
          num = line[0..1].to_i
          assert_equal(count, num)
          count += 1
        end
      end
    end
    assert_equal(10, count)

    count = 0
    File.open(@file) do |file|
      while (thing = file.gets(' '))
        count += 1
      end
    end
    assert_equal(41, count)
  end

  def test_gets_para
    File.open(@file, "w") do |file|
      file.print "foo\n"*4096, "\n"*4096, "bar"*4096, "\n"*4096, "zot\n"*1024
    end
    File.open(@file) do |file|
      assert_equal("foo\n"*4096+"\n", file.gets(""))
      assert_equal("bar"*4096+"\n\n", file.gets(""))
      assert_equal("zot\n"*1024, file.gets(""))
    end
  end

  def test_ioctl
    # TODO: needs impl
  end

  # see tty?
  def test_isatty
    File.open(@file) { |f|  assert(!f.isatty) }
    if WIN32 
      File.open("con") { |f| assert(f.isatty) }
    end
    unless WIN32
      begin
        File.open("/dev/tty") { |f| assert(f.isatty) }
      rescue
        # in run from (say) cron, /dev/tty can't be opened
      end
    end
  end

  def test_lineno
    count = 1
    File.open(@file) do |file|
      while (line = file.gets)
        assert_equal(count, file.lineno)
        count += 1
      end
      assert_equal(11, count)
      file.rewind
      assert_equal(0, file.lineno)
    end

    count = 1
    File.open(@file) do |file|
      while (line = file.gets('i'))
        assert_equal(count, file.lineno)
        count += 1
      end
      assert_equal(32, count)
    end
  end

  def test_lineno=
    File.open(@file) do |f|
      assert_equal(0, f.lineno)
      assert_equal(123, f.lineno = 123)
      assert_equal(123, f.lineno)
      f.gets
      assert_equal(124, f.lineno)
      f.lineno = 0
      f.gets
      assert_equal(1, f.lineno)
      f.gets
      assert_equal(2, f.lineno)
    end
  end

    # TODO: fails on 1.8.6
=begin
  def test_pid
    assert_nil($stdin.pid)
    pipe = nil
    if WIN32
      pipe = IO.popen("#$interpreter -e 'p $$'", "r")
    else
      pipe = IO.popen("exec #$interpreter -e 'p $$'", "r")
    end

    pid = pipe.gets
    assert_equal(pid.to_i, pipe.pid)
    pipe.close
  end
=end

  def test_pos
    pos = 0
    File.open(@file, "rb") do |file|
      assert_equal(0, file.pos)
      while (line = file.gets)
        pos += line.length
        assert_equal(pos, file.pos)
      end
    end
  end

  def test_pos=
    nums = [ 5, 8, 0, 1, 0 ]

    File.open(@file) do |file|
      file.pos = 999
      assert_nil(file.gets)
      #
      # Errno::EFBIG added below for FreeBSD, because of
      # fseeko(3) behaviour there.
      #
      assert_raise(Errno::EFBIG, Errno::EINVAL, SystemCallError) {
        file.pos = -1
      }
      for pos in nums
        assert_equal(LINE_LENGTH*pos, file.pos = LINE_LENGTH*pos)
        line = file.gets
        assert_equal(pos, line[0..1].to_i)
      end
    end
  end

  def test_print
    File.open(@file, "w") do |file|
      file.print "hello"
      file.print 1,2
      $_ = "wombat\n"
      file.print
      $\ = ":"
      $, = ","
      file.print 3, 4
      file.print 5, 6
      $\ = nil
      file.print "\n"
      $, = nil
    end

    File.open(@file) do |file|
      content = file.gets(nil)
      assert_equal("hello12wombat\n3,4:5,6:\n", content)
    end
  end

  def test_printf
    # tested under Kernel.sprintf
  end

  def test_putc
    File.open(@file, "wb") do |file|
      file.putc "A"
      0.upto(255) { |ch| file.putc ch }
    end

    File.open(@file, "rb") do |file|
      assert_equal(?A, file.getc)
      0.upto(255) { |ch| assert_equal(ch, file.getc) }
    end
  end

  def test_puts
    File.open(@file, "w") do |file|
      file.puts "line 1", "line 2"
      file.puts [ Dummy.new, 4 ]
    end

    File.open(@file) do |file|
      assert_equal("line 1\n",  file.gets)
      assert_equal("line 2\n",  file.gets)
      assert_equal("dummy\n",   file.gets)
      assert_equal("4\n",       file.gets)
    end
  end

  def test_read
    File.open(@file) do |file|
      content = file.read
      assert_equal(SAMPLE.length*10, content.length)
      count = 0
      content.split(/\n/).each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
    end

    File.open(@file) do |file|
      assert_equal("00: This is ", file.read(12))
      assert_equal("a line\n01: T", file.read(12))
    end
  end

  def test_readchar
    count = 0
    data = 
      "00: This is a line\n" +
      "01: This is a line\n" +
      "02: This is a line\n" +
      "03: This is a line\n" +
      "04: This is a line\n" +
      "05: This is a line\n" +
      "06: This is a line\n" +
      "07: This is a line\n" +
      "08: This is a line\n" +
      "09: This is a line\n" 
    
    File.open(@file) do |file|
      190.times do |count|
        ch = file.readchar
        assert_equal(data[count], ch)
        count += 1
      end
      assert_raise(EOFError) { file.readchar }
    end
  end

  def test_readline
    count = 0
    File.open(@file) do |file|
      10.times do |count|
        line = file.readline
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_raise(EOFError) { file.readline }
    end

    count = 0
    File.open(@file) do |file|
      contents = file.readline(nil)
      contents.split(/\n/).each do |line|
        num = line[0..1].to_i
        assert_equal(count, num)
        count += 1
      end
      assert_raise(EOFError) { file.readline }
    end
    assert_equal(10, count)

    count = 0
    File.open(@file) do |file|
      41.times do |count|
        thing = file.readline(' ')
        count += 1
      end
      assert_raise(EOFError) { file.readline }
    end
  end

  def test_readlines
    File.open(@file) do |file|
      lines = file.readlines
      assert_equal(10, lines.size)
    end

    File.open(@file) do |file|
      lines = file.readlines(nil)
      assert_equal(1, lines.size)
      assert_equal(SAMPLE.length*10, lines[0].size)
    end
  end

  def test_reopen1
    f1 = File.new(@file)
    assert_equal("00: This is a line\n", f1.gets)
    assert_equal("01: This is a line\n", f1.gets)

    f2 = File.new(@file1)
    assert_equal("Line 00\n", f2.gets)
    assert_equal("Line 01\n", f2.gets)

    f2.reopen(@file)
    assert_equal("00: This is a line\n", f2.gets)
    assert_equal("01: This is a line\n", f2.gets)
  ensure
    f1.close if f1
    f2.close if f2
  end

  def test_reopen2 
    f1 = File.new(@file)
    assert_equal("00: This is a line\n", f1.read(SAMPLE.length))
    assert_equal("01: This is a line\n", f1.read(SAMPLE.length))

    f2 = File.new(@file1)
    assert_equal("Line 00\n", f2.read(8))
    assert_equal("Line 01\n", f2.read(8))

    f2.reopen(f1)
    assert_equal("02: This is a line\n", f2.read(SAMPLE.length))
    assert_equal("03: This is a line\n", f2.read(SAMPLE.length))

  ensure
    f1.close if f1
    f2.close if f2
  end

  def test_rewind
    f1 = File.new(@file)
    assert_equal("00: This is a line\n", f1.gets)
    assert_equal("01: This is a line\n", f1.gets)
    f1.rewind
    assert_equal("00: This is a line\n", f1.gets)

    f1.readlines
    assert_nil(f1.gets)
    f1.rewind
    assert_equal("00: This is a line\n", f1.gets)

    f1.close
  end

  def test_seek
    nums = [ 5, 8, 0, 1, 0 ]

    File.open(@file, "rb") do |file|
      file.seek(999, IO::SEEK_SET)
      assert_nil(file.gets)
      #
      # Errno::EFBIG added below for FreeBSD, because of
      # fseeko(3) behaviour there.
      #
      assert_raise(Errno::EFBIG, Errno::EINVAL, SystemCallError) {
        file.seek(-1)
      }
      for pos in nums
        assert_equal(0, file.seek(LINE_LENGTH*pos))
        line = file.gets
        assert_equal(pos, line[0..1].to_i)
      end
    end

    nums = [5, -2, 4, -7, 0 ]
    File.open(@file) do |file|
      count = -1
      file.seek(0)
      for pos in nums
        assert_equal(0, file.seek(LINE_LENGTH*pos, IO::SEEK_CUR))
        line = file.gets
        count = count + pos + 1
        assert_equal(count, line[0..1].to_i)
      end
    end

    nums = [ 5, 8, 1, 10, 1 ]

    File.open(@file) do |file|
      file.seek(0)
      for pos in nums
        assert_equal(0, file.seek(-LINE_LENGTH*pos, IO::SEEK_END))
        line = file.gets
        assert_equal(10-pos, line[0..1].to_i)
      end
    end
  end

  # Stat is pretty much tested elsewhere, so we're minimal here
  # Excluded due to JRUBY-2665
  def XXXtest_stat
    io = IO.new($stdin.fileno)
    assert_instance_of(File::Stat, io.stat)
    io.close
  end

  def test_sync
    $stderr.sync = false
    assert(!$stderr.sync)
    $stderr.sync = true
    assert($stderr.sync)
  end

  
  def test_sync=()
    unless WIN32
      read, write = IO.pipe
      write.sync = false
      write.print "hello"
      assert_nil(select([read], nil,  [read], 0.1))
      write.sync = true
      write.print "there"
      assert_equal([[read],[],[]], select([read], nil,  [read], 0.1))
      read.close
      write.close
    end
  end

  def test_sysread
    File.open(@file) do |file|
      assert_equal("", file.sysread(0))
      assert_equal("0", file.sysread(1))
      assert_equal("0:", file.sysread(2))
      assert_equal(" Thi", file.sysread(4))
      rest = file.sysread(100000)
      assert_equal(SAMPLE.length*10 - (1+2+4), rest.length)
      assert_raise(EOFError) { file.sysread(1) }
    end
  end

  def test_syswrite
    File.open(@file, "w") do |file|
      file.syswrite ""
      file.syswrite "hello"
      file.syswrite 1
      file.syswrite "\n"
    end

    File.open(@file) do |file|
      assert_equal("hello1\n", file.gets)
    end
  end

  # see also pos
  def test_tell
    pos = 0
    File.open(@file, "rb") do |file|
      assert_equal(0, file.tell)
      while (line = file.gets)
        pos += line.length
        assert_equal(pos, file.tell)
      end
    end
  end

  def test_to_i
    assert_equal(0, $stdin.to_i)
    assert_equal(1, $stdout.to_i)
    assert_equal(2, $stderr.to_i)
  end

  # see isatty
  def test_tty?
    File.open(@file) { |f|  assert(!f.tty?) }
    if WIN32 
      File.open("con") { |f| assert(f.tty?) }
    end
    unless WIN32
      begin
        File.open("/dev/tty") { |f| assert(f.isatty) }
      rescue
        # Can't open from crontab jobs
      end
    end
  end

  def test_ungetc
    File.open(@file) do |file|
      assert_equal(?0, file.getc)
      assert_equal(?0, file.getc)
      assert_equal(?:, file.getc)
      assert_equal(?\s, file.getc)
      assert_nil(file.ungetc(?:))
      assert_equal(?:, file.getc)
      1 while file.getc
      assert_nil(file.ungetc(?A))
      assert_equal(?A, file.getc)
    end
  end

  def test_write
    File.open(@file, "w") do |file|
      assert_equal(10, file.write('*' * 10))
      assert_equal(5,  file.write('!' * 5))
      assert_equal(0,  file.write(''))
      assert_equal(1,  file.write(1))
      assert_equal(3,  file.write(2.30000))
      assert_equal(1,  file.write("\n"))
    end
    
    File.open(@file) do |file|
      assert_equal("**********!!!!!12.3\n", file.gets)
    end
  end
end
