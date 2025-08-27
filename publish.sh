#!/usr/bin/env bash

# 用于发布Android SDK到jcenter上
# 此脚本需要环境变量如下:
# pb_pf: 发布方式： test official
# pb_vs: 发布版本
# pb_project: 是否需全量发布

set -e
export pb_project=game-autotrack
# echo $pb_project
export pb_vs=2.10.5
export pb_pf=official
# export pb_vs=2.8.27

function publish(){
	versionName=$1
	echo "开始发布版本:" $versionName
	./gradlew clean
	./gradlew -P pb_vs=$versionName -P pb_pf=$pb_pf jenkinsTask assembleAar
	./gradlew -P pb_vs=$versionName -P pb_pf=$pb_pf jenkinsTask publish --stacktrace
	# ./gradlew -P pb_vs=$versionName -P pb_pf=$pb_pf jenkinsTask tryCloseAndReleaseRepository

	# bintrayUpload会首先判断文件是否存在
	# ./gradlew -P pb_vs=$versionName -P pb_pf=$pb_pf -P pb_vaa=$pb_vaa -P pb_vgp=$pb_vgp clean assembleAar :vds-gradle-plugin:install
	# ./gradlew -P pb_vs=$versionName -P pb_pf=$pb_pf -P pb_vaa=$pb_vaa -P pb_vgp=$pb_vgp bintrayUpload --info
}

if [ "$pb_project" == "all" ]
then
	echo "发布全量版本"
	echo "发布环境为：" $pb_pf
	publish track-$pb_vs
	publish autotrack-$pb_vs
else
	echo "发布单个版本：" $pb_project-$pb_vs
	echo "发布环境为：" $pb_pf
	publish $pb_project-$pb_vs
fi

echo "================== 发布完成 ==================== "


#com.growingio.android:vds-android-agent:autotrack-2.10.5
#com.growingio.android:vds-android-agent:track-2.10.5
#com.growingio.android:vds-android-agent:RN-track-2.10.5
#com.growingio.android:vds-android-agent:RN-autotrack-2.10.5
#com.growingio.android:vds-android-agent:game-track-2.10.5
#com.growingio.android:vds-android-agent:game-autotrack-2.10.5
