package com.chenjimou.recyclerviewpullrefreshdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

public class MyHeader extends FrameLayout
{
    int a;
    int b;
    //gogogo

    public MyHeader(Context context)
    {
        this(context, null);
    }

    public MyHeader(Context context, @Nullable AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public MyHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.head, this);
    }

    public int getHeaderHeight()
    {
        return getMeasuredHeight();
    }
}
