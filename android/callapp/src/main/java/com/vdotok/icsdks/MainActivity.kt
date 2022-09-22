package com.vdotok.icsdks

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.norgic.icsdks.utils.NetworkStatusLiveData
import com.vdotok.callingappdemo.interfaces.ClickCallbacks
import com.vdotok.callingappdemo.prefs.Prefs
import com.vdotok.icsdks.adapters.UsersListAdapter
import com.vdotok.icsdks.databinding.ActivityMainBinding
import com.vdotok.icsdks.dialog.SessionListDialog
import com.vdotok.icsdks.fragments.IncomingCallBottomSheet
import com.vdotok.icsdks.models.CallNameModel
import com.vdotok.icsdks.network.ApiClient
import com.vdotok.icsdks.network.ApiGateway
import com.vdotok.icsdks.network.models.requestModels.UserSignInRequest
import com.vdotok.icsdks.network.models.responseModels.*
import com.vdotok.icsdks.utils.PROJECT_ID
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.commands.RegisterResponse
import com.vdotok.streaming.enums.*
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.*
import com.vdotok.streaming.utils.checkInternetAvailable
import com.vdotok.streaming.views.CallViewRenderer
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import org.webrtc.*


class MainActivity : AppCompatActivity(), ClickCallbacks, CallSDKListener {
    private lateinit var prefs: Prefs
    private lateinit var callClient: CallClient

    lateinit var usersListAdapter: UsersListAdapter
    private lateinit var rcvUserList: RecyclerView

    private var remoteView: CallViewRenderer? = null
    private lateinit var ownView: CallViewRenderer

    private val MY_PERMISSIONS_REQUEST_CAMERA = 100
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
    private val MY_PERMISSIONS_REQUEST = 102
    private var mediaUrl: String = ""
    private var mediaEndPoint: String = ""
    var sessionCount = 0
    var isVideoCall: Boolean = false
    private var mainSessionMap: HashMap<String, SessionModel> = HashMap()
    private lateinit var serviceIntent: Intent

    private var incomingCallBottomSheet: IncomingCallBottomSheet? = null

    private lateinit var binding: ActivityMainBinding

    private var isVideoPaused = false
    private var isPublicScreenShare = false
    private var isPublicMultiSession = false
    private var isGroupMultiSession = false
    private var isInternetConnectionRestored = false
    private var reConnectStatus = false
    private lateinit var mLiveDataNetwork: NetworkStatusLiveData
    private var isAppAudioEnable = false
//    private lateinit var remoteProxyRenderer: ProxyVideoSink
//    private lateinit var localProxyVideoSink: ProxyVideoSink

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        askForPermissions()
//        remoteProxyRenderer = ProxyVideoSink()
//        localProxyVideoSink = ProxyVideoSink()

