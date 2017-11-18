package edu.xyc.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CustomProgressBar extends View {

    /**
     * 完整圆环的背景颜色
     */
    private int mBackgroundColor;

    /**
     * 部分圆环的背景颜色
     */
    private int mProgressColor;

    /**
     * 圆环的宽度
     */
    private float mAnnulusWidth;

    /**
     * 画完整圆环的画笔
     */
    private Paint mWholeAnnulusPaint;

    /**
     * 画部分圆环的画笔
     */
    private Paint mMutilatedAnnulusPaint;

    private RectF mRectF;

    /**
     * 当前进度
     */
    private int mCurrentProgress;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            changeProgress((Integer) msg.obj);
        }
    };

    public CustomProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        initAttrs(context, attrs);
        initVariable();
    }

    private void initAttrs(Context context, AttributeSet attrs) {

        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomProgressBarStyle, 0, 0);

        mProgressColor = typeArray.getColor(R.styleable.CustomProgressBarStyle_progressColor, 0xFF28C996);
        mBackgroundColor = typeArray.getColor(R.styleable.CustomProgressBarStyle_backgroundColor, 0xFFC9C9C9);
        mAnnulusWidth = typeArray.getDimension(R.styleable.CustomProgressBarStyle_width, 15);

        typeArray.recycle();
    }

    private void initVariable() {

        // 设置画完整圆环的画笔
        mWholeAnnulusPaint = new Paint();
        // 防锯齿
        mWholeAnnulusPaint.setAntiAlias(true);
        // Style.STROKE：空心,Style.FILL：实心
        mWholeAnnulusPaint.setStyle(Paint.Style.STROKE);
        mWholeAnnulusPaint.setColor(mBackgroundColor);
        mWholeAnnulusPaint.setStrokeWidth(mAnnulusWidth);

        // 设置画部分圆环的画笔
        mMutilatedAnnulusPaint = new Paint();
        mMutilatedAnnulusPaint.setAntiAlias(true);
        mMutilatedAnnulusPaint.setStyle(Paint.Style.STROKE);
        mMutilatedAnnulusPaint.setColor(mProgressColor);
        mMutilatedAnnulusPaint.setStrokeWidth(mAnnulusWidth);

        mRectF = new RectF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 圆环默认的大小
        int mDefaultSize = 100;

        // 分别获取测量模式 和 测量大小
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // 如果是精确度模式,就按xml中定义的来
        // 如果是最大值模式,就按我们定义的来
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mDefaultSize, mDefaultSize);
        } else if (widthMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(mDefaultSize, heightSize);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSize, mDefaultSize);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int mWidth = getWidth();
        int mHeight = getHeight();

        // 将画布坐标原点移动到中心位置
        canvas.translate(mWidth / 2, mHeight / 2);

        // 弧半径
        float r = (float) (Math.min(mWidth, mHeight) / 2 * 0.8);

        mRectF.left = -r;
        mRectF.top = -r;
        mRectF.right = r;
        mRectF.bottom = r;

        canvas.drawArc(mRectF, 0, 360, false, mWholeAnnulusPaint);

        // 总进度
        int mTotalProgress = 100;

        canvas.drawArc(mRectF, -90, ((float) mCurrentProgress / mTotalProgress) * 360, false, mMutilatedAnnulusPaint);
    }

    /**
     * 设置当前进度
     *
     * @param progress 进度
     */
    public void setProgress(int progress) {
        // 目标进度
        final int mTargetProgress = progress;

        ThreadPoolExecutor mThreadPoolExecutor = newThreadPoolExecutor();
        mThreadPoolExecutor.execute(new Runnable() {

            int i = 0;
            int j = 0;

            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    SystemClock.sleep(1000);

                    i++;
                    if (j < mTargetProgress) {
                        j += i;
                    } else {
                        j = mTargetProgress;
                        return;
                    }

                    Message message = mHandler.obtainMessage();
                    message.obj = j;
                    mHandler.sendMessage(message);
                }
            }
        });
    }

    /**
     * 创建线程池
     *
     * @return 线程池
     */
    private ThreadPoolExecutor newThreadPoolExecutor() {
        /**
         * @param corePoolSize    核心线程池大小
         * @param maximumPoolSize 线程池最大容量大小
         * @param keepAliveTime   线程池空闲时,线程存活的时间
         * @param unit            时间单位
         * @param workQueue       任务缓存队列,用来存放等待执行的任务
         * @param handler         任务拒绝策略
         */
        return new ThreadPoolExecutor(
                1,
                3,
                60L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * 更改进度
     *
     * @param progress 进度
     */
    private void changeProgress(int progress) {
        mCurrentProgress = progress;
        // UI重绘
        postInvalidate();
    }
}
