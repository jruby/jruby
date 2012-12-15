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

# def ant(*args)
#   raise 'running ant failed!' unless system "ant -logger org.apache.tools.ant.NoBannerLogger #{args.join(' ')}"
# end
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