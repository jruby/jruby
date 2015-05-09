require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel#puts" do
  before :each do
    @before_separator = $/
    @stdout = $stdout
    @name = tmp("kernel_puts.txt")
    $stdout = new_io @name
  end

  after :each do
    $/ = @before_separator
    $stdout.close
    $stdout = @stdout
    rm_r @name
  end

  it "is a private method" do
    Kernel.should have_private_instance_method(:puts)
  end

  it "writes just a newline when given no args" do
    $stdout.should_receive(:write).with("\n")
    Kernel.puts.should == nil
  end

  # Declared intentional in
  # http://redmine.ruby-lang.org/issues/show/1748
  it "writes a newline when given nil as an arg" do
    $stdout.should_receive(:write).with('')
    $stdout.should_receive(:write).with("\n")
    Kernel.puts(nil).should == nil
  end

  it "calls to_s before writing non-string objects" do
    object = mock('hola')
    object.should_receive(:to_s).and_return("hola")

    $stdout.should_receive(:write).with("hola")
    $stdout.should_receive(:write).with("\n")
    Kernel.puts(object).should == nil
  end

  it "writes each arg if given several" do
    $stdout.should_receive(:write).with("1")
    $stdout.should_receive(:write).with("two")
    $stdout.should_receive(:write).with("3")
    $stdout.should_receive(:write).with("\n").exactly(3).times
    Kernel.puts(1, "two", 3).should == nil
  end

  it "flattens a nested array before writing it" do
    $stdout.should_receive(:write).with("1")
    $stdout.should_receive(:write).with("2")
    $stdout.should_receive(:write).with("3")
    $stdout.should_receive(:write).with("\n").exactly(3).times
    Kernel.puts([1, 2, [3]]).should == nil
  end

  it "writes [...] for a recursive array arg" do
    x = []
    x << 2 << x
    $stdout.should_receive(:write).with("2")
    $stdout.should_receive(:write).with("[...]")
    $stdout.should_receive(:write).with("\n").exactly(2).times
    Kernel.puts(x).should == nil
  end

  it "writes a newline after objects that do not end in newlines" do
    $stdout.should_receive(:write).with("5")
    $stdout.should_receive(:write).with("\n")
    Kernel.puts(5).should == nil
  end

  it "does not write a newline after objects that end in newlines" do
    $stdout.should_receive(:write).with("5\n")
    Kernel.puts("5\n").should == nil
  end

  it "ignores the $/ separator global" do
    $/ = ":"
    $stdout.should_receive(:write).with("5")
    $stdout.should_receive(:write).with("\n")
    Kernel.puts(5).should == nil
  end
end

describe "Kernel.puts" do
  it "needs to be reviewed for spec completeness"
end
