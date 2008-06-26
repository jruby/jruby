module Gem

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # We don't want gems installed in lib/jruby/gems, but rather to
  # preserve the old location: lib/ruby/gems.
  def self.default_dir
    File.join ConfigMap[:libdir], 'ruby', 'gems', ConfigMap[:ruby_version]
  end
end
