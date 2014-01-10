require File.expand_path('../../../spec_helper', __FILE__)
require 'csv'

describe "CSV.generate_line" do

  ruby_version_is "" ... "1.9" do
    it "generates an empty string" do
      result = CSV::generate_line([])
      result.should == ""
    end

    it "generates the string 'foo,bar'" do
      result = CSV::generate_line(["foo", "bar"])
      result.should == "foo,bar"
    end

    it "generates the string 'foo;bar'" do
      result = CSV::generate_line(["foo", "bar"], ?;)
      result.should == "foo;bar"
    end

    it "generates the string 'foo,,bar'" do
      result = CSV::generate_line(["foo", nil, "bar"])
      result.should == "foo,,bar"
    end

    it "generates the string 'foo;;bar'" do
      result = CSV::generate_line(["foo", nil, "bar"], ?;)
      result.should == "foo;;bar"
    end
  end

  ruby_version_is "1.9" do
    it "generates an empty string" do
      result = CSV::generate_line([])
      result.should == "\n"
    end

    it "generates the string 'foo,bar'" do
      result = CSV::generate_line(["foo", "bar"])
      result.should == "foo,bar\n"
    end

    it "generates the string 'foo;bar'" do
      result = CSV::generate_line(["foo", "bar"], :col_sep => ?;)
      result.should == "foo;bar\n"
    end

    it "generates the string 'foo,,bar'" do
      result = CSV::generate_line(["foo", nil, "bar"])
      result.should == "foo,,bar\n"
    end

    it "generates the string 'foo;;bar'" do
      result = CSV::generate_line(["foo", nil, "bar"], :col_sep => ?;)
      result.should == "foo;;bar\n"
    end
  end
end
