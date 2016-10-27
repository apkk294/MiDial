package com.lk.midial;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lk on 2016/10/27.
 */

public class MiDialView extends View {

    private int mBgColor;   //背景颜色

    private Paint mPaint;   //画笔

    private int mMinHeight; //最小高度

    public MiDialView(Context context) {
        super(context);
        init(context);
    }

    public MiDialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MiDialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context mContext) {
        mBgColor = 0xff0000ff;
        mPaint = new Paint();
        mMinHeight = dp2px(mContext, 800);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.AT_MOST) {
            heightSize = mMinHeight;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //mPaint.setColor(mBgColor);
        canvas.drawColor(mBgColor);
    }

    private int dp2px(Context context, int dp) {
        final float mDensity = context.getResources().getDisplayMetrics().density;
        return (int) (dp * mDensity + 0.5f);
    }

}
