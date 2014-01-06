require File.expand_path('../../../spec_helper', __FILE__)

process_is_foreground do
  with_feature :readline do
    require 'readline'

    describe "Readline.completion_append_character" do
      it "returns not nil" do
        Readline.completion_append_character.should_not be_nil
      end
    end

    describe "Readline.completion_append_character=" do
      it "returns the first character of the passed string" do
        Readline.completion_append_character = "test"
        Readline.completion_append_character.should == "t"
      end
    end
  end
end
