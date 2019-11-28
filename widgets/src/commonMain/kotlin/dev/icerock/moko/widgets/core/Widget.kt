/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.widgets.core

import dev.icerock.moko.widgets.style.view.MarginValues
import dev.icerock.moko.widgets.style.view.WidgetSize

abstract class Widget<WS : WidgetSize> {
    abstract fun buildView(viewFactoryContext: ViewFactoryContext): ViewBundle<WS>
}

abstract class FactoryWidget<WS : WidgetSize> : Widget<WS>() {
    abstract val factory: ViewFactory<Widget<out WidgetSize>>
    abstract val size: WS

    override fun buildView(viewFactoryContext: ViewFactoryContext): ViewBundle<WS> {
        return factory.build(this, size, viewFactoryContext)
    }
}

interface ViewFactory<W : Widget<out WidgetSize>> {
    fun <WS : WidgetSize> build(
        widget: W,
        size: WS,
        viewFactoryContext: ViewFactoryContext
    ): ViewBundle<WS>
}

data class ViewBundle<WS : WidgetSize>(
    val view: View,
    // all information that affects anything outside the view itself
    val size: WS,
    val margins: MarginValues?
)
