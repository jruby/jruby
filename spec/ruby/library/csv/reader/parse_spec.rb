require File.expand_path('../../../../spec_helper', __FILE__)
require 'csv'

ruby_version_is "" ... "1.9" do
  describe "CSV::Reader.parse" do

    it "processes empty input without calling block" do
      empty_input = mock('empty file')
      empty_input.should_receive(:read).once.and_return(nil)
      CSV::Reader.parse(empty_input) do |row|
        Expectation.fail_with('block should not be executed', 'but executed')
      end
    end

    it "calls block once for one row of input" do
      input_stream = File.open(File.dirname(__FILE__) + '/../fixtures/one_line.csv', 'rb')
      count = 0
      CSV::Reader.parse(input_stream) do |row|
        count += 1
      end
      count.should == 1
    end

  end
end
