#!/usr/bin/env jruby
require 'rspec'

describe "JRUBY-6307: Powering operation of Integer sometimes gets a wrong calculation when 1.9 mode." do
  it "returns the powers correctly" do
    values             = [ 854374, 3487497, 3671557, 4344799, 4992054, 5289035]
  
    values.each do |v|
      power            = v**2
      multiply_by_self = v*v
      expect(power).to eq(multiply_by_self)
    end
  end
end
