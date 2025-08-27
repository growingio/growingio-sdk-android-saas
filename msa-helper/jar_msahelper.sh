#!/bin/bash

# 用于生成 msahelper.jar 的编译工具，步骤：
# 1. 确保你的 ANDROID_HOME 环境变量设置和对应的Android api是否存在
# 2. ./jar_msahelper.sh
ANDROID_API=android-31
ANDROID_JAR_CLASSPATH="${ANDROID_HOME}/platforms/$ANDROID_API/android.jar"
OAID_DIST=./src/main/java/com/growingio/android/sdk/utils
javac -d classes/ -classpath $ANDROID_JAR_CLASSPATH:./libs/miit_mdid_1.0.10.jar $OAID_DIST/OaidHelper1010.java -Xlint:unchecked
javac -d classes/ -classpath $ANDROID_JAR_CLASSPATH:./libs/msa_mdid_1.0.13.jar $OAID_DIST/OaidHelper1013.java -Xlint:unchecked
javac -d classes/ -classpath $ANDROID_JAR_CLASSPATH:./libs/oaid_sdk_1.0.25.jar $OAID_DIST/OaidHelper1025.java -Xlint:unchecked
javac -d classes/ -classpath $ANDROID_JAR_CLASSPATH:./libs/oaid_sdk_1.1.0.jar $OAID_DIST/OaidHelper1100.java -Xlint:unchecked

cd classes
jar cvf ../../vds-base-library/libs/msa_helper.jar ./com/growingio/android/sdk/utils/*