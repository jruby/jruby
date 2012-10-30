require File.expand_path('../../../../spec_helper', __FILE__)

process_is_foreground do
  with_feature :readline do
    require 'readline'

    describe "Readline::HISTORY.to_s" do
      it "returns 'HISTORY'" do
        Readline::HISTORY.to_s.should == "HISTORY"
      end
    end
  end
end
