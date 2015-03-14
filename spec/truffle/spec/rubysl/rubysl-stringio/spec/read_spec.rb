# -*- encoding: utf-8 -*-
require "stringio"
require File.expand_path('../shared/read', __FILE__)

describe "StringIO#read when passed length, buffer" do
  it_behaves_like :stringio_read, :read
end

describe "StringIO#read when passed length" do
  it_behaves_like :stringio_read_length, :read

  it "returns nil when passed length > 0 and no data remains" do
    @io.send(@method, 8).should == "example"
    @io.send(@method, 2).should be_nil
  end

  # This was filed as a bug in redmine#156 but since MRI refused to change the
  # 1.8 behavior, it's now considered a version difference by RubySpec since
  # it could have a significant impact on user code.
  ruby_version_is ""..."1.9" do
    it "returns nil when passed 0 and no data remains" do
      @io.send(@method, 8).should == "example"
      @io.send(@method, 0).should be_nil
    end
  end

  ruby_version_is "1.9" do
    it "returns an empty String when passed 0 and no data remains" do
      @io.send(@method, 8).should == "example"
      @io.send(@method, 0).should == ""
    end

    it "truncates the buffer when limit > 0 and no data remains" do
      @io.send(@method)
      @io.send(@method, 2, buffer = "abc").should be_nil
      buffer.should == ""
    end
  end
end

describe "StringIO#read when passed no arguments" do
  it_behaves_like :stringio_read_no_arguments, :read

  it "returns an empty string if at EOF" do
    @io.read.should == "example"
    @io.read.should == ""
  end
end

describe "StringIO#read when passed nil" do
  it_behaves_like :stringio_read_nil, :read

  it "returns an empty string if at EOF" do
    @io.read(nil).should == "example"
    @io.read(nil).should == ""
  end
end

describe "StringIO#read when self is not readable" do
  it_behaves_like :stringio_read_not_readable, :read
end
