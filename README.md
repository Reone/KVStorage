# KVStorage
[![](https://jitpack.io/v/Reone/KVStorage.svg)](https://jitpack.io/#Reone/KVStorage)

a simple easy NoSQL database

* 一个简单的容易使用的数据库
* 非关系型Key-value数据库
* 底层使用sqlite实现


## 引用，添加依赖
```
allprojects {
    repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
dependencies {
	implementation 'com.github.Reone:KVStorage:v.1.0.1'
}

```

## 使用方式
> 详细的使用可以查看[SimpleActivity](https://github.com/Reone/KVStorage/blob/master/app/src/main/java/com/reone/kvstorage/SimpleActivity.java)

- 在Application中初始化
```java
    KVStorage.init(context);
```

- 保存
```java
    KVStorage.rxSave(demoKey, demoValue).subscribe();
```

- 查找
```java
KVStorage.rxGet(demoKey)
         .subscribe(new AsyncObserver<String>() {
             @Override
             public void onSuccess(String result) {
                 
             }

             @Override
             public void onError(Throwable e) {
                 
             }
         });
```
- 删除
```java
KVStorage.rxRemove(demoKey1...).subscribe();
```
##功能接口
- 异步保存
- 同步保存
- 异步获取
- 异步删除
- 获取所有key
- 清除所有key
- 合并保存json
- 待开发...
