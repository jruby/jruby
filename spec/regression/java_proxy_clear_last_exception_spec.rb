describe "JavaProxy" do
  it "should clear $! if there is not exception" do
    class JavaTester < org.jruby.RubyString
      field_reader :value
    end

    $!.should == nil
  end
end
