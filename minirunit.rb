
$testnum=0
$ntest=0
$failed = []
$curtestOK=true

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
	$curtestOK=false
  end
end

def test_print_report
  $failed.each { |error| puts error}
  puts "-" * 80
  puts "Tests: #$ntest. (Ok: #{$ntest - $failed.size}; Failed: #{$failed.size})"
end

def test_load(test)
  begin
	$curtestOK=true
	load(test)
  rescue Exception => boom
	puts 'KO'
	$failed.push(sprintf("exception raised %s %d -- \n\tException: %s\n\t%s", $what, $testnum, boom.to_s, boom.backtrace.join "\n\t"))
  else
	if $curtestOK
		puts 'OK'
	else
		puts 'KO'
	end
  end
end
