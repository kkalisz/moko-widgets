/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.widgets.factory

import android.R
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.MarginLayoutParamsCompat
import com.google.android.material.internal.CollapsingTextHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.icerock.moko.graphics.Color
import dev.icerock.moko.widgets.InputWidget
import dev.icerock.moko.widgets.core.ViewBundle
import dev.icerock.moko.widgets.core.ViewFactory
import dev.icerock.moko.widgets.core.ViewFactoryContext
import dev.icerock.moko.widgets.style.applyBackgroundIfNeeded
import dev.icerock.moko.widgets.style.applyInputType
import dev.icerock.moko.widgets.style.applyPaddingIfNeeded
import dev.icerock.moko.widgets.style.applyTextStyleIfNeeded
import dev.icerock.moko.widgets.style.background.Background
import dev.icerock.moko.widgets.style.ext.getGravity
import dev.icerock.moko.widgets.style.view.*
import dev.icerock.moko.widgets.utils.bind
import dev.icerock.moko.widgets.utils.dp
import dev.icerock.moko.widgets.utils.sp

actual class FloatingLabelInputViewFactory actual constructor(
    private val background: Background?,
    private val margins: MarginValues?,
    private val padding: PaddingValues?,
    private val textStyle: TextStyle?,
    private val labelTextStyle: TextStyle?,
    private val errorTextStyle: TextStyle?,
    private val underLineColor: Color?,
    private val underLineFocusedColor: Color?,
    private val textAlignment: TextAlignment?
) : ViewFactory<InputWidget<out WidgetSize>> {

    @SuppressLint("RestrictedApi")
    override fun <WS : WidgetSize> build(
        widget: InputWidget<out WidgetSize>,
        size: WS,
        viewFactoryContext: ViewFactoryContext
    ): ViewBundle<WS> {
        val context = viewFactoryContext.androidContext
        val lifecycleOwner = viewFactoryContext.lifecycleOwner

        val textInputLayout = TextInputLayout(context)
        val collapsingTextHelper = textInputLayout.getCollapsingTextHelper()

        textInputLayout.applyBackgroundIfNeeded(background)
        textInputLayout.applyPaddingIfNeeded(padding)

        val editText = TextInputEditText(context).apply {
            id = widget.id::javaClass.name.hashCode()

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                // EditText's default background have paddings 4dp, while we not change background to own we just change margins
                // https://stackoverflow.com/questions/31735291/removing-the-left-padding-on-an-android-edittext/44497551
                val dp4 = (-4).dp(context)
                MarginLayoutParamsCompat.setMarginStart(this, dp4)
                MarginLayoutParamsCompat.setMarginEnd(this, dp4)
            }

            applyTextStyleIfNeeded(textStyle)
            widget.inputType?.also { applyInputType(it) }

            this@FloatingLabelInputViewFactory.textAlignment?.let {
                gravity = it.getGravity()
            }

            val focusedColor = underLineFocusedColor?.argb?.toInt()
            val defaultColor = underLineColor?.argb?.toInt()

            if (focusedColor != null && defaultColor != null) {
                supportBackgroundTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(-R.attr.state_focused, -R.attr.state_pressed),
                        intArrayOf()
                    ),
                    intArrayOf(
                        defaultColor,
                        focusedColor
                    )
                )
            } else if (defaultColor != null) {
                supportBackgroundTintList = ColorStateList.valueOf(defaultColor)
            } else if (focusedColor != null) {
                supportBackgroundTintList = ColorStateList(
                    arrayOf(
                        intArrayOf(R.attr.state_focused, R.attr.state_activated)
                    ),
                    intArrayOf(
                        focusedColor
                    )
                )
            }


            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) widget.field.validate()
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s == null) return

                    widget.field.data.value = s.toString()
                }
            })
        }

        textInputLayout.addView(editText)

        labelTextStyle?.also {
            if (it.color != null) {
                val hintColor = ColorStateList.valueOf(it.color.argb.toInt())
                textInputLayout.defaultHintTextColor = hintColor
            }
            if (it.size != null) {
                collapsingTextHelper.collapsedTextSize = it.size.toFloat().sp(context)
            }
            if(it.fontStyle != null) {
                collapsingTextHelper.collapsedTypeface = when(it.fontStyle) {
                    FontStyle.BOLD -> Typeface.DEFAULT_BOLD
                    FontStyle.MEDIUM -> Typeface.DEFAULT
                }
                collapsingTextHelper.expandedTypeface = collapsingTextHelper.collapsedTypeface
            }
        }

        widget.field.data.bind(lifecycleOwner) { data ->
            if (editText.text?.toString() == data) return@bind

            editText.setText(data)
        }
        widget.field.error.bind(lifecycleOwner) { error ->
            textInputLayout.error = error?.toString(context)
            textInputLayout.isErrorEnabled = error != null

            if (textInputLayout.isErrorEnabled) {
                val errorText = textInputLayout.findViewById<TextView>(dev.icerock.moko.widgets.R.id.textinput_error)
                errorText.applyTextStyleIfNeeded(errorTextStyle)
            }
        }

        widget.label.bind(lifecycleOwner) { textInputLayout.hint = it?.toString(context) }
        widget.enabled?.bind(lifecycleOwner) { editText.isEnabled = it == true }
        widget.maxLines?.bind(lifecycleOwner) { maxLines ->
            when (maxLines) {
                null -> editText.setSingleLine(false)
                1 -> editText.setSingleLine(true)
                else -> {
                    editText.setSingleLine(false)
                    editText.maxLines = maxLines
                }
            }
        }

        return ViewBundle(
            view = textInputLayout,
            size = size,
            margins = margins
        )
    }

    private fun TextInputLayout.getCollapsingTextHelper(): CollapsingTextHelper {
        val clazz = TextInputLayout::class.java
        val field = clazz.getDeclaredField("collapsingTextHelper")
        field.isAccessible = true
        return field.get(this) as CollapsingTextHelper
    }

}
