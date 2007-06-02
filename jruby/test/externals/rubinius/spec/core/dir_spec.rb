# PRIMITIVE Dir testing. Add cases as they come along.
require File.dirname(__FILE__) + '/../spec_helper'

# Class methods:
#
#   .[]             OK - NEEDS MORE
#   .chdir          OK
#   .chroot         NOT ENABLED BY DEFAULT
#   .delete         OK
#   .entries        OK
#   .foreach        OK
#   .getwd          OK
#   .glob           OK - NEEDS MORE
#   .mkdir          OK - NEEDS MORE
#   .new            OK
#   .open           OK - REVISIT
#   .pwd            OK
#   .rmdir          OK
#   .tmpdir         NOT IN MRI
#   .unlink         OK
#
# Instance methods:
#
#   #close          OK
#   #each           OK
#   #path           OK
#   #pos            OK
#   #pos=           OK
#   #read           OK
#   #rewind         OK 
#   #seek           OK
#   #tell           OK
#

# Helpers
$mockdir = '/tmp/rubinius_mock_fs'
$nonexisting = "/rubinius_nonexist_#{$$}_00"
$mockdir.freeze
$nonexisting.freeze

while File.exist? $nonexisting
  $nonexisting = $nonexisting.succ
end


def setup_mock_fs()
  system "mkdir -p '#{$mockdir}';
          mkdir -p '#{$mockdir}/subdir_one';
          mkdir -p '#{$mockdir}/subdir_two';
          mkdir -p '#{$mockdir}/.dotsubdir';
          mkdir -p '#{$mockdir}/deeply/nested/directory/structure';
        
          touch '#{$mockdir}/.dotfile';
          touch '#{$mockdir}/nondotfile';
          touch '#{$mockdir}/file_one.ext';
          touch '#{$mockdir}/file_two.ext';
          touch '#{$mockdir}/subdir_one/.dotfile';
          touch '#{$mockdir}/subdir_one/nondotfile';
          touch '#{$mockdir}/subdir_two/nondotfile';
          touch '#{$mockdir}/subdir_two/nondotfile.ext';
          touch '#{$mockdir}/deeply/.dotfile';
          touch '#{$mockdir}/deeply/nondotfile';
          touch '#{$mockdir}/deeply/nested/.dotfile.ext';
          touch '#{$mockdir}/deeply/nested/directory/structure/file_one.ext';
          touch '#{$mockdir}/deeply/nested/directory/structure/file_one';
          touch '#{$mockdir}/deeply/nested/directory/structure/foo';
          touch '#{$mockdir}/deeply/nested/directory/structure/bar';
          touch '#{$mockdir}/deeply/nested/directory/structure/baz';
          touch '#{$mockdir}/deeply/nested/directory/structure/.ext';"
end

#def teardown_mock_fs()
#end

warn "Running Dir specs will leave you with a #{$mockdir}, feel free to delete it."
setup_mock_fs

warn 'Dir specs are incomplete. Please add corner cases.'


context 'Using Dir to move around the filesystem' do
  specify 'Dir.pwd and Dir.getwd return the current working directory' do
    Dir.pwd.should == `pwd`.chomp
    Dir.getwd.should == `pwd`.chomp
  end

  specify 'Dir.chdir can be used to change the working directory--temporary if a block is provided. Defaults to $HOME' do
    orig = Dir.pwd

    Dir.chdir
    Dir.pwd.should == ENV['HOME'] 
    Dir.chdir orig

    Dir.chdir $mockdir 
    Dir.pwd.should == $mockdir
    Dir.chdir orig 

    # Return values
    Dir.chdir(orig).should == 0
    Dir.chdir(orig) {:returns_block_value}.should == :returns_block_value

    Dir.chdir($mockdir) {|dir| [dir, Dir.pwd]}.should == [$mockdir, $mockdir] 
    Dir.pwd.should == orig

    should_raise(SystemCallError) { Dir.chdir $nonexisting }
  end

  # Need special perms to run chroot
