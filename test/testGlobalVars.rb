require 'test/minirunit'

test_check "Test Global Vars"

$output = "a global variable."
def testGlobalVariable
    test_ok("a global variable." == $output)
end
testGlobalVariable

$bar = 5
alias $foo $bar
test_ok($foo == 5)

$bar = 10
test_ok($foo == 10)

$foo = 5
test_ok($bar == 5)

test_equal("UTF8", $KCODE)

# Make last test so we don't have safety mucking with other tests
test_exception(SecurityError) { $SAFE = 3; $SAFE = 2 }

# Make sure ENV exists (TODO: add tests for env var support, once it works)
test_equal(nil, ENV['BOGUS_VARIABLE'])
