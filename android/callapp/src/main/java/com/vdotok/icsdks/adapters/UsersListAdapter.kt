package com.vdotok.icsdks.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.databinding.DataBindingUtil

import androidx.recyclerview.widget.RecyclerView
import com.vdotok.callingappdemo.interfaces.ClickCallbacks
import com.vdotok.icsdks.R
import com.vdotok.icsdks.databinding.ItemUserContactBinding
import com.vdotok.icsdks.network.models.responseModels.UserModel

class UsersListAdapter(private val context: Context, private var userModelList: List<UserModel>) :
    RecyclerView.Adapter<UsersListAdapter.ItemViewHolder>(), Filterable {

    var items: ArrayList<UserModel> = ArrayList()
    var filteredItems: ArrayList<UserModel> = ArrayList()
    var clickCallbacksInterface: ClickCallbacks? = null

    init {
        items.addAll(userModelList)
        filteredItems.addAll(userModelList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = DataBindingUtil.inflate<ItemUserContactBinding>(
            LayoutInflater.from(context),
            R.layout.item_user_contact,
            parent, false
        )
        return ItemViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val model = filteredItems[position]
        holder.binding?.userModel = model

        when {
            model.isSelected -> holder.binding?.selectBtn?.setImageResource(R.drawable.round_shape)
            else -> holder.binding?.selectBtn?.setImageResource(R.drawable.round_shape_unselected)
        }
    }

    override fun getItemCount(): Int {
        return filteredItems.size
    }

    fun getItem(position: Int): UserModel {
        return filteredItems[position]
    }

    fun setCallBackInterface(clickCallbacks: ClickCallbacks) {
        clickCallbacksInterface = clickCallbacks
    }

    fun updateData(userModelList: List<UserModel>) {
        items.clear()
        items.addAll(userModelList)
        filteredItems.clear()
        filteredItems.addAll(userModelList)
        notifyDataSetChanged()
    }

    fun getListItems(): ArrayList<UserModel> {
        return filteredItems
    }

    fun getSelectedUsersList(): List<UserModel> {
        val list: ArrayList<UserModel> = ArrayList()
        filteredItems.forEach {
            when {
                it.isSelected -> list.add(it)
            }
        }
        return list
    }

    inner class ItemViewHolder internal constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        internal var binding: ItemUserContactBinding? = null

        init {
            binding = DataBindingUtil.bind(itemView)



            binding?.root?.setOnClickListener {
                val userModel = filteredItems[adapterPosition]
                userModel.isSelected = userModel.isSelected.not()
                clickCallbacksInterface?.userItemClick(userModel, true)
                notifyItemChanged(adapterPosition)
            }

//            binding?.videoCall?.setOnClickListener {
//                val clickedPosition = adapterPosition
//                if (clickCallbacksInterface != null && clickedPosition != RecyclerView.NO_POSITION) {
//                    clickCallbacksInterface?.userItemClick(filteredItems[clickedPosition], true)
//                }
//            }
//
//            binding?.screenShare?.setOnClickListener {
//                val clickedPosition = adapterPosition
//                if (clickCallbacksInterface != null && clickedPosition != RecyclerView.NO_POSITION) {
//                    clickCallbacksInterface?.userItemClick(filteredItems[clickedPosition], false)
//                }
//            }
        }
    }

    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filteredItems = items
                } else {
                    val filteredList: ArrayList<UserModel> = ArrayList()
                    for (row in items) {
                        if (row.fullName?.toLowerCase()
                                ?.contains(charString.toLowerCase()) == true
                        ) {
                            filteredList.add(row)
                        }
                    }
                    filteredItems = filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = filteredItems
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                filteredItems = filterResults.values as ArrayList<UserModel>
                notifyDataSetChanged()
            }
        }
    }

}