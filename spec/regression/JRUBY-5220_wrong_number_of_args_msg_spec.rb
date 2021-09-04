describe "Calling behavior: JRUBY-5220" do

  context "on calling blocks with wrong number of arguments" do
    it "should use the same message MRI uses" do
      expect do
        lambda {||}.call(1) 
      end.to raise_error(ArgumentError)
    end
  end
  context "on calling methods with wrong number of arguments" do
    it "should use the same message MRI uses" do
      expect do
        def foo()
        end
        foo(1)
      end.to raise_error(ArgumentError)
    end
  end
end