require 'rbconfig'

module JRuby
  class Commands
    class << self
      dollar_zero = Config::CONFIG['bindir'] + "/#{$0}"
      # Provide method aliases to scripts commonly found in ${jruby.home}/bin.
      ruby_bin = File.open(dollar_zero) {|io| (io.readline rescue "") =~ /^#!.*ruby/} rescue nil
      if ruby_bin
        define_method File.basename(dollar_zero) do
          $0 = dollar_zero
          load dollar_zero
        end
      end

      def maybe_install_gems
        require 'rubygems'
        ARGV.delete_if do |g|
          begin
            # Want the kernel gem method here
            # Hack to make it public; RubyGems 1.3.1 made it private. TODO: less grossness.
            Object.class_eval { public :gem }
            Object.new.gem g
            puts "#{g} already installed"
            true
          rescue Gem::LoadError
            false
          end
        end
        unless ARGV.reject{|a| a =~ /^-/}.empty?
          ARGV.unshift "install"
          load Config::CONFIG['bindir'] + "/gem"
        end
        generate_bat_stubs
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

      def method_missing(name, *)
        $stderr.puts "jruby: No such file, directory, or command -- #{name}"
      end
    end
  end
end

