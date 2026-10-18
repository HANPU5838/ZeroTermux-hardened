package com.termux.zerocore.dialog.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.bean.ClipboardBean
import com.termux.zerocore.dialog.view_holder.CommonCommandsHolder
import com.termux.zerocore.utils.FileIOUtils

class CommonCommandsAdapter : RecyclerView.Adapter<CommonCommandsHolder> {
    private var mNoDataListener:NoDataListener? = null
    private var mClickDataListener:ClickDataListener? = null
    private var mArrayList:ArrayList<ClipboardBean.Clipboard> = ArrayList()
    constructor(mArrayList:ArrayList<ClipboardBean.Clipboard>) : super() {
       this.mArrayList = mArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommonCommandsHolder {
        return CommonCommandsHolder(UUtils.getViewLayViewGroup(R.layout.item_commend, parent))
    }

    override fun onBindViewHolder(holder: CommonCommandsHolder, position: Int) {
        val item = mArrayList[position]
        // Clipboard 只有 name(命令文本) 与 index；clip_msg 对应的 *单独消息* 字段并不存在，
        // 原代码把同一个 name 绑定到三行属于复制粘贴错误，这里只绑定有真实数据的两行。
        holder.clip_name?.text = UUtils.getString(R.string.clipboard_title) + item.name
        holder.clip_commend?.text = item.name
        if (position == mArrayList.size -1) {
            holder.line_tv?.let {
                it.visibility = View.GONE
            }
        } else {
            holder.line_tv?.let {
                it.visibility = View.VISIBLE
            }
        }
        holder.delete?.let {
            it.setOnClickListener {
                FileIOUtils.deleteClipBoardString(mArrayList[position])
                if (mArrayList.size > 1) {
                    mArrayList.removeAt(position)
                } else {
                    mArrayList.clear()
                    mNoDataListener?.noData()
                }
                notifyDataSetChanged()
            }
        }

        holder.itemView.setOnClickListener {
            mClickDataListener?.data(item.name)
        }
    }

    override fun getItemCount(): Int {
        return mArrayList.size
    }

    public fun setNoDataListener(mNoDataListener: NoDataListener?) {
        this.mNoDataListener = mNoDataListener
    }

    public fun setClickDataListener(mClickDataListener:ClickDataListener){
        this.mClickDataListener = mClickDataListener
    }

    public interface NoDataListener {
        fun noData()
    }

    public interface ClickDataListener {
        fun data(data: String)
    }
}
