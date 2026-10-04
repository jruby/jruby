require 'test/unit'
require 'test/jruby/test_helper'
require 'jruby'
require 'tempfile'

# The OpenFile flags behind IO text mode: on Windows an IO opened without binmode is in
# text mode (CRLF on write, universal newline on read) and binmode leaves it.
class TestIOTextMode < Test::Unit::TestCase
  include TestHelper

  def setup
    @file = Tempfile.new('test_io_text_mode')
    @path = @file.path
  end

  def teardown
    @file.close!
  end

  def open_file(io)
    JRuby.reference(io).getOpenFile
  end

  def with_open(*args, **opts)
    File.open(@path, *args, **opts) { |io| yield open_file(io) }
  end

  def test_binmode_flags_do_not_depend_on_the_platform
    with_open("wb") { |of| assert_true of.binmode?; assert_false of.text_mode? }
    with_open("rb") { |of| assert_true of.binmode?; assert_false of.text_mode? }
    with_open("w", binmode: true) { |of| assert_true of.binmode?; assert_false of.text_mode? }
    with_open("w") { |of| assert_false of.binmode? }
  end

  # A pipe's write end never converts newlines, on Windows included (MRI writes pipes raw).
  def test_pipe_write_end_does_not_convert_newlines
    r, w = IO.pipe
    w.write "a\nb\n"
    w.close
    r.binmode
    assert_equal "a\nb\n", r.read
  ensure
    r.close
    w.close unless w.closed?
  end

  def test_binmode_leaves_text_mode
    File.open(@path, "w") do |io|
      io.binmode
      of = open_file(io)
      assert_true of.binmode?
      assert_false of.text_mode?
    end
  end

  if WINDOWS
    def test_default_mode_is_text_mode_on_windows
      with_open(File::RDONLY | File::BINARY) { |of| assert_true of.binmode?; assert_false of.text_mode? }
      with_open("w") { |of| assert_true of.text_mode? }
      with_open("r") { |of| assert_true of.text_mode? }
      with_open("r+") { |of| assert_true of.text_mode? }
      with_open(File::RDWR) { |of| assert_true of.text_mode? }
      assert_true open_file($stdout).text_mode?
      assert_true open_file($stdin).text_mode?
    end

    def test_text_mode_converts_newlines_on_windows
      File.open(@path, "w") { |io| io.write "a\nb\n" }
      assert_equal "a\r\nb\r\n", File.binread(@path)
      assert_equal "a\nb\n", File.read(@path)
      File.open(@path, File::RDWR) do |io|
        io.write "x\n"
        io.rewind
        assert_equal "x\n", io.gets
        assert_equal "b\n", io.gets
      end
    end

    def test_pipe_ends_are_in_text_mode_on_windows
      r, w = IO.pipe
      assert_true open_file(r).text_mode?
      assert_true open_file(w).text_mode?
      w.write "a\nb\n"
      w.close
      assert_equal ["a\n", "b\n"], r.readlines
    ensure
      r.close
      w.close unless w.closed?
    end
  else
    def test_default_mode_is_not_text_mode_off_windows
      with_open("w") { |of| assert_false of.text_mode? }
      with_open("r") { |of| assert_false of.text_mode? }
      assert_false open_file($stdout).text_mode?
    end

    def test_no_newline_conversion_off_windows
      File.open(@path, "w") { |io| io.write "a\nb\r\n" }
      assert_equal "a\nb\r\n", File.binread(@path)
      assert_equal "a\nb\r\n", File.read(@path)
    end
  end
end
