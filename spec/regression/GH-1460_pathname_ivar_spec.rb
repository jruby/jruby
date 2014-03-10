require 'pathname'

describe "Pathname @path ivar should be transparent to Ruby land" do
  specify "instance_variable_get/set" do
    path = Pathname.new('test')
    path.instance_variable_get(:@path).should == 'test'
    path.instance_variable_set(:@path, 'foo')
    path.to_path.should == 'foo'
  end

  specify "YAML deserialization" do
    require 'yaml'
    path = Pathname.new('foo')
    yaml = path.to_yaml
    deserialized = YAML.load(yaml)
    deserialized.to_path.should == 'foo'
  end
end
