/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.widgets.screen.navigation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dev.icerock.moko.parcelize.Parcelable
import dev.icerock.moko.widgets.core.View
import dev.icerock.moko.widgets.screen.Args
import dev.icerock.moko.widgets.screen.FragmentNavigation
import dev.icerock.moko.widgets.screen.Screen
import dev.icerock.moko.widgets.screen.TypedScreenDesc
import dev.icerock.moko.widgets.screen.unsafeSetScreenArgument
import dev.icerock.moko.widgets.utils.ThemeAttrs
import dev.icerock.moko.widgets.utils.dp

actual abstract class NavigationScreen<S> actual constructor(
    private val initialScreen: TypedScreenDesc<Args.Empty, S>,
    private val router: Router
) : Screen<Args.Empty>() where S : Screen<Args.Empty>, S : NavigationItem {
    private val fragmentNavigation = FragmentNavigation(this)

    private var toolbar: Toolbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.addOnBackStackChangedListener {
            toolbar?.navigationIcon = if (childFragmentManager.backStackEntryCount > 0) {
                ThemeAttrs.getToolBarUpIndicator(requireContext())
            } else {
                null
            }
        }
        childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                    super.onFragmentStarted(fm, f)

                    updateNavigation(f)
                }
            },
            false
        )

        router.navigationScreen = this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val context = requireContext()
        val root = FrameLayout(context).apply {
            id = android.R.id.content
        }
        val toolbar = Toolbar(context).apply {
            setBackgroundColor(ThemeAttrs.getPrimaryColor(context))
            ViewCompat.setElevation(this, 4.dp(context).toFloat())
            setNavigationOnClickListener {
                childFragmentManager.popBackStack()
            }
        }

        this.toolbar = toolbar

        return LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ThemeAttrs.getContentBackgroundColor(context))

            addView(
                toolbar,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ThemeAttrs.getToolBarHeight(context)
                )
            )
            addView(
                root,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            val instance = initialScreen.instantiate()
            fragmentNavigation.setScreen(instance)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is ComponentActivity) {
            context.onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        fragmentManager!!.popBackStackImmediate()
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        toolbar = null
    }

    override fun onDestroy() {
        super.onDestroy()

        router.navigationScreen = null
    }

    actual class Router {
        var navigationScreen: NavigationScreen<*>? = null

        internal actual fun <T, Arg : Args, S> createPushRouteInternal(
            destination: TypedScreenDesc<Arg, S>,
            inputMapper: (T) -> Arg
        ): Route<T> where S : Screen<Arg>, S : NavigationItem {
            return object : Route<T> {
                override fun route(source: Screen<*>, arg: T) {
                    val screen = destination.instantiate()
                    val argument = inputMapper(arg)
                    if (argument is Args.Parcel<*>) {
                        unsafeSetScreenArgument(screen, argument.args)
                    }
                    navigationScreen!!.fragmentNavigation.routeToScreen(screen)
                }
            }
        }

        internal actual fun <IT, Arg : Args, OT, R : Parcelable, S> createPushResultRouteInternal(
            destination: TypedScreenDesc<Arg, S>,
            inputMapper: (IT) -> Arg,
            outputMapper: (R) -> OT
        ): RouteWithResult<IT, OT> where S : Screen<Arg>, S : Resultable<R>, S : NavigationItem {
            return object : RouteWithResult<IT, OT> {
                override fun route(source: Screen<*>, arg: IT, handler: RouteHandler<OT>) {
                    val screen = destination.instantiate()
                    val argument = inputMapper(arg)
                    if (argument is Args.Parcel<*>) {
                        unsafeSetScreenArgument(screen, argument.args)
                    }
                    navigationScreen!!.fragmentNavigation.routeToScreen(screen)
                    TODO()
                }
            }
        }

        internal actual fun <T, Arg : Args, S> createReplaceRouteInternal(
            destination: TypedScreenDesc<Arg, S>,
            inputMapper: (T) -> Arg
        ): Route<T> where S : Screen<Arg>, S : NavigationItem {
            return object : Route<T> {
                override fun route(source: Screen<*>, arg: T) {
                    val screen = destination.instantiate()
                    val argument = inputMapper(arg)
                    if (argument is Args.Parcel<*>) {
                        unsafeSetScreenArgument(screen, argument.args)
                    }
                    navigationScreen!!.fragmentNavigation.setScreen(screen)
                }
            }
        }
    }

    private fun updateNavigation(fragment: Fragment) {
        fragment as NavigationItem

        when (val navBar = fragment.navigationBar) {
            NavigationBar.None -> {
                toolbar?.visibility = View.GONE
            }
            is NavigationBar.Normal -> {
                toolbar?.visibility = View.VISIBLE
                toolbar?.title = navBar.title.toString(requireContext())
            }
        }
    }
}
