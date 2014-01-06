# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/readlines', __FILE__)

describe "IO.foreach" do
  before :each do
    @name = fixture __FILE__, "lines.txt"
    @count = 0
    ScratchPad.record []
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator when called without a block" do
      IO.foreach(@name).should be_an_instance_of(enumerator_class)
      IO.foreach(@name).to_a.should == IOSpecs.lines
    end
  end

  it "updates $. with each yield" do
    IO.foreach(@name) { $..should == @count += 1 }
  end

  describe "when the filename starts with |" do
    it "gets data from the standard out of the subprocess" do
      IO.foreach("|sh -c 'echo hello;echo line2'") { |l| ScratchPad << l }
      ScratchPad.recorded.should == ["hello\n", "line2\n"]
    end

    it "gets data from a fork when passed -" do
      parent_pid = $$

      ret = IO.foreach("|-") { |l| ScratchPad << l; true }

      if $$ == parent_pid
        ScratchPad.recorded.should == ["hello\n", "from a fork\n"]
      else # child
        puts "hello"
        puts "from a fork"
        exit!
      end
    end
  end
end

ruby_version_is ""..."1.9" do
  describe "IO.foreach" do
    before :each do
      @name = fixture __FILE__, "lines.txt"
      ScratchPad.record []
    end

    it_behaves_like :io_readlines, :foreach, IOSpecs.collector
    it_behaves_like :io_readlines_options_18, :foreach, IOSpecs.collector
  end
end

ruby_version_is "1.9" do
  describe "IO.foreach" do
    before :each do
      @external = Encoding.default_external
      Encoding.default_external = Encoding::UTF_8

      @name = fixture __FILE__, "lines.txt"
      ScratchPad.record []
    end

    after :each do
      Encoding.default_external = @external
    end

    it "sets $_ to nil" do
      $_ = "test"
      IO.foreach(@name) { }
      $_.should be_nil
    end

    it_behaves_like :io_readlines, :foreach, IOSpecs.collector
    it_behaves_like :io_readlines_options_19, :foreach, IOSpecs.collector
  end
end
