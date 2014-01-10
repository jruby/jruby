require File.expand_path('../../../shared/kernel/singleton_method_undefined', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#singleton_method_undefined" do
    it_behaves_like(:singleton_method_undefined, :singleton_method_undefined, BasicObject)
  end
end
