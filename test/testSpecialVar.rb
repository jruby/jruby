require 'minirunit'
test_check "Test Special Variables and constants:"
ARGV.each{|test| test}			#no exception should occur
test_equal(Array, ARGV.class)

