Object.const_set(:BASE_DIR, Dir.pwd)

# Everything run by JRuby in Rake should have assertions enabled.
ENV['JRUBY_OPTS'] = "#{ENV['JRUBY_OPTS']} -J-ea"

def load_build_properties_into_constants
  constant_names = []
  IO.readlines("default.build.properties").each do |line|
    # skip comments or empty lines (including "\r\n" on windows)
    next if line =~ /(^\W*#|^\r?$)/

    # build const name
    name, value = line.split("=", 2)
    name.gsub!(".", "_")
    name.upcase!
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

  def self.hash_for(filename, method)
    File.open(filename + "."+ method.name.split('::').last.downcase, 'w') do |f|
      f.puts HashTask.new(method.new, filename).calculate_hash
    end
  end
end

# Calculate a md5 checksum and save the file as same name + ".md5"
def md5_checksum(filename)
  HashTask.hash_for(filename, Digest::MD5)
end

# Calculate a sha1 checksum and save the file as same name + ".sha1"
def sha1_checksum(filename)
  HashTask.hash_for(filename, Digest::SHA1)
end

def sha256_checksum(filename)
  HashTask.hash_for(filename, Digest::SHA256)
end

def sha512_checksum(filename)
  HashTask.hash_for(filename, Digest::SHA512)
end

def checksums(filename)
  md5_checksum filename
  sha1_checksum filename
  sha256_checksum filename
  sha512_checksum filename
end

def permute_tests(base_name, options, *prereqs, &block)
  permute_task("test", Rake::TestTask, base_name, options, *prereqs, &block)
end

def permute_specs(base_name, options, *prereqs, &block)
  permute_task("spec", RSpec::Core::RakeTask, base_name, options, *prereqs, &block)
end

def permute_task(task_desc, task_type, base_name, options, *prereqs, &block)
  default_task = nil
  all_tasks = nil

  # iterate over all flag sets, noting default mapping
  tasks = {}
  options.each do |name, flags|
    if name == :default
      default_task = flags
      next
    end

    if name == :all
      all_tasks = flags
      next
    end

    test_task = task_type.new("#{base_name}:#{name}", &block).tap do |t|
      t.ruby_opts ||= []
      flags.each do |flag|
        t.ruby_opts.unshift flag
      end
    end
    tasks[name] = test_task.name
    Rake::Task[test_task.name].tap do |t|
      t.add_description "#{flags.inspect}"
      t.prerequisites.concat prereqs
    end
  end

  # set up default, if specified
  if default_task
    desc "Run #{task_desc}s for #{default_task}"
    task base_name => tasks[default_task]
  end

  # set up "all", if specified, or make it run everything
  all_tasks ||= tasks.keys
  desc "Run #{task_desc}s for #{all_tasks.inspect}"
  task "#{base_name}:all" => all_tasks.map {|key| tasks[key]}
end
