require File.dirname(__FILE__) + "/../spec_helper"

describe "Access constants defined in interfaces" do
  it "should be able to see lower case constants as methods" do
    expect(java::sql::ResultSetMetaData.columnNullable).to eq(1)
  end
end
