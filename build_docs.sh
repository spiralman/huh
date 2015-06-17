#!/bin/bash

lein with-profile doc doc

pushd doc
git checkout gh-pages
git add .
git commit -am "Regenerating docs"
git push -u origin gh-pages
popd
