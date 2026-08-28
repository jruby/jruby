require 'test/unit'
require 'rbconfig'
require 'tempfile'

class TestLineEndings < Test::Unit::TestCase
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

  def temp_file(name, flags = "w", &block)
    @tmpfile = Tempfile.new name
    @tmpfile.close
    if block
      File.open(@tmpfile.path, flags, &block)
      File.new(@tmpfile.path)
    else
      File.open(@tmpfile.path, flags)
    end
  end

  def existing_file
    "test/test_line_endings#{WINDOWS ? '-windows' : ''}.txt"
  end

  def teardown
    @tmpfile.unlink if @tmpfile
  end

  def test_readlines_eats_carriage_returns
    lines = IO.readlines(existing_file)
    assert_equal ["here is line 1\n", "here is line 2\n"], lines
  end

  def test_gets_eats_carriage_returns
    File.open(existing_file) do |f|
      assert_equal "here is line 1\n", f.gets
      assert_equal "here is line 2\n", f.gets
    end
  end

  def test_puts_adds_carriage_returns_on_windows
    io = temp_file("puts-line-endings")
    io.puts "hi"
    io.close

    File.open(io.path, "rb") do |f|
      expected = "hi"
      expected << "\r" if WINDOWS
      expected << "\n"
      assert_equal expected, f.read
    end
  end

  def a_b_temp_file
    temp_file('foo2.txt') do |c|
      c.puts 'A'
      c.puts 'B'
    end
  end

  # JRUBY-61
  def test_puts_file_size
    f = a_b_temp_file
    size = 4
    size = 6 if WINDOWS
    assert_equal(size, File.size(f.path))
  end

  def test_each_byte_reads_crlf_in_binary_mode
    f = a_b_temp_file
    f.close
    f = File.new(f.path, "rb")
    count=-1
    contents = [65,13,10,66,13,10]
    contents.delete_if {|x| x == 13 && !WINDOWS}
    f.each_byte {|x| assert_equal(contents[count+=1], x)  }
    f.close
  end

  def test_each_byte_reads_lf_in_text_mode
    f = a_b_temp_file
    count=-1
    f.each_byte {|x| assert_equal([65,10,66,10][count+=1], x)  }
    f.close
  end

  def test_io_getc_reads_cr_in_binary_mode
    f = a_b_temp_file
    f.close
    f = File.new(f.path, "rb")
    count=0
    positions = [0,1,2,3,4,5]
    contents = [65,13,10,66,13,10]
    positions = positions[0..3] unless WINDOWS
    contents.delete_if {|x| x == 13 && !WINDOWS}
    while not f.eof?
      assert_equal(positions[count], f.pos)
      assert_equal(contents[count], f.getc)
      count += 1
    end
    f.close
  end

  def test_io_getc_eats_cr_in_text_mode
    f = a_b_temp_file
    count=0
    positions = [0,1,2,3]
    positions = [0,1,3,4] if WINDOWS
    contents = [65,10,66,10]
    while not f.eof?
      assert_equal(positions[count], f.pos)
      assert_equal(contents[count], f.getc)
      count += 1
    end
    f.close
  end
end
