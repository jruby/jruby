require 'test/minirunit'
test_check "Test Range"
values = []

(5..10).each do |i|
    values.push i
end
test_ok([5,6,7,8,9,10] == values)
