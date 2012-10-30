class Object
  # Returns the name of a fixture file by adjoining the directory
  # of the +dir+ argument with "fixtures" and the contents of the
  # +args+ array. For example,
  #
  #   +dir+ == "some/path"
  #
  # and
  #
  #   +args+ == ["dir", "file.txt"]
  #
  # then the result is the expanded path of
  #
  #   "some/fixtures/dir/file.txt".
  def fixture(dir, *args)
    path = File.dirname(dir)
    path = path[0..-7] if path[-7..-1] == "/shared"
    dir = path[-9..-1] == "/fixtures" ? "" : "fixtures"
    File.expand_path(File.join(path, dir, args))
  end
end
