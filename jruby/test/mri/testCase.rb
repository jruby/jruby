require 'test/minirunit'

test_check "case"

case 5
when 1, 2, 3, 4, 6, 7, 8
  test_ok(false)
when 5
  test_ok(true)
end

case 5
when 5
  test_ok(true)
when 1..10
  test_ok(false)
end

case 5
when 1..10
  test_ok(true)
else
  test_ok(false)
end

case 5
when 5
  test_ok(true)
else
  test_ok(false)
end

case "foobar"
when /^f.*r$/
  test_ok(true)
else
  test_ok(false)
end


