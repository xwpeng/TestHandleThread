package cn.lunkr.example.android.testimageloader;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by xwpeng on 16-7-20.
 */
public class ImageLoader {
    private Thread mPoolThread;
    private Handler mPoolThreadHander;
    private volatile Semaphore mSemaphore = new Semaphore(1);
    private volatile Semaphore mPoolSemaphore;
    private ExecutorService mThreadPool;
    private LruCache<String, Bitmap> mLruCache;
    private LinkedList<Runnable> mTasks;
    private Type mType;
    /**
     * 运行在UI线程的handler，用于给ImageView设置图片
     */
    private Handler mHandler;

    /**
     * 队列的调度方式
     */
    public enum Type {
        FIFO, LIFO
    }

    private static ImageLoader mInstance;

    public static ImageLoader getInstance() {
        synchronized (ImageLoader.class) {
            if (mInstance == null) {
                mInstance = new ImageLoader(3, Type.LIFO);
            }
        }
        return mInstance;
    }


    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        // loop thread
        mPoolThread = new Thread() {
            @Override
            public void run() {
                try {
                    // 请求一个信号量
                    mSemaphore.acquire();
                } catch (InterruptedException e) {
                }
                Looper.prepare();
                mPoolThreadHander = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        mThreadPool.execute(getTask());
                        try {
                            mPoolSemaphore.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                // 释放一个信号量
                mSemaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        // 获取应用程序最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mPoolSemaphore = new Semaphore(threadCount);
        mTasks = new LinkedList<Runnable>();
        mType = type == null ? Type.LIFO : type;
    }

    public Runnable getTask() {
        if (mType == Type.FIFO) {
           return mTasks.removeFirst();
        } else if (mType == Type.LIFO) {
           return mTasks.removeLast();
        }
        return null;
    }

    private synchronized void addTask(Runnable runnable)
    {
        try
        {
            // 请求信号量，防止mPoolThreadHander为null
            if (mPoolThreadHander == null)
                mSemaphore.acquire();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        mTasks.add(runnable);
        mPoolThreadHander.sendEmptyMessage(0x110);
    }

    /**
     * 从LruCache中获取一张图片，如果不存在就返回null。
     */
    private Bitmap getBitmapFromLruCache(String key)
    {
        return mLruCache.get(key);
    }

    /**
     * 往LruCache中添加一张图片
     *
     * @param key
     * @param bitmap
     */
    private void addBitmapToLruCache(String key, Bitmap bitmap)
    {
        if (getBitmapFromLruCache(key) == null)
        {
            if (bitmap != null)
                mLruCache.put(key, bitmap);
        }
    }



    /**
     * 加载图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView)
    {
        // set tag
        imageView.setTag(path);
        // UI线程
        if (mHandler == null)
        {
            mHandler = new Handler()
            {
                @Override
                public void handleMessage(Message msg)
                {
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    ImageView imageView = holder.imageView;
                    Bitmap bm = holder.bitmap;
//                    String path = holder.path;
                   /* if (imageView.getTag().toString().equals(path))
                    {*/
                        imageView.setImageBitmap(bm);
//                    }
                }
            };
        }

        Bitmap bm = getBitmapFromLruCache(path);
        if (bm != null)
        {
            ImgBeanHolder holder = new ImgBeanHolder();
            holder.bitmap = bm;
            holder.imageView = imageView;
            holder.path = path;
            Message message = Message.obtain();
            message.obj = holder;
            mHandler.sendMessage(message);
        } else
        {
            addTask(new Runnable()
            {
                @Override
                public void run()
                {

                    ImageSize imageSize = ImageUtil.getImageViewWidth(imageView);
                    int reqWidth = imageSize.width;
                    int reqHeight = imageSize.height;
                    Bitmap bm = ImageUtil.decodeSampledBitmapFromResource(path, reqWidth,
                            reqHeight);
                    addBitmapToLruCache(path, bm);
                    ImgBeanHolder holder = new ImgBeanHolder();
                    holder.bitmap = getBitmapFromLruCache(path);
                    holder.imageView = imageView;
                    holder.path = path;
                    Message message = Message.obtain();
                    message.obj = holder;
                    // Log.e("TAG", "mHandler.sendMessage(message);");
                    mHandler.sendMessage(message);
                    mPoolSemaphore.release();
                }
            });
        }

    }

    private class ImgBeanHolder
    {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

}
