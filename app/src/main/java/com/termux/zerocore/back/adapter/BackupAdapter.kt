package com.termux.zerocore.back.adapter

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.back.listener.BackupClickListener
import com.termux.zerocore.bean.ItemMenuBean
import com.termux.zerocore.dialog.view_holder.ItemMenuViewHolder
import com.termux.zerocore.zero.engine.ZeroCoreManage

class BackupAdapter : RecyclerView.Adapter<ItemMenuViewHolder>{
    // Never null: a null constructor argument is normalised to an empty list, so the adapter can
    // always report a size and safely resolve an item. The old version stored null and used !!
    // in four places, crashing with a NullPointerException when the list was empty.
    private var mList: ArrayList<ItemMenuBean.Data> = ArrayList()
    private var mContext: Context? = null
    private var mBackupClickListener: BackupClickListener? = null
    constructor(mList:ArrayList<ItemMenuBean.Data>?, mContext: Context, mBackupClickListener: BackupClickListener?) : super() {
        this.mList = mList ?: ArrayList()
        this.mContext = mContext
        this.mBackupClickListener = mBackupClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemMenuViewHolder {
        return ItemMenuViewHolder(UUtils.getViewLayViewGroup(R.layout.dialog_item_menu, parent))
    }

    override fun onBindViewHolder(holder: ItemMenuViewHolder, position: Int) {
        val item = mList.getOrNull(position) ?: return
        holder.menu_image?.setImageResource(item.id)
        holder.menu_name?.text = item.title
        if (TextUtils.isEmpty(ZeroCoreManage.getVersionName()) && item.isEg) {
            holder.eg_install_tv?.visibility = View.VISIBLE
        } else {
            holder.eg_install_tv?.visibility = View.INVISIBLE
        }
        holder.itemView.setOnClickListener {
            mBackupClickListener?.backupClick(it, item.key)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}
