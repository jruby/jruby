require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Singleton._instantiate?" do

  it "is private" do
    lambda { SingletonSpecs::MyClass._instantiate? }.should raise_error(NoMethodError)
  end

  ruby_version_is "" ... "1.9" do
    # JRuby doesn't support "_instantiate?" intentionally (JRUBY-2239)
    not_compliant_on :jruby do
      it "returns nil until it is instantiated" do
        SingletonSpecs::NotInstantiated.send(:_instantiate?).should == nil
        SingletonSpecs::NotInstantiated.instance
        inst = SingletonSpecs::NotInstantiated.send(:_instantiate?)
        inst.should eql(SingletonSpecs::NotInstantiated.instance)
      end
    end
  end
end
