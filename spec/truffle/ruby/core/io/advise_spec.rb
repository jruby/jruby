# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is '1.9.3' do
  describe "IO#advise" do
    before :each do
      @kcode, $KCODE = $KCODE, "utf-8"
      @io = IOSpecs.io_fixture "lines.txt"
    end

    after :each do
      @io.close unless @io.closed?
      $KCODE = @kcode
    end

    it "raises a TypeError if advise is not a Symbol" do
      lambda {
        @io.advise("normal")
      }.should raise_error(TypeError)
    end

    it "raises a TypeError if offsert cannot be coerced to an Integer" do
      lambda {
        @io.advise(:normal, "wat")
      }.should raise_error(TypeError)
    end

    it "raises a TypeError if len cannot be coerced to an Integer" do
      lambda {
        @io.advise(:normal, 0, "wat")
      }.should raise_error(TypeError)
    end

    it "raises a RangeError if offset is too big" do
      lambda {
        @io.advise(:normal, 10 ** 32)
      }.should raise_error(RangeError)
    end

    it "raises a RangeError if len is too big" do
      lambda {
        @io.advise(:normal, 0, 10 ** 32)
      }.should raise_error(RangeError)
    end

    it "raises a NotImplementedError if advise is not recognized" do
      lambda{
        @io.advise(:foo)
      }.should raise_error(NotImplementedError)
    end

    it "supports the normal advice type" do
      @io.advise(:normal).should be_nil
    end

    it "supports the sequential advice type" do
      @io.advise(:sequential).should be_nil
    end

    it "supports the random advice type" do
      @io.advise(:random).should be_nil
    end

    it "supports the dontneed advice type" do
      @io.advise(:dontneed).should be_nil
    end

    it "supports the noreuse advice type" do
      @io.advise(:noreuse).should be_nil
    end

    it "supports the willneed advice type" do
      @io.advise(:willneed).should be_nil
    end

    it "raises an IOError if the stream is closed" do
      @io.close
      lambda { @io.advise(:normal) }.should raise_error(IOError)
    end
  end
end
