require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel.rand" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:rand)
  end

  it "returns a float if no argument is passed" do
    rand.should be_kind_of(Float)
  end

  it "returns an integer for an integer argument" do
    rand(77).should be_kind_of(Integer)
  end

  it "returns an integer for a float argument greater than 1" do
    rand(1.3).should be_kind_of(Integer)
  end

  it "returns a float for an argument between -1 and 1" do
    rand(-0.999).should be_kind_of(Float)
    rand(-0.01).should be_kind_of(Float)
    rand(0).should be_kind_of(Float)
    rand(0.01).should be_kind_of(Float)
    rand(0.999).should be_kind_of(Float)
  end

  it "ignores the sign of the argument" do
    [0, 1, 2, 3].should include(rand(-4))
  end

  it "never returns a value greater or equal to 1.0 with no arguments" do
    1000.times do
      (0...1.0).should include(rand)
    end
  end

  it "never returns a value greater or equal to any passed in max argument" do
    1000.times do
      (0...100).to_a.should include(rand(100))
    end
  end

  ruby_version_is ""..."1.9" do
    it "calls to_i on its argument" do
      l = mock('limit')
      l.should_receive(:to_i).and_return 7

      rand l
    end
  end

  ruby_version_is "1.9" do
    it "calls to_int on its argument" do
      l = mock('limit')
      l.should_receive(:to_int).and_return 7

      rand l
    end
  end

  ruby_version_is "1.9" do
    it "returns a float for an range argument where max is < 1" do
      rand(0.25..0.75).should be_kind_of(Float)
    end
  end
end

describe "Kernel#rand" do
  it "needs to be reviewed for spec completeness"
end
