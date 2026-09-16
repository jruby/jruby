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
