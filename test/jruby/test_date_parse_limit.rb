require 'test/unit'
require 'date'
require 'timeout'

class TestDateOverflow < Test::Unit::TestCase
  def test_length_limit
    assert_raise(ArgumentError) { Date._parse("1" * 1000) }
    assert_raise(ArgumentError) { Date._iso8601("1" * 1000) }
    assert_raise(ArgumentError) { Date._rfc3339("1" * 1000) }
    assert_raise(ArgumentError) { Date._xmlschema("1" * 1000) }
    assert_raise(ArgumentError) { Date._rfc2822("1" * 1000) }
    assert_raise(ArgumentError) { Date._rfc822("1" * 1000) }
    assert_raise(ArgumentError) { Date._jisx0301("1" * 1000) }

    assert_raise(ArgumentError) { Date.parse("1" * 1000) }
    assert_raise(ArgumentError) { Date.iso8601("1" * 1000) }
    assert_raise(ArgumentError) { Date.rfc3339("1" * 1000) }
    assert_raise(ArgumentError) { Date.xmlschema("1" * 1000) }
    assert_raise(ArgumentError) { Date.rfc2822("1" * 1000) }
    assert_raise(ArgumentError) { Date.rfc822("1" * 1000) }
    assert_raise(ArgumentError) { Date.jisx0301("1" * 1000) }

    assert_raise(ArgumentError) { DateTime.parse("1" * 1000) }
    assert_raise(ArgumentError) { DateTime.iso8601("1" * 1000) }
    assert_raise(ArgumentError) { DateTime.rfc3339("1" * 1000) }
    assert_raise(ArgumentError) { DateTime.xmlschema("1" * 1000) }
    assert_raise(ArgumentError) { DateTime.rfc2822("1" * 1000) }
    assert_raise(ArgumentError) { DateTime.rfc822("1" * 1000) }
    assert_raise(ArgumentError) { DateTime.jisx0301("1" * 1000) }

    assert_raise(ArgumentError) { Date._parse("Jan " + "9" * 1000000) }
    assert_raise(Timeout::Error) { Timeout.timeout(1) { Date._parse("Jan " + "9" * 1000000, limit: nil) } }
  end
end
