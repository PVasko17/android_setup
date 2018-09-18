package com.devforfun.app.callbacks;

import android.view.View;

public interface IListElementClickCallback {
    void onItemClicked(View view, int position);
    boolean onItemLongClick(View view, int position);
}
