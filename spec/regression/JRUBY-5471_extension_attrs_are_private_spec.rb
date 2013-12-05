path = File.expand_path('../../../target/test-classes', __FILE__)
# This test depends on test build. Skip if test classes are not built.
if File.exist?(File.join(path, 'dummy'))
  require 'rspec'
  require 'java'
  $CLASSPATH << path
  require 'dummy/dummy'

  describe 'A Java-based BasicLibraryService extension' do
    it "gets its own frame with public visibility" do
      d = XYZ_Dummy_XYZ.new
      d.dummy_attr = 'foo'
      d.dummy_attr.should == 'foo'
    end
  end
end
