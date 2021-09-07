require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java object's builtin inspect method" do
  it "produces the \"hashy\" inspect output" do
    o = java.lang.Object.new
    expect(o.inspect).to match(/\#<Java::JavaLang::Object:0x[0-9a-f]+>/)
  end
end

describe "java.lang.Class" do
  it "produces Ruby-style inspect" do
    klass = java.lang.String.java_class.to_java
    expect(klass.inspect).to eq '#<Java::JavaLang::Class: java.lang.String>'

    klass = Java::short[0].new.java_class.to_java
    expect(klass.inspect).to eq '#<Java::JavaLang::Class: short[]>'

    klass = Java::java.lang.Double[1].new.java_class.to_java
    expect(klass.inspect).to eq '#<Java::JavaLang::Class: java.lang.Double[]>'
  end
end

describe "java.lang.String" do
  it "inspects like a Ruby string" do
    str = java.lang.String.new 'foo'
    expect(str.inspect).to eq '"foo"'
  end
end

describe "java.lang.StringBuilder" do
  it "inspects with Ruby type" do
    str = java.lang.StringBuilder.new 'bar'
    expect(str.inspect).to eq '#<Java::JavaLang::StringBuilder: "bar">'
  end
end

describe "java.lang.Thread" do
  it "inspects with a custom format" do
    thread = java.lang.Thread.new { Thread.pass }
    thread.name = 'a-thread'
    thread.start
    sleep(0.25)
    expect(thread.inspect).to match /#<Java::JavaLang::Thread:\d+ a-thread TERMINATED>/
  end
end

describe "java.nio.CharSequence" do # implements CharSequence
  it "inspects as other Buffer impls" do
    buf = java.nio.CharBuffer.allocate(12)
    buf.append('a'); buf.put('b')
    expect(buf.inspect).to match /#<Java::JavaNio::HeapCharBuffer:.*? position=2, limit=12, capacity=12, readOnly=false>/
  end
end

describe "java.nio.ByteBuffer" do
  it "inspects as other Buffer impls" do
    buf = java.nio.ByteBuffer.allocateDirect(8)
    buf.put(1)
    expect(buf.inspect).to match /#<Java::JavaNio::DirectByteBuffer:.*? position=1, limit=8, capacity=8, readOnly=false>/
  end
end

describe "java.util.TimeZone/ZoneInfo" do
  it "inspects as scalar" do
    zone = java.util.TimeZone.getTimeZone 'America/Los_Angeles'
    expect(zone.inspect).to eq 'America/Los_Angeles'
    expect(zone.to_zone_id.inspect).to eq 'America/Los_Angeles'
  end
end
