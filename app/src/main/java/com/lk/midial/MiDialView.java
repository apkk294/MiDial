package com.lk.midial;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lk on 2016/10/27.
 */

public class MiDialView extends View {

    private float mDensity;

    private int   mBgColor;                  //背景颜色
    private Paint mPaint;                   //画笔

    private int   mMinHeight;   //最小高度
    private float centerX;      //中心点x坐标
    private float centerY;      //中心点y坐标

    private RectF mArcRect;     //最外层弧线区域
    private float mArcRadio;    //外层圆弧的半径
    private int   mArcWidth;    //外层圆弧的宽度
    private int   mArcColor;    //外层圆弧的颜色

    private int mTickWidth;             //刻度线的宽度
    private int mUncoveringTickColor;   //没有选中时的刻度指针颜色
    private int mCoveringTickrColor;    //选中时的刻度指针颜色
    private int mTickIntervalToArc;     //刻度线距离弧线的间距
    private int mTickLength;            //刻度线的长度

    private int mInnerCircleWidth;  //中间圆圈的宽
    private int mInnerCircleRadio;  //中间圆圈的半径

    private int mPointerLength; //指针长度
    private int mPointerAngle;   //指针当前所指刻度

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
        mDensity = getContext().getResources().getDisplayMetrics().density;

        mPaint = new Paint();
        mBgColor = 0xFF1E90FF;
        mMinHeight = dp2px(400);

        mArcWidth = dp2px(1);
        mArcColor = 0xFFCCCCCC;

        mTickWidth = dp2px(2);
        mUncoveringTickColor = 0xFFAEAEAE;
        mCoveringTickrColor = 0xFFFFFFFF;
        mTickIntervalToArc = dp2px(10);
        mTickLength = dp2px(20);

        mInnerCircleWidth = dp2px(4);
        mInnerCircleRadio = dp2px(10);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //计算View的中心点坐标
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        //计算最外层圆弧的半径
        mArcRadio = getWidth() > getHeight() ? centerY * 0.9f : centerX * 0.9f;
        //圆弧所在的正方形区域
        mArcRect = new RectF(centerX - mArcRadio, centerY - mArcRadio, centerX + mArcRadio,
                centerY + mArcRadio);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        //这里设置View的高度为固定值800dp
        setMeasuredDimension(widthSize, mMinHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画背景色
        canvas.drawColor(mBgColor);

        drawRectArc(canvas);

        drawTick(canvas);

        drawInnerCircle(canvas);

        drawPointer(canvas);
    }

    /**
     * 最外层的弧线
     *
     * @param canvas 画布
     */
    private void drawRectArc(Canvas canvas) {
        mPaint.setColor(mArcColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(mArcWidth);
        canvas.drawArc(mArcRect, 140, 260f, false, mPaint);
    }

    /**
     * 刻度线
     */
    private void drawTick(Canvas canvas) {
        mPaint.setColor(mUncoveringTickColor);
        mPaint.setStrokeWidth(mTickWidth);

        float startX, startY, endX, endY;
        float mPointLargeRadio = mArcRadio - mTickIntervalToArc;
        float mPointSmallRadio = mPointLargeRadio - mTickLength;

        //因为第一条刻度代表0，所以一共有101条刻度
        //整个弧度是260°，101条刻度线也就是260除以101，大约每个刻度之间间隔2.6°
        for (int i = 0; i <= 100; i++) {
            startX = (float) (Math.sin(Math.toRadians(50 + i * 2.6)) * mPointLargeRadio);
            startY = (float) (Math.cos(Math.toRadians(50 + i * 2.6)) * mPointLargeRadio);
            endX = (float) (Math.sin(Math.toRadians(50 + i * 2.6)) * mPointSmallRadio);
            endY = (float) (Math.cos(Math.toRadians(50 + i * 2.6)) * mPointSmallRadio);

            startX += centerX;
            startY += centerY;
            endX += centerX;
            endY += centerY;

            mPaint.setColor(i > 100 - mPointerAngle ? mCoveringTickrColor : mUncoveringTickColor);

            canvas.drawLine(startX, startY, endX, endY, mPaint);
        }
    }

    /**
     * 画中间的圆圈
     *
     * @param canvas 画布
     */
    private void drawInnerCircle(Canvas canvas) {
        mPaint.setStrokeWidth(mInnerCircleWidth);
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, mInnerCircleRadio, mPaint);
    }

    /**
     * 画指针。
     * 方法是首先画出一条水平的指针，然后根据要指到多少刻度来计算画布的旋转角度，通过画布的旋转就可以实现
     * 改变指针方向的效果
     *
     * @param canvas 画布
     */
    private void drawPointer(Canvas canvas) {
        //指针的长度 = 外层弧的长度 - 弧和刻度线的间距 - 刻度线的长度 - 再减去一个指针和刻度线的间距
        mPointerLength = (int) (mArcRadio - mTickIntervalToArc - mTickLength - mTickIntervalToArc);
        mPaint.setStrokeWidth(dp2px(3));

        canvas.save();

        //画布旋转140度，之后每加一个刻度，就多一个2.6度
        //根据要指到多少刻度就能算出画布要旋转多少度（140 + 刻度 * 2.6）
        canvas.rotate(140f + mPointerAngle * 2.6f, centerX, centerY);

        //第一条线的起始位置稍微靠上一点，第二条线的起始位置稍微向下一点，但是终点都是相同的，所以看上去
        //会有一头粗一头细的效果
        Path path = new Path();
        path.moveTo(centerX + mInnerCircleRadio - dp2px(1), centerY + dp2px(1));
        path.lineTo(centerX + mPointerLength, centerY);
        canvas.drawPath(path, mPaint);

        Path path2 = new Path();
        path2.moveTo(centerX + mInnerCircleRadio - dp2px(1), centerY - dp2px(1));
        path2.lineTo(centerX + mPointerLength, centerY);
        canvas.drawPath(path2, mPaint);

        canvas.restore();
    }

    private int dp2px(int dp) {
        return (int) (dp * mDensity + 0.5f);
    }

    void setPointer(int pointer) {
        this.mPointerAngle = pointer;
        invalidate();
    }

}
