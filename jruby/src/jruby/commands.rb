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
              JRuby::Extract.new.extract unless File.exist?(Config::CONFIG['bindir'] + "/jruby")
              load f
            end
          end
        end
      else
        # allow use of 'gem' and 'jirb' without prior extraction
        def gem
          require 'jruby/extract'
          JRuby::Extract.new.extract unless File.exist?(Config::CONFIG['bindir'] + "/jruby")
          require 'rubygems'
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
        JRuby::Extract.new.extract unless File.exist?(Config::CONFIG['bindir'] + "/jruby")
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
        unless ARGV.empty?
          ARGV.unshift "install"
          ARGV << "-y" << "--no-ri" << "--no-rdoc"
          self.gem
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
