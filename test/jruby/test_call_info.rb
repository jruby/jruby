require 'test/unit'
require 'tempfile'
require 'stringio'

# Tests for ThreadContext#callInfo not leaking between method calls.
#
# When a method is invoked with keyword arguments, the IR sets a flag
# (CALL_KEYWORD) in ThreadContext#callInfo. If the receiving method does
# not properly reset callInfo, that flag "leaks" to the next call and
# causes incorrect keyword handling (ArgumentError, wrong defaults, etc.).
class TestCallInfo < Test::Unit::TestCase

  class KwargsReceiver
    attr_reader :a, :k
    def initialize(a, k:)
      @a = a
      @k = k
    end
  end

  # ── Hash#initialize ────────────────────────────────────────────────

  def test_hash_new_capacity_keyword_recognized
    # Hash.new(capacity: N) should treat capacity: as a sizing hint, not as the default value.
    h = Hash.new(capacity: 10)
    assert_nil h.default, "capacity: should be recognized as keyword, not default value"

    # If callInfo leaked, this would raise ArgumentError
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]

    h = Hash.new(42, capacity: 10)
    assert_equal 42, h.default

    # If callInfo leaked, this would raise ArgumentError
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Struct.new callInfo leak ───────────────────────────────────────

  def test_struct_new_with_keyword_init_does_not_leak
    Struct.new(:x, keyword_init: true)
    # If callInfo leaked, this would raise ArgumentError
    v = KwargsReceiver.new(1, k: 2)
    assert_equal 1, v.a
    assert_equal 2, v.k
  end

  def test_struct_new_with_empty_splat_does_not_leak
    opts = {}
    Struct.new(:y, **opts)
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Struct#initialize callInfo leak ────────────────────────────────

  def test_struct_initialize_1arg_empty_splat_does_not_leak
    s = Struct.new(:x)
    s.new(1, **{})
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_struct_initialize_2arg_empty_splat_does_not_leak
    s = Struct.new(:x, :y)
    s.new(1, 2, **{})
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_struct_initialize_3arg_empty_splat_does_not_leak
    s = Struct.new(:x, :y, :z)
    s.new(1, 2, 3, **{})
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_struct_initialize_keyword_init_works
    s = Struct.new(:a, :b, keyword_init: true)
    instance = s.new(a: 10, b: 20)
    assert_equal 10, instance.a
    assert_equal 20, instance.b

    # Verify no leak after keyword_init construction
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Regexp.linear_time? keyword handling ───────────────────────────

  def test_regexp_linear_time_with_timeout_keyword
    # timeout: must be recognized as keyword, not raise ArgumentError
    result = Regexp.linear_time?("abc", timeout: 1.0)
    assert_include [true, false], result

    # Verify no callInfo leak after keyword call
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_regexp_linear_time_with_regexp_and_timeout_keyword
    result = Regexp.linear_time?(/abc/, timeout: 1.0)
    assert_include [true, false], result

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_regexp_linear_time_no_leak_without_keyword
    Regexp.linear_time?("abc")
    # Non-keyword call should not affect subsequent keyword call
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Regexp#initialize keyword handling ───────────────────────────

  def test_regexp_initialize_with_timeout_keyword
    # 2-arg form: Regexp.new(source, timeout: val)
    re = Regexp.new("abc", timeout: 1.0)
    assert_match re, "abc"

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_regexp_initialize_3arg_with_timeout_keyword
    # 3-arg form: Regexp.new(source, flags, timeout: val)
    re = Regexp.new("abc", Regexp::IGNORECASE, timeout: 1.0)
    assert_match re, "ABC"

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── ARGF keyword handling ──────────────────────────────────────────

  def test_argf_readlines_chomp_keyword
    tmpfile = Tempfile.new('test_call_info_readlines')
    tmpfile.write("hello\nworld\n")
    tmpfile.close

    lines = File.open(tmpfile.path) { |f| f.readlines(chomp: true) }
    assert_equal ["hello", "world"], lines

    # Verify no callInfo leak
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  ensure
    tmpfile.unlink if tmpfile
  end

  def test_argf_gets_chomp_keyword
    tmpfile = Tempfile.new('test_call_info_gets')
    tmpfile.write("hello\nworld\n")
    tmpfile.close

    line = File.open(tmpfile.path) { |f| f.gets(chomp: true) }
    assert_equal "hello", line

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  ensure
    tmpfile.unlink if tmpfile
  end

  def test_argf_each_line_chomp_keyword
    tmpfile = Tempfile.new('test_call_info_each_line')
    tmpfile.write("hello\nworld\n")
    tmpfile.close

    lines = []
    File.open(tmpfile.path) { |f| f.each_line(chomp: true) { |l| lines << l } }
    assert_equal ["hello", "world"], lines

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  ensure
    tmpfile.unlink if tmpfile
  end

  # ── Kernel#open with to_open redirect ──────────────────────────────

  class ToOpenTarget
    def to_open(*args)
      StringIO.new("hello from to_open")
    end
  end

  def test_kernel_open_to_open_redirect_with_keywords
    io = open(ToOpenTarget.new, mode: "r")
    assert_equal "hello from to_open", io.read
    io.close

    # Verify no callInfo leak after to_open redirect
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_kernel_open_to_open_redirect_without_keywords
    io = open(ToOpenTarget.new)
    assert_equal "hello from to_open", io.read
    io.close

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── to_enum keyword propagation ────────────────────────────────────

  class EachWithKeyword
    def each(k:)
      yield k
    end
  end

  def test_to_enum_propagates_keywords
    enum = EachWithKeyword.new.to_enum(:each, k: 42)
    assert_equal [42], enum.to_a

    # Verify no callInfo leak
    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_to_enum_without_keywords_does_not_leak
    enum = [1, 2, 3].to_enum(:each)
    enum.to_a

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Data class keyword handling ────────────────────────────────────

  def test_data_new_with_keywords
    d = Data.define(:x, :y)
    instance = d.new(x: 1, y: 2)
    assert_equal 1, instance.x
    assert_equal 2, instance.y

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_data_new_single_member_with_keyword
    d = Data.define(:x)
    instance = d.new(x: 42)
    assert_equal 42, instance.x

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_data_with_keyword
    d = Data.define(:x, :y)
    original = d.new(x: 1, y: 2)
    updated = original.with(x: 10)
    assert_equal 10, updated.x
    assert_equal 2, updated.y

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Dir.glob / Dir[] keyword handling ──────────────────────────────

  def test_dir_glob_with_sort_keyword
    entries = Dir.glob("*", sort: true)
    assert_kind_of Array, entries

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_dir_aref_does_not_leak
    Dir["*"]

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Proc#parameters keyword handling ───────────────────────────────

  def test_proc_parameters_with_lambda_keyword
    pr = Proc.new { |a, k:| }
    params = pr.parameters(lambda: true)
    assert_equal [[:req, :a], [:keyreq, :k]], params

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_proc_parameters_without_keyword
    pr = Proc.new { |a, k:| }
    params = pr.parameters
    assert_include params, [:opt, :a]
    assert_include params, [:keyreq, :k]

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── Warning.warn keyword handling ──────────────────────────────────

  def test_warning_warn_with_category_keyword
    # Warning.warn 2-arg form accepts category: keyword
    assert_nothing_raised do
      Warning.warn("test callinfo message\n", category: :deprecated)
    end

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── IO.foreach keyword handling ────────────────────────────────────

  def test_io_foreach_chomp_keyword
    tmpfile = Tempfile.new('test_call_info_foreach')
    tmpfile.write("hello\nworld\n")
    tmpfile.close

    lines = []
    IO.foreach(tmpfile.path, chomp: true) { |l| lines << l }
    assert_equal ["hello", "world"], lines

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  ensure
    tmpfile.unlink if tmpfile
  end

  # ── File#initialize keyword handling ────────────────────────────────

  def test_file_open_with_mode_keyword
    tmpfile = Tempfile.new('test_call_info_file')
    tmpfile.write("file content")
    tmpfile.close

    f = File.open(tmpfile.path, mode: "r")
    assert_equal "file content", f.read
    f.close

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  ensure
    tmpfile.unlink if tmpfile
  end

  def test_file_open_with_fd_and_mode_keyword
    tmpfile = Tempfile.new('test_call_info_file_fd')
    tmpfile.write("fd content")
    tmpfile.close

    # Open via path first, then reopen via fd to exercise the fd delegation path
    original = File.open(tmpfile.path, "r")
    fd_file = IO.new(original.fileno, mode: "r", autoclose: false)
    assert_equal "fd content", fd_file.read
    fd_file.close
    original.close

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  ensure
    tmpfile.unlink if tmpfile
  end

  # ── NoMatchingPatternKeyError keyword handling ─────────────────────

  def test_no_matching_pattern_key_error_with_keywords
    err = NoMatchingPatternKeyError.new("test", matchee: { a: 1 }, key: :b)
    assert_equal "test", err.message
    assert_equal({ a: 1 }, err.matchee)
    assert_equal :b, err.key

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  def test_no_matching_pattern_key_error_without_keywords
    err = NoMatchingPatternKeyError.new("test")
    assert_equal "test", err.message

    v = KwargsReceiver.new(1, k: 2)
    assert_equal [1, 2], [v.a, v.k]
  end

  # ── General: keywords after non-keyword calls ──────────────────────

  def test_keywords_work_after_various_calls
    # Exercise a sequence of calls mixing keyword and non-keyword usage
    # to ensure callInfo is properly reset between calls.
    Hash.new(capacity: 10)
    v1 = KwargsReceiver.new(1, k: :a)
    assert_equal :a, v1.k

    [1, 2, 3].size  # non-keyword call
    v2 = KwargsReceiver.new(2, k: :b)
    assert_equal :b, v2.k

    Struct.new(:tmp, keyword_init: true)
    v3 = KwargsReceiver.new(3, k: :c)
    assert_equal :c, v3.k
  end
end