#  specify 'Dir.chroot can be used to change the process\' root directory, see chroot(2)' do
#    example do
#      Kernel.fork {
#        begin
#          ret = Dir.chroot $mockdir
#          File.open('/root_contents.txt', 'wb') {|f| f.puts ret; f.puts Dir.entries('/').sort}
#          FileUtils.chmod 0777, '/root_contents.txt'
#        rescue SystemCallError
#          warn '**WARN: Insufficient permissions to test Dir.chroot! (Not a huge problem.)'
#        end
#      }
#
#      Process.waitall
#  
#      contents = File.read "#{$mockdir}/root_contents.txt"
#      FileUtils.rm "#{$mockdir}/root_contents.txt"
#      
#      # Should have the return value + the filenames
#      contents.split("\n").sort
#    end.should == %w|0 . .. .dotfile .dotsubdir subdir_one subdir_two deeply nondotfile file_one.ext file_two.ext root_contents.txt|.sort
#  end

  # Not enough info to ensure this is correct
#  specify 'Dir.tempdir points to the system\'s temporary directory' do
#    example do
#      [ENV['TMPDIR'], ENV['TEMPDIR'], '/tmp'].select {|dir| 
#        dir and File.directory?(dir) and File.writable?(dir)
#      }.include? Dir.tempdir 
#    end.should == true
#  end
end

context 'Using Dir to modify the filesystem' do
#  setup do 
#    @orig = Dir.pwd
#    Dir.chdir $mockdir
#  end
#
#  teardown do 
#    Dir.chdir @orig
#  end

  # FIX: manual setup :)
  @orig = Dir.pwd
  Dir.chdir $mockdir

  specify 'Dir.mkdir creates the named directory with the given permissions' do
    File.exist?('nonexisting').should == false
    Dir.mkdir 'nonexisting'
    File.exist?('nonexisting').should == true
    
    Dir.mkdir 'default_perms'
    a = File.stat('default_perms').mode
    Dir.mkdir 'reduced', (a - 1)
    File.stat('reduced').mode.should_not == a

    Dir.mkdir('always_returns_0').should == 0

    system "chmod 0777 nonexisting default_perms reduced always_returns_0"
    Dir.rmdir 'nonexisting'
    Dir.rmdir 'default_perms'
    Dir.rmdir 'reduced'
    Dir.rmdir 'always_returns_0'
  end

  specify 'Dir.mkdir raises without adequate permissions in the parent dir' do
    Dir.mkdir 'noperms', 0000

    should_raise(SystemCallError) { Dir.mkdir 'noperms/subdir' }

    system 'chmod 0777 noperms'
    Dir.rmdir 'noperms'
  end

  specify 'Dir.mkdir cannot create directory hierarchies' do
    should_raise(SystemCallError) { Dir.mkdir "#{$nonexisting}/subdir" }
  end

  specify 'Dir.rmdir, .delete and .unlink remove non-empty directories' do
    %w|rmdir delete unlink|.each {|cmd|
      Dir.mkdir 'empty_subdir'
      Dir.send(cmd, 'empty_subdir').should == 0 
    }
  end

  specify 'Dir.rmdir, .delete and .unlink will raise an exception trying to remove a nonempty directory' do
    %w|rmdir delete unlink|.each {|cmd|
      should_raise(SystemCallError) { Dir.send cmd, 'subdir_one' }
    }
  end

  specify 'Dir.rmdir, .delete and .unlink need adequate permissions to remove a directory or will raise' do
    %w|rmdir delete unlink|.each {|cmd|
      system "mkdir -p noperm_#{cmd}/child"
      system "chmod 0000 noperm_#{cmd}"

      should_raise(SystemCallError) { Dir.send cmd, "noperm_#{cmd}/child" }

      system "chmod 0777 noperm_#{cmd}"
      Dir.rmdir "noperm_#{cmd}/child"
      Dir.rmdir "noperm_#{cmd}"
    }
  end

  Dir.chdir @orig
end

context 'Examining directory contents with Dir' do
  specify 'Dir.entries gives an Array of filenames in an existing directory including dotfiles' do

    Dir.entries($mockdir).sort.should == %w|. .. subdir_one subdir_two .dotsubdir deeply 
                                            .dotfile nondotfile file_one.ext file_two.ext|.sort

    Dir.entries("#{$mockdir}/deeply/nested").sort.should == %w|. .. .dotfile.ext directory|.sort


    should_raise(SystemCallError) { Dir.entries $nonexisting }
  end

  specify 'Dir.foreach yields all filenames (including dotfiles) in an existing directory to block provided, returns nil' do
      a, b = [], []
     
      Dir.foreach($mockdir) {|f| a << f}
      Dir.foreach("#{$mockdir}/deeply/nested") {|f| b << f}

      a.sort.should == %w|. .. subdir_one subdir_two .dotsubdir deeply .dotfile nondotfile file_one.ext file_two.ext|.sort
      b.sort.should == %w|. .. .dotfile.ext directory|.sort

      should_raise(SystemCallError) { Dir.foreach $nonexisting }

      Dir.foreach($mockdir) {|f| f}.should == nil
  end
