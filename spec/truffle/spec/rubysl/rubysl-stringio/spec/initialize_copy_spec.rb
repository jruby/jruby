require 'stringio'

describe "StringIO#initialize_copy" do
  before(:each) do
    @io      = StringIO.new("StringIO example")
    @orig_io = StringIO.new("Original StringIO")
  end

  it "is private" do
    StringIO.should have_private_instance_method(:initialize_copy)
  end

  it "returns self" do
    @io.send(:initialize_copy, @orig_io).should equal(@io)
  end

  it "tries to convert the passed argument to a StringIO using #to_strio" do
    obj = mock('to_strio')
    obj.should_receive(:to_strio).and_return(StringIO.new("converted"))
    @io.send(:initialize_copy, obj)
    @io.string.should == "converted"
  end

  it "copies the passed StringIO's content to self" do
    @io.send(:initialize_copy, @orig_io)
    @io.string.should == "Original StringIO"
  end

  it "copies the passed StringIO's position to self" do
    @orig_io.pos = 5
    @io.send(:initialize_copy, @orig_io)
    @io.pos.should eql(5)
  end

  it "taints self when the passed StringIO is tainted" do
    @orig_io.taint
    @io.send(:initialize_copy, @orig_io)
    @io.tainted?.should be_true
  end

  it "copies the passed StringIO's mode to self" do
    orig_io = StringIO.new("read-only", "r")
    @io.send(:initialize_copy, orig_io)
    @io.closed_read?.should be_false
    @io.closed_write?.should be_true

    orig_io = StringIO.new("write-only", "w")
    @io.send(:initialize_copy, orig_io)
    @io.closed_read?.should be_true
    @io.closed_write?.should be_false

    orig_io = StringIO.new("read-write", "r+")
    @io.send(:initialize_copy, orig_io)
    @io.closed_read?.should be_false
    @io.closed_write?.should be_false

    orig_io = StringIO.new("read-write", "w+")
    @io.send(:initialize_copy, orig_io)
    @io.closed_read?.should be_false
    @io.closed_write?.should be_false
  end

  it "copies the passed StringIO's append mode" do
    orig_io = StringIO.new("read-write", "a")
    @io.send(:initialize_copy, orig_io)

    @io.pos = 0
    @io << " test"

    @io.string.should == "read-write test"
  end

  it "does not truncate self's content when the copied StringIO was in truncate mode" do
    orig_io = StringIO.new("original StringIO", "w+")
    orig_io.write("not truncated") # make sure the content is not empty

    @io.send(:initialize_copy, orig_io)
    @io.string.should == "not truncated"
  end

  it "makes both StringIO objects to share position, eof status" do
    @io.send(:initialize_copy, @orig_io)
    @orig_io.pos.should == 0
    @io.getc
    @io.pos.should == 1
    @orig_io.pos.should == 1
    @orig_io.read(1).should == "r"
    @io.read(1).should == "i"
    @orig_io.pos.should == 3
    @orig_io.pos.should == 3
    @io.gets(nil)
    @io.eof?.should == true
    @orig_io.eof?.should == true
  end
end
