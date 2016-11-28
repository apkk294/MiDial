package com.lk.midial;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by lk on 2016/10/27.
 * 绘制顺序 最外层圆弧 → 刻度线 → 中间的圆圈 → 指针 → 按钮
 */

public class MiDialView extends View {

    private static final String TAG = "MiDialView";

    private float mDensity;

    private int   mDefaultBackground;   //背景颜色
    private Paint mPaint;   //画笔

    private int   mMinHeight;   //最小高度
    private float centerX;      //中心点x坐标
    private float centerY;      //中心点y坐标

    //最外层圆弧
    private RectF mArcRect;     //最外层弧线区域
    private float mArcRadio;    //外层圆弧的半径
    private int   mArcWidth;    //外层圆弧的宽度
    private int   mDefaultArcColor;    //外层圆弧的颜色

    //刻度线
    private int mTickWidth;             //刻度线的宽度
    private int mUncoveringTickColor;   //没有选中时的刻度指针颜色
    private int mCoveringTickColor;     //选中时的刻度指针颜色
    private int mTickIntervalToArc;     //刻度线距离弧线的间距
    private int mTickLength;            //刻度线的长度

    //中间的圆圈
    private int mInnerCircleStroke; //中间圆圈的宽度
    private int mInnerCircleRadio;  //中间圆圈的半径

    //指针
    private int mPointerLength; //指针长度
    private int mPointerColor;  //指针颜色
    private int mPointerAngle;  //指针当前所指刻度

    //按钮
    private RectF   mButtonRectF;   //按钮中间的方框区域
    private int     mButtonColor;   //按钮的颜色
    private String  mButtonText;    //按钮的文字
    private int     mButtonTextSize;    //按钮的文字大小
    private int     mButtonTextColor;   //按钮文字颜色
    private boolean mIsButtonTouched;   //开始按钮是否正在点中

    //按钮下面的提示语
    private String mTipText;   //提示语文字
    private int    mTipTextColor;  //提示语文字颜色
    private int    mTipTextSize;   //提示语文字大小

    //刚开始表盘会循环亮一遍的那个角度，动画结束后这个值等于100，所以可以判断这个是否等于100来
    //判断初始动画是否完成
    private int mInitAngle;

    private OnButtonClickListener mOnButtonClickListener;

    public MiDialView(Context context) {
        this(context, null);
    }

    public MiDialView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiDialView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mDensity = getContext().getResources().getDisplayMetrics().density;

        mPaint = new Paint();

        mMinHeight = dp2px(400);

        mArcWidth = dp2px(1);

        mTickWidth = dp2px(2);
        mTickIntervalToArc = dp2px(10);
        mTickLength = dp2px(25);

        mInnerCircleRadio = dp2px(10);

        mButtonText = "开始体检";

        mTipText = "手机出现问题，点击体检";


