# Load built-in rbconfig library
JRuby::Util.load_ext("org.jruby.ext.rbconfig.RbConfigLibrary")

module RbConfig
  # call-seq:
  #
  #   RbConfig.expand(val)         -> string
  #   RbConfig.expand(val, config) -> string
  #
  # expands variable with given +val+ value.
  #
  #   RbConfig.expand("$(bindir)") # => /home/foobar/all-ruby/ruby19x/bin
  def RbConfig::expand(val, config = CONFIG)
    newval = val.gsub(/\$\$|\$\(([^()]+)\)|\$\{([^{}]+)\}/) {
      var = $&
      if !(v = $1 || $2)
        '$'
      elsif key = config[v = v[/\A[^:]+(?=(?::(.*?)=(.*))?\z)/]]
        pat, sub = $1, $2
        config[v] = false
        config[v] = RbConfig::expand(key, config)
        key = key.gsub(/#{Regexp.quote(pat)}(?=\s|\z)/n) {sub} if pat
        key
      else
        var
      end
    }
    val.replace(newval) unless newval == val
    val
  end
  CONFIG.each_value do |val|
    RbConfig::expand(val)
  end

  # call-seq:
  #
  #   RbConfig.fire_update!(key, val)               -> array
  #   RbConfig.fire_update!(key, val, mkconf, conf) -> array
  #
  # updates +key+ in +mkconf+ with +val+, and all values depending on
  # the +key+ in +mkconf+.
  #
  #   RbConfig::MAKEFILE_CONFIG.values_at("CC", "LDSHARED") # => ["gcc", "$(CC) -shared"]
  #   RbConfig::CONFIG.values_at("CC", "LDSHARED")          # => ["gcc", "gcc -shared"]
  #   RbConfig.fire_update!("CC", "gcc-8")                  # => ["CC", "LDSHARED"]
  #   RbConfig::MAKEFILE_CONFIG.values_at("CC", "LDSHARED") # => ["gcc-8", "$(CC) -shared"]
  #   RbConfig::CONFIG.values_at("CC", "LDSHARED")          # => ["gcc-8", "gcc-8 -shared"]
  #
  # returns updated keys list, or +nil+ if nothing changed.
  def RbConfig.fire_update!(key, val, mkconf = MAKEFILE_CONFIG, conf = CONFIG) # :nodoc:
    return if mkconf[key] == val
    mkconf[key] = val
    keys = [key]
    deps = []
    begin
      re = Regexp.new("\\$\\((?:%1$s)\\)|\\$\\{(?:%1$s)\\}" % keys.join('|'))
      deps |= keys
      keys.clear
      mkconf.each {|k,v| keys << k if re =~ v}
    end until keys.empty?
    deps.each {|k| conf[k] = mkconf[k].dup}
    deps.each {|k| expand(conf[k])}
    deps
  end

  # call-seq:
  #
  #   RbConfig.ruby -> path
  #
  # returns the absolute pathname of the ruby command.
  def RbConfig.ruby
    File.join(
      RbConfig::CONFIG["bindir"],
      RbConfig::CONFIG["ruby_install_name"] + RbConfig::CONFIG["EXEEXT"]
    )
  end
end
