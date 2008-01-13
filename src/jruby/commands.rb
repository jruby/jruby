require 'rbconfig'

module JRuby
  class Commands
    class << self
      # Provide method aliases to scripts commonly found in ${jruby.home}/bin.
      if File.directory?(Config::CONFIG['bindir'])
        Dir[Config::CONFIG['bindir'] + '/*'].each do |f|
          if File.file?(f) && File.open(f) {|io| (io.readline rescue "") =~ /^#!.*ruby/}
            meth = File.basename(f)
            define_method meth do
              require 'jruby/extract'
              JRuby::Extract.new.extract
              load f
            end
          end
        end
      else
        # allow use of 'gem' and 'jirb' without prior extraction
        def gem
          require 'jruby/extract'
          JRuby::Extract.new.extract
          require 'rubygems'
          require 'rubygems/gem_runner'
          Gem.manage_gems
          Gem::GemRunner.new.run(ARGV)
        end

        def jirb
          require 'irb'
          IRB.start(__FILE__)
        end
      end

      def maybe_install_gems
        require 'jruby/extract'
        JRuby::Extract.new.extract
        require 'rubygems'
        ARGV.delete_if do |g|
          begin
            # Want the kernel gem method here
            Object.new.gem g
            puts "#{g} already installed"
            true
          rescue Gem::LoadError
            false
          end
        end
        unless ARGV.reject{|a| a =~ /^-/}.empty?
          ARGV.unshift "install"
          self.gem
        end 
      end

      def generate_bat_stubs
        Dir[Config::CONFIG['bindir'] + '/*'].each do |fn|
          next unless File.file?(fn)
          next if fn =~ /.bat$/
          next if File.exist?("#{fn}.bat")
          next unless File.open(fn) {|io| (io.readline rescue "") =~ /^#!.*ruby/}
          puts "Generating #{File.basename(fn)}.bat"
          File.open("#{fn}.bat", "wb") do |f|
            f << "@echo off\n"
            f << "call \"%~dp0jruby\" -S #{File.basename(fn)} %*\n"
          end
        end
      end

      def extract
        require 'jruby/extract'
        JRuby::Extract.new(ARGV.first).extract
      end

      alias_method :irb, :jirb
    end
  end
end
