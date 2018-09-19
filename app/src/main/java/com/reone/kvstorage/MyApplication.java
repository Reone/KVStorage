package com.reone.kvstorage;

import com.reone.kvstoragelib.KVStorage;
import com.reone.talklibrary.TalkApp;

/**
 * Created by wangxingsheng on 2018/9/19.
 */
public class MyApplication extends TalkApp {
    @Override
    public void onCreate() {
        super.onCreate();
        KVStorage.init(this);
    }
}
