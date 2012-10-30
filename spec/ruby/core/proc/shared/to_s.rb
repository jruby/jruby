describe :proc_to_s, :shared => true do
  ruby_version_is ""..."1.9" do
    it "returns a description of self" do
      Proc.new { "hello" }.send(@method).should =~ /^#<Proc:(.*?)@(.*)\/to_s\.rb:4>$/
      lambda { "hello" }.send(@method).should =~ /^#<Proc:(.*?)@(.*)\/to_s\.rb:5>$/
      proc { "hello" }.send(@method).should =~ /^#<Proc:(.*?)@(.*)\/to_s\.rb:6>$/
    end
  end

  ruby_version_is "1.9" do
    it "returns a description of self" do
      Proc.new { "hello" }.send(@method).should =~ /^#<Proc:(.*?)@(.*)\/to_s\.rb:12>$/
      lambda { "hello" }.send(@method).should =~ /^#<Proc:(.*?)@(.*)\/to_s\.rb:13 \(lambda\)>$/
      proc { "hello" }.send(@method).should =~ /^#<Proc:(.*?)@(.*)\/to_s\.rb:14>$/
    end
  end
end
