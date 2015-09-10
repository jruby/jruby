require 'rspec'
require 'fileutils'

describe 'GH-2264: Illegal hex characters in escape pattern' do
  let(:dir_name) { 'test_dir!' }
  let(:file_name) { 'test_file%' }

  before :each do
    FileUtils.mkdir(dir_name)
    FileUtils.touch("#{dir_name}/#{file_name}")
  end

  after :each do
    FileUtils.rm_r(dir_name) if Dir.exist?(dir_name)
  end

  it 'can glob directories' do
    expect(Dir.glob("#{dir_name}/**/*").size).to eq 1
  end
end
