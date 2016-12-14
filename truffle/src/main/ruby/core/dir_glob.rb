# Copyright (c) 2007-2015, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

class Dir
  module Glob
    class Node
      def initialize(nxt, flags)
        @flags = flags
        @next = nxt
        @separator = nil
      end

      attr_writer :separator

      def separator
        @separator || "/"
      end

      def path_join(parent, ent)
        return ent unless parent
        if parent == "/"
          "/#{ent}"
        else
          "#{parent}#{separator}#{ent}"
        end
      end
    end

    class ConstantDirectory < Node
      def initialize(nxt, flags, dir)
        super nxt, flags
        @dir = dir
      end

      def call(env, path)
        full = path_join(path, @dir)

        # Don't check if full exists. It just costs us time
        # and the downstream node will be able to check properly.
        @next.call env, full
      end
    end

    class ConstantEntry < Node
      def initialize(nxt, flags, name)
        super nxt, flags
        @name = name
      end

      def call(env, parent)
        path = path_join(parent, @name)

        if File.exist? path
          env.matches << path
        end
      end
    end

    class RootDirectory < Node
      def call(env, path)
        @next.call env, "/"
      end
    end

    class RecursiveDirectories < Node
      def call(env, start)
        return if !start || !File.exist?(start)

        # Even though the recursive entry is zero width
        # in this case, its left separator is still the
        # dominant one, so we fix things up to use it.
        switched = @next.dup
        switched.separator = @separator
        switched.call env, start

        stack = [start]

        allow_dots = ((@flags & File::FNM_DOTMATCH) != 0)

        until stack.empty?
          path = stack.pop
          begin
            dir = Dir.new(path)
          rescue Errno::ENOTDIR
            next
          end

          while ent = dir.read
            next if ent == "." || ent == ".."
            full = path_join(path, ent)
            stat = File::Stat.lstat full

            if stat.directory? and (allow_dots or ent.getbyte(0) != 46) # ?.
              stack << full
              @next.call env, full
            end
          end
          dir.close
        end
      end
    end

    class StartRecursiveDirectories < Node
      def call(env, start)
        raise "invalid usage" if start

        # Even though the recursive entry is zero width
        # in this case, its left separator is still the
        # dominant one, so we fix things up to use it.
        if @separator
          switched = @next.dup
          switched.separator = @separator
          switched.call env, start
        else
          @next.call env, start
        end

        stack = []

        allow_dots = ((@flags & File::FNM_DOTMATCH) != 0)

        dir = Dir.new(".")
        while ent = dir.read
          next if ent == "." || ent == ".."
          stat = File::Stat.lstat ent

          if stat.directory? and (allow_dots or ent.getbyte(0) != 46) # ?.
            stack << ent
            @next.call env, ent
          end
        end
        dir.close

        until stack.empty?
          path = stack.pop
          dir = Dir.new(path)
          while ent = dir.read
            next if ent == "." || ent == ".."
            full = path_join(path, ent)
            stat = File::Stat.lstat full

            if stat.directory? and ent.getbyte(0) != 46  # ?.
              stack << full
              @next.call env, full
            end
          end
          dir.close
        end
      end
    end

    class Match < Node
      def initialize(nxt, flags, glob)
        super nxt, flags
        @glob = glob || ""
      end

      def match?(str)
        File.fnmatch @glob, str, @flags
      end
    end

    class DirectoryMatch < Match
      def initialize(nxt, flags, glob)
        super

        @glob.gsub! "**", "*"
      end

      def call(env, path)
        return if path and !File.exist?("#{path}/.")

        dir = Dir.new(path ? path : ".")
        while ent = dir.read
          if match? ent
            full = path_join(path, ent)

            if File.directory? full
              @next.call env, full
            end
          end
        end
        dir.close
      end
    end

    class EntryMatch < Match
      def call(env, path)
        return if path and !File.exist?("#{path}/.")

        begin
          dir = Dir.new(path ? path : ".")
        rescue SystemCallError
          return
        end

        while ent = dir.read
          if match? ent
            env.matches << path_join(path, ent)
          end
        end
        dir.close
      end
    end

    class DirectoriesOnly < Node
      def call(env, path)
        if path and File.exist?("#{path}/.")
          env.matches << "#{path}/"
        end
      end
    end

    class Environment
      attr_reader :matches

      def initialize(matches=[])
        @matches = matches
      end
    end

    def self.path_split(str)
      start = 0
      ret = []

      last_match = nil

      while match = %r!/+!.match_from(str, start)
        cur_start = match.byte_begin(0)
        cur_end = match.byte_end(0)
        ret << str.byteslice(start, cur_start - start)
        ret << str.byteslice(cur_start, cur_end - cur_start)

        start = cur_end

        last_match = match
      end

      if last_match
        ret << last_match.post_match
      else
        ret << str
      end

      # Trim from end
      if !ret.empty?
        while s = ret.last and s.empty?
          ret.pop
        end
      end

      ret
    end

    def self.single_compile(glob, flags=0)
      parts = path_split(glob)

      if glob.getbyte(-1) == 47 # ?/
        last = DirectoriesOnly.new nil, flags
      else
        file = parts.pop
        if /^[a-zA-Z0-9._]+$/.match(file)
          last = ConstantEntry.new nil, flags, file
        else
          last = EntryMatch.new nil, flags, file
        end
      end

      until parts.empty?
        last.separator = parts.pop
        dir = parts.pop

        if dir == "**"
          if parts.empty?
            last = StartRecursiveDirectories.new last, flags
          else
            last = RecursiveDirectories.new last, flags
          end
        elsif /^[^\*\?\]]+$/.match(dir)
          while /^[^\*\?\]]+$/.match(parts[-2])
            next_sep = parts.pop
            next_sect = parts.pop

            dir = next_sect << next_sep << dir
          end

          last = ConstantDirectory.new last, flags, dir
        elsif !dir.empty?
          last = DirectoryMatch.new last, flags, dir
        end
      end

      if glob.getbyte(0) == 47  # ?/
        last = RootDirectory.new last, flags
      end

      last
    end

    def self.run(node, all_matches=[])
      matches = []
      env = Environment.new(matches)
      node.call env, nil
      # Truffle: ensure glob'd files are always sorted in consistent order,
      # it avoids headaches due to platform differences (OS X is sorted, Linux not).
      matches.sort!
      all_matches.concat(matches)
    end

    def self.glob(pattern, flags, matches=[])
      # Rubygems uses Dir[] as a glorified File.exist?  to check for multiple
      # extensions. So we went ahead and sped up that specific case.

      if flags == 0 and
             m = /^([a-zA-Z0-9_.\/\s]*[a-zA-Z0-9_.])(?:\{([^{}\/\*\?]*)\})?$/.match(pattern)
        # no meta characters, so this is a glorified
        # File.exist? check. We allow for a brace expansion
        # only as a suffix.

        if braces = m[2]
          stem = m[1]

          braces.split(",").each do |s|
            path = "#{stem}#{s}"
            if File.exist? path
              matches << path
            end
          end

          # Split strips an empty closing part, so we need to add it back in
          if braces.getbyte(-1) == 44 # ?,
            matches << stem if File.exist? stem
          end
        else
          matches << pattern if File.exist?(pattern)
        end

        return matches
      end

      if pattern.include? "{"
        patterns = compile(pattern, flags)

        patterns.each do |node|
          run node, matches
        end
      elsif node = single_compile(pattern, flags)
        run node, matches
      else
        matches
      end
    end

    def self.compile(pattern, flags=0, patterns=[])
      escape = (flags & File::FNM_NOESCAPE) == 0

      rbrace = nil
      lbrace = nil

      # Do a quick search for a { to start the search better
      i = pattern.index("{")

      # If there was a { found, then search
      if i
        nest = 0

        while i < pattern.size
          char = pattern[i]

          if char == "{"
            lbrace = i if nest == 0
            nest += 1
          end

          if char == "}"
            nest -= 1
          end

          if nest == 0
            rbrace = i
            break
          end

          if char == "\\" and escape
            i += 1
          end

          i += 1
        end
      end

      # There was a full {} expression detected, expand each part of it
      # recursively.
      if lbrace and rbrace
        pos = lbrace
        front = pattern[0...lbrace]
        back = pattern[(rbrace + 1)..-1]

        while pos < rbrace
          nest = 0
          pos += 1
          last = pos

          while pos < rbrace and not (pattern[pos] == "," and nest == 0)
            nest += 1 if pattern[pos] == "{"
            nest -= 1 if pattern[pos] == "}"

            if pattern[pos] == "\\" and escape
              pos += 1
              break if pos == rbrace
            end

            pos += 1
          end

          brace_pattern = "#{front}#{pattern[last...pos]}#{back}"

          compile brace_pattern, flags, patterns
        end

        # No braces found, match the pattern normally
      else
        # Don't use .glob here because this code can detect properly
        # if a { is a brace or a just a normal character, but .glob can't.
        # if .glob is used and there is a { as a normal character, it will
        # recurse forever.
        if node = single_compile(pattern, flags)
          patterns << node
        end
      end

      patterns
    end
  end
end
