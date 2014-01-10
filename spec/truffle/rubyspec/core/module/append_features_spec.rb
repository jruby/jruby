require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#append_features" do
  it "gets called when self is included in another module/class" do
    begin
      m = Module.new do
        def self.append_features(mod)
          $appended_to = mod
        end
      end

      c = Class.new do
        include m
      end

      $appended_to.should == c
    ensure
      $appended_to = nil
    end
  end

  it "raises an ArgumentError on a cyclic include" do
    lambda {
      ModuleSpecs::CyclicAppendA.send(:append_features, ModuleSpecs::CyclicAppendA)
    }.should raise_error(ArgumentError)

    lambda {
      ModuleSpecs::CyclicAppendB.send(:append_features, ModuleSpecs::CyclicAppendA)
    }.should raise_error(ArgumentError)

  end
end
