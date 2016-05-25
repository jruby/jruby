module Truffle

  def self.exclude_rspec_examples(exclusions)
    exclusions.each do |mod_name, tests|

      a_module = Object.const_get mod_name

      Array(tests).each do |test|
        puts "Excluding: #{a_module}##{test}"
        a_module.send :undef_method, test
        a_module.send :define_method, test do
          skip 'excluded test'
        end
      end

    end
  end

end
