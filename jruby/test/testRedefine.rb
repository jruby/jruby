require 'test/minirunit'
test_check "Test redefinition of methods:"

def a() yield end
test_equal('x', a { 'x' })
def a() yield 'y' end
test_equal('y', a {|e| e})
