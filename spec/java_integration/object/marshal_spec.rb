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

  java_import 'java_integration.fixtures.InnerClasses'

  it "marshals local Java class" do
    hash = { :local => InnerClasses.localMethodClass }
    caps_method = hash[:local].capsMethod

    #pending 'Marshal.load can not resolve local class: Java::Java_integrationFixtures::InnerClasses::1CapsImpl'

    local = Marshal.load(Marshal.dump(hash))[:local]
    expect( local.class.java_class.member_class? ).to be false
    expect( local.class.java_class.anonymous_class? ).to be false
    expect( local.capsMethod ).to eql caps_method
  end

  it "marshals anonymous Java class" do
    array = [ InnerClasses.anonymousMethodClass ]
    caps_method = array[0].capsMethod

    #pending 'Marshal.load can not resolve anonymous class: Java::Java_integrationFixtures::InnerClasses::1'

    anon = Marshal.load(Marshal.dump(array))[0]

    expect( anon.class.java_class.member_class? ).to be false
    expect( anon.class.java_class.anonymous_class? ).to be true
    expect( anon.capsMethod ).to eql caps_method
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
