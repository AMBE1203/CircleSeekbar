package com.ambe.circleseekbar;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by AMBE on 29/6/2019 at 17:20 PM.
 */
public class CircleSeekbar extends View {

    private static int INVALID_PROGRESS_VALUE = -1;


    // The initial rotational offset -90 means we start at 12 o'clock
    private final int mAngleOffset = -90;
    // draw thumb
    private Drawable mThumb;
    // max values
    private int mMax = 100;
    // current values
    private int mProgress = 0;
    // progress width
    private int mProgressWidth = 4;
    // background width
    private int mCircleWidth = 2;
    // start angle to draw seek
    private int mStartAngle = 0;
    //	 The Angle through which to draw the circle (Max is 360)
    private int mSweepAngle = 360;

    // rotate seek 0 -12h
    private int mRotate = 0;
    // Give the Seek rounded edges
    private boolean mRoundedEdges = false;
    // Enable touch inside the Seek
    private boolean mTouchInside = true;
    //  Will the progress increase clockwise or anti-clockwise
    private boolean mClockwise = true;
    // is the control enabled/touchable
    private boolean mEnabled = true;

    private int mCircleRadius = 0;
    private float mProgressSweep = 0;
    private RectF mSeekRect = new RectF();
    private Paint mSeekPaint;
    private Paint mProgressPaint;
    private int mTranslateX;
    private int mTranslateY;
    private int mThumbXPos;
    private int mThumbYPos;
    private double mTouchAngle;
    private float mTouchIgnoreRadius;

    private OnSeekbarCircleChangeListener mOnSeekbarCircleChangeListener;

    public interface OnSeekbarCircleChangeListener {
        void onProgressChanged(CircleSeekbar seekbar, int progress, boolean fromUser);

        void onStartTrackingTouch(CircleSeekbar seekbar);

        void onStopTrackingTouch(CircleSeekbar seekbar);
    }


    public CircleSeekbar(Context context) {
        super(context);
        init(context, null, 0);

    }

