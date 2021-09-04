unless RbConfig::CONFIG['host_os'] == 'mswin32'
  require 'tempfile'
  require 'fileutils'

  describe "Paths added to $LOADED_FEATURES by require" do
    it "do not expand symlinks" do
      begin
        dir = "GH1941"
        Dir.mkdir(dir)
        File.symlink(dir, dir + ".link")
        file = File.open("GH1941/test.rb", "w")

        expect($LOADED_FEATURES.inspect["GH1941"]).to eq(nil)
      ensure
        file.close rescue nil
        FileUtils.rm_rf(dir) rescue nil
        FileUtils.rm_rf(dir + ".link") rescue nil
      end
    end
  end
end