require 'minirunit'
#test backtrace
if !(defined? $recurse)
	test_check "Test Exception"
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
		trace.sub!(/^.:/,'')
		res = trace.index(':')
		#		puts "res #{res}"
		res = res.succ
		resend = trace.index(':',res)
		#		puts "resend #{resend}"
		if resend
			trace[res, resend-res].to_i  #return value from block
		else
			trace[res, trace.length].to_i  #return value from block
		end
	}
	#the slice is so the stack checked is the same if the test is run
	#by itself or as part of a testSuite
	test_equal([10,13,13] , result[0..2])
	#	puts; puts boom.backtrace[0..2]  #debug statement
end

#test_print_report if $recurse 
$recurse = false
