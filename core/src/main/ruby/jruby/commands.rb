require 'rbconfig'

module JRuby
  class Commands
    class << self
      dollar_zero = RbConfig::CONFIG['bindir'] + "/#{$0}"
      # Provide method aliases to scripts commonly found in ${jruby.home}/bin.
      ruby_bin = File.open(dollar_zero) {|io| (io.readline rescue "") =~ /^#!.*ruby/} rescue nil
      if ruby_bin
        define_method File.basename(dollar_zero) do
          $0 = dollar_zero
          load dollar_zero
        end
      end

      def generate_dir_info( dir, is_root = true )
        return if dir != nil && !File.directory?( dir )
        File.open( dir + '/.jrubydir', 'w' ) do |f|
          f.puts ".." unless is_root
          f.puts "."
          Dir[ dir + '/*'].entries.each do |e|
            f.print File.basename( e )
            if File.directory?( e )
              generate_dir_info( e, false )
            end
            f.puts
          end
        end
      end

      def maybe_install_gems
        require 'rubygems'
        require 'rubygems/package'

        ARGV.delete_if do |g|
          # skip options
          next false if g =~ /^-/
          
          if File.exist?(g) # local gem
            begin
              gem = Gem::Package.new(g)
              name = gem.spec.name
              ver = gem.spec.version
              dep = Gem::Dependency.new(name, ver)

              # check, whether the same gem is already installed
              Gem::DependencyResolver.for_current_gems([dep]).resolve
            rescue Gem::Exception
              false
            end
          else
            raise "no local gem found for #{g}"
          end
        end

        unless ARGV.reject{|a| a =~ /^-/}.empty?
          ARGV.unshift "install"
          begin
            load RbConfig::CONFIG['bindir'] + "/gem"
          rescue SystemExit => e
            # don't exit in case of 0 return value from 'gem'
            exit(e.status) unless e.success?
          end
        end
        generate_bat_stubs
      end

      def generate_bat_stubs
        Dir[RbConfig::CONFIG['bindir'] + '/*'].each do |fn|
          next unless File.file?(fn)
          next if fn =~ /.bat$/
          next if File.exist?("#{fn}.bat")
          next unless File.open(fn, 'r', :internal_encoding => 'ASCII-8BIT') do |io|
            line = io.readline rescue ""
            line =~ /^#!.*ruby/
          end
          puts "Generating #{File.basename(fn)}.bat"
          File.open("#{fn}.bat", "wb") do |f|
            f << "@ECHO OFF\r\n"
            f << "@\"%~dp0jruby.exe\" -S #{File.basename(fn)} %*\r\n"
          end
        end if File.writable?(File.join(RbConfig::CONFIG['bindir'], 'jruby.bash'))
      end

      def method_missing(name, *)
        $stderr.puts "jruby: No such file or directory -- #{name} (LoadError)" # matches MRI's output
        exit 1
      end
    end
  end
end

