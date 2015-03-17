module TempfileSpecs
  def self.cleanup(tempfile)
    tempfile.close true unless tempfile.closed?
    File.delete tempfile.path if tempfile.path and File.exist? tempfile.path
  end
end
