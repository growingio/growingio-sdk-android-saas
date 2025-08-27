#!/usr/bin/env bash

echo "输入的diff为: ${1}"
echo "删除本地已有的diff分支"
git checkout arcpatch-${1}
if [ $? == 0 ];then
    echo "本地存在旧patch分支，即将删除"
    git checkout master
    git branch -D arcpatch-${1}
else
    echo "仓库内不存在该patch分支"
fi
echo "即将patch最新diff"
arc patch ${1}
git checkout master-2.0
git checkout -b "test-"$(date "+%Y%m%d%H%M%S")
git merge arcpatch-${1}
BUILD_VERSION=$(cat build.gradle | grep "currentVersion     : " | sed 's/[^"]*"\([^"]*\)".*/\1/')
echo "发版到本地的版本号为：$BUILD_VERSION"


if [[ $BUILD_VERSION =~ ^auto.* ]] ; then
    echo "发版无埋点$BUILD_VERSION 到本地"
    ./gradlew clean \
    && ./gradlew :vds-android-agent:publishToMavenLocal \
    && ./gradlew :vds-gradle-plugin:publishToMavenLocal
elif [[ $BUILD_VERSION =~ ^track.* ]] ; then
    echo "仓库内不存在该patch分支"
    ./gradlew clean \
    && ./gradlew :vds-android-agent:publishToMavenLocal
else
    echo "版本号异常，请先校验"
fi

echo "打印发版结果"
tree -dfL 2 ~/.m2/repository/com/growingio/android/test  | grep $BUILD_VERSION