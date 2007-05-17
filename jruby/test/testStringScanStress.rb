require 'test/minirunit'
test_check "Test string#scan stress test:"

# this test originally caused out of memory errors on the 0.9.0 release

mess="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghihklmnopqrstuvwxyz0123456789!+" #64
mess << mess #128
mess << mess #256
mess << mess #512
mess << mess #1024
mess << mess #2048
mess << mess #4096
mess << mess #8192  # failed on WinXPsp2 Sun1.5._05 here with mx256m, linux/osx need more
mess << mess #16000
mess << mess #32000 

# puts mess.size
# this test originally caused out of memory errors on the 0.9.0 release
# print "Expecting + got " + mess.scan(/./)[-1]
result = mess.scan(/./)[-1]
test_equal("+", result)

# this test originally caused out of memory errors on the 0.9.0 release
count = 0
my_arr = []
mess.scan(/./) do |c|
  # mri and jruby do not see the new value of mess being show in |c| within the block
  mess = "12345"
  my_arr << c
  count += 1
  # this shows E under mri and jruby
  test_equal("E", c) if count == 5
  # print "Expecting E got " + c if count == 5
end

