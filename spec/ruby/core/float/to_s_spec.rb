require File.expand_path('../../../spec_helper', __FILE__)

describe "Float#to_s" do
  it "returns 'NaN' for NaN" do
    nan_value().to_s.should == 'NaN'
  end

  it "returns 'Infinity' for positive infinity" do
    infinity_value().to_s.should == 'Infinity'
  end

  it "returns '-Infinity' for negative infinity" do
    (-infinity_value()).to_s.should == '-Infinity'
  end

  it "returns '0.0' for 0.0" do
    0.0.to_s.should == "0.0"
  end

  platform_is_not :openbsd do
    it "emits '-' for -0.0" do
      -0.0.to_s.should == "-0.0"
    end
  end

  it "emits a '-' for negative values" do
    -3.14.to_s.should == "-3.14"
  end

  it "emits a trailing '.0' for a whole number" do
    50.0.to_s.should == "50.0"
  end

  it "emits a trailing '.0' for the mantissa in e format" do
    1.0e20.to_s.should == "1.0e+20"
  end

  it "uses non-e format for a positive value with fractional part having 5 significant figures" do
    0.0001.to_s.should == "0.0001"
  end

  it "uses non-e format for a negative value with fractional part having 5 significant figures" do
    -0.0001.to_s.should == "-0.0001"
  end

  it "uses e format for a positive value with fractional part having 6 significant figures" do
    0.00001.to_s.should == "1.0e-05"
  end

  it "uses e format for a negative value with fractional part having 6 significant figures" do
    -0.00001.to_s.should == "-1.0e-05"
  end

  it "uses non-e format for a positive value with whole part having 15 significant figures" do
    10000000000000.0.to_s.should == "10000000000000.0"
  end

  it "uses non-e format for a negative value with whole part having 15 significant figures" do
    -10000000000000.0.to_s.should == "-10000000000000.0"
  end

  ruby_version_is "" ... "1.9" do
    it "uses e format for a positive value with whole part having 16 significant figures" do
      100000000000000.0.to_s.should == "1.0e+14"
    end

    it "uses e format for a negative value with whole part having 16 significant figures" do
      -100000000000000.0.to_s.should == "-1.0e+14"
    end
  end

  ruby_version_is "1.9" do
    it "uses non-e format for a positive value with whole part having 17 significant figures" do
      1000000000000000.0.to_s.should == "1000000000000000.0"
    end

    it "uses non-e format for a negative value with whole part having 17 significant figures" do
      -1000000000000000.0.to_s.should == "-1000000000000000.0"
    end

    it "uses e format for a positive value with whole part having 18 significant figures" do
      10000000000000000.0.to_s.should == "1.0e+16"
    end

    it "uses e format for a negative value with whole part having 18 significant figures" do
      -10000000000000000.0.to_s.should == "-1.0e+16"
    end
  end

  ruby_bug "#3273", "1.8.7" do
    it "outputs the minimal, unique form necessary to recreate the value" do
      value = 0.21611564636388508
      string = "0.21611564636388508"

      value.to_s.should == string
      string.to_f.should == value
    end
  end

  it "outputs the minimal, unique form to represent the value" do
    0.56.to_s.should == "0.56"
  end
end
