require 'java'
require 'fileutils'
require 'rbconfig'

module JRuby
  ZipFile = java.util.zip.ZipFile
  ZipEntry = java.util.zip.ZipEntry

  # extract META-INF/jruby.home/**/* to somewhere.
  class Extract
    def initialize(dir = nil)
      @this_archive = (class_loader = java.lang.Class.forName('org.jruby.Ruby').getClassLoader) &&
        (res = class_loader.getResource('jruby/extract.rb')) && res.to_s[/^(.*!\/)/, 1]
      if need_extract?
        raise "error: can't locate enclosed archive from #{__FILE__}" if @this_archive.nil?
        @this_archive = java.net.URLDecoder.decode(@this_archive, "utf-8")
        @zip = java.net.URL.new(@this_archive).openConnection.jar_file
        @destination = dir || Config::CONFIG['prefix']
      end
    end

    def need_extract?
      !File.directory?(Config::CONFIG['rubylibdir'])
    end

    def entries
      enum = @zip.entries
      def enum.each
        while hasMoreElements
          yield nextElement
        end
      end
      enum
    end

    def extract
      return nil unless need_extract?
      entries.each do |entry|
        if entry.name =~ %r"^META-INF/jruby.home/"
          path = write_entry entry, entry.name.sub(%r{META-INF/jruby.home/},'')
          FileUtils.chmod 0755, path if entry.name =~ %r"jruby.home/bin/"
        elsif entry.name =~ %r"\.rb$"
          write_entry entry, "lib/ruby/1.8/#{entry.name}"
        end
      end

      if @this_archive = @this_archive =~ /^jar:file:(.*)!/ && $1
        puts "copying #{@this_archive} to #{@destination}/lib"
        FileUtils.cp(@this_archive, "#{@destination}/lib")
      end
    end
    
    def write_entry(entry, name)
      entry_path = "#{@destination.sub(%r{/$},'')}/#{name}"
      puts "creating #{entry_path}"
      FileUtils.mkdir_p(File.dirname(entry_path))
      instream = @zip.getInputStream(entry)
      outstream = java.io.FileOutputStream.new(entry_path)
      buffer = java.lang.reflect.Array.newInstance(java.lang.Byte::TYPE, 8192)
      while (num_read = instream.read(buffer)) != -1
        outstream.write buffer, 0, num_read
      end
      entry_path
    ensure
      instream.close if instream
      outstream.close if outstream
    end
  end
end