end

context 'Wildcard-matching directory contents with Dir.glob (Dir[PATTERN] is equivalent to Dir.glob(PATTERN, 0)' do
#  setup do 
#    @orig = Dir.pwd
#    Dir.chdir $mockdir
#  end
#
#  teardown do 
#    Dir.chdir @orig
#  end

  @orig = Dir.pwd
  Dir.chdir $mockdir

  specify "* by itself matches any non-dotfile" do
  %w|glob []|.each {|msg|
    Dir.send(msg,'*').sort.should == %w|subdir_one subdir_two deeply nondotfile file_one.ext file_two.ext|.sort
  }
  end

  specify ".* by itself matches any dotfile" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '.*').sort.should == %w|. .. .dotfile .dotsubdir|.sort
  }
  end

  specify "* with option File::FNM_DOTMATCH matches both dot- and nondotfiles" do
    Dir.glob('*', File::FNM_DOTMATCH).sort.should == %w|. .. .dotfile .dotsubdir subdir_one subdir_two deeply nondotfile file_one.ext file_two.ext|.sort
  end

  specify "* followed by literals matches any (or no) beginning for nondot filenames" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '*file').sort.should == %w|nondotfile|.sort
  }
  end

  specify ".* followed by a string matches any (or no) beginning for dotfile names" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '.*file').sort.should == %w|.dotfile|.sort
  }
  end

  specify "* with File::FNM_DOTMATCH followed by literals matches any (or no) beginning for any filenames" do
    Dir.glob('*file', File::FNM_DOTMATCH).sort.should == %w|.dotfile nondotfile|.sort
  end

  specify "* in the end of a string matches any (or no) ending" do
  %w|glob []|.each {|msg|
    Dir.send(msg, 'file*').sort.should == %w|file_one.ext file_two.ext|.sort
  }
  end

  specify "* in the middle matches any (or no) characters" do
  %w|glob []|.each {|msg|
    Dir.send(msg, 'sub*_one').sort.should == %w|subdir_one|.sort
  }
  end

  specify "multiple * may appear in a glob to use all above capabilities" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '*fi*e*').sort.should == %w|nondotfile file_one.ext file_two.ext|.sort
  }
  end

  specify "** by itself matches any nondot files in the current directory" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '**').sort.should == %w|subdir_one subdir_two deeply nondotfile file_one.ext file_two.ext|.sort
  }
  end

  specify ".** by itself matches any dotfiles in the current directory" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '.**').sort.should == %w|. .. .dotsubdir .dotfile|.sort
  }
  end

  specify ".** with File::FNM_DOTMATCH matches any files in the current directory" do
    Dir.glob('**', File::FNM_DOTMATCH).sort.should == %w|. .. .dotsubdir .dotfile subdir_one subdir_two 
                                                         deeply nondotfile file_one.ext file_two.ext|.sort
  end

  specify "**/ recursively matches any nondot subdirectories" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '**/').sort.should == %w|subdir_one/ subdir_two/ deeply/ deeply/nested/ 
                                           deeply/nested/directory/ deeply/nested/directory/structure/|.sort
  }
  end

  specify ".**/ recursively matches any subdirectories including ./ and ../" do
  %w|glob []|.each {|msg|
    Dir.chdir "#{$mockdir}/subdir_one"
    Dir.send(msg, '.**/').sort.should == %w|./ ../|.sort

    Dir.chdir $mockdir
  }
  end

  specify "**/ with File::FNM_DOTMATCH recursively matches any subdirectories (not ./ or ../)" do
    Dir.glob('**/', File::FNM_DOTMATCH).sort.should == %w|.dotsubdir/ subdir_one/ subdir_two/ deeply/ deeply/nested/ 
                                                          deeply/nested/directory/ deeply/nested/directory/structure/|.sort
  end

  specify "? can be used anywhere in a file name to match any one character except leading ." do
  %w|glob []|.each {|msg|
    Dir.send(msg, '?ubdir_one').sort.should == %w|subdir_one|.sort
  }
  end

  specify "multiple ? can appear to match any one character each" do
  %w|glob []|.each {|msg|
    Dir.send(msg, 'subdir_???').sort.should == %w|subdir_one subdir_two|.sort
  }
  end

  specify "[CHARACTERS] can be used to match any one character of the ones in the brackets" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '[stfu]ubdir_one').sort.should == %w|subdir_one|.sort
  }
  end

  specify "[CHAR-OTHER] can contain ranges of characters such as a-z" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '[a-zA-Z]ubdir_one').sort.should == %w|subdir_one|.sort
  }
  end

  specify "[^CHARACTERS] matches anything BUT those characters or range" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '[^wtf]ubdir_one').sort.should == %w|subdir_one|.sort
  }
  end

  specify "[^CHAR-OTHER] matches anything BUT those characters or range" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '[^0-9]ubdir_one').sort.should == %w|subdir_one|.sort
  }
  end

  specify "{string,other,...} matches one of the provided strings exactly in any part of a filename" do
  %w|glob []|.each {|msg|
    Dir.send(msg, 'subdir_{one,two,three}').sort.should == %w|subdir_one subdir_two|.sort
  }
  end

  specify "{string,,other} can have an empty string" do
  %w|glob []|.each {|msg|
    a = Dir.send(msg, 'deeply/nested/directory/structure/file_one{.ext,}').sort
    a.should == %w|deeply/nested/directory/structure/file_one.ext
                   deeply/nested/directory/structure/file_one|.sort
  }
  end

  specify "{,.}* etc. can be used to match any dot- or nondot file" do
  %w|glob []|.each {|msg|
    Dir.send(msg, '{,.}*').sort.should == %w|. .. .dotsubdir subdir_one subdir_two deeply 
                                             .dotfile nondotfile file_one.ext file_two.ext|.sort
  }
  end

  specify 'In a single-quoted pattern, \ escapes the following character of any special meaning' do
  %w|glob []|.each {|msg|
    Dir.mkdir 'foo*bar'
    
    Dir.glob('foo?bar').should == %w|foo*bar|
    Dir.glob('foo\?bar').should == []
    Dir.glob('nond\otfile').should == %w|nondotfile| 

    Dir.rmdir 'foo*bar'
  }
  end

  specify 'In a single-quoted pattern, File::FNM_NOESCAPE treats \ as the literal backslash' do
    Dir.mkdir 'foo?bar'
    
    Dir.glob('foo?bar', File::FNM_NOESCAPE).should == %w|foo?bar|
    Dir.glob('foo\?bar', File::FNM_NOESCAPE).should == []

    Dir.mkdir 'foo\?bar'

    Dir.glob('foo\?bar', File::FNM_NOESCAPE).should == %w|foo\\?bar|

    Dir.rmdir 'foo?bar'
    Dir.rmdir 'foo\?bar'
  end

  specify 'Normally, / is a special character. File::FNM_PATHNAME treats it like any regular character' do
    # There is no meaningful use of File::FNM_PATHNAME in glob. See File.fnmatch for proper use
    Dir.glob('subdir_one/nondotfile').should == Dir.glob('subdir_one/nondotfile', File::FNM_PATHNAME)
  end

  specify "**/PATTERN recursively matches the pattern (as above) in itself and all subdirectories" do
    warn 'MRI (glob(3)) and shell glob return deeply/nested/directory/structure/file_one.ext twice!' 

    %w|glob []|.each {|msg|
      Dir.send(msg, '**/*fil?{,.}*').sort.should == %w|nondotfile file_one.ext file_two.ext subdir_one/nondotfile 
                                                       subdir_two/nondotfile subdir_two/nondotfile.ext deeply/nondotfile 
                                                       deeply/nested/directory/structure/file_one.ext 
                                                       deeply/nested/directory/structure/file_one|.sort
    }
  end
  
  Dir.chdir @orig
