##################################################
# bench_rational.rb
#
# Benchmark suite for the Rational class.
##################################################
require "benchmark"
require "rational"

MAX = 30000
NUM = 27
DEN = 3

RAT1 = Rational(NUM, DEN)
RAT2 = Rational(DEN, NUM)
BIG  = 2**64

Benchmark.bm(25) do |x|
   x.report("Rational()"){
      MAX.times{ Rational(NUM, DEN) }
   }
   
   x.report("Rational.reduce"){
      MAX.times{ Rational.reduce(NUM, DEN) }
   }

   x.report("Rational.new!"){
      MAX.times{ Rational.new!(NUM, DEN) }
   }

   x.report("Rational#+(Rational)"){
      MAX.times{ RAT1 + RAT2 }
   }

   x.report("Rational#+(Integer)"){
      MAX.times{ RAT1 + 30 }
   }

   x.report("Rational#+(Float)"){
      MAX.times{ RAT1 + 30.37 }
   }

   x.report("Rational#-(Rational)"){
      MAX.times{ RAT1 - RAT2 }
   }

   x.report("Rational#-(Integer)"){
      MAX.times{ RAT1 - 30 }
   }

   x.report("Rational#-(Float)"){
      MAX.times{ RAT1 - 30.37 }
   }

   x.report("Rational#*(Rational)"){
      MAX.times{ RAT1 * RAT2 }
   }

   x.report("Rational#*(Integer)"){
      MAX.times{ RAT1 * 30 }
   }

   x.report("Rational#*(Float)"){
      MAX.times{ RAT1 * 30.37 }
   }

   x.report("Rational#/(Rational)"){
      MAX.times{ RAT1 / RAT2 }
   }

   x.report("Rational#/(Integer)"){
      MAX.times{ RAT1 / 30 }
   }

   x.report("Rational#/(Float)"){
      MAX.times{ RAT1 / 30.37 }
   }

   x.report("Rational#**(Rational)"){
      MAX.times{ RAT1 ** RAT2 }
   }

   x.report("Rational#*(Integer)"){
      MAX.times{ RAT1 ** 30 }
   }

   x.report("Rational#*(Float)"){
      MAX.times{ RAT1 ** 30.37 }
   }

   x.report("Rational#%(Rational)"){
      MAX.times{ RAT1 % RAT2 }
   }

   x.report("Rational#%(Integer)"){
      MAX.times{ RAT1 % 30 }
   }

   x.report("Rational#%(Float)"){
      MAX.times{ RAT1 % 30.37 }
   }

   x.report("Rational#divmod(Rational)"){
      MAX.times{ RAT1.divmod RAT2 }
   }

   x.report("Rational#divmod(Integer)"){
      MAX.times{ RAT1.divmod 30 }
   }

   x.report("Rational#divmod(Float)"){
      MAX.times{ RAT1.divmod 30.37 }
   }

   x.report("Rational#abs"){
      MAX.times{ RAT1.abs }
   }

   x.report("Rational#==(Rational)"){
      MAX.times{ RAT1 == RAT2 }
   }

   x.report("Rational#==(Integer)"){
      MAX.times{ RAT1 == 30 }
   }

   x.report("Rational#==(Float)"){
      MAX.times{ RAT1 == 30.37 }
   }

   x.report("Rational#<=>(Rational)"){
      MAX.times{ RAT1 <=> RAT2 }
   }

   x.report("Rational#<=>(Integer)"){
      MAX.times{ RAT1 <=> 30 }
   }

   x.report("Rational#<=>(Float)"){
      MAX.times{ RAT1 <=> 30.37 }
   }

   x.report("Rational#to_i"){
      MAX.times{ RAT1.to_i }
   }

   x.report("Rational#to_f"){
      MAX.times{ RAT1.to_f }
   }

   x.report("Rational#to_s"){
      MAX.times{ RAT1.to_s }
   }

   x.report("Rational#to_r"){
      MAX.times{ RAT1.to_r }
   }
   
   x.report("Rational#hash"){
      MAX.times{ RAT1.hash }
   }

   x.report("Integer#numerator"){
      MAX.times{ NUM.numerator }
   }

   x.report("Integer#denominator"){
      MAX.times{ NUM.denominator }
   }

   x.report("Integer#to_r"){
      MAX.times{ NUM.to_r }
   }

   x.report("Integer#gcd"){
      MAX.times{ NUM.gcd(3) }
   }

   x.report("Integer#gcd2"){
      MAX.times{ NUM.gcd2(3) }
   }

   x.report("Integer#lcm"){
      MAX.times{ NUM.lcm(3) }
   }

   x.report("Integer#gcdlcm"){
      MAX.times{ NUM.gcdlcm(3) }
   }

   x.report("Fixnum#quo"){
      MAX.times{ NUM.quo(DEN) }
   }

   x.report("Fixnum#rpower"){
      MAX.times{ NUM.rpower(DEN) }
   }

   x.report("Bignum#quo"){
      MAX.times{ BIG.quo(NUM) }
   }

   x.report("Bignum#rpower"){
      MAX.times{ BIG.rpower(NUM) }
   }
end
