require 'test/minirunit'
test_check "Test fixnums:"

test_equal(Fixnum,(Fixnum.induced_from 2).class)
test_equal(0,(Fixnum.induced_from 0.9))

test_no_exception{10.to_sym}

test_exception(ArgumentError){10.to_s(-1)}
test_exception(ArgumentError){10.to_s(37)}
test_equal("1010",10.to_s(2))
test_exception(RangeError){10.to_s 100**100}

test_equal((10.div 4),2)
test_equal((10.div 4.0).class,Fixnum) # awesome
test_equal((10 / 4.0),2.5)
test_equal((10 / 4).class,Fixnum)

test_equal(10.modulo(4).class,Fixnum)
test_equal(10.modulo(4.0).class,Float)

test_equal(1.divmod(2),[0,1])

test_equal((2 ** 100).class,Bignum)
test_equal((2 ** 100.0).class,Float)

test_equal(1==1.0,true)

test_equal(2 <=> 1,1)
test_equal(1 <=> 2,-1)
test_equal(2 <=> 2,0)
test_equal(1 <=> 1/0.0,-1)
test_equal(1 <=> -1/0.0,1)
test_equal(1 <=> 0/0.0,nil)
test_equal(1 <=> 2,-1)

test_equal(1.0.eql?(1),false)

test_equal(1234&4321,192)
big = 123**123
test_equal(1234&big,1090)
test_equal((1234&big).class,Fixnum)
test_equal((1234|big).class,Bignum)
test_equal((1234^big).class,Bignum)

test_equal(1234[0],0)
test_equal(1234[1],1)
test_equal(1234[-1],0)
test_equal(-1234[0],0)
test_equal(-1234[1],1)
test_equal(-1234[-1],0)

test_equal(1234 << -5,38)
test_equal(1234 >> -5,39488)

test_equal(1.to_f.class,Float)
test_equal(1.zero?,false)
test_equal(0.nonzero?,nil) # awesome

test_equal(0,1*0)

h = 2
test_equal(3, h +1)
