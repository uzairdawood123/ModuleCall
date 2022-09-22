package com.practiceproject

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText


@SuppressLint("AppCompatCustomView")

class BulbView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {
//    var editText: TextInputEditText

    init {
//        val view = LayoutInflater.from(context).inflate(R.layout.view_edit_text, this, false)
//        addView(view)
//        editText = view.findViewById(R.id.editText)

    }
}
//class BulbView : Button {
//    constructor(context: Context?) : super(context) {
//        this.setTextColor(Color.BLUE)
//        this.text = "This button is created from JAVA code"
//    }
//
//    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
//    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
//        context,
//        attrs,
//        defStyle
//    ) {
//    }
//}
