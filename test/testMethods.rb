require 'minirunit'
test_check "Test Methods:"

def testMethod
    $toto = true
    "some output"
end


test_ok("some output" == testMethod)
test_ok($toto)

