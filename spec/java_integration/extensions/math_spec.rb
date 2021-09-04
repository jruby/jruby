require File.dirname(__FILE__) + "/../spec_helper"

describe "java.math.BigDecimal extensions" do

  it 'supports zero? / nonzero? protocol' do
    val = java.math.BigDecimal::ZERO
    expect( val.zero? ).to eql true
    val = java.math.BigDecimal.new('0')
    expect( val.nonzero? ).to be nil
    expect( val.zero? ).to eql true
    expect( val.nonzero? ).to be nil

    val = java.math.BigDecimal.new('0.1')
    expect( val.zero? ).to eql false
    expect( val.nonzero? ).to eql val
    val = java.math.BigDecimal.new('0.00001')
    expect( val.zero? ).to eql false
    expect( val.nonzero? ).to eql val
  end

  it 'converts to a Float' do
    val = java.math.BigDecimal.new('0.1')
    expect( val.to_f ).to eql 0.1

    val = java.math.BigDecimal.new('0.0')
    expect( val.to_f ).to be_a Float
    expect( val.to_f ).to eql 0.0
  end

  before(:all) { require 'bigdecimal' }

  it 'supports to_d conversion' do
    val = java.math.BigDecimal::ZERO
    expect( val.to_d ).to be_a BigDecimal
    expect( val.to_d.zero? ).to be true

    val = java.math.BigDecimal.new('100.123')
    expect( val.to_d ).to eql BigDecimal('100.123')
  end

end
