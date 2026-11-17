require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "IO#autoclose?" do
  before :each do
    @io = IOSpecs.io_fixture "lines.txt"
  end

  after :each do
    @io.autoclose = true unless @io.closed?
    @io.close unless @io.closed?
  end

  it "is set to true by default" do
    @io.should.autoclose?
  end

  it "cannot be queried on a closed IO object" do
    @io.close
    -> { @io.autoclose? }.should.raise(IOError, /closed stream/)
  end
end

describe "IO#autoclose=" do
  before :each do
    @io = IOSpecs.io_fixture "lines.txt"
  end

  after :each do
    @io.autoclose = true unless @io.closed?
    @io.close unless @io.closed?
  end

  it "can be set to true" do
    @io.autoclose = false
    @io.autoclose = true
    @io.should.autoclose?
  end

  it "can be set to false" do
    @io.autoclose = true
    @io.autoclose = false
    @io.should_not.autoclose?
  end

  it "can be set to any truthy value" do
    @io.autoclose = false
    @io.autoclose = 42
    @io.should.autoclose?

    @io.autoclose = false
    @io.autoclose = Object.new
    @io.should.autoclose?
  end

  it "can be set to any falsy value" do
    @io.autoclose = true
    @io.autoclose = nil
    @io.should_not.autoclose?
  end

  it "can be set multiple times" do
    @io.autoclose = true
    @io.should.autoclose?

    @io.autoclose = false
    @io.should_not.autoclose?

    @io.autoclose = true
    @io.should.autoclose?
  end

  it "cannot be set on a closed IO object" do
    @io.close
    -> { @io.autoclose = false }.should.raise(IOError, /closed stream/)
  end

  it "still flushes buffered writes at exit for an IO created with autoclose: false" do
    file = tmp("io_autoclose_flush.txt")
    begin
      ruby_exe("io = IO.new(IO.sysopen(#{file.inspect}, 'w'), 'w', autoclose: false); io.write('kept')")
      File.read(file).should == "kept"
    ensure
      rm_r file
    end
  end

  it "still flushes buffered writes at exit after being set to false" do
    file = tmp("io_autoclose_flush.txt")
    begin
      ruby_exe("io = File.open(#{file.inspect}, 'w'); io.autoclose = false; io.write('kept')")
      File.read(file).should == "kept"
    ensure
      rm_r file
    end
  end
end
