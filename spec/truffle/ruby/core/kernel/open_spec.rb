require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#open" do

  before :each do
    @name = tmp("kernel_open.txt")
    @content = "This is a test"
    touch(@name) { |f| f.write @content }
    @file = nil
  end

  after :each do
    @file.close if @file
    rm_r @name
  end

  it "is a private method" do
    Kernel.should have_private_instance_method(:open)
  end

  it "opens a file when given a valid filename" do
    @file = open(@name)
    @file.should be_kind_of(File)
  end

  it "opens a file when called with a block" do
    open(@name, "r") { |f| f.gets }.should == @content
  end

  platform_is_not :windows do
    it "opens an io when path starts with a pipe" do
      @io = open("|date")
      @io.should be_kind_of(IO)
    end

    it "opens an io when called with a block" do
      @output = open("|date") { |f| f.gets }
      @output.should_not == ''
    end
  end

  platform_is :windows do
    it "opens an io when path starts with a pipe" do
      @io = open("|date /t")
      @io.should be_kind_of(IO)
    end

    it "opens an io when called with a block" do
      @output = open("|date /t") { |f| f.gets }
      @output.should_not == ''
    end
  end

  it "raises an ArgumentError if not passed one argument" do
    lambda { open }.should raise_error(ArgumentError)
  end

  ruby_version_is "1.9" do
    describe "when given an object that responds to to_open" do
      before :each do
        ScratchPad.clear
      end

      it "calls #to_path to covert the argument to a String before calling #to_str" do
        obj = mock("open to_path")
        obj.should_receive(:to_path).at_least(1).times.and_return(@name)
        obj.should_not_receive(:to_str)

        open(obj, "r") { |f| f.gets }.should == @content
      end

      it "calls #to_str to convert the argument to a String" do
        obj = mock("open to_str")
        obj.should_receive(:to_str).at_least(1).times.and_return(@name)

        open(obj, "r") { |f| f.gets }.should == @content
      end

      it "calls #to_open on argument" do
        obj = mock('fileish')
        @file = File.open(@name)
        obj.should_receive(:to_open).and_return(@file)
        @file = open(obj)
        @file.should be_kind_of(File)
      end

      it "returns the value from #to_open" do
        obj = mock('to_open')
        obj.should_receive(:to_open).and_return(:value)

        open(obj).should == :value
      end

      it "passes its arguments onto #to_open" do
        obj = mock('to_open')
        obj.should_receive(:to_open).with(1,2,3)

        open(obj, 1, 2, 3)
      end

      it "passes the return value from #to_open to a block" do
        obj = mock('to_open')
        obj.should_receive(:to_open).and_return(:value)

        open(obj) do |mock|
          ScratchPad.record(mock)
        end

        ScratchPad.recorded.should == :value
      end
    end

    it "raises a TypeError if passed a non-String that does not respond to #to_open" do
      obj = mock('non-fileish')
      lambda { open(obj) }.should raise_error(TypeError)
      lambda { open(nil) }.should raise_error(TypeError)
      lambda { open(7)   }.should raise_error(TypeError)
    end
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if not passed a String type" do
      lambda { open(nil)       }.should raise_error(TypeError)
      lambda { open(7)         }.should raise_error(TypeError)
      lambda { open(mock('x')) }.should raise_error(TypeError)
    end
  end

  it "accepts nil for mode and permission" do
    open(@name, nil, nil) { |f| f.gets }.should == @content
  end
end

describe "Kernel.open" do
  it "needs to be reviewed for spec completeness"
end
