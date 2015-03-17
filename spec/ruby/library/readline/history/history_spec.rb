require File.expand_path('../../../../spec_helper', __FILE__)

process_is_foreground do
  with_feature :readline do
    require 'readline'

    describe "Readline::HISTORY" do
      it "is extended with the Enumerable module" do
        Readline::HISTORY.should be_kind_of(Enumerable)
      end
    end
  end
end
