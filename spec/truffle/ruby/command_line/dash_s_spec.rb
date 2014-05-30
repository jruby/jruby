require File.expand_path('../../spec_helper', __FILE__)

describe "The -s command line option" do
  describe "when using -- to stop parsing" do
    it "sets the value to true without an explicit value" do
      ruby_exe("p $n", :escape => true,
                          :options => "-s",
                          :args => "-- -n").chomp.should == "true"
    end

    it "parses single letter args into globals" do
      ruby_exe("puts $n", :escape => true,
                          :options => "-s",
                          :args => "-- -n=blah").chomp.should == "blah"
    end

    it "parses long args into globals" do
      ruby_exe("puts $_name", :escape => true,
                          :options => "-s",
                          :args => "-- --name=blah").chomp.should == "blah"
    end

    it "converts extra dashes into underscorse" do
      ruby_exe("puts $___name", :escape => true,
                          :options => "-s",
                          :args => "-- ----name=blah").chomp.should == "blah"
    end
  end

  describe "when running a script" do
    before :all do
      @script = fixture __FILE__, "dash_s_script.rb"
    end

    it "sets the value to true without an explicit value" do
      ruby_exe(@script, :options => "-s",
                        :args => "-n 0").chomp.should == "true"
    end

    it "parses single letter args into globals" do
      ruby_exe(@script, :options => "-s",
                        :args => "-n=blah 1").chomp.should == "blah"
    end

    it "parses long args into globals" do
      ruby_exe(@script, :options => "-s",
                        :args => "--name=blah 2").chomp.should == "blah"
    end

    it "converts extra dashes into underscorse" do
      ruby_exe(@script, :options => "-s",
                        :args => "----name=blah 3").chomp.should == "blah"
    end

  end
end
