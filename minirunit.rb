
$testnum=0
$ntest=0
$failed = []
$curtestOK=true

def test_check(what)
  printf "\n%s :", what
  $what = what
  $testnum = 0
end

def test_ok(cond, msg="")
  $testnum+=1
  $ntest+=1
  if cond
	#    printf "ok %d\n", $testnum
	print "."
  else
    where = caller.reject {|where| where =~ /minirunit/}[0]
	#printf "not ok %s %d -- %s\n", $what, $testnum, where
	#$failed+=1 
    $failed.push(sprintf("not ok %s %d %s-- %s\n", $what, $testnum, msg, where))
	print "F"
	$curtestOK=false
  end
end

def test_equal(a,b)
 test_ok(a == b, "expected #{a.inspect}, found #{b.inspect}") 
end

def test_exception(type=Exception, &proc)
  raised = false
  begin
    proc.call
  rescue type
    raised = true
  end
  test_ok(raised, "#{type} expected")
end

def test_print_report
  puts
  puts "-" * 80
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
