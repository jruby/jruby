require File.expand_path('../../../spec_helper', __FILE__)
require 'csv'
require 'tempfile'

describe "CSV.generate" do

  ruby_version_is "" ... "1.9" do
    before :each do
      @outfile_name = tmp("generate_test_#{$$}.csv")
    end

    it "creates a BasicWriter" do
      writer = CSV::generate(@outfile_name)
      writer.should be_kind_of(CSV::BasicWriter)
      writer.close
    end

    it "accepts a field separator" do
      writer = CSV::generate(@outfile_name, ",")
      writer.should be_kind_of(CSV::BasicWriter)
      writer.close
    end

    it "accepts a row separator" do
      writer = CSV::generate(@outfile_name, ".")
      writer.should be_kind_of(CSV::BasicWriter)
      writer.close
    end

    it "creates a BasicWriter to use in a block" do
      CSV::generate(@outfile_name) do |writer|
        writer.should be_kind_of(CSV::BasicWriter)
      end
    end

    it "creates a BasicWriter with ; as the separator inside the block" do
      CSV::generate(@outfile_name, ?;) do |writer|
        writer.should be_kind_of(CSV::BasicWriter)
      end
    end

    after :each do
      rm_r @outfile_name
    end
  end

  ruby_version_is "1.9" do
    it "returns CSV string" do
      csv_str = CSV.generate do |csv|
        csv.add_row [1, 2, 3]
        csv << [4, 5, 6]
      end
      csv_str.should == "1,2,3\n4,5,6\n"
    end

    it "accepts a col separator" do
      csv_str = CSV.generate(:col_sep => ";") do |csv|
        csv.add_row [1, 2, 3]
        csv << [4, 5, 6]
      end
      csv_str.should == "1;2;3\n4;5;6\n"
    end

    it "appends and returns the argument itself" do
      str = ""
      csv_str = CSV.generate(str) do |csv|
        csv.add_row [1, 2, 3]
        csv << [4, 5, 6]
      end
      csv_str.object_id.should == str.object_id
      str.should == "1,2,3\n4,5,6\n"
    end
  end
end
