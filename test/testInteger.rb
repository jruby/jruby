require 'test/minirunit'
test_check "Test integers:"

test_exception(NoMethodError){Integer.new}
test_exception(TypeError){Integer.allocate} rescue NoMethodError # allocate throws TypeError!

test_equal(true,1.integer?)

# for Fixnum operations - fast version

a = 0
10.times do |i|
    a+=i
end

test_equal(a,45)

a = 0
10.upto(20) do |i|
    a+=i
end

test_equal(a,165)

a = 0
20.downto(10) do |i|
    a+=i
end

test_equal(a,165)

test_equal(0.next,1)

# for Bignum operations - slow version

big = 10000000000000000000

test_equal(big.class,Bignum)

a = 0
big.times do |i|
    a+=i
    break if i > 10
end

test_equal(a,66)

a = 0
big.upto(big+10) do |i|
    a += i
end

test_equal(a,110000000000000000055)

a = 0
big.downto(big-10) do |i|
    a += i
end

test_equal(a,109999999999999999945)

test_equal(big.next,big + 1)

test_equal(1.chr,"\001")
test_equal(10.chr,"\n")

test_equal(1.to_i,1)
test_equal(10.to_i,10)

test_exception(TypeError){Integer.induced_from "2"}
test_equal(Fixnum,Integer.induced_from(2.0).class)
test_equal(Bignum,Integer.induced_from(100**100).class)

class Foo
  def to_i
    nil
  end
end

test_exception(TypeError){Integer(Foo.new)}
