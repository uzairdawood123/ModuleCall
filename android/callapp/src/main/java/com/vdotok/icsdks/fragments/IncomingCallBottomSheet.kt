package com.vdotok.icsdks.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vdotok.icsdks.R
import com.vdotok.icsdks.utils.ViewUtils.performSingleClick


/**
 * Created By: VdoTok
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 */
class IncomingCallBottomSheet(
    private val fromPeer: String,
    private val acceptCall : () -> Unit,
    private val declineCall : () -> Unit) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val view: View = inflater.inflate(R.layout.incoming_bottom_sheet_dialog, container, false)

        val username : TextView = view.findViewById(R.id.username)
        username.text = fromPeer

        val acceptButton: ImageView = view.findViewById(R.id.acceptCall)
        acceptButton.performSingleClick {
            acceptCall.invoke()
            dismiss()
        }

        val declineButton: ImageView = view.findViewById(R.id.declineCall)
        declineButton.performSingleClick {
            declineCall.invoke()
            dismiss()
        }

        return view
    }

    companion object {
        const val TAG = "IncomingCallBottomSheet"
    }
}