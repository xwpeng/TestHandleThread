package cn.lunkr.example.android.testimageloader;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter {

    private static final String TAG = ImageAdapter.class.getSimpleName();
    private List<String> mData;
    private Context mContext;
    public ImageAdapter(Context context, List<String> data) {
        mContext = context;
        mData = data;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       return new ItemViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_image, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageLoader.getInstance().loadImage(mData.get(position), ((ItemViewHolder)holder).mImageView);
    }


    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;
        public ItemViewHolder(final View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.item_imageview);
            DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
            ViewGroup.LayoutParams p = mImageView.getLayoutParams();
            p.height = dm.widthPixels / 3;
            p.width = dm.widthPixels / 3;
            mImageView.setLayoutParams(p);
        }
    }
}
