/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package com.icerockdev.library.sample

import dev.icerock.moko.mvvm.livedata.LiveData
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import dev.icerock.moko.mvvm.livedata.map
import dev.icerock.moko.mvvm.viewmodel.ViewModel
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.desc.desc
import dev.icerock.moko.widgets.container
import dev.icerock.moko.widgets.core.Theme
import dev.icerock.moko.widgets.core.Widget
import dev.icerock.moko.widgets.style.view.SizeSpec
import dev.icerock.moko.widgets.style.view.WidgetSize

class PostsScreen(
    private val theme: Theme,
    private val viewModel: PostsViewModelContract
) {
    fun createWidget(): Widget<WidgetSize.Const<SizeSpec.AsParent, SizeSpec.AsParent>> {
        return with(theme) {
            container(
                size = WidgetSize.Const(SizeSpec.AsParent, SizeSpec.AsParent),
                children = emptyMap()
            )
//            collection(
//                id = Id.Collection,
//                items = viewModel.posts.map { posts ->
//                    posts.map { post ->
//                        PostCollectionUnitItem(
//                            theme = this,
//                            itemId = post.id,
//                            data = post
//                        )
//                    }
//                },
//                styled = {
//                    it.copy(
//                        padding = PaddingValues(4f)
//                    )
//                }
//            )
        }
    }

    object Id {
//        object Collection : CollectionWidget.Id
    }
}

interface PostsViewModelContract {
    val posts: LiveData<List<PostItem>>

    interface PostItem {
        val id: Long
        val nickname: StringDesc
        val imageUrl: String
        val viewsCount: StringDesc
        val title: StringDesc
        val tags: StringDesc?
        val onClick: () -> Unit
    }
}

class PostsViewModel : ViewModel(), PostsViewModelContract {
    private val _postsTitle: MutableLiveData<List<String>> = MutableLiveData(
        initialValue = List(20) { "Test $it post" }
    )
    override val posts: LiveData<List<PostsViewModelContract.PostItem>> =
        _postsTitle.map { titles ->
            titles.map { title ->
                val id = title.hashCode().toLong()
                PostItem(
                    id = id,
                    nickname = "@alex009".desc(),
                    imageUrl = "https://html5box.com/html5lightbox/images/mountain.jpg",
                    viewsCount = "24.5K".desc(),
                    title = title.desc(),
                    tags = null,
                    onClick = { println("clicked $id!") }
                )
            }
        }

    data class PostItem(
        override val id: Long,
        override val nickname: StringDesc,
        override val imageUrl: String,
        override val viewsCount: StringDesc,
        override val title: StringDesc,
        override val tags: StringDesc?,
        override val onClick: () -> Unit
    ) : PostsViewModelContract.PostItem
}