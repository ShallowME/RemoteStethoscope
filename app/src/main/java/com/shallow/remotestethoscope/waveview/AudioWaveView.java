package com.shallow.remotestethoscope.waveview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import com.shallow.remotestethoscope.R;
import com.shallow.remotestethoscope.base.BaseRecorder;
import com.shallow.remotestethoscope.base.ConstantUtil;

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

    private int drawType;

    private Context mContext;

    private Bitmap mBitmap, mBackgroundBitmap;

    private Paint mPaint;

    private Paint mViewPaint;

    private Canvas mCanvas = new Canvas();

    private Canvas mBackCanvas = new Canvas();

    private final ArrayList<Integer> mRecDataList = new ArrayList<>();

    private BaseRecorder mBaseRecorder;

    private int mWidthSpecSize;

    private int mHeightSpecSize;

    private int mScale = 1;

    private int mBaseLine;

    private int mOffset = -11;

    private boolean mAlphaByVolume;

    private boolean mIsDraw = true;

    private boolean mDrawBase = true;

    private boolean mPause = false;

    private int mWaveCount;

    private int mWaveColor = Color.parseColor("#E55D61");

    private DrawThread mInnerThread;

    private int mDrawStartOffset = 0;

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


    public void init(Context context, AttributeSet attrs) {
        mContext = context;
        if (isInEditMode()) {
            return;
        }
        if (attrs != null) {
            @SuppressLint("CustomViewStyleable") TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.waveView);
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
                ArrayList<Integer> dataList = new ArrayList<>();
                synchronized (mRecDataList) {
                    if (mRecDataList.size() != 0) {
                        try {
                            dataList = (ArrayList<Integer>) deepClone(mRecDataList);
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

                    if (mBackCanvas != null) {
                        mBackCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                        int drawBufSize = dataList.size();

                        int startPosition = mDrawStartOffset;
                        int jOffset = mOffset;

                        mBackCanvas.drawLine(startPosition, mBaseLine, mWidthSpecSize, mBaseLine, mPaint);

                        if (drawType == ConstantUtil.DRAW_TONE) {
                            for (int i = 0, j = startPosition; i < drawBufSize; i++, j += jOffset) {
                                Integer sh = dataList.get(i);
                                drawTone(sh, j);
                            }
                        } else if (drawType == ConstantUtil.DRAW_EMG) {
                            for (int i = 0, j = startPosition; i < drawBufSize; i++, j += jOffset) {
                                Integer sh = dataList.get(i);
                                drawEmg(sh, j);
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
    private void resolveToWaveData(ArrayList<Integer> list) {
        int allMax = 0;
        for (int i = 0; i < list.size(); i++) {
            Integer sh = Math.abs((int)list.get(i));
            if (sh > allMax) {
                allMax = sh;
            }
        }
        int curScale = allMax / mBaseLine;
        if (curScale > mScale) {
            mScale = (curScale == 0) ? 1 : curScale;
        }
    }

    private void drawTone(Integer sh, int j) {
        if (sh != null) {
            int max = (mBaseLine - sh / mScale);
            int min;
            if (mWaveCount == 2) {
                min = sh / mScale + mBaseLine;
            } else {
                min = mBaseLine;
            }
            mBackCanvas.drawLine(j, mBaseLine, j, max, mPaint);
            mBackCanvas.drawLine(j, min, j, mBaseLine, mPaint);
        }
    }

    private void drawEmg(Integer sh, int j) {
        if (sh != null) {
            if (sh >= 0) {
                int max = mBaseLine - sh / mScale;
                mBackCanvas.drawLine(j, mBaseLine, j, max, mPaint);
            } else {
                int min = Math.abs(sh) / mScale + mBaseLine;
                mBackCanvas.drawLine(j, min, j, mBaseLine, mPaint);
            }
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


    /**
     * 开始绘制
     */
    public void startView(int type) {
        if (mInnerThread != null && mInnerThread.isAlive()) {
            mIsDraw = false;
            while (mInnerThread.isAlive());
            mBackCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        drawType = type;
        mIsDraw = true;
        mInnerThread = new DrawThread();
        mInnerThread.start();
    }

    /**
     * 停止绘制
     * @param cleanView 清空画布标志位
     */
    public void stopView(boolean cleanView) {
        mIsDraw = false;
        if (mInnerThread != null) {
            while (mInnerThread.isAlive());
        }
        if (cleanView) {
            mRecDataList.clear();
//            mBackCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//            Paint paint = new Paint();
//            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//            mBackCanvas.drawPaint(paint);
//            mCanvas.drawPaint(paint);

            invalidate();
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

//    public void setChangeColor(int colorFirst, int colorSecond, int colorThird) {
//        this.mColorFirst = colorFirst;
//        this.mColorSecond = colorSecond;
//        this.mColorThird = colorThird;
//    }

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
    public ArrayList<Integer> getRecList() {
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

//    /**
//     * 绘制相反方向
//     *
//     */
//    public void setDrawReverse(boolean drawReverse) {
//        this.mDrawReverse = drawReverse;
//    }
//
//    /**
//     * 数据相反方向
//     *
//     */
//    public void setDataReverse(boolean dataReverse) {
//        this.mDataReverse = dataReverse;
//    }

    private int dip2px(Context context, float dipValue) {
        float fontScale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * fontScale + 0.5f);
    }
}
