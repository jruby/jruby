#!/usr/bin/env bash

# Don't run this script directly
# This vagrant box and bootstrap was set up to be travis like with additional tools
# This script will be run by vagrant during `vagrant up`
# Install vagrant from: https://www.vagrantup.com/
#
# Run this command from the Jruby project home, which will import the vm and run this script
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant up
#
# Then to ssh to vagrant vm:
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant ssh
# You should now be inside the jruby project directory on but on the vm at /vagrant
# The usual build commands should work from here.
#
# When not is use, suspend the vm, vagrant suspend
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant suspend
#
# And resume, vagrant resume
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant resume
#
# If you want to test making updates to this script, after `vagrant up` run:
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant provision
# which will run the script again on the running vm
# 
# If you need to make an update to this script, run:
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant reload
# This is like running vagrant halt, vagrant up
#
# Delete the vm
# VAGRANT_VAGRANTFILE=tool/truffle/Vagrantfile vagrant destroy
#

# Install jdk-7
sudo apt-get update
sudo apt-get install -y openjdk-7-jdk
sudo apt-get install -y build-essential openssl libreadline6 libreadline6-dev curl git-core zlib1g zlib1g-dev libssl-dev libyaml-dev libsqlite3-dev sqlite3 libxml2-dev libxslt-dev autoconf libc6-dev ncurses-dev automake libtool bison nodejs subversion

echo "Downloading and installing ruby-install and ruby-1.9.3"
wget -O ruby-install-0.5.0.tar.gz https://github.com/postmodern/ruby-install/archive/v0.5.0.tar.gz
tar -xzvf ruby-install-0.5.0.tar.gz
cd ruby-install-0.5.0/
sudo make install
sudo ruby-install --system ruby 1.9.3

echo "Downloading and installing maven 3.2.5"
wget -q http://apache.osuosl.org/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
sudo mkdir /usr/local/apache-maven
sudo mkdir /usr/local/apache-maven/apache-maven-3.2.5
sudo tar -xvzf apache-maven-3.2.5-bin.tar.gz -C /usr/local/apache-maven

echo "Setup bash and environment"
echo 'function jt { ruby tool/jt.rb $@; }' >> /home/vagrant/.bashrc
echo "cd /vagrant" >> /home/vagrant/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64/jre" >> /home/vagrant/.bashrc
echo "export PATH=$PATH:/usr/local/apache-maven/apache-maven-3.2.5/bin"  >> /home/vagrant/.bashrc
echo 'export MAVEN_OPTS="-Xms512m -XX:MaxPermSize=2048m -Xmx2048m"' >> /home/vagrant/.bashrc

# Check versions
ruby -v
java -version
echo "Done"