require 'minirunit'
test_check "Test Blocks:"
values = []
5.times do |i|
   values.push i
end

test_ok([0,1,2,3,4] == values)
values = []
2.step 10, 2 do |i|
   values.push i
end

test_ok([2,4,6,8,10] == values)

