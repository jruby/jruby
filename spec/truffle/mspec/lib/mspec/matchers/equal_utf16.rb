require 'mspec/helpers/encode'

class EqualUtf16Matcher
  def initialize(expected)
    @expected = Array(expected).map { |x| encode x, "binary" }
  end

  def matches?(actual)
    @actual = Array(actual).map { |x| encode x, "binary" }
    @actual == @expected || @actual == expected_swapped
  end

  def expected_swapped
    @expected_swapped ||= @expected.map { |x| x.to_str.gsub(/(.)(.)/, '\2\1') }
  end

  def failure_message
    ["Expected #{@actual.pretty_inspect}",
     "to equal #{@expected.pretty_inspect} or #{expected_swapped.pretty_inspect}"]
  end

  def negative_failure_message
    ["Expected #{@actual.pretty_inspect}",
     "not to equal #{@expected.pretty_inspect} nor #{expected_swapped.pretty_inspect}"]
  end
end

class Object
  def equal_utf16(expected)
    EqualUtf16Matcher.new(expected)
  end
end
