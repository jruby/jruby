require 'test/minirunit'
test_check "Test Time:"

t1 = Time.at(981173106)
t2 = Time.utc(2001, 2, 3, 4, 5, 6)
# Sat Feb 03 13:05:06 UTC 2001
t3 = Time.at(981205506)
t4 = Time.local(2001, 2, 3, 4, 5, 6)
utc_offset_hours = t4.utc_offset.to_f / 3600

test_equal(true, t1 == t2)
test_equal(true, t1 === t2)
test_equal(false, t1.equal?(t2))
test_equal(0, t1 <=> t2)

test_equal(utc_offset_hours == -9.0, t3 == t4)
test_equal(utc_offset_hours == -9.0, t3 === t4)
test_equal(false, t3.equal?(t4))
test_equal(utc_offset_hours <=> -9.0, t3 <=> t4)

t = Time.at(0.5)
test_equal(0, t.tv_sec)
test_equal(500_000, t.tv_usec)

t = Time.at(0.1)
test_equal(0, t.tv_sec)
test_equal(100_000, t.tv_usec)

t = Time.at(0.9)
test_equal(0, t.tv_sec)
test_equal(900_000, t.tv_usec)

# Time floors floating point values if explicit usecs provided (odd)
t = Time.at(0.5, 500)
test_equal(0, t.tv_sec)
test_equal(500, t.tv_usec)

t = Time.at(0.1, 500)
test_equal(0, t.tv_sec)
test_equal(500, t.tv_usec)

t = Time.at(0.9, 500)
test_equal(0, t.tv_sec)
test_equal(500, t.tv_usec)

# test comparison with nil
t = Time.now
test_equal(nil, t == nil) # not with 1.9

# Time.utc can accept float values (by turning them into ints)
test_no_exception { Time::utc(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0) }
test_exception(ArgumentError) { Time::local(2**30, 11, 11, 11, 11, 11, 0) }

# Test getgm/local/utc methods
local = Time.now
gmt = local.getgm
local2 = gmt.getlocal
utc = local2.getutc
test_equal(local, local2)
test_equal(gmt, utc)

# time with usecs (JRUBY-1971)
local = Time.local(2008, 12, 30, 5, 30, 0, 100)
gmt = local.getgm
local2 = gmt.getlocal
test_equal(local, local2)

test_exception { Time::utc(nil,nil,nil,nil,nil,nil,0) }

# Sat Jan  1 14:58:42 2000
t = Time.local(2000,1,1,14,58,42)

#From bfts
stest = {
  '%a' => 'Sat',
  '%A' => 'Saturday',
  '%b' => 'Jan',
  '%B' => 'January',
  '%C' => '20',
  '%e' => ' 1',
  '%d' => '01',
  '%G' => '1999',
  '%h' => 'Jan',
  '%H' => '14',
  '%I' => '02',
  '%j' => '001',
  '%m' => '01',
  '%n' => "\n",
  '%M' => '58',
  '%p' => 'PM',
  '%P' => 'pm',
  '%S' => '42',
  '%t' => "\t",
  '%u' => '6',
  '%U' => '00',
  '%V' => '52',
  '%W' => '00',
  '%w' => '6',
  '%y' =>  '00',
  '%Y' =>  '2000',
  '%%' =>  '%',
}

stest.each {|flag,val|
  test_equal("Got "+val,t.strftime("Got " + flag))
}

# Time initialize and allocation should be handled correctly
# This test case used to raise an argument exception on the second line
class MyTime < Time; def initialize(v, *args) super(*args); @v = v; end end
test_no_exception {MyTime.new(10)}

tt = Time.new
test_equal tt.strftime("%Y-%m-%d"),tt.strftime("%F")

test_equal "12:00AM", Time.utc(2007,01,01,0,0).strftime("%I:%M%p")
test_equal "12:00PM", Time.utc(2007,01,01,12,0).strftime("%I:%M%p")

# MRI only accepts usec arguments that fit in signed int
test_exception {Time.utc(2001,2,3,4,5,6,0x80000000)}
test_exception {Time.utc(2001,2,3,4,5,6,-0x80000001)}

# Time.utc accepts 8 arguments for compatibility with parsedate (two last arguments are ignored)
test_no_exception {Time.utc(2007, 10, 10, 11, 55, 23, "-0200", 3)}

# Time.utc accepts negative usec and rolls over
test_equal 999999,  Time.utc(2007, 10, 10, 11, 55, 23, -1).usec

# Follow MRI year conversion logic
test_equal 2000, Time.utc(0).year
test_equal 1969, Time.utc(69).year
test_equal 2038, Time.utc(38).year
test_equal 2038, Time.utc(138).year
test_equal 1902, Time.utc(1902).year

# JRUBY-3815
test_equal("2000年01月01日(Sat) 14時58分42秒", t.strftime("%Y年%m月%d日(%a) %H時%M分%S秒"))
# JRUBY-3550
test_equal("Thursday csütörtök", Time.mktime(2009,1,1,0,0).strftime("%A csütörtök"))

test_exception {Time.mktime(2009,1,1,0,0).strftime(12345)}

=begin Disabled, see http://jira.codehaus.org/browse/JRUBY-3079
old_tz = ENV['TZ']
begin
  ENV['TZ']='Europe/Helsinki'
  test_equal "+0300 EEST", Time.now.strftime('%z %Z')
  ENV['TZ']='America/New_York'
  test_equal "-0400 EDT", Time.now.strftime('%z %Z')
ensure
  ENV['TZ'] = old_tz
end
=end

# JRUBY-3873
test_equal "12:00AM%", Time.utc(2007,01,01,0,0).strftime("%I:%M%p%")

test_equal ' 0', Time.utc(2000,1,1).strftime("%k")
test_equal '7', Time.utc(2000,1,2).strftime("%u") # Sunday
test_equal '0', Time.utc(2000,1,2).strftime("%w") # Sunday
test_equal '53', Time.utc(2009,12,31).strftime("%V")
test_equal '0', Time.utc(1970,1,1).strftime("%s")