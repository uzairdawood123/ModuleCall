package com.vdotok.icsdks.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.vdotok.callingappdemo.interfaces.ClickCallbacks
import com.vdotok.icsdks.adapters.SessionListAdapter
import com.vdotok.icsdks.databinding.SessionListingDialogBinding
import com.vdotok.icsdks.network.models.responseModels.SessionModel
import com.vdotok.icsdks.network.models.responseModels.UserModel

class SessionListDialog(private val sessionItem : (sessionId: String) -> Unit,
        private var sessionMap: HashMap<String, SessionModel>) : DialogFragment(),
    ClickCallbacks{

    private lateinit var binding: SessionListingDialogBinding
    lateinit var usersListAdapter: SessionListAdapter
    var sessionIdsList = ArrayList<String>()

    init {
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }

        binding = SessionListingDialogBinding.inflate(inflater, container, false)

        initRecyclerView()
        return binding.root
    }

    private fun initRecyclerView() {
        activity?.applicationContext?.let {
            val sessionListing = ArrayList(sessionMap.values)
            sessionListing.forEach { value ->
                sessionIdsList.add(value.sessionId)
            }
            usersListAdapter = SessionListAdapter(it, sessionIdsList)
            binding.rcvUserList.adapter = usersListAdapter
            usersListAdapter.setCallBackInterface(this)
        }
    }

    override fun userItemClick(user: UserModel, isCall: Boolean) {
        TODO("Not yet implemented")
    }

    override fun sessionItemClick(sessionID: String) {
        if (sessionID.isNotEmpty())
            sessionItem.invoke(sessionID)
        dismiss()
    }

    companion object{
        const val TAG = "SESSION_LIST_DIALOG"
    }

}
