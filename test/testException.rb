require 'minirunit'
test_check "Test Exception:"
#test backtrace
if !(defined? $recurse)
	$recurse = false
end

begin
  if $recurse
	raise Exception, 'test'
  else
	$recurse=true
	load('test/testException.rb')
  end
rescue Exception => boom
  result =  boom.backtrace.collect {|trace| 
	res = trace.index(':')
	res = res.succ
	resend = trace.index(':',res)
	if resend
	  trace[res, resend-res].to_i  #return value from block
	else
	  trace[res, trace.length].to_i  #return value from block
	end
  }
  test_ok([10,13,13] == result)
end

