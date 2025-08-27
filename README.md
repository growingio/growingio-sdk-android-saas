GrowingIO Autotracker
======
![GrowingIO](https://www.growingio.com/vassets/images/home_v3/gio-logo-primary.svg)  

## GrowingIO简介
[**GrowingIO**](https://www.growingio.com/)（北京易数科技有限公司）是基于用户行为数据的增长平台，国内领先的数据运营解决方案供应商。为产品、运营、市场、数据团队及管理者等，提供客户数据平台、获客分析、产品分析、智能运营等产品和咨询服务，帮助企业在数据化升级的路上，提升数据驱动能力，实现更好的增长。  
[**GrowingIO**](https://www.growingio.com/) 专注于零售、电商、金融、酒旅航司、教育、内容社区、B2B等行业，成立以来，服务上千家企业级客户，获得迪奥、安踏、喜茶、招商仁和人寿、上汽集团、东航、春航、首旅如家、陌陌、滴滴、爱奇艺、新东方等客户的青睐。

## SDK 简介
**GrowingIO Autotracker 2,0** 具备自动采集基本的用户行为事件，比如访问和行为数据等。目前支持代码埋点、无埋点、可视化圈选、数据监测等功能。

## 集成环境

- Android Studio 3.0 及以上
- Gradle 3.5
- JDK 1.8 及以上
- Android 4.1 及以上

## 快速集成

### 1. 添加工程依赖


**在 project 级别的 build.gradle 文件中添加`saas-gradle-plugin`依赖。**


> 2.9.0 版本后仓库从 JCenter 迁移到了 Maven Central, 请使用 mavenCentral() 替换 jcenter()


示例代码：


```javascript
buildscript {
    repositories {
        gradlePluginPortal()
        //如果使用 SNAPSHOT 版本，则需要使用如下该仓库。
        maven { url "https://central.sonatype.com/repository/maven-snapshots/" }


        google()
    }
    dependencies {
        //GrowingIO 无埋点 SDK
        classpath 'com.growingio.android:saas-gradle-plugin:2.10.5'
    }
}
```


**在 module 级别的 build.gradle 文件中添加`com.growingio.android`插件、`vds-android-agent`依赖和对应的资源。**


代码示例：


```javascript
apply plugin: 'com.android.application'
//添加 GrowingIO 插件
apply plugin: 'com.growingio.android.saas'
android {
    defaultConfig {
        resValue("string", "growingio_project_id", "您的项目ID")
        resValue("string", "growingio_url_scheme", "您的URL Scheme")
    }
}
dependencies {
    //GrowingIO 无埋点 SDK
    implementation 'com.growingio.android:vds-android-agent:autotrack-2.10.5'
}
```

### 2. 初始化 SDK

请将 GrowingIO.startWithConfiguration 加在您的 Application 的 onCreate 方法中。
示例代码：

```java
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GrowingIO.startWithConfiguration(this, new Configuration()
            .trackAllFragments()
            // 建议使用 BuildConfig 设置
            .setChannel("XXX应用商店")
            
        );
    }
}
```

1. 其中`GrowingIO.startWithConfiguration`第一个参数为`ApplicationContext`对象。
2. `setChannel`方法的参数定义了“自定义App渠道”这个维度的值，填写APP要发布的应用商店名称。
3. `trackAllFragments`方法作用是将 APP 内部的`Fragment`标记为代表一个页面。 常见于`Activity` 中包含多个 `Fragment`， 在业务场景上这里的每一个 Fragment 都可以代表一个页面。例如点击 Tab 切换页面。


## License
```
Copyright (C) 2020 Beijing Yishu Technology Co., Ltd.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```