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

a = false
a = true if 1..2
test_ok(a)

a = false
a = true if 1...2
test_ok(a)

a = false
a = true if 1.1..2.1
test_ok(a)

a = false
a = true if 1.1...2.1
test_ok(a)


