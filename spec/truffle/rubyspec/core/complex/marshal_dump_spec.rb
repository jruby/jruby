require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9"..."2.0" do
  describe "Complex#marshal_dump" do
    it "dumps real and imaginary parts" do
      Complex(1, 2).send(:marshal_dump).should == [1, 2]
    end
  end
end
