#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: moments.ruby,v 1.2 2005-06-10 00:57:22 igouy-guest Exp $
# http://www.bagley.org/~doug/shootout/ 

# throw away unused parameter sent by benchmark framework
ARGV.shift()

def main ()
    sum = 0.0
    nums = []
    num = nil

    for line in STDIN.readlines()
	num = Float(line)
	nums << num
	sum += num
    end

    n = nums.length()
    mean = sum/n;
    deviation = 0.0
    average_deviation = 0.0
    standard_deviation = 0.0
    variance = 0.0
    skew = 0.0
    kurtosis = 0.0
    
    for num in nums
	deviation = num - mean
	average_deviation += deviation.abs()
	variance += deviation**2;
	skew += deviation**3;
	kurtosis += deviation**4
    end
    average_deviation /= n
    variance /= (n - 1)
    standard_deviation = Math.sqrt(variance)

    if (variance > 0.0)
	skew /= (n * variance * standard_deviation)
	kurtosis = kurtosis/(n * variance * variance) - 3.0
    end

    nums.sort()
    mid = n / 2
    
    if (n % 2) == 0
	median = (nums.at(mid) + nums.at(mid-1))/2
    else
	median = nums.at(mid)
    end
    
    printf("n:                  %d\n", n)
    printf("median:             %f\n", median)
    printf("mean:               %f\n", mean)
    printf("average_deviation:  %f\n", average_deviation)
    printf("standard_deviation: %f\n", standard_deviation)
    printf("variance:           %f\n", variance)
    printf("skew:               %f\n", skew)
    printf("kurtosis:           %f\n", kurtosis)
end

main()
