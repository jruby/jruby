require 'zlib'
module RI
  # Modified version of RiCache#initialize to use a marshalled data file for
  # the "system" docs.
  class RiCache

    attr_reader :toplevel

    def initialize(dirs)
      # At the top level we have a dummy module holding the
      # overall namespace
      @toplevel = TopLevelEntry.new('', '::', nil)

      dirs.each do |dir|
        if dir == RI::Paths::SYSDIR
          # If we're loading from SYSDIR, try to use the cached index, or write one
          # if non exists.
          cache_file = File.join(Config::CONFIG['datadir'], 'ri_cache.bin.gz');

          if File.exist?(cache_file)
            @toplevel.load_cache(cache_file)
          else
            @toplevel.load_from(dir)

            if File.writable? Config::CONFIG['datadir']
              begin
                @toplevel.write_cache(cache_file)
              rescue Exception
                if $DEBUG
                  $stderr.puts "could not write RI cache file: #{cache_file}"
                end
              end
            end
          end
        else
          @toplevel.load_from(dir)
        end
      end
    end
  end
  
  DIR_MARKER = "RI_DOCS_DIR"

  class TopLevelEntry
    # load a cached RI index from the file specified and add it to entry's state
    def load_cache(file)
      data = nil
      Zlib::GzipReader.open(file) do |gz|
        data = Marshal.load(gz.read)
      end
      @class_methods.concat(data[0])
      @instance_methods.concat(data[1])
      @inferior_classes.concat(data[2])
    end

    # save current entry state to the specified RI index cache file
    def write_cache(file)
      Zlib::GzipWriter.open(file) do |gz|
        dumped = Marshal.dump([@class_methods, @instance_methods, @inferior_classes])
        gz.write(dumped)
      end
    end
  end

  class ClassEntry
    # custom dump to strip out system-specific SYSDIR and replace with a marker
    def marshal_dump
      [
        @path_names.map {|path_name| path_name.gsub(RI::Paths::SYSDIR, DIR_MARKER)},
        @name,
        @in_class,
        @class_methods,
        @instance_methods,
        @inferior_classes
      ]
    end

    # custom load to replace the marker with system-specific SYSDIR
    def marshal_load(values)
      @path_names = values[0].map {|path_name| path_name.gsub(DIR_MARKER, RI::Paths::SYSDIR)}
      @name = values[1]
      @in_class = values[2]
      @class_methods = values[3]
      @instance_methods = values[4]
      @inferior_classes = values[5]
    end
  end

  class MethodEntry
    # custom dump to strip out system-specific SYSDIR and replace with a marker
    def marshal_dump
      [
        @path_name.gsub(RI::Paths::SYSDIR, DIR_MARKER),
        @name,
        @is_class_method,
        @in_class
      ]
    end

    # custom load to replace the marker with system-specific SYSDIR
    def marshal_load(values)
      @path_name = values[0].gsub(DIR_MARKER, RI::Paths::SYSDIR)
      @name = values[1]
      @is_class_method = values[2]
      @in_class = values[3]
    end
  end
end