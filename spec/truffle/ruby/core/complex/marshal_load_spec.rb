require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "Complex#marshal_load" do
    it "loads real and imaginary parts" do
      c = Complex(0, 0)
      c.marshal_load([1, 2])
      c.should == Complex(1, 2)
    end

    it "loads instance variables" do
      a = [1, 2]
      a.instance_variable_set(:@ivar, :ivar)

      c = Complex(0, 0)
      c.marshal_load(a)
      c.instance_variable_get(:@ivar).should == :ivar
    end

    it "returns self" do
      c = Complex(0, 0)
      c.marshal_load([1, 2]).should equal(c)
    end
  end
end
