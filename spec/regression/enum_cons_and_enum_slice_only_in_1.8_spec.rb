require 'rspec'

shared_examples :enum do
  describe "#enum_slice" do
    it "is only defined in 1.8 mode" do
      if RUBY_VERSION =~ /1\.8/
        subject.respond_to?(:enum_slice).should == true
      else
        subject.respond_to?(:enum_slice).should == false
      end
    end
  end

  describe "#enum_cons" do
    it "is only defined in 1.8 mode" do
      if RUBY_VERSION =~ /1\.8/
        subject.respond_to?(:enum_cons).should == true
      else
        subject.respond_to?(:enum_cons).should == false
      end
    end
  end
end

describe "Enumerable" do
  subject {
    Class.new do
      include Enumerable
    end.new
  }
  it_behaves_like :enum
end

describe "Enumerator" do
  subject {
    [].each
  }
  it_behaves_like :enum
end
