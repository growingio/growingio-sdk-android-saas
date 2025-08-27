

# 概述

模块化打包是以EventBus(自定义的)作为通信工具，将业务代码进行拆分， 隔离。 其git位置为：<https://codes.growingio.com/source/ModularizationSDK/> 此处放置的为打出的Jar包， 为的是快速打开项目与Jenkins打包的方便. 

该目录包含: cp-annotation.jar, cp-base-library.jar, cp-compiler.jar, cp-eventcenter.aar, cp-plugin.jar 5个Jar包或aar包. 大致如下:

-   cp-annotation.jar: EventBus的注解, 理论上事件接受者只需要依赖此文件夹即可
-   cp-eventcenter.aar: 基于EventBus改写的EventCenter(请不要使用其多进程功能)
-   cp-base-library.jar: 基础信息， 为了cp-plugin处理过程中使用
-   cp-plugin: asm处理字节码(利用Android Transform API 或者是自己写的LibraryTransform API)


# cp-plugin asm字节码处理与打包

一个Gradle 插件， 在Android application下使用Transform API， 在Library下使用自定义的Transform. 逐个类过滤， 如果该类中使用了Subscribe注解生成一个方法: 

    @Subscribe(
    	threadMode = ThreadMode.MAIN,
    	sticky = true
    )
    public void onMessageEvent(RebuildEvent event) {
    	Log.e("MessageSubscriber", "onMessageEvent1:" + event.getMessage());
    }
    
    @Subscribe(
    	threadMode = ThreadMode.MAIN,
    	sticky = true
    )
    public void onMessageEvent(TestEvent event) {
    	Log.e("MessageSubscriber", "onMessageEvent1:" + event.getName());
    }
    
    public void do$Action(String methodString, Object event) {
    	if (methodString.equals("#onMessageEvent(com.cliff.base_library.event.RebuildEvent")) {
    		this.onMessageEvent((RebuildEvent)event);
    	} else if (methodString.equals("#onMessageEvent(com.cliff.base_library.event.TestEvent")) {
    		this.onMessageEvent((TestEvent)event);
    	} else {
    		System.out.println("No such method to delegate");
    	}
    
    }

    public SubscriberMethod[] get$SubscriberMethods() {
        return new SubscriberMethod[]{new SubscriberMethod("onSocketEvent", SocketEvent.class, "#onSocketEvent(com.growingio.android.sdk.base.event.SocketEvent", ThreadMode.BACKGROUND, 0, false), new SubscriberMethod("onPluginReadyEvent", DebuggerPluginReadyEvent.class, "#onPluginReadyEvent(com.growingio.android.sdk.debugger.event.DebuggerPluginReadyEvent", ThreadMode.MAIN, 0, false), new SubscriberMethod("onNetChanged", NetWorkChangedEvent.class, "#onNetChanged(com.growingio.android.sdk.base.event.net.NetWorkChangedEvent", ThreadMode.MAIN, 0, false), new SubscriberMethod("onSocketStatusEvent", SocketStatusEvent.class, "#onSocketStatusEvent(com.growingio.android.sdk.base.event.SocketStatusEvent", ThreadMode.MAIN, 0, false)};
    }

另一个功能就是在Library下可以将多个类库合并成一个aar包进行输出. task为assembleAar


# 注意事项

-   暂时不要使用EventBus提供的multiprocess Event

