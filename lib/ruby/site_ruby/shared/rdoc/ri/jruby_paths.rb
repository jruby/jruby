module RI
  # Replace SYSDIR with the jar URL and put it in PATH
  module Paths
    version = Config::CONFIG['ruby_version']
    base    = File.join("file:" + Config::CONFIG['datadir'], "ri.jar!/ri", version)
    SYSDIR.replace(File.join(base, "system"))
    PATH.unshift SYSDIR
  end
end