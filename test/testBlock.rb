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

values = []
[1,2,3].each {|v| values << v; break }
test_equal([1], values)

values = []
result = [1,2,3,4,5].collect {|v|
  if v > 2
    break
  end
  values << v
  v
}
test_equal([1,2], values)
test_ok(result.nil?)

test_print_report
