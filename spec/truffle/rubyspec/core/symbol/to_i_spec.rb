require File.expand_path('../../../spec_helper', __FILE__)

describe "Symbol#to_i" do
  ruby_version_is ""..."1.9" do
    not_compliant_on :rubinius do
      it "returns an integer that is unique for each symbol for each program execution" do
        :ruby.to_i.is_a?(Integer).should == true
        :ruby.to_i.should == :ruby.to_i
        :ruby.to_i.should_not == :rubinius.to_i
      end
    end
  end

  ruby_version_is "1.9" do
    it "has been removed as deprecated" do
      :ruby.should_not respond_to(:to_i)
    end
  end
end
