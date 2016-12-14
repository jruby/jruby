#!/usr/bin/env bash

set -e
set -x

revision=05b5d105d2ae58fb32a9d0a77550d176a0489991

git -C ../jruby-truffle-gem-test-pack checkout ${revision}
