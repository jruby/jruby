require 'test/minirunit'
test_check "Test Math:"

num = 0.987654321
num2 = 0.123456789
num3 = 1.23456789

class Float
    def mri_round
        ("%1.14f" % self).to_f
    end
end

test_equal(Math.atan2(num,num2),1.4464413333696737)

test_equal(Math.cos(num),0.5506493978619769)
test_equal(Math.sin(num),0.834736629503128)
test_equal(Math.tan(num),1.5159130886988805)

test_equal(Math.acos(num),0.1572969522594371)
test_equal(Math.asin(num),1.4134993745354596)
test_equal(Math.atan(num).mri_round,0.779187063150832.mri_round)

test_equal(Math.cosh(num),1.5286892059778199)
test_equal(Math.sinh(num).mri_round,1.1562398922685106.mri_round)
test_equal(Math.tanh(num).mri_round,0.7563603430619675.mri_round)

test_exception(Errno::EDOM){Math.acosh(num)}
test_equal(Math.acosh(num3).mri_round,0.6722071467083781.mri_round)
test_equal(Math.asinh(num),0.8726168749420165)
test_equal(Math.atanh(num),2.540702182995335)

test_equal(Math.exp(num),2.6849290982463305)
test_equal(Math.log(num),-0.012422519986057208)
test_equal(Math.log10(num).mri_round,-0.005395031881277506.mri_round)
test_equal(Math.sqrt(num),0.9938079900061179)
test_equal(Math.frexp(num),[0.987654321, 0])
test_equal(Math.ldexp(num,num2),0.987654321)
test_equal(Math.hypot(num,num2),0.99534046262581)
test_equal(Math.erf(num).mri_round,0.8375124813599835.mri_round)
test_equal(Math.erfc(num).mri_round,0.16248751864001654.mri_round)

nan = 0.0/0.0

test_exception(Errno::EDOM){Math.asin(nan)}
test_exception(Errno::EDOM){Math.acos(nan)}
test_exception(Errno::EDOM){Math.acosh(nan)}
test_exception(Errno::EDOM){Math.acosh(nan)}
test_exception(Errno::EDOM){Math.atanh(nan)}
test_exception(Errno::EDOM){Math.log(nan)}
test_exception(Errno::EDOM){Math.log10(nan)}
test_exception(Errno::EDOM){Math.sqrt(nan)}

test_exception(ArgumentError){Math.asin("")}
test_exception(ArgumentError){Math.cos("")}
test_exception(ArgumentError){Math.sqrt("")}
test_exception(ArgumentError){Math.exp("")}
