# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is '1.8.7' do
  describe "IO#each_char" do
    before :each do
      @io = IOSpecs.io_fixture "lines.txt"
      @kcode, $KCODE = $KCODE, "utf-8"
      ScratchPad.record []
    end

    after :each do
      @io.close unless @io.closed?
      $KCODE = @kcode
    end

    it "raises IOError on closed stream" do
      lambda { IOSpecs.closed_io.each_char {} }.should raise_error(IOError)
    end

    it "yields each character" do
      @io.readline.should == "Voici la ligne une.\n"

      count = 0
      @io.each_char do |c|
        ScratchPad << c
        break if 4 < count += 1
      end

      ScratchPad.recorded.should == ["Q", "u", "i", " ", "è"]
    end

    it "returns an Enumerator when passed no block" do
      enum = @io.each_char
      enum.should be_an_instance_of(enumerator_class)
      enum.each.first(5).should == ["V", "o", "i", "c", "i"]
    end
  end

  describe "IO#each_char" do
    before :each do
      @kcode, $KCODE = $KCODE, "utf-8"
      @name = tmp("io_each_char")
      @io = IOSpecs.io_fixture @name, "w+:utf-8"
      ScratchPad.record []
    end

    after :each do
      @io.close unless @io.closed?
      rm_r @name
      $KCODE = @kcode
    end

    it "does not yield any characters on an empty stream" do
      @io.each_char { |c| ScratchPad << c }
      ScratchPad.recorded.should == []
    end
  end
end
