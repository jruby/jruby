describe "JRUBY-6339: File does not load from inside path with a '#' symbol" do
  it "should load a file from inside a path with a '#' symbol with the current directory set as the load path" do
    Dir.chdir(File.expand_path("../dir#with##hashes", __FILE__)) do
      begin
        $LOAD_PATH.unshift "."

        require("foo")
        $LOADED_FEATURES.pop.should =~ /foo\.rb$/
      ensure
        $LOAD_PATH.shift
      end
    end
  end
end

