if RUBY_VERSION >= "1.9"
  describe "defined?(::BasicObject)" do
    it "returns \"constant\"" do
      defined?(::BasicObject).should == "constant"
    end
  end
end
