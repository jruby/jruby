require 'rspec'
require 'tmpdir'

describe "File.join" do
  context "when a file has the backslash as the initial character" do
    it "should build the correct path" do
      file_name = "\\tmpfile.txt"
      dir = Dir.tmpdir
      file_path = File.join(Dir.tmpdir, file_name)
      begin
        file_path.should == "#{dir}#{File::SEPARATOR}#{file_name}"
      ensure
        Dir.chdir(dir) do
          Dir["*tmpfile.txt"].each do |f|
            File.delete f
          end
        end
      end
    end
  end
end
