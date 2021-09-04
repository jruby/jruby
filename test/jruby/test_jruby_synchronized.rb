require 'test/unit'
require 'jruby/synchronized'

class TestJrubySynchronized < Test::Unit::TestCase
  # JRUBY-5168
  def test_to_s_on_synchronized_array
    a=[Object.new]
    a[0].extend(JRuby::Synchronized)
    assert_nothing_raised do
      a.to_s
    end
  end
end
