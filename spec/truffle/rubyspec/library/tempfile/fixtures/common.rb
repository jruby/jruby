module TempfileSpecs
  def self.cleanup(tempfile)
    tempfile.close true unless tempfile.closed?
    File.delete tempfile.path if tempfile.path and File.exists? tempfile.path
  end
end
