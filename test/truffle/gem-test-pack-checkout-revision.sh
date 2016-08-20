#!/usr/bin/env bash

set -e
set -x

revision=8a041d63982822462681305ff634a40429f7cd58

git -C ../jruby-truffle-gem-test-pack checkout ${revision}
