require 'test/minirunit'

# array sort test based on JRUBY-4538

a = (1601..1980).to_a +
    ( 941..1600).to_a +
    ( 561.. 660).to_a +
    (   1.. 280).to_a +
    ( 661.. 940).to_a +
    (2261..2640).to_a +
    ( 281.. 560).to_a +
    (2921..3300).to_a +
    (1981..2260).to_a +
    (4901..5000).to_a +
    (3961..4220).to_a

test_equal(a.sort, a.sort.sort)
