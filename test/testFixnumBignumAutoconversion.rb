require 'test/minirunit'
test_check "Test fixnum to bignum autoconversion:"

if RUBY_PLATFORM =~ /java/

    MAX = 9223372036854775807 # don't trust << and minus yet
    MIN = -9223372036854775808 # only lexer tested

    test_equal(MAX.class,Fixnum)
# FIXME: Broken with Negation optimization in parser.  An important question worth asking is 
# whether having this boundary honored is important from a compatibility standpoint?
#    test_equal(MIN.class,Fixnum)

    # no bignorm in Fixnum#-,+
    test_equal((MAX+1).class,Bignum)
    test_equal((MAX-(-1)).class,Bignum)

    test_equal((MIN+(-1)).class,Bignum)
    test_equal((MIN-1).class,Bignum)

    test_equal((MAX&1234).class,Fixnum)

    test_equal((MAX/2)*2+1,MAX)
    test_equal((MIN/2)*2,MIN)
    test_equal(((MAX/2)*2+1).class,Fixnum)
    test_equal(((MIN/2)*2).class,Fixnum)
    test_equal(MAX|MIN,-1)
    test_equal(MAX^MIN,-1)
    test_equal(MAX&MIN,0)

    test_equal((MAX << 1).class,Bignum)
    test_equal((MIN << 1).class,Bignum)

    BMAX = 9223372036854775808
    BMIN = -9223372036854775809

    test_equal(BMAX.class,Bignum)
    test_equal(BMIN.class,Bignum)

    test_equal((BMAX+(-1)).class,Fixnum)
    test_equal((BMAX-1).class,Fixnum)
    test_equal((BMIN+1).class,Fixnum)
    test_equal((BMIN-(-1)).class,Fixnum)
    
    test_equal((BMAX >> 1).class,Fixnum)
    test_equal((BMIN >> 1).class,Fixnum)

end
