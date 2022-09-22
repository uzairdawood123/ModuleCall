package com.vdotok.icsdks.network

import com.vdotok.callingappdemo.models.requestModels.UserSignUp
import com.vdotok.icsdks.network.models.requestModels.UserSignInRequest
import com.vdotok.icsdks.network.models.responseModels.GetAllUsersResponseModel
import com.vdotok.icsdks.network.models.responseModels.User
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST


/**
 * Created By: VdoTok
 * Date & Time: On 2/16/21 At 12:13 PM in 2021
 */
interface ApiGateway {

    @POST("API/v0/signup")
    fun signUp(@Body model: UserSignUp): Single<User>

    @POST("API/v0/Login")
    fun userSignIn(@Body model: UserSignInRequest): Single<User>

    @POST("API/v0/AllUsers")
    fun getAllUsers(@Header("Authorization") auth_token: String): Single<GetAllUsersResponseModel>

}