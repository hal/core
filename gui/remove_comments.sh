#!/bin/bash

for file in ./target/i18n/*properties; do
	sed '/^\#/d' $file > tt
	mv tt $file
done

