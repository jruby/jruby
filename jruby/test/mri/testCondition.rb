require 'test/minirunit'

test_check "condition"

$x = '0';

$x == $x && test_ok(true)
$x != $x && test_ok(false)
$x == $x || test_ok(false)
$x != $x || test_ok(true)


