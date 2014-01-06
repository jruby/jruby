require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Math.atan2" do
  it "returns a float" do
    Math.atan2(1.2, 0.5).should be_kind_of(Float)
  end

  it "returns the arc tangent of y, x" do
    Math.atan2(4.2, 0.3).should be_close(1.49948886200961, TOLERANCE)
    Math.atan2(0.0, 1.0).should be_close(0.0, TOLERANCE)
    Math.atan2(-9.1, 3.2).should be_close(-1.23265379809025, TOLERANCE)
    Math.atan2(7.22, -3.3).should be_close(1.99950888779256, TOLERANCE)
  end

  ruby_version_is ""..."1.9" do
    it "raises an ArgumentError if the argument cannot be coerced with Float()" do
      lambda { Math.atan2(1.0, "test")    }.should raise_error(ArgumentError)
      lambda { Math.atan2("test", 0.0)    }.should raise_error(ArgumentError)
      lambda { Math.atan2("test", "this") }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "1.9" do
    it "raises an TypeError if the argument cannot be coerced with Float()" do
      lambda { Math.atan2(1.0, "test")    }.should raise_error(TypeError)
      lambda { Math.atan2("test", 0.0)    }.should raise_error(TypeError)
      lambda { Math.atan2("test", "this") }.should raise_error(TypeError)
    end
  end

  it "raises a TypeError if the argument is nil" do
    lambda { Math.atan2(nil, 1.0)  }.should raise_error(TypeError)
    lambda { Math.atan2(-1.0, nil) }.should raise_error(TypeError)
    lambda { Math.atan2(nil, nil)  }.should raise_error(TypeError)
  end

  it "accepts any argument that can be coerced with Float()" do
    Math.atan2(MathSpecs::Float.new, MathSpecs::Float.new).should be_close(0.785398163397448, TOLERANCE)
  end

  it "returns positive zero when passed 0.0, 0.0" do
    Math.atan2(0.0, 0.0).should be_positive_zero
  end

  platform_is_not :solaris do
    it "returns negative zero when passed -0.0, 0.0" do
      Math.atan2(-0.0, 0.0).should be_negative_zero
    end
  end

  platform_is :solaris do
    it "returns positive zero when passed -0.0, 0.0" do
      Math.atan2(-0.0, 0.0).should be_positive_zero
    end
  end

  platform_is_not :solaris do
    it "returns Pi when passed 0.0, -0.0" do
      Math.atan2(0.0, -0.0).should == Math::PI
    end
  end

  platform_is :solaris do
    it "returns positive zero when passed 0.0, -0.0" do
      Math.atan2(0.0, -0.0).should be_positive_zero
    end
  end

  platform_is_not :solaris do
    it "returns -Pi when passed -0.0, -0.0" do
      Math.atan2(-0.0, -0.0).should == -Math::PI
    end
  end

  platform_is :solaris do
    it "returns positive zero when passed -0.0, -0.0" do
      Math.atan2(-0.0, -0.0).should be_positive_zero
    end
  end

end

describe "Math#atan2" do
  it "is accessible as a private instance method" do
    IncludesMath.new.send(:atan2, 1.1, 2.2).should be_close(0.463647609000806, TOLERANCE)
  end
end
