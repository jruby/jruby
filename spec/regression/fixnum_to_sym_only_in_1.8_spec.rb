require 'rspec'
require 'jruby'

describe "Fixnum#to_sym" do
  it "is only defined in 1.8 mode" do
    if RUBY_VERSION =~ /1\.8/
      1.respond_to?(:to_sym).should == true
    else
      1.respond_to?(:to_sym).should == false
    end
  end
end
