Object.const_set(:BASE_DIR, Dir.pwd)

def load_build_properties_into_constants
  constant_names = []
  IO.readlines("default.build.properties").each do |line|
    # skip comments
    next if line =~ /(^\W*#|^$)/

    # build const name
    name, value = line.split("=", 2)
    name.gsub!(".", "_").upcase!
    constant_names << name
    Object.const_set(name.to_sym, value)
  end

  # two-pass so substitutions can appear above where the var is defined
  constant_names.each do |name|
    Object.const_get(name).chop!.gsub!(/\$\{([^}]+)\}/) do |embed|
      Object.const_get($1.gsub!(".", "_").upcase!)
    end
    puts "#{name} = #{Object.const_get(name)}" if Rake.application.options.trace
  end
end
load_build_properties_into_constants

def ant(*args)
  raise 'running ant failed!' unless system "ant -logger org.apache.tools.ant.NoBannerLogger #{args.join(' ')}"
end
require 'digest'

class HashTask < Struct.new(:hash, :file)
  BUF = 100 * 1024

  def calculate_hash
    open(file) do |io|
      while !io.eof
        hash.update io.readpartial(BUF)
      end
    end
    hash.hexdigest
  end

  def self.hash_for(filename, method=Digest::MD5)
    File.open(filename + "."+ method.name.split('::').last.downcase, 'w') do |f|
      f.puts HashTask.new(method.new, filename).calculate_hash
    end
  end
end

# Calculate a md5 checksum and save the file as same name + ".md5"
def md5_checksum(filename)
  HashTask.hash_for(filename)
end

# Calculate a sha1 checksum and save the file as same name + ".sha1"
def sha1_checksum(filename)
  HashTask.hash_for(filename, Digest::SHA1)
end

