describe :proc_to_s, :shared => true do
  ruby_version_is ""..."1.9" do
    it "returns a description of self" do
      def hello; end

      Proc.new { "hello" }.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:6>$/
      lambda { "hello" }.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:7>$/
      proc { "hello" }.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:8>$/
      method("hello").to_proc.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:4>$/
    end
  end

  ruby_version_is "1.9" do
    it "returns a description of self" do
      def hello; end

      Proc.new { "hello" }.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:17>$/
      lambda { "hello" }.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:18 \(lambda\)>$/
      proc { "hello" }.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:19>$/
      method("hello").to_proc.send(@method).should =~ /^#<Proc:([^ ]*?)@([^ ]*)\/to_s\.rb:15 \(lambda\)>$/
    end
  end
end
