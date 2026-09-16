require 'test/unit/testcase'
require 'coverage'
require 'tmpdir'

class TestCoverage < Test::Unit::TestCase
  def test_coverage_handles_null_filename # jruby/jruby#5099
    Coverage.start
    JRuby.runtime.executeScript('1 + 1', nil)
    assert_nothing_raised { Coverage.result }
  end

  def test_lone_statement_in_interpolation_keeps_earlier_statement_on_its_line
    assert_equal [1, 1, 1], line_coverage(<<~'RUBY')
      def foo(*) = nil
      foo([1].map {
        y = 1; nil }, "#{3}")
    RUBY
  end

  def test_several_statements_in_interpolation_each_cover_their_line
    assert_equal [1, 1, 1, nil], line_coverage(<<~'RUBY')
      x = "#{
        a = 1
        a
      }"
    RUBY
  end

  def test_call_on_a_later_line_keeps_the_statement_count
    assert_equal [1, 1, nil, 1, nil], line_coverage(<<~'RUBY')
      o = Struct.new(:b).new
      o.b =
        [].size
      o.b.to_s.concat(
        [].size.to_s)
    RUBY
  end

  def test_statement_counts_on_the_line_of_its_first_instruction
    assert_equal [nil, 1, nil, 1, nil, nil, nil, 1, 1], line_coverage(<<~'RUBY')
      x =
        [].size
      y = <<~EOS
        #{[].size}
        #{[].size}
      EOS
      z = {
        a: [].size }
      x = y
    RUBY
  end

  def test_literal_array_and_hash_count_on_their_first_line
    assert_equal [1, nil, nil, 1, nil, nil], line_coverage(<<~'RUBY')
      x = [
        1,
        2]
      y = {
        a: :b,
        c: 1.0}
    RUBY
  end

  def test_statement_in_begin_counts_once
    assert_equal [1, 1, nil, 1, nil, 1, nil, nil, 1], line_coverage(<<~'RUBY')
      def g
        x = 1
        begin
          [].size
        ensure
          x = 2
        end
      end
      g
    RUBY
  end

  def test_conditional_in_interpolation_counts
    assert_equal [1, nil, 1, nil, 1], line_coverage(<<~'RUBY')
      x = <<~EOS
        abc
        #{[].empty? ? "a" : "b"}
      EOS
      x = x
    RUBY
  end

  private

  def line_coverage(code)
    Dir.mktmpdir do |dir|
      path = File.join(File.realpath(dir), 'covered.rb')
      File.write(path, code)
      Coverage.start(lines: true)
      load path
      Coverage.result.fetch(path)[:lines]
    end
  end
end
