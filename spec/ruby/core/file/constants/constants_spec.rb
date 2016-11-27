require File.expand_path('../../../../spec_helper', __FILE__)

["APPEND", "CREAT", "EXCL", "FNM_CASEFOLD",
  "FNM_DOTMATCH", "FNM_EXTGLOB", "FNM_NOESCAPE", "FNM_PATHNAME",
  "FNM_SYSCASE", "LOCK_EX", "LOCK_NB", "LOCK_SH",
  "LOCK_UN", "NONBLOCK", "RDONLY",
  "RDWR", "TRUNC", "WRONLY"].each do |const|
  describe "File::Constants::#{const}" do
    it "is defined" do
      File::Constants.const_defined?(const).should be_true
    end
  end
end

platform_is :windows do
  describe "File::Constants::BINARY" do
    it "is defined" do
      File::Constants.const_defined?(:BINARY).should be_true
    end
  end
end

platform_is_not :windows do
  ["NOCTTY", "SYNC"].each do |const|
    describe "File::Constants::#{const}" do
      it "is defined" do
        File::Constants.const_defined?(const).should be_true
      end
    end
  end
end

ruby_version_is "2.3" do
  platform_is :linux do
    describe "File::TMPFILE" do
      it "is defined" do
        # Since Linux 3.11, does not work Travis (probably because built on a older host).
        has_tmpfile = !ENV.key?('TRAVIS') && (`uname -r`.chomp.split('.').map(&:to_i) <=> [3,11]) >= 0
        File.const_defined?(:TMPFILE).should == has_tmpfile
      end
    end
  end
end
