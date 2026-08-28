require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java object" do
  it "marshals as custom Ruby marshal data" do
    list = java.util.ArrayList.new
    list << 'foo'

    hash = {:foo => list}

    marshaled = Marshal.load(Marshal.dump(hash))
    expect( marshaled[:foo].class ).to be java.util.ArrayList
    expect( marshaled[:foo][0] ).to eql 'foo'
  end

  it "marshals Java enums" do
    t_enum = java.lang.Thread::State::TERMINATED
    w_enum = java.lang.Thread::State::WAITING
    data = [ t_enum, w_enum ]

    marshaled = Marshal.load(Marshal.dump(data))
    expect( marshaled[0] ).to be_a java.lang.Thread::State
    expect( marshaled ).to eql data
  end

  it "marshals anonymous (enum) class" do
    mic = java.util.concurrent.TimeUnit::MICROSECONDS
    mil = java.util.concurrent.TimeUnit::MILLISECONDS
    hash = { 'mic' => mic, 'mil' => mil }

    #pending 'Marshal.load can not resolve anonymous class: Java::JavaUtilConcurrent::TimeUnit::2'

    marshaled = Marshal.load(Marshal.dump(hash))
    expect( marshaled['mic'] ).to be_a java.util.concurrent.TimeUnit
    expect( marshaled['mil'] ).to be java.util.concurrent.TimeUnit::MILLISECONDS
    expect( marshaled ).to eql hash
  end
end