    public CircleSeekbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, R.attr.seekCircleStyle);
    }

    public CircleSeekbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {

        final Resources res = getResources();
        float density = context.getResources().getDisplayMetrics().density;

        int circleColor = res.getColor(R.color.progress_gray);
        int progressColor = res.getColor(R.color.default_blue_light);
        int thumbHalfHeight = 0;
        int thumbHalfWidth = 0;

        mThumb = res.getDrawable(R.drawable.seek_circle_control_selector);
        // convert progress width to pixels for current density
        mProgressWidth = (int) (mProgressWidth * density);
        if (attrs != null) {
            final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SeekCircle, defStyleAttr, 0);
            Drawable thumb = typedArray.getDrawable(R.styleable.SeekCircle_thumb);
            if (thumb != null) {
                mThumb = thumb;
            }

            thumbHalfHeight = (int) mThumb.getIntrinsicHeight() / 2;
            thumbHalfWidth = (int) mThumb.getIntrinsicWidth() / 2;
            mThumb.setBounds(-thumbHalfWidth, -thumbHalfHeight, thumbHalfWidth,
                    thumbHalfHeight);

            mMax = typedArray.getInteger(R.styleable.SeekCircle_max, mMax);
            mProgress = typedArray.getInteger(R.styleable.SeekCircle_progress, mProgress);
            mProgressWidth = (int) typedArray.getDimension(
                    R.styleable.SeekCircle_progressWidth, mProgressWidth);
            mCircleWidth = (int) typedArray.getDimension(R.styleable.SeekCircle_circleWidth,
                    mCircleWidth);
            mStartAngle = typedArray.getInt(R.styleable.SeekCircle_startAngle, mStartAngle);
            mSweepAngle = typedArray.getInt(R.styleable.SeekCircle_sweepAngle, mSweepAngle);
            mRotate = typedArray.getInt(R.styleable.SeekCircle_rotation, mRotate);
            mRoundedEdges = typedArray.getBoolean(R.styleable.SeekCircle_roundEdges,
                    mRoundedEdges);
            mTouchInside = typedArray.getBoolean(R.styleable.SeekCircle_touchInside,
                    mTouchInside);
            mClockwise = typedArray.getBoolean(R.styleable.SeekCircle_clockwise,
                    mClockwise);
            mEnabled = typedArray.getBoolean(R.styleable.SeekCircle_enabled, mEnabled);

            circleColor = typedArray.getColor(R.styleable.SeekCircle_circleColor, circleColor);
            progressColor = typedArray.getColor(R.styleable.SeekCircle_progressColor,
                    progressColor);

            typedArray.recycle();
        }

        mProgress = (mProgress > mMax) ? mMax : mProgress;
        mProgress = (mProgress < 0) ? 0 : mProgress;

        mSeekPaint = new Paint();
        mSeekPaint.setColor(circleColor);
        mSeekPaint.setAntiAlias(true);
        mSeekPaint.setStyle(Paint.Style.STROKE);
        mSeekPaint.setStrokeWidth(mCircleWidth);
        //mArcPaint.setAlpha(45);

        mProgressPaint = new Paint();
        mProgressPaint.setColor(progressColor);
        mProgressPaint.setAntiAlias(true);
        mProgressPaint.setStyle(Paint.Style.STROKE);
        mProgressPaint.setStrokeWidth(mProgressWidth);

        if (mRoundedEdges) {
            mSeekPaint.setStrokeCap(Paint.Cap.ROUND);
            mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        }


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);

        final int min = Math.min(width, height);

        float top = 0;
        float left = 0;
        int circleDiameter = 0;

        mTranslateX = (int) (width * 0.5f);
        mTranslateY = (int) (height * 0.5f);

        circleDiameter = min - getPaddingLeft();
        mCircleRadius = circleDiameter / 2;

        top = height / 2 - (circleDiameter / 2);
        left = width / 2 - (circleDiameter / 2);

        mSeekRect.set(left, top, left + circleDiameter, top + circleDiameter);

        int circleStart = (int) mProgressSweep + mStartAngle + mRotate + 90;
        mThumbXPos = (int) (mCircleRadius * Math.cos(Math.toRadians(circleStart)));
        mThumbYPos = (int) (mCircleRadius * Math.sin(Math.toRadians(circleStart)));

        setTouchInSize(mTouchInside);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onDraw(Canvas canvas) {

        if (!mClockwise) {
            canvas.scale(-1, 1, mSeekRect.centerX(), mSeekRect.centerY());
        }

        final int circleStart = mStartAngle + mAngleOffset + mRotate;
        final int circleSweep = mSweepAngle;

        canvas.drawArc(mSeekRect, circleStart, circleSweep, false, mSeekPaint);
        canvas.drawArc(mSeekRect, circleStart, mProgressSweep, false, mProgressPaint);

        if (mEnabled) {
            canvas.translate(mTranslateX - mThumbXPos, mTranslateY - mThumbYPos);
            mThumb.draw(canvas);
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mEnabled) {
            this.getParent().requestDisallowInterceptTouchEvent(true);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    onStartTrackingTouch();
                    updateOnTouchEvent(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    updateOnTouchEvent(event);
                    break;
                case MotionEvent.ACTION_UP:
                    onStopTrackingTouch();
                    setPressed(false);
                    this.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    onStopTrackingTouch();
                    setPressed(false);
                    this.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mThumb != null && mThumb.isStateful()) {
            int[] state = getDrawableState();
            mThumb.setState(state);
        }
        invalidate();
    }

    private void onStopTrackingTouch() {
        if (mOnSeekbarCircleChangeListener != null) {
            mOnSeekbarCircleChangeListener.onStopTrackingTouch(this);
        }
    }

    private void onStartTrackingTouch() {
        if (mOnSeekbarCircleChangeListener != null) {
            mOnSeekbarCircleChangeListener.onStartTrackingTouch(this);
        }

    }

    private void updateOnTouchEvent(MotionEvent event) {
        boolean ignoreTouch = ignoreTouch(event.getX(), event.getY());
        if (ignoreTouch) {
            return;
        }
        setPressed(true);
        mTouchAngle = getTouchDegress(event.getX(), event.getY());
        int progress = getProgressForAngle(mTouchAngle);
        onProgressRefresh(progress, true);

    }

    private void onProgressRefresh(int progress, boolean fromUser) {
        updateProgress(progress, fromUser);


    }


    private int getProgressForAngle(double mTouchAngle) {
        int touchProgress = (int) Math.round(valuePerDegree() * mTouchAngle);
        touchProgress = (touchProgress < 0) ? INVALID_PROGRESS_VALUE : touchProgress;
        touchProgress = (touchProgress > mMax) ? INVALID_PROGRESS_VALUE : touchProgress;
        return touchProgress;
    }

    private float valuePerDegree() {
        return (float) mMax / mSweepAngle;
    }


    private void updateProgress(int progress, boolean fromUser) {
        if (progress == INVALID_PROGRESS_VALUE) {
            return;
        }

        progress = (progress > mMax) ? mMax : progress;
        progress = (progress < 0) ? 0 : progress;
        mProgress = progress;

        if (mOnSeekbarCircleChangeListener != null) {
            mOnSeekbarCircleChangeListener.onProgressChanged(this, progress, fromUser);
        }

        mProgressSweep = (float) progress / mMax * mSweepAngle;

        updateThumbPosition();

        invalidate();


    }

    private void updateThumbPosition() {
        int thumbAngle = (int) (mStartAngle + mProgressSweep + mRotate + 90);
        mThumbXPos = (int) (mCircleRadius * Math.cos(Math.toRadians(thumbAngle)));
        mThumbYPos = (int) (mCircleRadius * Math.sin(Math.toRadians(thumbAngle)));

    }

    private double getTouchDegress(float xPos, float yPos) {
        float x = xPos - mTranslateX;
        float y = yPos - mTranslateY;
        x = (mClockwise) ? x : -x;
        // convert to circle angle

        double angle = Math.toDegrees(Math.atan2(y, x) + (Math.PI / 2) - Math.toRadians(mRotate));
        if (angle < 0) {
            angle = 360 + angle;
        }

        angle -= mStartAngle;

        return angle;
    }

    private boolean ignoreTouch(float xPos, float yPos) {
        boolean ignore = false;
        float x = xPos - mTranslateX;
        float y = yPos - mTranslateY;

        float touchRadius = (float) Math.sqrt((x * x) + (y * y));
        if (touchRadius < mTouchIgnoreRadius) {
            ignore = true;
        }
        return ignore;
    }


    private void setTouchInSize(boolean isEnabled) {
        int thumbHalfHeight = (int) mThumb.getIntrinsicHeight() / 2;
        int thumbHalfWidth = (int) mThumb.getIntrinsicWidth() / 2;
        mTouchInside = isEnabled;
        if (mTouchInside) {
            mTouchIgnoreRadius = (float) mCircleRadius / 4;
        } else {
            mTouchIgnoreRadius = mCircleRadius - Math.min(thumbHalfWidth, thumbHalfHeight);
        }

    }

    public void setOnSeekbarCircleChangeListener(OnSeekbarCircleChangeListener listener) {
        this.mOnSeekbarCircleChangeListener = listener;
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int mProgress) {
        updateProgress(mProgress, false);
    }

    public int getProgressWidth() {
        return mProgressWidth;
    }

    public void setProgressWidth(int mProgressWidth) {
        this.mProgressWidth = mProgressWidth;
        mProgressPaint.setStrokeWidth(mProgressWidth);
    }

    public int getCircleWidth() {
        return mCircleWidth;
    }

    public void setCircleWidth(int mCircleWidth) {
        this.mCircleWidth = mCircleWidth;
        mSeekPaint.setStrokeWidth(mCircleWidth);
    }

    public int getCircleRotation() {
        return mRotate;
    }

    public void setCircleRotation(int mRotation) {
        this.mRotate = mRotation;
        updateThumbPosition();
    }

    public int getStartAngle() {
        return mStartAngle;
    }

    public void setStartAngle(int mStartAngle) {
        this.mStartAngle = mStartAngle;
        updateThumbPosition();
    }

    public int getSweepAngle() {
        return mSweepAngle;
    }

    public void setSweepAngle(int mSweepAngle) {
        this.mSweepAngle = mSweepAngle;
        updateThumbPosition();
    }

    public void setRoundedEdges(boolean isEnabled) {
        mRoundedEdges = isEnabled;
        if (mRoundedEdges) {
            mSeekPaint.setStrokeCap(Paint.Cap.ROUND);
            mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        } else {
            mSeekPaint.setStrokeCap(Paint.Cap.SQUARE);
            mProgressPaint.setStrokeCap(Paint.Cap.SQUARE);
        }
    }

    public void setTouchInSide(boolean isEnabled) {
        int thumbHalfheight = (int) mThumb.getIntrinsicHeight() / 2;
        int thumbHalfWidth = (int) mThumb.getIntrinsicWidth() / 2;
        mTouchInside = isEnabled;
        if (mTouchInside) {
            mTouchIgnoreRadius = (float) mCircleRadius / 4;
        } else {
            // Don't use the exact radius makes interaction too tricky
            mTouchIgnoreRadius = mCircleRadius
                    - Math.min(thumbHalfWidth, thumbHalfheight);
        }
    }

    public void setClockwise(boolean isClockwise) {
        mClockwise = isClockwise;
    }

    public boolean isClockwise() {
        return mClockwise;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public int getProgressColor() {
        return mProgressPaint.getColor();
    }

    public void setProgressColor(int color) {
        mProgressPaint.setColor(color);
        invalidate();
    }

    public int getArcColor() {
        return mSeekPaint.getColor();
    }

    public void setArcColor(int color) {
        mSeekPaint.setColor(color);
        invalidate();
    }

    public int getMax() {
        return mMax;
    }

    public void setMax(int mMax) {
        this.mMax = mMax;
    }
}
