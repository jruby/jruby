describe "Kernel#singleton_class" do
  it "returns class extended from an object" do
    x = Object.new
    xs = class << x; self; end
    xs.should == x.singleton_class
  end

  it "returns NilClass for nil" do
    nil.singleton_class.should == NilClass
  end

  it "returns TrueClass for true" do
    true.singleton_class.should == TrueClass
  end

  it "returns FalseClass for false" do
    false.singleton_class.should == FalseClass
  end

  it "raises TypeError for Fixnum" do
    lambda { 123.singleton_class }.should raise_error(TypeError)
  end

  it "raises TypeError for Symbol" do
    lambda { :foo.singleton_class }.should raise_error(TypeError)
  end
end

ruby_version_is "2.1" do
  describe "Kernel#singleton_class?" do
    it "returns true for singleton classes" do
      xs = self.singleton_class
      xs.singleton_class?.should == true
    end

    it "returns false for other objects" do
      c = Class.new
      c.singleton_class?.should == false
    end

    describe("with singleton values") do
      it "returns false for nil's singleton class" do
        NilClass.singleton_class?.should == false
      end

      it "returns false for true's singleton class" do
        TrueClass.singleton_class?.should == false
      end

      it "returns false for false's singleton class" do
        FalseClass.singleton_class?.should == false
      end
    end
  end
end
