file = File.open("../../../jruby-patches/test_output_more.txt", "rb")
contents = file.read

## Expirimental MRI test tagging script
## Needs to be run from the jruby/test/mri directory
## Error console output read from the file path above

## Usage
## E.g.
# Create txt file with MRI test output into the file and set the path above
# cd test/mri
# ruby ../../tool/truffle/parse_mri_errors.rb

load_error_output = "0 tests, 0 assertions, 0 failures, 0 errors, 0 skips"

summary_regex = /\d+\stests,\s\d+\sassertions,\s\d+\sfailures,\s\d+\serrors,\s\d+\sskips/
split_errors = contents.split(load_error_output)
if split_errors.size > 1

  puts "split_errors #{split_errors.size}"
  err_files = split_errors.collect { |e| e.scan(/filesf \[\"(.*)\"\]/).last[0] }
  patt =  err_files.collect { |err| err.split("/mri/")[1] }

  all_tests = contents.scan(/filesf \[\"(.*)\"\]/)
  all_tests_patt = all_tests.collect { |err| err[0].split("/mri/")[1] }

  non_excluded = all_tests_patt - patt

  puts "# Test index"

  i_hash = Hash[non_excluded.collect { |v| [v, true] }]
  e_hash = Hash[patt.collect { |v| [v, false] }]

  all_hash = i_hash.merge(e_hash)
  all_hash = Hash[all_hash.sort_by{|k,v| k}]
  all_hash.each do |k,v|
    if v
      puts k
    else
      puts "# #{k}"
    end
  end

end

t = /(\w+(::))?(.*)#(.*).*=.*=\s([.FE])/

require 'fileutils'
test_results = contents.scan(t)
test_results.each do |r|
  if r[0]
    unless r[4] == "."
      dirname = "excludes_truffle/#{r[0].chop.chop}"
      #puts "dirname #{dirname}"
      Dir.mkdir(dirname) unless Dir.exist?(dirname)
      if r[2].include?("::")
        name_split = r[2].split('::')
        nested_dirname = "#{dirname}/#{name_split[0]}"
        Dir.mkdir(nested_dirname) unless Dir.exist?(nested_dirname)
        File.open("#{nested_dirname}/#{name_split[1]}.rb", 'a') {|f| f.write("exclude :#{r[3].strip}, \"needs investigation\"\n") }
      else
        File.open("#{dirname}/#{r[2]}.rb", 'a') {|f| f.write("exclude :\"#{r[3].strip}\", \"needs investigation\"\n") }
      end
    end
  else
    unless r[4] == "."
      File.open("excludes_truffle/#{r[2]}.rb", 'a') {|f| f.write("exclude :\"#{r[3].strip}\", \"needs investigation\"\n") }
    end
  end
end

