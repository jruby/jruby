require 'rspec'

if RUBY_VERSION =~ /1\.9/
  describe "A custom respond_to? on a BasicObject subclass" do
    it "should not try to invoke respond_to_missing? on false result" do
      cls = Class.new(BasicObject) do
        def respond_to?(meth); false; end
      end

      cls.new.respond_to?(:hello).should == false
    end
  end
end
