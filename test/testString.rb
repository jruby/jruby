require 'test/minirunit'
test_check "Test string:"

#########    test1   #################
testcase='toto'
test_ok(1 == idx = testcase.index('o'))
test_ok(3 == testcase.index('o',idx.succ))
test_ok(3 == idx = testcase.rindex('o'))
test_ok(1 == testcase.rindex('o', idx-1))

test_equal(2, "\001\001\000".sum)

test_equal(["1", "2", "3"], "1x2x3".split('x'))
test_equal(["1", "2", "3"], "1   2     3".split(' '))
test_equal(["1", "2", "3"], "1 2 3".split())
test_equal(["1", "2", "3"], "1x2y3".split(/(x|y)/))
test_equal(["foo"], "foo".split("whatever", 1))

test_equal("hihihi", "hi" * 3)
