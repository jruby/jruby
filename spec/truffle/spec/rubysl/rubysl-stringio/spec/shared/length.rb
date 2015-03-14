# -*- encoding: utf-8 -*-
describe :stringio_length, :shared => true do
  it "returns the length of the wrapped string" do
    StringIO.new("example").send(@method).should == 7
  end

  with_feature :encoding do
    it "returns the number of bytes" do
      StringIO.new("ありがとう").send(@method).should == 15
    end
  end
end
