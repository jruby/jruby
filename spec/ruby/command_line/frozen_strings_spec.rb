require File.expand_path('../../spec_helper', __FILE__)

ruby_version_is "2.3" do
  describe "The --enable-frozen-string-literal flag causes string literals to" do

    it "produce the same object each time" do
      ruby_exe(fixture(__FILE__, "freeze_flag_one_literal.rb"), options: "--enable-frozen-string-literal").chomp.should == "true"
    end

    it "produce the same object for literals with the same content" do
      ruby_exe(fixture(__FILE__, "freeze_flag_two_literals.rb"), options: "--enable-frozen-string-literal").chomp.should == "true"
    end

    it "produce the same object for literals with the same content in different files" do
      ruby_exe(fixture(__FILE__, "freeze_flag_across_files.rb"), options: "--enable-frozen-string-literal").chomp.should == "true"
    end

    it "produce different objects for literals with the same content in different files if they have different encodings" do
      ruby_exe(fixture(__FILE__, "freeze_flag_across_files_diff_enc.rb"), options: "--enable-frozen-string-literal").chomp.should == "true"
    end

  end
end
