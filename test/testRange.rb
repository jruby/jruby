require 'test/minirunit'
test_check "Test Range"
values = []

(5..10).each do |i|
    values.push i
end
test_ok([5,6,7,8,9,10] == values)

values = []
for i in 1 .. 3
  values << i
end
test_equal([1,2,3], values)

# Parser once choked on the range below
def testBreakWithInterval
  break unless 1..2
end
