module Truffle::Tool

  def self.exclude_rspec_examples(exclusions, ignore_missing: false)
    exclusions.each do |mod_name, tests|
      begin
        a_module = Object.const_get mod_name.to_s
      rescue NameError => e
        puts "Exclusion FAILED of module: #{mod_name}"
        if ignore_missing
          next
        else
          raise e
        end
      end

      Array(tests).each do |test|
        print "Excluding: #{a_module}##{test}"
        begin
          a_module.send :undef_method, test
          a_module.send :define_method, test do
            skip 'excluded test'
          end
        rescue NameError => e
          print ' (NOT FOUND)'
          raise e unless ignore_missing
        end
        puts
      end

    end
  end

end
