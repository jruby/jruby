require 'rspec'

if RbConfig::CONFIG['host_os'] =~ /mingw|mswin/ 
  describe "GH-2008: multi args 'system' method on Windows" do
    let(:test_dir_name){"this_is_test_dir"}
    before :each do
      if Dir.exists? test_dir_name
        Dir.rmdir test_dir_name
      end
    end
    after :each do
      if Dir.exists? test_dir_name
        Dir.rmdir test_dir_name
      end
    end

    it "can create directory by intenal command" do
      result = system("mkdir", test_dir_name)
      expect(result).to be_truthy
      expect(Dir).to be_exists(test_dir_name)
    end
  end
end