        prefs = Prefs(this)
        rcvUserList = findViewById(R.id.rcvUserList)
//        remoteView = findViewById(R.id.remoteView)
//        ownView = findViewById(R.id.ownView)

        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this)
        }

        initRecyclerView()
        addInternetConnectionObserver()
        login(null)
        setListeners()

    }

    private fun addInternetConnectionObserver() {
        mLiveDataNetwork = NetworkStatusLiveData(this.application)

        mLiveDataNetwork.observe(this) { isInternetConnected ->
            when {
                isInternetConnected == true && isInternetConnectionRestored -> {
                    Log.e("Internet", "internet connection restored!")
                    performSocketReconnection()
                }
                isInternetConnected == false -> {
                    isInternetConnectionRestored = true
                    reConnectStatus = true
                    Log.e("Internet", "internet connection lost!")
                }
                else -> {
                }
            }
        }
    }

    private fun performSocketReconnection() {
        prefs.userRegisterInfo?.let {
            if (checkInternetAvailable(this)) {
                binding.fullName.text = it.full_name
                mediaUrl = getMediaServerAddress(it.mediaServer)
                mediaEndPoint = it.mediaServer.end_point
                callClient.connect(mediaUrl, mediaEndPoint)
            } else {
                Snackbar.make(
                    binding.root,
                    "Socket Connection failed! No Internet!",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } ?: kotlin.run {
            Snackbar.make(
                binding.root,
                "No user data found please re-login",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }


    var mService: ProjectionService? = null
    var mBound = false

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ProjectionService.LocalBinder = service as ProjectionService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    @TargetApi(21)
    private fun startScreenCapture() {

        serviceIntent = Intent(this, ProjectionService::class.java)
        this.bindService(serviceIntent, mConnection, BIND_AUTO_CREATE)

        val mediaProjectionManager =
            application.getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            callClient.CAPTURE_PERMISSION_REQUEST_CODE
        )
    }

    private fun setListeners() {

//        one to one calling clicks
        binding.audioCall.setOnClickListener { dialOneToOneCall(MediaType.AUDIO) }
        binding.videoCall.setOnClickListener { dialOneToOneCall(MediaType.VIDEO) }
        binding.endOneToOneCall.setOnClickListener { openSessionDialog() }

//        many to many calling clicks
        binding.m2mAudioCall.setOnClickListener { dialM2MCall(MediaType.AUDIO) }
        binding.m2mVideoCall.setOnClickListener { dialM2MCall(MediaType.VIDEO) }
        binding.endManyToManyCall.setOnClickListener { openSessionDialog() }

//        one to many group broadcast
        binding.o2mGroupVideoCall.setOnClickListener { dialOneToManyCall(MediaType.VIDEO, false) }
        binding.endOneToManyGroupCall.setOnClickListener { openSessionDialog() }
        binding.o2mGroupScreenShare.setOnClickListener { startScreenCapture() }
        binding.endGroupScreenShareSession.setOnClickListener { endScreenShareSession() }
        binding.appAduio.isChecked = isAppAudioEnable
        binding.appAduio.setOnCheckedChangeListener { buttonView, isChecked ->
            isAppAudioEnable = isChecked
        }

//        one to many public
        binding.o2mPublicVideoCall.setOnClickListener { dialOneToManyCall(MediaType.VIDEO, true) }
        binding.endOneToManyPublicCall.setOnClickListener { openSessionDialog() }
        binding.o2mPublicScreenShare.setOnClickListener {
            isPublicScreenShare = true
            startScreenCapture()
        }
        binding.endPublicScreenShareSession.setOnClickListener { endScreenShareSession() }

//        multi session public
        binding.imgMultiSession.setOnClickListener {
            isPublicMultiSession = true
            startScreenCapture()
        }

//        multi session group
        binding.imgGroupMultiSession.setOnClickListener {
            isGroupMultiSession = true
            startScreenCapture()
        }

        binding.screenShare.setOnClickListener { startScreenCapture() }
        binding.endCall.setOnClickListener { openSessionDialog() }

        binding.btnMic.setOnClickListener {
            getActiveSession()?.let {
                callClient.muteUnMuteMic(
                    sessionKey = it,
                    refId = prefs.userRegisterInfo?.ref_id.toString()
                )
                if (ownView.isMuteIconShown()) ownView.showHideMuteIcon(false)
                else ownView.showHideMuteIcon(true)
            }
        }
        binding.btnSpeaker.setOnClickListener {
            if (callClient.isSpeakerEnabled())
                callClient.setSpeakerEnable(false)
            else
                callClient.setSpeakerEnable(true)
        }
        binding.btnPause.setOnClickListener {
            getActiveSession()?.let {
                if (isVideoPaused) {
                    callClient.resumeVideo(
                        sessionKey = it,
                        refId = prefs.userRegisterInfo?.ref_id.toString()
                    )
                    ownView.showHideAvatar(false)
                } else {
                    callClient.pauseVideo(
                        sessionKey = it,
                        refId = prefs.userRegisterInfo?.ref_id.toString()
                    )
                    ownView.showHideAvatar(true)
                }
            }

            isVideoPaused = isVideoPaused.not()
        }
        binding.btnSwitchCam.setOnClickListener {
            getActiveSession()?.let { it1 ->
                callClient.switchCamera(
                    it1
                )
            }
        }
        binding.isMute.setOnClickListener {
            getActiveSession()?.let {
                Toast.makeText(this, callClient.isAudioEnabled(it).toString(), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.refreshLogin.setOnClickListener {
            if (!callClient.isConnected())
                login(null)
            else {
                Snackbar.make(binding.root, "Already Connected!", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun endScreenShareSession() {
        if (mainSessionMap.isNotEmpty())
            SessionListDialog(this::endSSSession, mainSessionMap).show(
                supportFragmentManager,
                SessionListDialog.TAG
            )
    }

    private fun endSSSession(sessionId: String) {
//        val list = ArrayList<String>()
//        mainSessionMap.values.forEach {
//            list.add(it.sessionId)
//        }
        callClient.endCallSession(arrayListOf(sessionId))
        removeMapValue(sessionId)
    }

    //    while ending one2one call we need to provide the receiver's refID to get session
    private fun openSessionDialog() {
        if (mainSessionMap.isNotEmpty())
            SessionListDialog(this::endOneToOneCall, mainSessionMap).show(
                supportFragmentManager,
                SessionListDialog.TAG
            )
    }

    private fun endOneToOneCall(sessionId: String) {
//        val list = ArrayList<String>()
//        mainSessionMap.keys.forEach {
//            list.add(it)
//        }
        callClient.endCallSession(arrayListOf(sessionId))
    }

    private fun removeMapValue(sessionId: String) {
        val mapValue = mainSessionMap.values
        val sessionModel: SessionModel? = mapValue.find { it.sessionId == sessionId }
        sessionModel?.let {
            mainSessionMap.values.remove(it)
        }
    }

    private fun dialOneToManyCall(mediaType: MediaType, isPublicBroadcast: Boolean) {
        prefs.userRegisterInfo?.let {
            val activeSession = callClient.dialOne2ManyCall(
                callParams = CallParams(
                    refId = it.ref_id,
                    toRefIds = getRefIdFromSelectedList(),
                    mediaType = mediaType,
                    callType = CallType.ONE_TO_MANY,
                    sessionType = SessionType.CALL,
                    isAppAudio = false,
                    isRecord = 1,
                    isBroadcast = if (isPublicBroadcast) 1 else 0
                )
            )
            activeSession?.let { sessionKey ->
                mainSessionMap["session $sessionCount"] = SessionModel(
                    sessionKey,
                    getRefIdFromSelectedList(),
                    HashMap(),
                    null,
                    mediaType,
                    CallType.ONE_TO_MANY,
                    SessionType.CALL
                )
                sessionCount++
            }
        }
    }

//    private fun initiatePublicMultiBroadcast(
//        mediaProjection: MediaProjection?,
//        isGroupSession: Boolean
//    ) {
//        prefs.userRegisterInfo?.let {
//            val activeSession = callClient.startMultiSession(
//                callParams = CallParams(
//                    refId = it.ref_id,
//                    toRefIds = getRefIdFromSelectedList(),
//                    isRecord = 1,
//                    callType = CallType.ONE_TO_MANY
//                ),
//                mediaProjection = mediaProjection,
//                isGroupSession = isGroupSession
//            )
//
//            activeSession?.let { keyPair ->
//                mainSessionMap["session $sessionCount"] = SessionModel(
//                    keyPair.first,
//                    getRefIdFromSelectedList(),
//                    HashMap(),
//                    null,
//                    MediaType.VIDEO,
//                    CallType.ONE_TO_MANY,
//                    SessionType.CALL
//                )
//
//                sessionCount++
//
//                mainSessionMap["session $sessionCount"] = SessionModel(
//                    keyPair.second,
//                    getRefIdFromSelectedList(),
//                    HashMap(),
//                    null,
//                    MediaType.VIDEO,
//                    CallType.ONE_TO_MANY,
//                    SessionType.SCREEN
//                )
//
//                sessionCount++
//            }
//        }
//    }

    private fun initiatePublicMultiBroadcast(
        mediaProjection: MediaProjection?,
        isGroupSession: Boolean
    ) {
        prefs.userRegisterInfo?.let {
            callClient.startMultiSessionV2(
                callParams = CallParams(
                    refId = it.ref_id,
                    toRefIds = getRefIdFromSelectedList(),
                    isRecord = 1,
                    callType = CallType.ONE_TO_MANY
                ),
                mediaProjection = mediaProjection,
                isGroupSession = isGroupSession
            )
        }
    }

    override fun multiSessionCreated(sessionIds: Pair<String, String>) {
        sessionIds.let { keyPair ->
            runOnUiThread {
                Toast.makeText(
                    this,
                    "id1: ${keyPair.first}, id2: ${keyPair.second}",
                    Toast.LENGTH_LONG
                ).show()
            }
            mainSessionMap["session $sessionCount"] = SessionModel(
                keyPair.first,
                getRefIdFromSelectedList(),
                HashMap(),
                null,
                MediaType.VIDEO,
                CallType.ONE_TO_MANY,
                SessionType.CALL
            )

            sessionCount++

            mainSessionMap["session $sessionCount"] = SessionModel(
                keyPair.second,
                getRefIdFromSelectedList(),
                HashMap(),
                null,
                MediaType.VIDEO,
                CallType.ONE_TO_MANY,
                SessionType.SCREEN
            )

            sessionCount++
        }
    }


    private fun dialM2MCall(mediaType: MediaType) {
        prefs.userRegisterInfo?.let {
            val activeSession = callClient.dialMany2ManyCall(
                callParams = CallParams(
                    refId = it.ref_id,
                    toRefIds = getRefIdFromSelectedList(),
                    mcToken = it.mcToken,
                    mediaType = mediaType,
                    callType = CallType.MANY_TO_MANY,
                    sessionType = SessionType.CALL,
                    isAppAudio = false,
                    customDataPacket = Gson().toJson(
                        CallNameModel(
                            calleName = "test Data",
                            groupName = "test Data",
                            groupAutoCreatedValue = "1"
                        )
                    ),
                    isRecord = 1,
                )
            )
            activeSession?.let { sessionKey ->
                mainSessionMap["session $sessionCount"] = SessionModel(
                    sessionKey,
                    getRefIdFromSelectedList(),
                    HashMap(),
                    null,
                    mediaType,
                    CallType.MANY_TO_MANY,
                    SessionType.CALL
                )
                sessionCount++
            }
        }
    }

    //    while dialing one2one call we need to save the sessionID based on the receiver's refID
    private fun dialOneToOneCall(mediaType: MediaType) {
        prefs.userRegisterInfo?.let {
            val activeSession = callClient.dialOne2OneCall(
                callParams = CallParams(
                    refId = it.ref_id,
                    toRefIds = getRefIdFromSelectedList(),
                    mediaType = mediaType,
                    callType = CallType.ONE_TO_ONE,
                    sessionType = SessionType.CALL,
                    isAppAudio = false,
                    isRecord = 1
                )
            )
            activeSession?.let { sessionKey ->
                mainSessionMap["session $sessionCount"] = SessionModel(
                    sessionKey,
                    getRefIdFromSelectedList(),
                    HashMap(),
                    null,
                    mediaType,
                    CallType.ONE_TO_ONE,
                    SessionType.CALL
                )
                sessionCount++
            }
        }
    }

    private fun getActiveSession(): String? {
        val key = sessionCount.minus(1)
        return mainSessionMap["session $key"]?.sessionId
    }

    private fun startSession(mediaProjection: MediaProjection?) {
        prefs.userRegisterInfo?.let {
            val activeSession = callClient.startSession(
                callParams = CallParams(
                    refId = it.ref_id,
                    toRefIds = getRefIdFromSelectedList(),
                    mediaType = MediaType.VIDEO,
                    callType = CallType.ONE_TO_MANY,
                    sessionType = SessionType.SCREEN,
                    isRecord = 1,
                    isBroadcast = if (isPublicScreenShare) 1 else 0
                ),
                mediaProjection = mediaProjection
            )
            activeSession?.let { sessionKey ->
                mainSessionMap["session $sessionCount"] = SessionModel(
                    sessionKey,
                    getRefIdFromSelectedList(),
                    HashMap(),
                    null,
                    null,
                    CallType.ONE_TO_MANY,
                    SessionType.SCREEN
                )
                sessionCount++
            }
        }
    }

    private fun getRefIdFromSelectedList(): ArrayList<String> {
        val list = ArrayList<String>()
        for (item in usersListAdapter.getSelectedUsersList()) {
            item.refID?.let { list.add(it) }
        }
        return list
    }

    override fun onDestroy() {
        callClient.disConnectSocket()
        super.onDestroy()
    }

    override fun onPublicURL(publicURL: String) {
        runOnUiThread { Toast.makeText(this, publicURL, Toast.LENGTH_SHORT).show() }
        runOnUiThread { Log.e("publicUrl", publicURL) }
        runOnUiThread { Log.e("session_invite", publicURL) }
        runOnUiThread { binding.publicUrl.text = publicURL }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == callClient.CAPTURE_PERMISSION_REQUEST_CODE) {
                callClient.initSession(data, resultCode, this, isAppAudioEnable)
            }
        }
    }

    private fun askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSIONS_REQUEST_CAMERA
            )
        }
    }


    private fun initRecyclerView() {
        usersListAdapter = UsersListAdapter(this, ArrayList())
        rcvUserList.adapter = usersListAdapter
        usersListAdapter.setCallBackInterface(this)
    }


    override fun userItemClick(user: UserModel, isCall: Boolean) {
    }

    override fun sessionItemClick(sessionID: String) {
        TODO("Not yet implemented")
    }

    private fun getMediaServerAddress(mediaServer: MessagingServerMap): String {
//        return "https://ssig.vdotok.dev:${mediaServer.port}"
        return "https://${mediaServer.host}:${mediaServer.port}"
    }

    fun login(view: View?) {
        if (checkInternetAvailable(this)) {
            val apiService: ApiGateway =
                ApiClient.getClient(this@MainActivity).create(ApiGateway::class.java)
            var userName = "yaShyk@gmail.com"
            var password = "12345678"
            when {
                getDeviceName()!!.contains("Huawei", true) -> {
                    userName = "jameelaaas1@gmail.com"
                    password = "12345678"
                }
                else -> {
                    userName = "jameelaaas12@gmail.com"
                    password = "12345678"
                }
            }
            Log.e("LoggedIn", getDeviceName().toString())

            val call: Single<User> =
                apiService.userSignIn(UserSignInRequest(userName, password, PROJECT_ID))

            call.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<User> {
                    override fun onSubscribe(disposable: Disposable) {}
                    override fun onError(throwable: Throwable) {
                        Log.d("TAG", throwable.message.toString())
                    }

                    override fun onSuccess(response: User) {

                        response.status?.let { statusCode ->
                            when {
                                statusCode == 200 -> {
                                    Log.d("TAG", response.toString())
                                    Toast.makeText(
                                        this@MainActivity,
                                        response.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    prefs.userRegisterInfo = response
                                    mediaUrl = getMediaServerAddress(response.mediaServer)
                                    mediaEndPoint = response.mediaServer.end_point
                                    callClient.connect(mediaUrl, mediaEndPoint)
                                    binding.fullName.text = response.full_name
                                    getAllUsers()
                                }
                                statusCode >= 400 -> Toast.makeText(
                                    this@MainActivity,
                                    response.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                                else -> {
                                }
                            }
                        }
                    }
                })
        } else {
            Snackbar.make(binding.root, "Login failed! No Internet!", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun getAllUsers() {

        prefs.userRegisterInfo?.auth_token?.let {
            val apiService: ApiGateway = ApiClient.getClient(this).create(ApiGateway::class.java)
            val call: Single<GetAllUsersResponseModel> =
                apiService.getAllUsers("Bearer $it")

            call.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(object : SingleObserver<GetAllUsersResponseModel> {
                    override fun onSubscribe(disposable: Disposable) {}
                    override fun onError(throwable: Throwable) {
                        Log.d("TAG", throwable.message.toString())

                    }

                    override fun onSuccess(response: GetAllUsersResponseModel) {
                        Log.d("TAG", response.toString())
                        var userModelList: ArrayList<UserModel> = ArrayList()
                        for (singleuser in response.users) {
                            if (singleuser.email.equals("jameelaaas1@gmail.com", true) ||
                                singleuser.email.equals("jameelaaas12@gmail.com", true) ||
                                singleuser.fullName.equals("iphone 11 pro", true) ||
                                singleuser.email.equals("pixelEmulator@gmail.com", true)
                            ) {
                                userModelList.add(singleuser)
                            }
                        }
                        usersListAdapter.updateData(userModelList)
//                        usersListAdapter.updateData(response.users)
                    }
                })
        } ?: kotlin.run {

        }
    }

    private fun acceptIncomingCall(
        callParams: CallParams
    ) {
        prefs.userRegisterInfo?.let {
            val activeSession = callClient.acceptIncomingCall(
                it.ref_id,
                callParams.apply { isRecord = 1 }
            )
            mainSessionMap["session $sessionCount"] = SessionModel(
                activeSession,
                arrayListOf(callParams.refId),
                HashMap(),
                null,
                callParams.mediaType,
                callParams.callType,
                callParams.sessionType
            )
            sessionCount++
        }
    }

    private fun rejectIncomingCall(refId: String, sessionUUID: String) {
        callClient.rejectIncomingCall(refId, sessionUUID)
    }

    override fun permissionError(permissionErrorList: ArrayList<PermissionType>) {
        Log.e("Permission Error", "permissionError: $permissionErrorList")
    }

    override fun connectionStatus(enumConnectionStatus: EnumConnectionStatus) {
        updateMessage(enumConnectionStatus.toString())
        when (enumConnectionStatus) {
            EnumConnectionStatus.CONNECTED -> {
                runOnUiThread {

                    Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show()
                    registerUser()
                }
            }
            EnumConnectionStatus.NOT_CONNECTED -> {
                runOnUiThread {

                    Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.ERROR -> {
                runOnUiThread {

                    Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {

            }
        }
    }

    private fun registerUser() {
        callClient.register(
            authToken = prefs.userRegisterInfo?.authorization_token!!,
            refId = prefs.userRegisterInfo?.ref_id!!,
            reconnectStatus = if (reConnectStatus) 1 else 0
        )
        reConnectStatus = false
    }

    override fun onClose(reason: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onError(cause: String) {
        Log.e("Socket error: ", cause)
    }

    override fun onResume() {
        super.onResume()
        Log.e("onResumeStatus: ", callClient.isConnected().toString())
        Log.e("onResumeSession: ", callClient.recentSession().toString())
    }

    override fun incomingCall(
        callParams: CallParams
    ) {
//        callParams.mcToken = prefs.userRegisterInfo?.mcToken.toString()

        runOnUiThread {
            incomingCallBottomSheet = IncomingCallBottomSheet(
                callParams.refId,
                {
                    acceptIncomingCall(
                        callParams = callParams
                    )
                },
                {
                    rejectIncomingCall(
                        prefs.userRegisterInfo?.ref_id!!,
                        callParams.sessionUUID
                    )
                }
            )
            incomingCallBottomSheet?.isCancelable = false
            incomingCallBottomSheet?.show(supportFragmentManager, IncomingCallBottomSheet.TAG)
        }
    }

    override fun registrationStatus(registerResponse: RegisterResponse) {
        updateMessage(registerResponse.registrationStatus.value)
        when (registerResponse.registrationStatus) {
            RegistrationStatus.REGISTER_SUCCESS -> {
                Log.e("register", "message: ${registerResponse.responseMessage}")
                val userModel: User? = prefs.userRegisterInfo
                userModel?.mcToken = registerResponse.mcToken.toString()
                userModel?.let { prefs.userRegisterInfo = it }
                if (registerResponse.reConnectStatus == 1) {
                    callClient.initiateReInviteProcess()
                }
            }

            RegistrationStatus.UN_REGISTER,
            RegistrationStatus.REGISTER_FAILURE,
            RegistrationStatus.INVALID_REGISTRATION -> {
                Handler(Looper.getMainLooper()).post {
                    Log.e("register", "message: ${registerResponse.responseMessage}")
                }
            }
        }
    }

    private fun updateMessage(message: String) {
        runOnUiThread {
            binding.titleString.setText(message)
        }
    }

    override fun onSessionReady(
        mediaProjection: MediaProjection?
    ) {
        runOnUiThread {
            when {
                isPublicMultiSession -> {
                    initiatePublicMultiBroadcast(mediaProjection, false)
                    isPublicMultiSession = false
                }
                isGroupMultiSession -> {
                    initiatePublicMultiBroadcast(mediaProjection, true)
                    isGroupMultiSession = false
                }
                else -> startSession(mediaProjection)
            }
        }
    }


    override fun callStatus(callInfoResponse: CallInfoResponse) {
//        will handle response callbacks here according to the data
        when (callInfoResponse.callStatus) {
            CallStatus.OUTGOING_CALL_ENDED, CallStatus.NO_ANSWER_FROM_TARGET -> {
                callInfoResponse.callParams?.sessionUUID?.let { removeRemoteView(it) }
                callInfoResponse.callParams?.sessionUUID?.let { removeMapValue(it) }
                removeService(callInfoResponse.callParams)
            }
            CallStatus.INVALID_AUTHENTICATION -> {
                Log.e("CallStatus", "INVALID_AUTHENTICATION")
            }
            CallStatus.NO_SESSION_EXISTS -> {
                Log.e("SessionCallback", "No Session Exists!")
                incomingCallBottomSheet?.dismiss()
            }
            CallStatus.CALL_CONNECTED -> {
                Log.e("SessionCallback", "Call connected!")
                runOnUiThread { Toast.makeText(this, "Call connected!", Toast.LENGTH_SHORT).show() }
                incomingCallBottomSheet?.dismiss()
            }
            CallStatus.CALL_NOT_CONNECTED -> {
                Log.e("SessionCallback", "Call not connected!")
                runOnUiThread {
                    Toast.makeText(this, "Call not connected!", Toast.LENGTH_SHORT).show()
                }
                incomingCallBottomSheet?.dismiss()
            }
            CallStatus.CALL_MISSED -> {
                Log.e("SessionCallback", "Call Missed!")
                runOnUiThread {
                    Toast.makeText(this, "Call Missed!", Toast.LENGTH_SHORT).show()
                }
                incomingCallBottomSheet?.dismiss()
            }
            CallStatus.CALL_REJECTED -> {
                Log.e("SessionCallback", "Call Rejected!")
                runOnUiThread {
                    Toast.makeText(this, "Call Rejected!", Toast.LENGTH_SHORT).show()
                }
            }
            CallStatus.BAD_REQUEST -> {
                Log.e("SessionCallback", "Bad Request!")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Bad Request: ${callInfoResponse.responseMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            CallStatus.NEW_PARTICIPANT_ARRIVED -> {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "new Participant Arrived: ${callInfoResponse.callParams?.participantCount}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            CallStatus.EXISTING_PARTICIPANTS -> {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "existing Participant: ${callInfoResponse.callParams?.participantCount}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                Log.e("SessionCallback", "Else block")
            }
        }
    }

    private fun removeService(callParams: CallParams?) {
        if (!callClient.getActiveExistingSession(SessionType.SCREEN)) {
            callParams?.let {
                if ((it.callType == CallType.ONE_TO_MANY && it.sessionType == SessionType.SCREEN) &&
                    callParams.isInitiator
                ) {
                    this.unbindService(mConnection)
                    this.stopService(serviceIntent)
                }
            }
        }
    }

    override fun sessionHold(sessionUUID: String) {
        runOnUiThread {
            Toast.makeText(
                this,
                "Session Broken -> Internet Lost!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun removeRemoteView(sessionId: String) {
        val mapValue = mainSessionMap.values
        val sessionModel: SessionModel? = mapValue.find { it.sessionId == sessionId }
        Handler(Looper.getMainLooper()).post {
            sessionModel?.remoteViewMap?.let { map ->
                for (value in map.values) {
                    views_container.removeView(value)
                }
            }
        }
    }

    fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else capitalize(manufacturer).toString() + " " + model
    }

    private fun capitalize(str: String): String? {
        if (TextUtils.isEmpty(str)) {
            return str
        }
        val arr = str.toCharArray()
        var capitalizeNext = true
        val phrase = StringBuilder()
        for (c in arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c))
                capitalizeNext = false
                continue
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true
            }
            phrase.append(c)
        }
        return phrase.toString()
    }

    private fun addViewToSessionMap(sessionId: String, refId: String, rowView: CallViewRenderer) {
        val mapValue = mainSessionMap.values
        val hashKey: String =
            mainSessionMap.filterValues { it.sessionId == sessionId }.keys.first()
        val sessionModel: SessionModel? = mapValue.find { it.sessionId == sessionId }
//        views_container.removeView(sessionModel?.remoteView)
//        sessionModel?.remoteViewMap?.let { map ->
//            map[refId] = rowView
//        }
        sessionModel?.remoteViewMap?.set(refId, rowView)
//        sessionModel?.remoteView = rowView
        mainSessionMap[hashKey] = sessionModel!!
    }

//    private fun addViewToSessionMap(sessionId: String, rowView: ViewGroup) {
//        val mapValue = mainSessionMap.values
//        val hashKey: String =
//            mainSessionMap.filterValues { it.sessionId == sessionId }.keys.first()
//        val sessionModel: SessionModel? = mapValue.find { it.sessionId == sessionId }
//        views_container.removeView(sessionModel?.remoteView)
//        sessionModel?.remoteView = rowView
//        mainSessionMap[hashKey] = sessionModel!!
//    }

    override fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String) {
        Log.i("SocketLog", "onStreamAvailable: available! refId: $refId")
        val mainHandler = Handler(this.mainLooper)
        val myRunnable = Runnable {
            val rowView = this.layoutInflater.inflate(R.layout.peer_video, null)
            val lp = LinearLayout.LayoutParams(
                350,
                350
            )
            lp.setMargins(0, 0, 0, 20)
            rowView.layoutParams = lp
            val rowId = View.generateViewId()
            rowView.id = rowId
            Log.i("SocketLog", "onStreamAvailable: available! rowId: ${rowView.id}")

            views_container.addView(rowView)

            remoteView = rowView as CallViewRenderer//(rowView as ViewGroup).getChildAt(0)

            remoteView?.setOnClickListener {
                swapViewIfmultiSession()

            }
            stream.addSink(remoteView!!.setView())
            addViewToSessionMap(sessionID, refId, rowView)

        }
        mainHandler.post(myRunnable)

    }

    fun swapViewIfmultiSession() {
        val viewOne = mainSessionMap["session 1"]?.remoteView
        val viewTwo = mainSessionMap["session 0"]?.remoteView
        remoteView?.swapViews(this, viewOne!!, viewTwo!!)
    }

    override fun audioVideoState(sessionStateInfo: SessionStateInfo) {
        runOnUiThread {
            val mapValue = mainSessionMap.values
            val sessionModel: SessionModel? =
                mapValue.find { it.sessionId == sessionStateInfo.sessionKey }
            if (sessionStateInfo.videoState == 1) {
                sessionModel?.remoteView?.showHideAvatar(false)
            } else {
                sessionModel?.remoteView?.showHideAvatar(true)
            }
        }
    }

    // private var isSwappedFeeds = false
//    private fun setSwappedFeeds(isSwappedFeeds: Boolean) {
//        this.isSwappedFeeds = isSwappedFeeds
//        localProxyVideoSink.setTarget(if (isSwappedFeeds) ownView.getPreview() else remoteView.getPreview())
//        val temp = ownView.tag
//        ownView.tag = remoteView.tag
    //remoteProxyRenderer.setTarget(if (isSwappedFeeds) remoteView.getPreview() else ownView.getPreview())
//        remoteView.tag = temp
    //}

    override fun onRemoteStream(refId: String, sessionID: String) {

    }

    override fun onCameraStream(stream: VideoTrack) {
        Log.i("SocketLog", "onStreamAvailable: available!")
        val mainHandler = Handler(this.mainLooper)
        val myRunnable = Runnable {
            val rowView = this.layoutInflater.inflate(R.layout.peer_video, null)
            val lp = LinearLayout.LayoutParams(
                350,
                350
            )
            lp.setMargins(0, 0, 0, 20)
            rowView.layoutParams = lp
            val rowId = View.generateViewId()
            rowView.id = rowId
            views_container.addView(rowView)
            ownView =
                rowView as CallViewRenderer//(rowView as ViewGroup).getChildAt(0) as CallViewRenderer
            stream.addSink(ownView.setView())
            ownView.setOnClickListener {
                ownView.swapViews(this, remoteView!!, ownView)
            }
//            localProxyVideoSink.setTarget()
        }
        mainHandler.post(myRunnable)
    }

    override fun sendCurrentDataUsage(sessionKey: String, usage: Usage) {
        prefs.userRegisterInfo?.ref_id?.let { refId ->
            Log.e(
                "statsSdk",
                "currentSentUsage: ${usage.currentSentBytes}, currentReceivedUsage: ${usage.currentReceivedBytes}"
            )
            callClient.sendEndCallLogs(
                refId = refId,
                sessionKey = sessionKey,
                stats = PartialCallLogs(
                    upload_bytes = usage.currentSentBytes.toString(),
                    download_bytes = usage.currentReceivedBytes.toString()
                )
            )
        }
    }

    override fun sendEndDataUsage(sessionKey: String, sessionDataModel: SessionDataModel) {
        prefs.userRegisterInfo?.ref_id?.let { refId ->
            Log.e("statsSdk", "sessionData: $sessionDataModel")
            callClient.sendEndCallLogs(
                refId = refId,
                sessionKey = sessionKey,
                stats = sessionDataModel
            )
        }
    }
}