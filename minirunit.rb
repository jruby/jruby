
$testnum=0
$ntest=0
$failed = []

def test_check(what)
  printf "\n%s :", what
  $what = what
  $testnum = 0
end
def test_ok(cond)
  $testnum+=1
  $ntest+=1
  if cond
	#    printf "ok %d\n", $testnum
	print "."
  else
    where = caller[0]
	#printf "not ok %s %d -- %s\n", $what, $testnum, where
	#$failed+=1 
    $failed.push(sprintf("not ok %s %d -- %s\n", $what, $testnum, where))
	print "F"
  end
end
def test_print_report
  $failed.each { |error| puts error}
end
def test_load(test)
  begin
	load(test)
  rescue Exception => boom
	puts 'KO'
	$failed.push(sprintf("exception raised %s %d -- Exception: %s\n", $what, $testnum, boom.to_s))
  else
	puts 'OK'
  end
end
