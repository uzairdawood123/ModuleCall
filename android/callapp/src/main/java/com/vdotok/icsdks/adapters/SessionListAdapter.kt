package com.vdotok.icsdks.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil

import androidx.recyclerview.widget.RecyclerView
import com.vdotok.callingappdemo.interfaces.ClickCallbacks
import com.vdotok.icsdks.R
import com.vdotok.icsdks.databinding.LayoutSessionItemBinding

class SessionListAdapter(private val context: Context, private var userModelList: List<String>) :
    RecyclerView.Adapter<SessionListAdapter.ItemViewHolder>() {

    var items: ArrayList<String> = ArrayList()
    var clickCallbacksInterface: ClickCallbacks? = null

    init {
        items.addAll(userModelList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = DataBindingUtil.inflate<LayoutSessionItemBinding>(
            LayoutInflater.from(context),
            R.layout.layout_session_item,
            parent, false
        )
        return ItemViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = items[position]
        holder.binding?.sessionID?.text = model
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItem(position: Int): String {
        return items[position]
    }

    fun setCallBackInterface(clickCallbacks: ClickCallbacks) {
        clickCallbacksInterface = clickCallbacks
    }

    fun updateData(userModelList: List<String>) {
        items.clear()
        items.addAll(userModelList)
        notifyDataSetChanged()
    }

    inner class ItemViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        internal var binding: LayoutSessionItemBinding? = null

        init {
            binding = DataBindingUtil.bind(itemView)



            binding?.root?.setOnClickListener {
                val sessionItem = items[adapterPosition]
                clickCallbacksInterface?.sessionItemClick(sessionItem)
                notifyItemChanged(adapterPosition)
            }

        }
    }


}