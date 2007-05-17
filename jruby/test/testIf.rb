require 'test/minirunit'
test_check "Test ifs:"
if (true)
  test_ok(true)
else
  test_ok(false)
end
if (FALSE | true)
  test_ok(true)
else
  test_ok(false)
end

