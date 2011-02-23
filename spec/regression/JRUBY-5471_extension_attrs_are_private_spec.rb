require 'rspec'
require 'java'
$CLASSPATH << File.expand_path('../../../build/classes/test', __FILE__)
require 'dummy/dummy'

describe 'A Java-based BasicLibraryService extension' do
  it "gets its own frame with public visibility" do
    d = XYZ_Dummy_XYZ.new
    d.dummy_attr = 'foo'
    d.dummy_attr.should == 'foo'
  end
end
