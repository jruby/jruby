#!/bin/sh

git merge --no-commit --no-ff origin/master

# git reset appveyor.yml
git rm -f appveyor.yml

for f in .travis.yml ci.hocon; do
	git reset $f
	git checkout $f
done
