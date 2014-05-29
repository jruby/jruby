describe :complex_inspect, :shared => true do
  ruby_version_is ""..."1.9" do
    it "returns Complex(real, image)" do
      # Guard against the Mathn library
      conflicts_with :Prime do
        Complex(1).inspect.should == "Complex(1, 0)"
        Complex(7).inspect.should == "Complex(7, 0)"
      end

      Complex(-1, 4).inspect.should == "Complex(-1, 4)"
      Complex(-7, 6.7).inspect.should == "Complex(-7, 6.7)"
    end
  end

  ruby_version_is "1.9" do
    it "returns (${real}+${image}i) for positive imaginary parts" do
      Complex(1).inspect.should == "(1+0i)"
      Complex(7).inspect.should == "(7+0i)"
      Complex(-1, 4).inspect.should == "(-1+4i)"
      Complex(-7, 6.7).inspect.should == "(-7+6.7i)"
    end

    it "returns (${real}-${image}i) for negative imaginary parts" do
      Complex(0, -1).inspect.should == "(0-1i)"
      Complex(-1, -4).inspect.should == "(-1-4i)"
      Complex(-7, -6.7).inspect.should == "(-7-6.7i)"
    end
  end
end
