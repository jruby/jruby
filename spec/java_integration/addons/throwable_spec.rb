require File.dirname(__FILE__) + "/../spec_helper"
require 'java'

describe "A Java Throwable" do
  it "implements backtrace" do
    ex = java.lang.Exception.new
    trace = nil
    lambda {trace = ex.backtrace}.should_not raise_error
    expect(trace).to eq(ex.stack_trace.map(&:to_s))
  end
  
  it "implements backtrace= as a no-op" do
    ex = java.lang.Exception.new
    backtrace = ex.backtrace
    ex.set_backtrace ['blah']
    expect(ex.backtrace).to eq backtrace
  end
  
  it "implements to_s as message" do
    ex = java.lang.Exception.new
    expect(ex.to_s).to eq ""
    expect(ex.to_s).to eq ex.message
    
    ex = java.lang.Exception.new('hello')
    expect(ex.to_s).to eq 'hello'
    expect(ex.to_s).to eq ex.message
  end
  
  it "implements to_str to call to_s" do
    ex = java.lang.Exception.new
    def ex.to_s
      'hello'
    end
    
    expect(ex.to_str).to eq 'hello'
  end
  
  it "implements inspect as toString" do
    ex = java.lang.Exception.new('hello')
    expect(ex.inspect).to eq "java.lang.Exception: hello"
  end

  it "can be rescued by rescue Exception" do
    begin
      raise ex = java.lang.Exception.new
    rescue Exception => e
      expect(e).to eq(ex)
    end
  end
  
  it "can be rescued by rescue java.lang.Throwable" do
    begin
      raise ex = java.lang.Exception.new
    rescue java.lang.Exception => e
      expect(e).to eq(ex)
    end
  end
  
  it "can be rescued by rescue Object" do
    begin
      raise ex = java.lang.Exception.new
    rescue Object => e
      expect(e).to eq(ex)
    end
  end
end

describe "Rescuing a Java exception using Exception" do
  it "does not prevent non-local return from working" do
    x = Object.new
    def x.foo
      loop do
        begin
          return 1
        rescue Exception
          2
        end
      end
    end
    expect(x.foo).to eq 1
  end

  it "does not prevent non-local break from working" do
    expect(loop do
      begin
        break 1
      rescue Exception
        2
      end
    end).to eq 1
  end

  it "does not prevent non-local redo from working" do
    i = 0
    loop do
      begin
        i += 1
        redo if i < 2
        break
      rescue Exception
        i = 3
      end
    end
    expect(i).to eq 2
  end

  it "does not prevent catch/throw from working" do
    expect do
      catch :blah do
        begin
          throw :blah
        rescue Exception
          raise
        end
      end
    end.not_to raise_error
  end

  it "does not prevent retry from working" do
    i = 0
    begin
      i += 1
      raise StandardError if i < 2
    rescue StandardError
      begin
        retry
      rescue Exception
        i = 3
      end
    end
    expect(i).to eq 2
  end
end