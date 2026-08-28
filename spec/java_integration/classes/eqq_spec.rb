require File.dirname(__FILE__) + "/../spec_helper"

describe "java.lang.Throwable" do

  it 'class is not === to itself (like in Ruby)' do
    # NOTE: Exception === Exception does not hold!
    expect( java.lang.Throwable === Java::JavaLang::Throwable ).to be false
    expect( java.lang.Throwable === java.lang.Throwable.java_class ).to be false

    expect( java.lang.RuntimeException === java.lang.RuntimeException ).to be false
  end

  it '=== matches instances' do
    expect( java.lang.Throwable === java.lang.Throwable.new('msg') ).to be true
    expect( java.lang.Exception === java.lang.RuntimeException.new(nil, nil) ).to be true
    begin
      java.lang.Integer.parseInt('aa', 10)
    rescue java.lang.Exception => ex
      expect( java.lang.Throwable === ex ).to be true
      expect( java.lang.Exception === ex ).to be true
      expect( java.lang.NumberFormatException === ex ).to be true
      expect( java.lang.IllegalStateException === ex ).to be false
    else
      fail 'excepted to rescue!'
    end
  end

  it '=== matches NativeException instances' do
    begin
      java.lang.Integer.parseInt('gg', 16)
    rescue NativeException => ex
      expect( java.lang.Throwable === ex ).to be true
      expect( java.lang.Exception === ex ).to be true
      expect( java.lang.NumberFormatException === ex ).to be true
      expect( java.lang.IllegalStateException === ex ).to be false
    else
      fail 'excepted to rescue!'
    end
  end

end