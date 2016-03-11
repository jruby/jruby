#!/usr/bin/env bash

sed -i.orig "s/  truffle_version = '.*'/  truffle_version = '$1'/g" truffle/pom.rb