end


context 'Creating Dir objects' do
  specify 'Both Dir.new and Dir.open return a new Dir instance' do
    a = Dir.new($mockdir)
    a.class.should == Dir
    b = Dir.open($mockdir)
    b.class.should == Dir

    a.close
    b.close
  end

  specify 'Dir.new and Dir.open will raise if the directory does not exist' do
    %w|new open|.each {|msg|
      should_raise(SystemCallError) { Dir.send msg, $nonexisting }
    }
  end

  specify 'Dir.open may also take a block which yields the Dir instance and closes it after. Returns block value' do
    # This is a bit convoluted but we are trying to ensure the file gets closed.
    # To do that, we peek to see what the next FD number is and then probe that
    # to see whether it has been closed.
    peek = IO.sysopen $mockdir
    File.for_fd(peek).close

    Dir.open($mockdir) {|dir| File.for_fd peek; dir.class}.should == Dir  # Should be open here
    should_raise(SystemCallError) { File.for_fd peek }                    # And closed here
  end
end

dir_spec_object_text = <<END 
Using Dir objects

#  The pointer associated with an open directory is not like an Array
#  index, more like an instruction sequence pointer. Pretty much any
#  operation below increments the pointer. It is, for example, possible
#  that both positions 1 and 27 refer to the point where the next read
#  will be the first entry in the directory. So the position number is
#  not as important as the /time at which it was obtained/.