        //获取自定义属性
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.MiDialView, defStyleAttr, 0);
        mDefaultBackground = typedArray.getColor(R.styleable.MiDialView_dial_background,
                0xFF1E90FF);
        mDefaultArcColor = typedArray.getColor(R.styleable.MiDialView_dial_arc_color,
                0x66EEEEEE);
        mUncoveringTickColor = typedArray.getColor(
                R.styleable.MiDialView_dial_uncovering_tick_color, 0x66EEEEEE);
        mCoveringTickColor = typedArray.getColor(
                R.styleable.MiDialView_dial_covering_tick_color, 0xFFFFFFFF);
        mInnerCircleStroke = typedArray.getDimensionPixelSize(
                R.styleable.MiDialView_dial_inner_circle_stroke,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                        getResources().getDisplayMetrics()));
        mPointerColor = typedArray.getColor(R.styleable.MiDialView_dial_pointer_color,
                0xFFFFFFFF);
        mButtonColor = typedArray.getColor(R.styleable.MiDialView_dial_button_color,
                0x66EEEEEE);
        if (typedArray.hasValue(R.styleable.MiDialView_dial_button_text)) {
            mButtonText = typedArray.getString(R.styleable.MiDialView_dial_button_text);
        }
        mButtonTextColor = typedArray.getColor(R.styleable.MiDialView_dial_button_text_color,
                0xFFFFFFFF);
        //TypedValue自带的转换成sp的方法
        mButtonTextSize = typedArray.getDimensionPixelSize(
                R.styleable.MiDialView_dial_button_text_size,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16,
                        getResources().getDisplayMetrics()));
        if (typedArray.hasValue(R.styleable.MiDialView_dial_tip_text)) {
            mTipText = typedArray.getString(R.styleable.MiDialView_dial_tip_text);
        }
        mTipTextColor = typedArray.getColor(R.styleable.MiDialView_dial_tip_text_color,
                0x66EEEEEE);
        mTipTextSize = typedArray.getDimensionPixelSize(
                R.styleable.MiDialView_dial_tip_text_size,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12,
                        getResources().getDisplayMetrics()));
        typedArray.recycle();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        //计算View的中心点坐标
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;

        //计算最外层圆弧的半径
        mArcRadio = getWidth() > getHeight() ? centerY * 0.8f : centerX * 0.8f;
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
        canvas.drawColor(mDefaultBackground);

        drawRectArc(canvas);

        drawTick(canvas);

        drawInnerCircle(canvas);

        drawPointer(canvas);

        drawButton(canvas);

        drawTip(canvas);
        if (mInitAngle == 0) {
            startInitAnim();
        }
    }

    private void startInitAnim() {
        ValueAnimator animator = ValueAnimator.ofInt(1, 100);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mInitAngle = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.setDuration(1000);
        animator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isTouchedInButton(event.getX(), event.getY())) {
                    mIsButtonTouched = true;
                    postInvalidate();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isTouchedInButton(event.getX(), event.getY())) {
                    mIsButtonTouched = false;
                    postInvalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsButtonTouched) {
                    setPointer(100);
                    if (mOnButtonClickListener != null) {
                        mOnButtonClickListener.onButtonClick(this);
                    }
                }
                mIsButtonTouched = false;
                postInvalidate();
                break;
        }
        if (mIsButtonTouched) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 最外层的弧线
     *
     * @param canvas 画布
     */
    private void drawRectArc(Canvas canvas) {
        mPaint.setColor(mDefaultArcColor);
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

        //因为第一条刻度代表0，所以一共有100条刻度
        //整个弧度是260°，100条刻度线也就是260除以101，大约每个刻度之间间隔2.6°
        //后来发现260°的话会少一个刻度的距离，所以再加一个刻度的角度，即262.6°
        for (int i = 0; i < 100; i++) {
            startX = (float) (Math.sin(Math.toRadians(50 + i * 2.626)) * mPointLargeRadio);
            startY = (float) (Math.cos(Math.toRadians(50 + i * 2.626)) * mPointLargeRadio);
            endX = (float) (Math.sin(Math.toRadians(50 + i * 2.626)) * mPointSmallRadio);
            endY = (float) (Math.cos(Math.toRadians(50 + i * 2.626)) * mPointSmallRadio);

            startX += centerX;
            startY += centerY;
            endX += centerX;
            endY += centerY;

            //mInitAngle < 100 说明开始的动画还没有结束
            if (mInitAngle < 100) {
                //这里实现有一段刻度是亮的这种效果
                if (i > 99 - mInitAngle - 10 && i <= 99 - mInitAngle) {
                    mPaint.setColor(mCoveringTickColor);
                } else {
                    mPaint.setColor(mUncoveringTickColor);
                }
            } else {
                //因为刻度线是从右边开始的，而我们平常说的多少度是从左边开始的，所以就是i > 99 - 刻度
                mPaint.setColor(i > 99 - mPointerAngle ? mCoveringTickColor : mUncoveringTickColor);
            }


            canvas.drawLine(startX, startY, endX, endY, mPaint);
        }
    }

    /**
     * 画中间的圆圈
     *
     * @param canvas 画布
     */
    private void drawInnerCircle(Canvas canvas) {
        mPaint.setStrokeWidth(mInnerCircleStroke);
        mPaint.setColor(mPointerColor);
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
        mPaint.setColor(mPointerColor);

        canvas.save();

        //画布旋转140度，之后每加一个刻度，就多一个2.6度
        //根据要指到多少刻度就能算出画布要旋转多少度（140 + 刻度 * 2.6）
        canvas.rotate(140f + mPointerAngle * 2.6f, centerX, centerY);

        //第一条线的起始位置稍微靠上一点，第二条线的起始位置稍微向下一点，但是终点都是相同的，所以看上去
        //会有一头粗一头细的效果
        Path path = new Path();
        path.moveTo(centerX + mInnerCircleRadio, centerY + dp2px(1));
        path.lineTo(centerX + mPointerLength, centerY);
        canvas.drawPath(path, mPaint);

        Path path2 = new Path();
        path2.moveTo(centerX + mInnerCircleRadio, centerY - dp2px(1));
        path2.lineTo(centerX + mPointerLength, centerY);
        canvas.drawPath(path2, mPaint);

        canvas.restore();
    }

    /**
     * 画按钮
     *
     * @param canvas 画布
     */
    private void drawButton(Canvas canvas) {
        mPaint.setColor(mButtonColor);
        mPaint.setStrokeWidth(1);
        if (mIsButtonTouched) {
            //点击状态时设置画笔为填充
            mPaint.setStyle(Paint.Style.FILL);
        } else {
            mPaint.setStyle(Paint.Style.STROKE);
        }

        //按钮矩形的宽度为指针长度,高度为宽的一半
        int buttonWidth  = mPointerLength;
        int buttonHeight = buttonWidth / 2;

        mButtonRectF = new RectF(centerX - buttonWidth / 2, centerY + mArcRadio - buttonHeight,
                centerX + buttonWidth / 2, centerY + mArcRadio);

        Path mButtonPath = new Path();
        //下面那条线
        mButtonPath.moveTo(mButtonRectF.left, mButtonRectF.bottom);
        mButtonPath.lineTo(mButtonRectF.right, mButtonRectF.bottom);
        if (mIsButtonTouched) {
            //点击时需要画出右边的线，形成封闭图形才能正常填充颜色
            mButtonPath.lineTo(mButtonRectF.right, mButtonRectF.top);
        }
        //右边半圆
        RectF rightArc = new RectF(mButtonRectF.right - mButtonRectF.height() / 2,
                mButtonRectF.top, mButtonRectF.right + mButtonRectF.height() / 2, mButtonRectF.bottom);
        mButtonPath.addArc(rightArc, -90, 180);
        //上面那条线
        mButtonPath.moveTo(mButtonRectF.right, mButtonRectF.top);
        mButtonPath.lineTo(mButtonRectF.left, mButtonRectF.top);
        if (mIsButtonTouched) {
            //点击的话需要画出左边的线，形成封闭图形才能正常填充颜色
            mButtonPath.lineTo(mButtonRectF.left, mButtonRectF.bottom);
        }
        //左边半圆
        RectF leftArc = new RectF(mButtonRectF.left - mButtonRectF.height() / 2,
                mButtonRectF.top, mButtonRectF.left + mButtonRectF.height() / 2, mButtonRectF.bottom);
        mButtonPath.addArc(leftArc, 90, 180);

        canvas.drawPath(mButtonPath, mPaint);


        //按钮文字画笔的配置
        mPaint.setColor(mButtonTextColor);
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mButtonTextSize);


        //画出文字
        Rect textBounds = new Rect();
        mPaint.getTextBounds(mButtonText, 0, mButtonText.length(), textBounds);
        /*canvas.drawText(mButtonText, centerX - textBounds.width() / 2,
                centerY + mArcRadio - (mButtonRectF.height() / 2 - textBounds.height() / 2),
                mPaint);*/
        canvas.drawText(mButtonText, centerX - textBounds.width() / 2,
                mButtonRectF.centerY() + textBounds.height() / 2,
                mPaint);
    }

    private void drawTip(Canvas canvas) {
        mPaint.setColor(mTipTextColor);
        mPaint.setStrokeWidth(dp2px(1));
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(mTipTextSize);
        Rect textBounds = new Rect();
        mPaint.getTextBounds(mTipText, 0, mTipText.length(), textBounds);

        canvas.drawText(mTipText, centerX - textBounds.width() / 2,
                centerY + mArcRadio + textBounds.height() + dp2px(3), mPaint);
    }

    /**
     * Determine whether the coordinate position is within the range of the button
     * 判断坐标位置是否在按钮范围内
     *
     * @param x the x position
     * @param y the y position
     * @return Return true if the coordinate in the button rage
     */
    private boolean isTouchedInButton(float x, float y) {
        //坐标是否在按钮的矩形范围内
        if (x >= mButtonRectF.left && x <= mButtonRectF.right &&
                y >= mButtonRectF.top && y <= mButtonRectF.bottom) {
            return true;
        }

        //如果坐标与圆心之间的长度小于半径，说明坐标在圆内，即 x的平方 + y的平方 < 半径的平方

        //坐标是否在 左边的半圆范围内
        float leftCenterX = mButtonRectF.left; //左边半圆圆心x坐标
        float leftCenterY = mButtonRectF.top + mButtonRectF.height() / 2;   //左边半圆圆心y坐标
        float newLeftX    = x - leftCenterX;   //坐标距离左半圆圆心的X方向上的距离
        float newLeftY    = y - leftCenterY;   //坐标距离左半圆圆心的Y方向上的距离
        if (newLeftX * newLeftX + newLeftY * newLeftY <
                mButtonRectF.height() / 2 * mButtonRectF.height() / 2) {
            return true;
        }
        //坐标是否在 右边的半圆范围内
        float rightCenterX = mButtonRectF.right;    //右边半圆圆心的X坐标
        float rightCenterY = mButtonRectF.top + mButtonRectF.height() / 2;  //右边半圆圆心的Y坐标
        float newRightX    = x - rightCenterX;
        float newRightY    = y - rightCenterY;
        if (newRightX * newRightX + newRightY * newRightY <
                mButtonRectF.height() / 2 * mButtonRectF.height() / 2) {
            return true;
        }

        return false;
    }

    /**
     * Start pointer animator.
     * 开始指针动画
     *
     * @param pointer target pointer
     */
    private void startPointerAnim(int pointer) {
        if (pointer < 0 || pointer > 100) {
            throw new IndexOutOfBoundsException("The pointer should be in the range of 0 to 100");
        }

        ValueAnimator animator = ValueAnimator.ofInt(mPointerAngle, pointer);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mPointerAngle = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.setDuration(500);
        animator.start();
    }

    private int dp2px(int dp) {
        return (int) (dp * mDensity + 0.5f);
    }

    public int sp2px(float spValue) {
        final float fontScale = getContext().getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * Sets the pointer position
     * 设置指针位置
     *
     * @param pointer The pointer position, should between 0 to 100
     */
    void setPointer(int pointer) {
        //this.mPointerAngle = pointer;
        //invalidate();
        startPointerAnim(pointer);
    }

    /**
     * Set the tip text below start button ,if not set,then the default is "phone problems,
     * click to check"
     * 设置开始按钮下面的提示文字，如果不设置的话默认文字是“手机出现问题，点击体检”
     *
     * @param tipText The tip text String
     */
    void setTipText(String tipText) {
        mTipText = tipText;
        invalidate();
    }

    void setOnButtonClickListener(OnButtonClickListener onButtonClickListener) {
        this.mOnButtonClickListener = onButtonClickListener;
    }

    public interface OnButtonClickListener {

        void onButtonClick(View view);

    }

}