require 'minirunit'
test_check "Test Global Vars:"

$output = "a global variable."
def testGlobalVariable
    test_ok("a global variable." == $output)
end
testGlobalVariable

