package com.shallow.remotestethoscope.waveview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.shallow.remotestethoscope.R;
import com.shallow.remotestethoscope.base.BaseRecorder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

public class AudioWaveView extends View {
    final protected  Object mLock = new Object();

    private Context mContext;

    private Bitmap mBitmap, mBackgroundBitmap;

    private Paint mPaint;

    private Paint mViewPaint;

    private Canvas mCanvas = new Canvas();

    private Canvas mBackCanvas = new Canvas();

    private final ArrayList<Short> mRecDataList = new ArrayList<>();

    private BaseRecorder mBaseRecorder;

    private int mWidthSpecSize;

    private int mHeightSpecSize;

    private int mScale = 1;

    private int mBaseLine;

    private int mOffset = -11;

    private boolean mAlphaByVolume;

    private boolean mIsDraw = true;

    private boolean mDrawBase = true;

    private boolean mDrawReverse = false;

    private boolean mDataReverse = false;

    private boolean mPause = false;

    private int mWaveCount = 2;

    private int mWaveColor = Color.parseColor("#E55D61");

    private int mColorPoint = 1;

    private int mPreFFtCurrentFrequency;

    private int mColorChangeFlag;

    private int mColorFirst = Color.argb(0xfa, 0x6f, 0xff, 0x81);

    private int mColorSecond = Color.argb(0xfa, 0xff, 0xff, 0xff);

    private int mColorThird = Color.argb(0xfa, 0x42, 0xff, 0xff);

    private DrawThread mInnerThread;

    private int mDrawStartOffset = 0;

    //    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AudioWaveView.this.invalidate();
        }
    };

    public AudioWaveView(Context context) {
        super(context);
        init(context, null);
    }

    public AudioWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AudioWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsDraw = false;
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        if (mBackgroundBitmap != null && mBackgroundBitmap.isRecycled()) {
            mBackgroundBitmap.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMessageSpec, int heightMeasureSpec){
        super.onMeasure(widthMessageSpec, heightMeasureSpec);
        createBackGroundBitmap();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changeView, int visibility) {
        super.onVisibilityChanged(changeView, visibility);
        if (visibility == VISIBLE && mBackgroundBitmap == null) {
            createBackGroundBitmap();
        }
    }