END

context dir_spec_object_text do
#  setup do
#    @dir = Dir.open $mockdir
#  end
#
#  teardown do
#    @dir.close rescue nil
#  end


  specify 'Dir#path gives the path that was supplied to .new or .open' do
    @dir = Dir.open $mockdir

    @dir.path.should == $mockdir

    @dir.close rescue nil
  end

  specify 'Dir#read gives the file name in the current seek position' do
    @dir = Dir.open $mockdir

    @dir.read.should == '.'

    @dir.close rescue nil
  end

  specify 'Both Dir#pos and Dir#tell give the current dir position' do
    @dir = Dir.open $mockdir

    @dir.pos.should == 1
    @dir.tell.should == 2
    @dir.pos.should == 3
    @dir.tell.should == 4

    @dir.close rescue nil
  end

  specify 'Dir#seek can be used to return to a certain position (obtained from #pos or #tell), returns the Dir object' do
    @dir = Dir.open $mockdir

    pos = @dir.pos
    a   = @dir.read
    b   = @dir.read
    ret = @dir.seek pos
    c   = @dir.read
    
    a.should_not == b
    b.should_not == c
    c.should     == a

    ret.class.should == Dir

    @dir.close rescue nil
  end

  specify 'Dir#pos= also seeks to a certain position but returns the position number instead' do
    @dir = Dir.open $mockdir

    pos = @dir.pos
    a   = @dir.read
    b   = @dir.read
    ret = @dir.pos = pos
    c   = @dir.read
    
    a.should_not == b
    b.should_not == c
    c.should     == a

    ret.should == pos

    @dir.close rescue nil
  end

  specify 'Dir#rewind will reset the next read to start from the first entry but *does not reset the pointer to 1*' do
    @dir = Dir.open $mockdir

    first   = @dir.pos
    a       = @dir.read
    b       = @dir.read
    prejmp  = @dir.pos
    ret     = @dir.rewind
    second  = @dir.pos
    c       = @dir.read
    
    a.should_not == b
    b.should_not == c
    c.should     == a

    ret.class.should == Dir

    second.should_not == first
    second.should_not == prejmp

    @dir.close rescue nil
  end

  specify 'Dir#each will yield each directory entry in succession' do
    @dir = Dir.open $mockdir

    a = []
    @dir.each {|dir| a << dir}
    a.sort.should == %w|. .. .dotfile .dotsubdir nondotfile subdir_one subdir_two deeply file_one.ext file_two.ext|.sort

    @dir.close rescue nil
  end

  specify 'Dir#each returns the directory which remains open' do
    @dir = Dir.open $mockdir

    @dir.each {}.should == @dir
    @dir.read.should == nil
    @dir.rewind
    @dir.read.should == '.'

    @dir.close rescue nil
  end

  specify 'Dir#close will close the stream and fd and returns nil' do
    # This is a bit convoluted but we are trying to ensure the file gets closed.
    # To do that, we peek to see what the next FD number is and then probe that
    # to see whether it has been closed.
    peek = IO.sysopen $mockdir
    File.for_fd(peek).close

    dir = Dir.open $mockdir
    File.for_fd(peek).close                   # Should be open here

    dir.close.should == nil
    should_raise(SystemCallError) { File.for_fd(peek).close }  # And closed here
  end

  specify 'Further attempts to use a dir that has been #closed will result in an error' do
    @dir = Dir.open $mockdir

    %w|close each path pos read rewind tell|.each {|msg|
      should_raise(IOError) do
        dir = Dir.open $mockdir
        dir.close
        dir.send msg
      end
    }
  end

  @dir.close rescue nil
end


# Try to clean up
system "rm -r #{$mockdir}"
