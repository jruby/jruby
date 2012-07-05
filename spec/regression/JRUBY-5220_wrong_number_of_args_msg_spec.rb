describe "Calling behavior: JRUBY-5220" do

  context "on calling blocks with wrong number of arguments" do
    it "should use the same message MRI uses" do
      lambda do
        lambda {||}.call(1) 
      end.should raise_error(ArgumentError)
    end
  end
  context "on calling methods with wrong number of arguments" do
    it "should use the same message MRI uses" do
      lambda do
        def foo()
        end
        foo(1)
      end.should raise_error(ArgumentError)
    end
  end
end