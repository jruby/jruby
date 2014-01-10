require File.expand_path('../../../spec_helper', __FILE__)
require 'weakref'

describe "WeakRef#__send__" do
  after :all do
    GC.start
  end

  module WeakRefSpecs
    def self.delegated_method
      :result
    end

    def self.protected_method
      :result
    end
    class << self
      protected :protected_method
    end

    def self.private_method
      :result
    end
    class << self
      private :private_method
    end
  end

  it "delegates to public methods of the weakly-referenced object" do
    wr = WeakRef.new(WeakRefSpecs)
    wr.delegated_method.should == :result
  end

  ruby_version_is ""..."2.0" do
    it "delegates to protected methods of the weakly-referenced object" do
      wr = WeakRef.new(WeakRefSpecs)
      wr.protected_method.should == :result
    end
  end

  ruby_version_is "2.0" do
    it "delegates to protected methods of the weakly-referenced object" do
      wr = WeakRef.new(WeakRefSpecs)
      lambda { wr.protected_method }.should raise_error(NameError)
    end
  end

  it "does not delegate to private methods of the weakly-referenced object" do
    wr = WeakRef.new(WeakRefSpecs)
    lambda { wr.private_method }.should raise_error(NameError)
  end
end
