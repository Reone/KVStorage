package com.reone.kvstorage;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.reone.kvstoragelib.KVStorage;

import java.text.MessageFormat;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SimpleActivity extends AppCompatActivity {

    @BindView(R.id.et_demo_key)
    EditText etDemoKey;
    @BindView(R.id.et_demo_value)
    EditText etDemoValue;
    @BindView(R.id.tv_result)
    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_save, R.id.btn_get, R.id.btn_delete, R.id.btn_get_all, R.id.btn_clear})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.btn_save:
                String demoKey = getEditText(etDemoKey);
                String demoValue = getEditText(etDemoValue);
                if (TextUtils.isEmpty(demoKey)) {
                    showShortToast("demo Key值为空");
                }
                if (TextUtils.isEmpty(demoValue)) {
                    showShortToast("demo value值为空");
                }
                KVStorage.rxSave(demoKey, demoValue)
                        .subscribe(new AsyncObserver<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                showShortToast("储存成功,返回：" + result);
                            }

                            @Override
                            public void onError(Throwable e) {
                                showShortToast("储存失败," + e.getMessage());
                                tvResult.setText(e.getMessage());
                            }
                        });
                break;
            case R.id.btn_get:
                demoKey = getEditText(etDemoKey);
                KVStorage.rxGet(demoKey)
                        .subscribe(new AsyncObserver<String>() {
                            @Override
                            public void onSuccess(String result) {
                                showShortToast("查找成功,返回：" + result);
                                tvResult.setText(result);
                            }

                            @Override
                            public void onError(Throwable e) {
                                showShortToast("查找失败," + e.getMessage());
                                tvResult.setText(e.getMessage());
                            }
                        });
                break;
            case R.id.btn_delete:
                demoKey = getEditText(etDemoKey);
                KVStorage.rxRemove(demoKey)
                        .subscribe(new AsyncObserver<Integer>() {
                            @Override
                            public void onSuccess(Integer result) {
                                showShortToast("删除成功,返回：" + result);
                                tvResult.setText(MessageFormat.format("delete {0}line", result));
                            }

                            @Override
                            public void onError(Throwable e) {
                                showShortToast("删除失败," + e.getMessage());
                                tvResult.setText(e.getMessage());
                            }
                        });
                break;
            case R.id.btn_get_all:
                KVStorage.rxGetAllKeys()
                        .subscribe(new AsyncObserver<List<String>>() {
                            @Override
                            public void onSuccess(List<String> result) {
                                showShortToast("获取成功");
                                StringBuilder sb = new StringBuilder();
                                int count = result.size();
                                for (int i = 0; i < count; i++) {
                                    sb.append(result.get(i));
                                    sb.append("\n");
                                }
                                tvResult.setText(sb.toString());
                            }

                            @Override
                            public void onError(Throwable e) {
                                showShortToast("获取失败," + e.getMessage());
                                tvResult.setText(e.getMessage());
                            }
                        });
                break;
            case R.id.btn_clear:
                KVStorage.rxClear()
                        .subscribe(new AsyncObserver<Integer>() {
                            @Override
                            public void onSuccess(Integer result) {
                                showShortToast("清除成功");
                                tvResult.setText(MessageFormat.format("clear {0}line", result));
                            }

                            @Override
                            public void onError(Throwable e) {
                                showShortToast("清除失败," + e.getMessage());
                                tvResult.setText(e.getMessage());
                            }
                        });
                break;
        }
    }

    private String getEditText(EditText editText) {
        try {
            return editText.getText().toString().trim();
        } catch (Exception ignore) {
        }
        return "";
    }

    private void showShortToast(String talk) {
        MyApplication.talk(talk);
    }
}
