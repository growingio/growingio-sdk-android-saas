#!/usr/bin/env bash

echo "输入的diff为: ${1}"
echo "删除本地patch分支"
git checkout arcpatch-${1}
if [ $? == 0 ];then
    echo "本地存在旧patch分支，即将删除"
    git checkout master-2.0
    git branch -D arcpatch-${1}
else
    echo "仓库内不存在该patch分支"
fi
echo "即将patch最新diff"
arc patch ${1}
echo "即将land最新diff"
arc land --onto master-2.0