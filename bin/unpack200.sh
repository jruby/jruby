#!/bin/sh
find . -name '*.pack.gz' | sed 's/\(.*\).pack.gz/\1.pack.gz \1.jar/' | xargs -L1 unpack200 --remove-pack-file
