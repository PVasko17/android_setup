package com.devforfun.app.util;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.devforfun.app.R;

public class MyFragment extends Fragment {

    protected View rootView;

    protected Context context;
    protected MyActivity activity;

    protected int currentApiVersion = android.os.Build.VERSION.SDK_INT;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = (MyActivity) getActivity();
    }

    protected void showProgress(boolean isShow, int formId) {
        View progressView = rootView.findViewById(R.id.progress);
        View formView = rootView.findViewById(formId);
        if (progressView != null) {
            progressView.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }

        if (formView != null) {
            formView.setVisibility(isShow ? View.GONE : View.VISIBLE);
        }
    }

    protected void showProgress(boolean isShow) {
        View progressView = rootView.findViewById(R.id.progress);
        if (progressView != null) {
            progressView.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }
}
