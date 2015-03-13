require File.expand_path('../shared/matched_size.rb', __FILE__)
require 'strscan'

ruby_version_is "" ... "1.9" do
  describe "StringScanner#matchedsize" do
    it_behaves_like(:strscan_matched_size, :matchedsize)

    it "warns in verbose mode that the method is obsolete" do
      s = StringScanner.new("abc")
      begin
        old = $VERBOSE
        lambda {
          $VERBOSE = true
          s.matchedsize
        }.should complain(/matchedsize.*obsolete.*matched_size/)

        lambda {
          $VERBOSE = false
          s.matchedsize
        }.should_not complain
      ensure
        $VERBOSE = old
      end
    end
  end
end
