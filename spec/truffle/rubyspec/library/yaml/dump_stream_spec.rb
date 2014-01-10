require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)

describe "YAML.dump_stream" do
  ruby_version_is "" ... "2.0" do
    it "returns an empty string when not passed any objects" do
      YAML.dump_stream.should == ""
    end
  end

  it "returns a YAML stream containing the objects passed" do
    YAML.dump_stream('foo', 20, [], {}).should match_yaml("--- foo\n--- 20\n--- []\n\n--- {}\n\n")
  end
end
