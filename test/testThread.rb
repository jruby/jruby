require 'minirunit'
test_check "Test Thread:"
aProc = proc { 
	$toto = 1
}
thread = Thread.new &aProc
thread.join
test_equal(1, $toto)
