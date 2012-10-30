require File.expand_path('../../../spec_helper', __FILE__)
require 'csv'

ruby_version_is "" ... "1.9" do
  describe "CSV.parse_row" do

    it "parses 'foo\nbar' one row at a time" do
      parse_me = "foo\nbar"

      parsed_row = []
      parsed_count, next_row_index = CSV::parse_row parse_me, 0, parsed_row
      parsed_count.should == 1
      next_row_index.should == 4
      parsed_row.should == ['foo']

      parsed_row = []
      parsed_count, next_row_index = CSV::parse_row parse_me, next_row_index, parsed_row
      parsed_count.should == 1
      next_row_index.should == 7
      parsed_row.should == ['bar']

      parsed_row = []
      parsed_count, next_row_index = CSV::parse_row parse_me, next_row_index, parsed_row
      parsed_count.should == 0
      next_row_index.should == 0
      parsed_row.should == []
    end

  end
end
