require File.dirname(__FILE__) + "/../spec_helper"

java_import 'java_integration.fixtures.SingleMethodInterface'
java_import 'java_integration.fixtures.BeanLikeInterface'

describe 'interface comparison' do

  it 'compares against interface like with an included module' do
    value_holder1 = Class.new do
      include SingleMethodInterface
      def initialize(val)
        @value = val
      end
      def callIt
        @value
      end
    end

    expect( value_holder1 < SingleMethodInterface ).to be true
    expect( value_holder1 === SingleMethodInterface ).to be false
    expect( SingleMethodInterface === value_holder1 ).to be false

    val = value_holder1.new(111)
    expect( val.class < SingleMethodInterface ).to be true
    expect( SingleMethodInterface === val.class ).to be false
    expect( SingleMethodInterface === val ).to be true
    expect( val === SingleMethodInterface ).to be false
  end

  it 'compares with interface like with an included module' do
    mod = Module.new { include Enumerable }
    val = Class.new { include mod }.new

    expect( val.class < Enumerable ).to be true
    expect( Enumerable === val.class ).to be false
    expect( val.class === Enumerable ).to be false
    expect( Enumerable === val ).to be true
    expect( val === Enumerable ).to be false
    expect( val === val.class.new ).to be false
    expect( val === mod ).to be false

    mod = Module.new do
      include BeanLikeInterface
    end
    sup = Class.new do
      include mod, java.lang.Runnable, java.lang.Iterable # includes Enumerable
      def each; yield nil end
    end
    child = Class.new(sup) { include java.lang.Cloneable; def getValue; 1; end }
    obj = child.new

    expect( child < BeanLikeInterface ).to be true
    expect( obj.class < java.lang.Runnable ).to be true
    expect( java.lang.Cloneable === obj ).to be true
    expect( java.lang.Iterable === obj ).to be true
    expect( BeanLikeInterface === obj ).to be true
    expect( Enumerable === obj ).to be true
    expect( obj === BeanLikeInterface ).to be false

    expect( obj === obj ).to be true
    expect( obj === sup.new ).to be false
    expect( obj === java.lang.Runnable.impl {} ).to be false # was true in < 9.1 !
    expect( obj === java.util.HashSet.new ).to be false # was true in < 9.1 !
  end

end