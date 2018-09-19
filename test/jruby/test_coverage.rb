require 'test/unit/testcase'
require 'coverage'

class TestCoverage < Test::Unit::TestCase
  def test_coverage_handles_null_filename # jruby/jruby#5099
    Coverage.start
    JRuby.runtime.executeScript('1 + 1', nil)
    assert_nothing_raised { Coverage.result }
  end
end

