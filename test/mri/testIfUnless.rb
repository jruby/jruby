require 'test/minirunit'

test_check "if/unless";

$x = 'test';
test_ok(if $x == $x then true else false end)
$bad = false
unless $x == $x
  $bad = true
end
test_ok(!$bad)
test_ok(unless $x != $x then true else false end)
