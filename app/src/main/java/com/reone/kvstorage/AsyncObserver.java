package com.reone.kvstorage;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public abstract class AsyncObserver<T> implements Observer<T> {

    @Override
    public void onSubscribe(Disposable d) {

    }

    @Override
    public void onNext(T t) {
        onSuccess(t);
    }


    @Override
    public void onComplete() {

    }

    public abstract void onSuccess(T result);
}
