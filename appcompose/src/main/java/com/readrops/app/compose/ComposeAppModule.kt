package com.readrops.app.compose

import com.readrops.app.compose.account.selection.AccountSelectionViewModel
import com.readrops.app.compose.feeds.FeedViewModel
import com.readrops.app.compose.repositories.BaseRepository
import com.readrops.app.compose.repositories.LocalRSSRepository
import com.readrops.app.compose.timelime.TimelineViewModel
import com.readrops.db.entities.account.Account
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val composeAppModule = module {

    viewModel { TimelineViewModel(get()) }

    viewModel { FeedViewModel(get()) }

    viewModel { AccountSelectionViewModel(get()) }

    // repositories

    factory<BaseRepository> { (account: Account) ->
        LocalRSSRepository(get(), get(), account)
    }

}