//    public AudioWaveView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//        init(context,attrs);
//    }

    public void init(Context context, AttributeSet attrs) {
        mContext = context;
        if (isInEditMode()) {
            return;
        }
        if (attrs != null) {
            TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.waveView);
            mOffset = ta.getInt(R.styleable.waveView_waveOffset, dip2px(context, -11));
            mWaveColor = ta.getColor(R.styleable.waveView_waveColor, Color.parseColor("#E55D61"));
            mWaveCount = ta.getInt(R.styleable.waveView_waveCount, 2);
            ta.recycle();
        }

        if (mOffset == dip2px(context, -11)) {
            mOffset = dip2px(context, 1);
        }

        if (mWaveCount < 1) {
            mWaveCount = 1;
        } else if (mWaveCount > 2) {
            mWaveCount = 2;
        }

        mPaint = new Paint();
        mViewPaint = new Paint();
        mPaint.setColor(mWaveColor);
    }

    private void createBackGroundBitmap() {
        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (getWidth() > 0 && getHeight() > 0) {
                    mWidthSpecSize = getWidth();
                    mHeightSpecSize = getHeight();
                    mBaseLine = mHeightSpecSize / 2;
                    mBitmap = Bitmap.createBitmap(mWidthSpecSize, mHeightSpecSize, Bitmap.Config.ARGB_8888);
                    mBackgroundBitmap = Bitmap.createBitmap(mWidthSpecSize, mHeightSpecSize, Bitmap.Config.ARGB_8888);
                    mCanvas.setBitmap(mBitmap);
                    mBackCanvas.setBitmap(mBackgroundBitmap);
                    ViewTreeObserver vto = getViewTreeObserver();
                    vto.removeOnPreDrawListener(this);
                }
                return true;
            }
        });
    }

    private class DrawThread extends Thread {
        @SuppressWarnings("unchecked")
        @Override
        public void run(){
            while (mIsDraw) {
                ArrayList<Short> dataList = new ArrayList<>();
                synchronized (mRecDataList) {
                    if (mRecDataList.size() != 0) {
                        try {
                            dataList = (ArrayList<Short>) deepClone(mRecDataList);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }
                    }
                }
                if (mBackgroundBitmap == null) {
                    continue;
                }

                if (!mPause) {
                    resolveToWaveData(dataList);
                    if (dataList.size() > 0) {
                        updateColor();
                    }
                    if (mBackCanvas != null) {
                        mBackCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        int drawBufSize = dataList.size();

                        int startPosition = mDataReverse ? mWidthSpecSize - mDrawStartOffset : mDrawStartOffset;
                        int jOffset = mDrawReverse ? -mOffset : mOffset;

                        if (mDrawBase) {
                            if (mDataReverse) {
                                mBackCanvas.drawLine(startPosition, mBaseLine, 0, mBaseLine, mPaint);
                            } else {
                                mBackCanvas.drawLine(startPosition, mBaseLine, mWidthSpecSize, mBaseLine, mPaint);
                            }
                        }

                        if (mDataReverse) {
                            for (int i = drawBufSize - 1, j = startPosition; i >= 0; i--, j += jOffset) {
                                Short sh = dataList.get(i);
                                drawNow(sh, j);
                            }
                        } else {
                            for (int i = 0, j = startPosition; i < drawBufSize; i++, j += jOffset) {
                                Short sh = dataList.get(i);
                                drawNow(sh, j);
                            }
                        }

                        synchronized (mLock) {
                            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                            mCanvas.drawBitmap(mBackgroundBitmap, 0, 0, mPaint);
                        }

                        Message msg = new Message();
                        msg.what = 0;
                        handler.sendMessage(msg);
                    }
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    System.out.println("sleep error");
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 根据当前块数据来判断缩放音频显示的比例
     *
     * @param list 音频数据
     */
    private void resolveToWaveData(ArrayList<Short> list) {
        short allMax = 0;
        for (int i = 0; i < list.size(); i++) {
            Short sh = list.get(i);
            if (sh != null && sh > allMax) {
                allMax = sh;
            }
        }
        int curScale = allMax / mBaseLine;
        if (curScale > mScale) {
            mScale = (curScale == 0) ? 1 : curScale;
        }
    }

    private void drawNow(Short sh, int j) {
        if (sh != null) {
            short max = (short) (mBaseLine - sh / mScale);
            short min;
            if (mWaveCount == 2) {
                min = (short) (sh / mScale + mBaseLine);
            } else {
                min = (short) mBaseLine;
            }
            mBackCanvas.drawLine(j, mBaseLine, j, max, mPaint);
            mBackCanvas.drawLine(j, min, j, mBaseLine, mPaint);
        }
    }

    /**
     * Deep clone to avoid ConcurrentModificationException
     *
     * @param src list
     * @return dest
     */
    private List deepClone(List src) throws IOException, OptionalDataException, ClassNotFoundException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(src);

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream in = new ObjectInputStream(byteIn);
        @SuppressWarnings("unchecked")
        List dest = (List) in.readObject();
        return dest;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (mIsDraw && mBitmap != null){
            c.drawBitmap(mBitmap, 0, 0, mViewPaint);
        }
    }

    private void updateColor() {
        if (mBaseRecorder == null) {
            return;
        }

        int volume = mBaseRecorder.getRealVolume();
//        Log.e("Volume", "Volume" + volume);

        int scale = volume / 100;

        if (scale < 5) {
            mPreFFtCurrentFrequency = scale;
            return;
        }

        int fftScale = 0;
        if (mPreFFtCurrentFrequency != 0) {
            fftScale = scale / mPreFFtCurrentFrequency;
        }

        if (mColorChangeFlag == 4 || fftScale > 10) {
            mColorChangeFlag = 0;
        }

        if (mColorChangeFlag == 0) {
            if (mColorPoint == 1) {
                mColorPoint = 2;
            } else if (mColorPoint == 2) {
                mColorPoint = 3;
            } else if (mColorPoint == 3) {
                mColorPoint = 1;
            }

            int color;
            if (mColorPoint == 1) {
                color = Color.argb(mAlphaByVolume ? 50 * scale : 0xff, Color.red(mColorFirst), Color.green(mColorFirst), Color.blue(mColorFirst));
            } else if (mColorPoint == 2) {
                color = Color.argb(mAlphaByVolume ? 50 * scale : 0xff, Color.red(mColorSecond), Color.green(mColorSecond), Color.blue(mColorSecond));
            } else {
                color = Color.argb(mAlphaByVolume ? 50 * scale : 0xff, Color.red(mColorThird), Color.green(mColorThird), Color.blue(mColorThird));
            }
            mPaint.setColor(color);
        }
        mColorChangeFlag++;

        if (scale != 0) {
            mPreFFtCurrentFrequency = scale;
        }
    }

    /**
     * 开始绘制
     */
    public void startView() {
        if (mInnerThread != null && mInnerThread.isAlive()) {
            mIsDraw = false;
            while (mInnerThread.isAlive());
            mBackCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        mIsDraw = true;
        mInnerThread = new DrawThread();
        mInnerThread.start();
    }

    /**
     * 停止绘制
     * @param cleanView 清空画不标志位
     */
    public void stopView(boolean cleanView) {
        mIsDraw = false;
        if (mInnerThread != null) {
            while (mInnerThread.isAlive());
        }
        if (cleanView) {
            mRecDataList.clear();
            mBackCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        }
    }

    /**
     * 停止绘制
     */
    public void stopView() { stopView(true); }

    public boolean isPause() {
        return mPause;
    }

    public void setPause(boolean pause) {
        synchronized (mRecDataList) {
            this.mPause = pause;
        }
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setChangeColor(int colorFirst, int colorSecond, int colorThird) {
        this.mColorFirst = colorFirst;
        this.mColorSecond = colorSecond;
        this.mColorThird = colorThird;
    }

    public void setmAlphaByVolume(boolean mAlphaByVolume) {
        this.mAlphaByVolume = mAlphaByVolume;
    }

    public boolean isAlphaByVolume() {
        return mAlphaByVolume;
    }

    public void setBaseRecorder(BaseRecorder baseRecorder) {
        this.mBaseRecorder = baseRecorder;
    }

    /**
     * 将mRecDataList传到Record线程中装载数据
     *
     * @return mRecDataList
     */
    public ArrayList<Short> getRecList() {
        return mRecDataList;
    }

    public void setOffset(int offset) {
        this.mOffset = offset;
    }

    public int getWaveColor() { return mWaveColor; }

    /**
     * 设置波形颜色
     * @param waveColor 颜色值
     */
    public void setWaveColor (int waveColor) {
        this.mWaveColor = waveColor;
        if (mPaint != null) {
            mPaint.setColor(mWaveColor);
        }
    }

    public void setPaint(Paint paint) {
        if (paint != null) {
            this.mPaint = paint;
        }
    }

    /**
     * 设置波形数量
     *
     * @param waveCount 波形数量 1或2
     */
    public void setWaveCount(int waveCount) {
        mWaveCount = waveCount;
        if (mWaveCount < 1) {
            mWaveCount = 1;
        } else if (mWaveCount > 2) {
            mWaveCount = 2;
        }
    }

    /**
     * 是否画出基线
     *
     */
    public void setDrawBase(boolean drawBase) {
        this.mDrawBase = drawBase;
    }

    /**
     * 绘制相反方向
     *
     */
    public void setDrawReverse(boolean drawReverse) {
        this.mDrawReverse = drawReverse;
    }

    /**
     * 数据相反方向
     *
     */
    public void setDataReverse(boolean dataReverse) {
        this.mDataReverse = dataReverse;
    }

    private int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }
}
