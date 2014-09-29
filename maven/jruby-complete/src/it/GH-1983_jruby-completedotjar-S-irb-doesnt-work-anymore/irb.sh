JAVA=`which java`
# jirb_swing does not work in those test environments
for i in irb jirb ; do
    echo "puts 'hello $i'" | PATH= $JAVA -jar jruby-complete-1.7.16-SNAPSHOT.jar -S $i
done
