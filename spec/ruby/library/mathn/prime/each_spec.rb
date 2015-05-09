require File.expand_path('../../../../spec_helper', __FILE__)
require'mathn'

describe "Prime#each" do
  it "enumerates the elements" do
    primes = Prime.new
    result = []

    primes.each { |p|
      result << p
      break if p > 10
    }

    result.should == [2, 3, 5, 7, 11]
  end

  it "don't rewind the generator, each loop start at the current value" do
    primes = Prime.new
    primes.next
    result = []

    primes.each { |p|
      result << p
      break if p > 10
    }

    result.should == [3, 5, 7, 11]
  end
end
