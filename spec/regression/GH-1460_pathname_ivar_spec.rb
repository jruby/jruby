require 'pathname'

describe "Pathname @path ivar should be transparent to Ruby land" do
  specify "instance_variable_get/set" do
    path = Pathname.new('test')
    expect(path.instance_variable_get(:@path)).to eq('test')
    path.instance_variable_set(:@path, 'foo')
    expect(path.to_path).to eq('foo')
  end

  specify "YAML deserialization" do
    require 'yaml'
    path = Pathname.new('foo')
    yaml = path.to_yaml
    deserialized = YAML.load(yaml)
    expect(deserialized.to_path).to eq('foo')
  end
end
