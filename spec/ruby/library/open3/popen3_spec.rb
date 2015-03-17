require File.expand_path('../../../spec_helper', __FILE__)
require 'open3'

describe "Open3.popen3" do
  after :each do
    [@in, @out, @err].each do |io|
      io.close if io && !io.closed?
    end
  end

  it "executes a process with a pipe to read stdout" do
    @in, @out, @err = Open3.popen3(ruby_cmd("print :foo"))
    @out.read.should == "foo"
  end

  it "executes a process with a pipe to read stderr" do
    @in, @out, @err = Open3.popen3(ruby_cmd("STDERR.print :foo"))
    @err.read.should == "foo"
  end

  it "executes a process with a pipe to write stdin" do
    @in, @out, @err = Open3.popen3(ruby_cmd("print STDIN.read"))
    @in.write("foo")
    @in.close
    @out.read.should == "foo"
  end

  it "needs to be reviewed for spec completeness"
end
