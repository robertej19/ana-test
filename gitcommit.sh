#!/bin/bash
echo "Current date: $now"
VAR1='"'
MESS="$1"
VAR3="$VAR1$MESS$VAR1"
echo $MESS

git add .
git commit -m "$VAR3"
git push origin master
git status
