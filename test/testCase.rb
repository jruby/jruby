require 'test/minirunit'
test_check "case"

x = nil
case
when true
  x = 1
when false
  x = 2
end
test_equal(1, x)

x = nil
case
when false
  x = 1
when true
  x = 2
end
test_equal(2, x)

x = nil
case 10
when 1..3
  x = 'a'
when 4..8
  x = 'b'
when 9..22
  x = 'c'
else
  x = 'd'
end
test_equal('c', x)

x = nil
case 10
when 1
  x = 'a'
when 100
  x = 'b'
else
  x = 'c'
end
test_equal('c', x)
