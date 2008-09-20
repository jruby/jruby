require 'test/minirunit'

class TestEnumerable
  include Enumerable
  def initialize(value)
    @value = value
  end
  def each(&block)
    @value.each(&block)
  end
end

test_equal nil, TestEnumerable.new([]).first
test_equal :foo, TestEnumerable.new([:foo]).first
test_equal [], TestEnumerable.new([[], 1, 2, 3]).first
test_equal [1], TestEnumerable.new([[1], 1, 2, 3]).first
test_equal [], TestEnumerable.new([]).first(0)

test_exception(ArgumentError) do
  TestEnumerable.new([]).first(-1)
end

test_equal [], TestEnumerable.new([1,2,3]).first(0)

test_exception(ArgumentError) do
  TestEnumerable.new([1,2,3]).first(-1)
end

test_equal [1], TestEnumerable.new([1,2,3]).first(1)
test_equal [1,2], TestEnumerable.new([1,2,3]).first(2)
test_equal [1,2,3], TestEnumerable.new([1,2,3]).first(3)
test_equal [1,2,3], TestEnumerable.new([1,2,3]).first(4)

test_equal({ }, TestEnumerable.new([]).group_by(&:length))
test_equal({5=>["pelle"], 6=>["marcus"], 3=>["ola", "tom"], 4=>["bini", "pele"], 10=>["gustafsson"]},
           TestEnumerable.new(%w(ola bini gustafsson pelle tom marcus pele)).group_by(&:length))
test_equal({[5]=>["pelle"], [6]=>["marcus"], [3]=>["ola", "tom"], [4]=>["bini", "pele"], [10]=>["gustafsson"]},
           TestEnumerable.new(%w(ola bini gustafsson pelle tom marcus pele)).group_by{ |v| [v.length]})
