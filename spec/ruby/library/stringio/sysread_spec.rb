require File.expand_path('../../../spec_helper', __FILE__)
require "stringio"
require File.expand_path('../shared/read', __FILE__)

describe "StringIO#sysread when passed length, buffer" do
  it_behaves_like :stringio_read, :sysread
end

describe "StringIO#sysread when passed [length]" do
  it_behaves_like :stringio_read_length, :sysread
end

describe "StringIO#sysread when passed no arguments" do
  it_behaves_like :stringio_read_no_arguments, :sysread

  ruby_bug "http://redmine.ruby-lang.org/projects/ruby-18/issues/show?id=156", "1.8.7" do
    it "returns an empty String if at EOF" do
      @io.sysread.should == "example"
      @io.sysread.should == ""
    end
  end
end

describe "StringIO#sysread when self is not readable" do
  it_behaves_like :stringio_read_not_readable, :sysread
end

describe "StringIO#sysread when passed nil" do
  it_behaves_like :stringio_read_nil, :sysread

  ruby_bug "http://redmine.ruby-lang.org/projects/ruby-18/issues/show?id=156", "1.8.7" do
    it "returns an empty String if at EOF" do
      @io.sysread(nil).should == "example"
      @io.sysread(nil).should == ""
    end
  end
end

describe "StringIO#sysread when passed [length]" do
  before(:each) do
    @io = StringIO.new("example")
  end

  it "raises an EOFError when self's position is at the end" do
    @io.pos = 7
    lambda { @io.sysread(10) }.should raise_error(EOFError)
  end

  ruby_bug "http://redmine.ruby-lang.org/projects/ruby-18/issues/show?id=156", "1.8.7" do
    it "returns an empty String when length is 0" do
      @io.sysread(0).should == ""
    end
  end
end
