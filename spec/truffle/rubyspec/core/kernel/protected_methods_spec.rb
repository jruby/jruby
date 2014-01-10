require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../../../fixtures/reflection', __FILE__)

# TODO: rewrite

# The reason why having include() and ruby_version_is is to show the specification explicitly.
# You should use have_protected_method() with the exception of this spec.
describe "Kernel#protected_methods" do
  ruby_version_is "" ... "1.9" do
    it "returns a list of the names of protected methods accessible in the object" do
      KernelSpecs::Methods.protected_methods(false).sort.should include("juu_ichi")
      KernelSpecs::Methods.new.protected_methods(false).should include("ku")
    end

    it "returns a list of the names of protected methods accessible in the object and from its ancestors and mixed-in modules" do
      l1 = KernelSpecs::Methods.protected_methods(false)
      l2 = KernelSpecs::Methods.protected_methods
      (l1 & l2).should include("juu_ichi")
      KernelSpecs::Methods.new.protected_methods.should include('ku')
    end

    it "returns protected methods mixed in to the metaclass" do
      m = KernelSpecs::Methods.new
      m.extend(KernelSpecs::Methods::MetaclassMethods)
      m.protected_methods.should include('nopeeking')
    end
  end

  ruby_version_is "1.9" do
    it "returns a list of the names of protected methods accessible in the object" do
      KernelSpecs::Methods.protected_methods(false).sort.should include(:juu_ichi)
      KernelSpecs::Methods.new.protected_methods(false).should include(:ku)
    end

    it "returns a list of the names of protected methods accessible in the object and from its ancestors and mixed-in modules" do
      l1 = KernelSpecs::Methods.protected_methods(false)
      l2 = KernelSpecs::Methods.protected_methods
      (l1 & l2).should include(:juu_ichi)
      KernelSpecs::Methods.new.protected_methods.should include(:ku)
    end

    it "returns methods mixed in to the metaclass" do
      m = KernelSpecs::Methods.new
      m.extend(KernelSpecs::Methods::MetaclassMethods)
      m.protected_methods.should include(:nopeeking)
    end
  end
end

describe :kernel_protected_methods_supers, :shared => true do
  it "returns a unique list for an object extended by a module" do
    m = ReflectSpecs.oed.protected_methods(*@object)
    m.select { |x| x == stasy(:pro) }.sort.should == [stasy(:pro)]
  end

  it "returns a unique list for a class including a module" do
    m = ReflectSpecs::D.new.protected_methods(*@object)
    m.select { |x| x == stasy(:pro) }.sort.should == [stasy(:pro)]
  end

  it "returns a unique list for a subclass of a class that includes a module" do
    m = ReflectSpecs::E.new.protected_methods(*@object)
    m.select { |x| x == stasy(:pro) }.sort.should == [stasy(:pro)]
  end
end

describe "Kernel#protected_methods" do
  describe "when not passed an argument" do
    it_behaves_like :kernel_protected_methods_supers, nil, []
  end

  describe "when passed true" do
    it_behaves_like :kernel_protected_methods_supers, nil, true
  end
end
