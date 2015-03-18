require File.expand_path('../../../spec_helper', __FILE__)

process_is_foreground do
  with_feature :readline do
    require 'readline'

    describe "Readline.basic_quote_characters" do
      it "returns not nil" do
        Readline.basic_quote_characters.should_not be_nil
      end
    end

    describe "Readline.basic_quote_characters=" do
      it "returns the passed string" do
        Readline.basic_quote_characters = "test"
        Readline.basic_quote_characters.should == "test"
      end
    end
  end
end
