require 'test/minirunit'
test_check "Test string#gsub stress test:"

# this test originally caused out of memory errors on the 0.9.0 release 
mess="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghihklmnopqrstuvwxyz0123456789!+" #64
mess << mess #128
mess << mess #256
mess << mess #512
mess << mess #1024
mess << mess #2048
mess << mess #4096
mess << mess 
mess << mess 




# puts mess.size
result = mess.gsub(/(.)/,'(\1)')
test_equal("+", result[-2].chr)

# this test originally caused out of memory errors on the 0.9.0 release
count = 0
my_arr = []
result = mess.gsub(/./) do |m|
  my_arr << m     # this is what actually causes the OOM error - without this line its just very slow
  count += 1
  # test_equal("E\n", line) if count == 5
  "(#{m})"
  # print "Expecting E got " + c if count == 5
end
test_equal("+", result[-2].chr)
# puts count

