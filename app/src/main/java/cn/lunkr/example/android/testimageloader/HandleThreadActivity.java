package cn.lunkr.example.android.testimageloader;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.widget.TextView;

/**
 * Created by xwpeng on 16-7-21.
 */
public class HandleThreadActivity extends AppCompatActivity {
    private TextView mServiceInfoTextView;
    private HandlerThread mCheckMsgThread;
    private Handler mCheckMsgHandler;
    private boolean isUpdateInfo;

    //与UI线程管理的handler
    private Handler mHandler = new Handler();



    private static final int MSG_UPDATE_INFO = 0x110;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handlethread);
        initView();
        //创建后台线程
        initBackThread();
    }

    private void initView() {
        mServiceInfoTextView = (TextView)findViewById(R.id.handle_thread_textview);
    }

    private void initBackThread()
    {
        mCheckMsgThread = new HandlerThread("check-message-coming");
        mCheckMsgThread.start();
        mCheckMsgHandler = new Handler(mCheckMsgThread.getLooper())
        {
            @Override
            public void handleMessage(Message msg)
            {
                checkForUpdate();
                if (isUpdateInfo)
                {
                    mCheckMsgHandler.sendEmptyMessageDelayed(MSG_UPDATE_INFO, 1000);
                }
            }
        };
    }

    /**
     * 模拟从服务器解析数据
     */
    private void checkForUpdate()
    {
        try
        {
            //模拟耗时
            Thread.sleep(1000);
            //UI Thread update
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    String result = "实时更新中，当前大盘指数：<font color='red'>%d</font>";
                    result = String.format(result, (int) (Math.random() * 3000 + 1000));
                    mServiceInfoTextView.setText(Html.fromHtml(result));
                }
            });

        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        //开始查询
        isUpdateInfo = true;
        mCheckMsgHandler.sendEmptyMessage(MSG_UPDATE_INFO);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //停止查询
        isUpdateInfo = false;
        mCheckMsgHandler.removeMessages(MSG_UPDATE_INFO);

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //释放资源
        mCheckMsgThread.quit();
    }

}
