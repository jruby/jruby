require File.expand_path('../../../../spec_helper', __FILE__)
require 'net/telnet'

describe "Net::Telnet#binmode when passed no arguments or nil" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "returns the current Binmode value" do
    @telnet.binmode.should be_false
    @telnet.binmode(nil).should be_false
    @telnet.binmode = true
    @telnet.binmode.should be_true
    @telnet.binmode.should be_true
  end
end

describe "Net::Telnet#binmode when passed [true]" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "returns true" do
    @telnet.binmode(true).should be_true
  end

  it "sets the Binmode to true" do
    @telnet.binmode(true)
    @telnet.binmode.should be_true
  end
end

describe "Net::Telnet#binmode when passed [false]" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "returns false" do
    @telnet.binmode(false).should be_false
  end

  it "sets the Binmode to false" do
    @telnet.binmode(false)
    @telnet.binmode.should be_false
  end
end


describe "Net::Telnet#binmode when passed [Object]" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "raises an ArgumentError" do
    lambda { @telnet.binmode(Object.new) }.should raise_error(ArgumentError)
    lambda { @telnet.binmode("") }.should raise_error(ArgumentError)
    lambda { @telnet.binmode(:sym) }.should raise_error(ArgumentError)
  end

  it "does not change the Binmode" do
    mode = @telnet.binmode
    @telnet.binmode(Object.new) rescue nil
    @telnet.binmode.should == mode
  end
end

describe "Net::Telnet#binmode= when passed [true]" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "returns true" do
    (@telnet.binmode = true).should be_true
  end

  it "sets the Binmode to true" do
    @telnet.binmode = true
    @telnet.binmode.should be_true
  end
end

describe "Net::Telnet#binmode= when passed [false]" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "returns false" do
    (@telnet.binmode = false).should be_false
  end

  it "sets the Binmode to false" do
    @telnet.binmode = false
    @telnet.binmode.should be_false
  end
end

describe "Net::Telnet#binmode when passed [Object]" do
  before(:each) do
    @socket = mock("Telnet Socket")
    def @socket.kind_of?(klass)
      klass == IO
    end
    @telnet = Net::Telnet.new("Proxy" => @socket)
  end

  it "raises an ArgumentError" do
    lambda { @telnet.binmode = Object.new }.should raise_error(ArgumentError)
    lambda { @telnet.binmode = "" }.should raise_error(ArgumentError)
    lambda { @telnet.binmode = nil }.should raise_error(ArgumentError)
    lambda { @telnet.binmode = :sym }.should raise_error(ArgumentError)
  end

  it "does not change the Binmode" do
    @telnet.binmode = true
    (@telnet.binmode = Object.new) rescue nil
    @telnet.binmode.should be_true
  end
end
