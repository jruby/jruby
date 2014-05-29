require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  require 'complex'

  describe "Complex#%" do
    describe "with Complex" do
      it "returns the remainder from complex division" do
        (Complex(13, 44) % Complex(5, 20)).should == Complex(13 % 5, 44 % 20)
        (Complex(13.5, 44.5) % Complex(5.5, 20.5)).should == Complex(13.5 % 5.5, 44.5 % 20.5)
      end
    end

    describe "with Integer" do
      it "returns the remainder from dividing both parts of self by the given Integer" do
        (Complex(21, 42) % 10).should == Complex(21 % 10, 42 % 10)
        (Complex(15.5, 16.5) % 2.0).should be_close(Complex(15.5 % 2, 16.5 % 2), TOLERANCE)
      end
    end

    describe "Complex#% with Object" do
      it "tries to coerce self into other" do
        value = Complex(3, 9)

        obj = mock("Object")
        obj.should_receive(:coerce).with(value).and_return([2, 5])
        (value % obj).should == 2 % 5
      end
    end
  end
end
