
$silentTests = false
$testnum=0
$ntest=0
$failed = []
$curtestOK=true

module MiniRUnit
  class Failure
    def initialize(what, testnum, msg, where)
      @what, @testnum, @msg, @where = what, testnum, msg, where
    end

    def to_s
      sprintf("FAILED %s %d %s-- %s\n", @what, @testnum, @msg, @where)
    end
  end

  class Error
    def initialize(what, testnum, boom)
      @what, @testnum, @boom = what, testnum, boom
    end

    def to_s
      sprintf("EXCEPTION raised %s %d -- \n\tException: %s\n\t%s",
              @what, @testnum, @boom.to_s, @boom.backtrace.join("\n\t"))
    end
  end
end


def test_check(what)
  printf "%s : ", what unless $silentTests
  $what = what
  $testnum = 0
end

def test_ok(cond, msg="")
  $testnum+=1
  $ntest+=1
  if cond
    print "." unless $silentTests
  else
    where = caller.reject {|where| where =~ /minirunit/}[0]
    $failed.push(MiniRUnit::Failure.new($what, $testnum, msg, where))
    print "F" unless $silentTests
    $curtestOK=false
  end
end

def test_fail(msg="")
  test_ok(false, msg)
end

def test_equal(a,b)
 test_ok(a == b, "expected #{a.inspect}, found #{b.inspect}") 
end

def test_no_exception(&proc)
  raised = false
  begin
    proc.call
  rescue exception
    raised = x
  end
  test_ok(!raised, "unexpected exception #{raised}")	
end

def test_exception(type=Exception, &proc)
  raised = false
  begin
    proc.call
  rescue type=>e
    raised = true
  end
  test_ok(raised, "#{type} expected")
  e
end

def test_get_last_failed
  if $failed.empty?
    return nil
  end
  return $failed.last
end

def test_print_report
  puts
  puts "-" * 80
  $failed.each { |error| puts error }
  puts "-" * 80
  puts "Tests: #$ntest. (Ok: #{$ntest - $failed.size}; Failed: #{$failed.size})"
end

def test_load(test)
  begin
	$curtestOK=true
	load(test)
  rescue Exception => boom
	puts 'ERROR' unless $silentTests
	$failed.push(MiniRUnit::Error.new($what, $testnum, boom))
  else
	if $curtestOK
		puts 'OK' unless $silentTests
	else
		puts 'FAILED' unless $silentTests
	end
  end
end
