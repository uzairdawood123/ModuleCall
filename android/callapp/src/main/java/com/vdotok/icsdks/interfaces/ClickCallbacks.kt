package com.vdotok.callingappdemo.interfaces

import com.vdotok.icsdks.network.models.responseModels.UserModel


/**
 * Created By: VdoTok
 * Date & Time: On 2/22/21 At 12:10 PM in 2021
 */
interface ClickCallbacks {

    fun userItemClick(user: UserModel, isCall: Boolean)
    fun sessionItemClick(sessionID: String)

}