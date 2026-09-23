require 'test/unit'
require 'test/jruby/test_helper'

# GH-9727 seek/pos/pread on (deflated) jar entries
class TestIOJarSeek < Test::Unit::TestCase
  include TestHelper

  JAR = File.expand_path('test_io_jar_seek.jar', __dir__)

  # same generator as the one used to build seek/lines.txt in the jar
  DATA = (1..20_000).map { |i| format("%05d %s\n", i, "abcdefghij"[0, i % 10]) }.join.freeze

  PATHS = [ "file:#{JAR}!/seek/lines.txt", 'uri:classloader:/seek/lines.txt', 'classpath:seek/lines.txt' ]

  def setup
    $CLASSPATH << JAR
  end

  def test_fixture
    each_path { |path| assert_equal DATA, File.binread(path), path }
  end

  def test_pos
    each_open do |f|
      f.read(3)
      assert_equal 3, f.pos
      f.gets
      assert_equal DATA.index("\n") + 1, f.pos
      f.read(100_000)
      assert_equal DATA.index("\n") + 1 + 100_000, f.pos
    end
  end

  def test_seek
    each_open do |f|
      assert_equal 0, f.seek(200_000)
      assert_equal 200_000, f.pos
      assert_equal DATA[200_000, 7], f.read(7)

      f.seek(10) # backwards
      assert_equal DATA[10, 5], f.read(5)

      f.seek(-5, IO::SEEK_CUR)
      assert_equal DATA[10, 5], f.read(5)

      f.seek(-3, IO::SEEK_END)
      assert_equal DATA[-3..], f.read
      assert f.eof?

      f.pos = 123
      assert_equal DATA[123, 2], f.read(2)

      f.rewind
      assert_equal DATA, f.read
    end
  end

  def test_seek_past_end
    each_open do |f|
      f.seek(DATA.size + 10)
      assert_equal DATA.size + 10, f.pos
      assert_equal '', f.read
      assert_nil f.read(1)
    end
  end

  def test_sysseek
    each_open do |f|
      assert_equal 1000, f.sysseek(1000)
      assert_equal DATA[1000, 3], f.sysread(3)
    end
  end

  def test_pread
    each_open do |f|
      f.read(3)
      assert_equal DATA[50, 10], f.pread(10, 50)
      assert_equal 3, f.pos # position not changed
      assert_equal DATA[3, 4], f.read(4)

      assert_equal DATA[150_000, 70_000], f.pread(70_000, 150_000)
      assert_equal DATA[10, 5], f.pread(5, 10) # backwards
      assert_equal DATA[-4..], f.pread(10, DATA.size - 4)
      assert_raise(EOFError) { f.pread(1, DATA.size) }
    end
  end

  def test_pread_after_buffered_read
    each_open do |f|
      line = f.readline
      assert_equal DATA[0, 5], f.pread(5, 0)
      assert_equal DATA[line.size, 5], f.read(5)
    end
  end

  private

  def each_path(&block) = PATHS.each(&block)

  def each_open
    each_path { |path| File.open(path, 'rb') { |f| yield f } }
  end

end
