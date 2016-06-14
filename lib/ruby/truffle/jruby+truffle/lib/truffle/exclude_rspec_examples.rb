module Truffle

  def self.exclude_rspec_examples(exclusions)
    exclusions.each do |mod_name, tests|

      a_module = Object.const_get mod_name.to_s

      Array(tests).each do |test|
        print "Excluding: #{a_module}##{test}"
        begin
          a_module.send :undef_method, test
          a_module.send :define_method, test do
            skip 'excluded test'
          end
        rescue NameError => e
          print ' (NOT FOUND)'
        end
        puts
      end

    end
  end

end
