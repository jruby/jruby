# results_to_excludes: convert test/unit output to exclude files based on failure lines
#
# usage: results_to_excludes result_output.txt target_dir "exclude reason"

require 'fileutils'

results_filename = ARGV[0]
target_dir = ARGV[1] || "excludes"
comment = ARGV[2] || "work in progress"

cls_to_method = Hash.new {|h, k| h[k] = []}
cls_to_file = Hash.new {|h, k| h[k] = []}

File.readlines(results_filename).grep(/^\[\w*\d+\/\d+\] (.*) =/) {|l|
  failure = $1
  cls, method = failure.split('#')
  cls_pieces = cls.split('::')
  file = cls_pieces[-1] + '.rb'
  if cls_pieces.size > 1
    dir = cls_pieces[0...-1].join('/')
    cls_to_file[cls] = [dir, file]
  else
    cls_to_file[cls] = [file]
  end

  cls_to_method[cls] << method
}

cls_to_file.each do |cls, path|
  puts path.join("/")
  target = target_dir
  if path.size == 2
    target = File.join(target_dir, path[0])
  end
  FileUtils.mkdir_p(target)

  File.open(File.join(target, path[-1]), 'w+') do |file|
    cls_to_method[cls].sort.each do |method|
      file.puts "exclude :\"#{method}\", \"#{comment}\""
    end
  end
end
