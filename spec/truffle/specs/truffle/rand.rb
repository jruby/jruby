# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../../ruby/spec_helper'

describe "Random" do
  
  describe "#rand" do

    it "with no argument returns the same numbers as other implementations of Ruby, given the same seed" do
      sequences = {
        0   => [0.5488135039273248,
                0.7151893663724195,
                0.6027633760716439,
                0.5448831829968969,
                0.4236547993389047,
                0.6458941130666561,
                0.4375872112626925,
                0.8917730007820798,
                0.9636627605010293,
                0.3834415188257777],
        493 => [0.9242902863998772,
                0.9535656843746261,
                0.6644462711803232,
                0.36372128443753204,
                0.9372152897557885,
                0.7102477795195669,
                0.7378911367923787,
                0.5161160225418484,
                0.9743222615926629,
                0.5440861076647412]
      }

      sequences.each do |seed, sequence|
        r = Random.new(seed)
        sequence.each do |n|
          r.rand.to_s.should == n.to_s
        end
      end
    end

  end
  
  describe "#rand" do

    it "with a maximum of 100 returns the same numbers as other implementations of Ruby, given the same seed" do
      sequences = {
        0   => [44,
                47,
                64,
                67,
                67,
                9,
                83,
                21,
                36,
                87],
        493 => [82,
                34,
                44,
                25,
                17,
                1,
                87,
                76,
                86,
                46]
      }

      sequences.each do |seed, sequence|
        r = Random.new(seed)
        sequence.each do |n|
          r.rand(100).should == n
        end
      end
    end

  end
  
end
