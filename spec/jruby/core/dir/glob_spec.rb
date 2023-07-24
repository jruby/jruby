require 'rspec'
require 'fileutils'

describe "Dir.glob" do
  let(:dir_name) { 'test_dir!' }
  let(:file_name) { 'test_file%' }

  before :each do
    FileUtils.mkdir(dir_name)
    FileUtils.touch("#{dir_name}/#{file_name}")
  end

  after :each do
    FileUtils.rm_r(dir_name) if Dir.exist?(dir_name)
  end

  it 'can glob directories with URI special chars in it (GH-2264)' do
    expect(Dir.glob("#{dir_name}/**/*").size).to eq 1
  end
end

describe "Dir.glob" do
  # We have common option processing code and a logic mistake made
  # us process argv[2] as both a kwarg then try to convert it to an
  # integer.
  it "does not think the third arg should be an integer when it is kwargs" do
    expect { Dir["*", "*", base: "."] }.not_to raise_error
  end
end


