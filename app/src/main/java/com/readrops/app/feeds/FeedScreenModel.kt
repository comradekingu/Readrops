package com.readrops.app.feeds

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import cafe.adriel.voyager.core.model.screenModelScope
import com.readrops.api.localfeed.LocalRSSDataSource
import com.readrops.api.services.Credentials
import com.readrops.api.utils.AuthInterceptor
import com.readrops.api.utils.HtmlParser
import com.readrops.app.R
import com.readrops.app.base.TabScreenModel
import com.readrops.app.repositories.BaseRepository
import com.readrops.app.repositories.GetFoldersWithFeeds
import com.readrops.app.util.components.TextFieldError
import com.readrops.app.util.components.dialog.TextFieldDialogState
import com.readrops.db.Database
import com.readrops.db.entities.Feed
import com.readrops.db.entities.Folder
import com.readrops.db.entities.account.Account
import com.readrops.db.filters.MainFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class FeedScreenModel(
    database: Database,
    private val getFoldersWithFeeds: GetFoldersWithFeeds,
    private val localRSSDataSource: LocalRSSDataSource,
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TabScreenModel(database), KoinComponent {

    private val _feedState = MutableStateFlow(FeedState())
    val feedsState = _feedState.asStateFlow()

    private val _addFeedDialogState = MutableStateFlow(AddFeedDialogState())
    val addFeedDialogState = _addFeedDialogState.asStateFlow()

    private val _updateFeedDialogState = MutableStateFlow(UpdateFeedDialogState())
    val updateFeedDialogState = _updateFeedDialogState.asStateFlow()

    private val _folderState = MutableStateFlow(TextFieldDialogState())
    val folderState = _folderState.asStateFlow()

    init {
        screenModelScope.launch(dispatcher) {
            accountEvent.flatMapLatest { account ->
                _feedState.update {
                    it.copy(config = account.config)
                }

                _updateFeedDialogState.update {
                    it.copy(
                        isFeedUrlReadOnly = account.config.isFeedUrlReadOnly,
                    )
                }

                getFoldersWithFeeds.get(
                    account.id,
                    MainFilter.ALL,
                    account.config.useSeparateState
                )
            }
                .catch { throwable ->
                    _feedState.update {
                        it.copy(foldersAndFeeds = FolderAndFeedsState.ErrorState(Exception(throwable)))
                    }
                }
                .collect { foldersAndFeeds ->
                    _feedState.update {
                        it.copy(foldersAndFeeds = FolderAndFeedsState.LoadedState(foldersAndFeeds))
                    }
                }
        }

        screenModelScope.launch(dispatcher) {
            database.accountDao()
                .selectAllAccounts()
                .collect { accounts ->
                    if (accounts.isNotEmpty()) {
                        _addFeedDialogState.update { dialogState ->
                            dialogState.copy(
                                accounts = accounts,
                                selectedAccount = accounts.find { it.isCurrentAccount }
                                    ?: accounts.first()
                            )
                        }
                    }
                }
        }

        screenModelScope.launch(dispatcher) {
            accountEvent.flatMapLatest { account ->
                _updateFeedDialogState.update {
                    it.copy(
                        isFeedUrlReadOnly = account.config.isFeedUrlReadOnly,
                    )
                }

                database.folderDao().selectFolders(account.id)
            }
                .collect { folders ->
                    _updateFeedDialogState.update {
                        it.copy(
                            folders = if (currentAccount!!.config.addNoFolder) {
                                folders + listOf(
                                    Folder(
                                        id = 0,
                                        name = context.resources.getString(R.string.no_folder)
                                    )
                                )
                            } else {
                                folders
                            }
                        )
                    }
                }
        }
    }

    fun setFolderExpandState(isExpanded: Boolean) =
        _feedState.update { it.copy(areFoldersExpanded = isExpanded) }

    fun closeDialog(dialog: DialogState? = null) {
        when (dialog) {
            is DialogState.AddFeed -> {
                _addFeedDialogState.update {
                    it.copy(
                        url = "",
                        error = null,
                        exception = null,
                        isLoading = false
                    )
                }
            }

            is DialogState.AddFolder, is DialogState.UpdateFolder -> {
                _folderState.update {
                    it.copy(
                        value = "",
                        textFieldError = null,
                        exception = null,
                        isLoading = false
                    )
                }
            }

            is DialogState.UpdateFeed -> {
                _updateFeedDialogState.update { it.copy(exception = null, isLoading = false) }
            }

            else -> {}
        }

        _feedState.update { it.copy(dialog = null) }
    }

    fun openDialog(state: DialogState) {
        when (state) {
            is DialogState.UpdateFeed -> {
                _updateFeedDialogState.update {
                    it.copy(
                        feedId = state.feed.id,
                        feedName = state.feed.name!!,
                        feedUrl = state.feed.url!!,
                        selectedFolder = state.folder
                            ?: it.folders.find { folder -> folder.id == 0 },
                        feedRemoteId = state.feed.remoteId
                    )
                }
            }

            is DialogState.UpdateFolder -> {
                _folderState.update {
                    it.copy(
                        value = state.folder.name.orEmpty()
                    )
                }
            }

            is DialogState.AddFeed -> {
                _addFeedDialogState.update {
                    it.copy(url = state.url.orEmpty())
                }
            }

            else -> {}
        }

        _feedState.update { it.copy(dialog = state) }
    }

    fun deleteFeed(feed: Feed) {
        screenModelScope.launch(dispatcher) {
            try {
                repository?.deleteFeed(feed)
            } catch (e: Exception) {
                _feedState.update { it.copy(exception = e) }
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        screenModelScope.launch(dispatcher) {
            try {
                repository?.deleteFolder(folder)
            } catch (e: Exception) {
                _feedState.update { it.copy(exception = e) }
            }
        }
    }

    //region Add Feed

    fun setAddFeedDialogURL(url: String) {
        _addFeedDialogState.update {
            it.copy(
                url = url,
                error = null,
            )
        }
    }

    fun setAddFeedDialogSelectedAccount(account: Account) {
        _addFeedDialogState.update {
            it.copy(
                selectedAccount = account,
                isAccountDropDownExpanded = false
            )
        }
    }

    fun setAccountDropDownExpanded(isExpanded: Boolean) {
        _addFeedDialogState.update { it.copy(isAccountDropDownExpanded = isExpanded) }
    }

    fun addFeedDialogValidate() {
        val url = _addFeedDialogState.value.url

        when {
            url.isEmpty() -> {
                _addFeedDialogState.update {
                    it.copy(error = TextFieldError.EmptyField)
                }

                return
            }

            !Patterns.WEB_URL.matcher(url).matches() -> {
                _addFeedDialogState.update {
                    it.copy(error = TextFieldError.BadUrl)
                }

                return
            }

            else -> screenModelScope.launch(dispatcher) {
                _addFeedDialogState.update { it.copy(exception = null, isLoading = true) }

                try {
                    if (localRSSDataSource.isUrlRSSResource(url)) {
                        insertFeeds(listOf(Feed(url = url)))
                    } else {
                        val rssUrls = HtmlParser.getFeedLink(url, get())

                        if (rssUrls.isEmpty()) {
                            _addFeedDialogState.update {
                                it.copy(error = TextFieldError.NoRSSFeed, isLoading = false)
                            }
                        } else {
                            insertFeeds(rssUrls.map { Feed(url = it.url) })
                        }
                    }
                } catch (e: Exception) {
                    when (e) {
                        is UnknownHostException -> _addFeedDialogState.update {
                            it.copy(
                                error = TextFieldError.UnreachableUrl,
                                isLoading = false
                            )
                        }

                        else -> _addFeedDialogState.update {
                            it.copy(
                                error = TextFieldError.NoRSSFeed,
                                isLoading = false
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun insertFeeds(feeds: List<Feed>) {
        val selectedAccount = _addFeedDialogState.value.selectedAccount

        if (!selectedAccount.isLocal) {
            get<SharedPreferences>().apply {
                selectedAccount.login = getString(selectedAccount.loginKey, null)
                selectedAccount.password = getString(selectedAccount.passwordKey, null)
            }
            get<AuthInterceptor>().apply {
                credentials = Credentials.toCredentials(selectedAccount)
            }
        }

        val repository = get<BaseRepository> { parametersOf(selectedAccount) }

        val errors = repository.insertNewFeeds(
            newFeeds = feeds,
            onUpdate = { /* TODO */ }
        )

        if (errors.isEmpty()) {
            closeDialog(_feedState.value.dialog)
        } else {
            _addFeedDialogState.update {
                it.copy(
                    exception = errors.values.first(),
                    isLoading = false
                )
            }
        }
    }

    //endregion

    //region Update feed

    fun setFolderDropDownState(isExpanded: Boolean) {
        _updateFeedDialogState.update {
            it.copy(isFolderDropDownExpanded = isExpanded)
        }
    }

    fun setSelectedFolder(folder: Folder) {
        _updateFeedDialogState.update {
            it.copy(selectedFolder = folder)
        }
    }

    fun setUpdateFeedDialogStateFeedName(feedName: String) {
        _updateFeedDialogState.update {
            it.copy(
                feedName = feedName,
                feedNameError = null,
            )
        }
    }

    fun setUpdateFeedDialogFeedUrl(feedUrl: String) {
        _updateFeedDialogState.update {
            it.copy(
                feedUrl = feedUrl,
                feedUrlError = null,
            )
        }
    }

    fun updateFeedDialogValidate() {
        val feedName = _updateFeedDialogState.value.feedName
        val feedUrl = _updateFeedDialogState.value.feedUrl

        when {
            feedName.isEmpty() -> {
                _updateFeedDialogState.update {
                    it.copy(feedNameError = TextFieldError.EmptyField)
                }
                return
            }

            feedUrl.isEmpty() -> {
                _updateFeedDialogState.update {
                    it.copy(feedUrlError = TextFieldError.EmptyField)
                }
                return
            }

            !Patterns.WEB_URL.matcher(feedUrl).matches() -> {
                _updateFeedDialogState.update {
                    it.copy(feedUrlError = TextFieldError.BadUrl)
                }
                return
            }

            else -> {
                _updateFeedDialogState.update { it.copy(exception = null, isLoading = true) }

                screenModelScope.launch(dispatcher) {
                    with(_updateFeedDialogState.value) {
                        try {
                            repository?.updateFeed(
                                Feed(
                                    id = feedId,
                                    name = feedName,
                                    url = feedUrl,
                                    folderId = if (selectedFolder?.id != 0)
                                        selectedFolder?.id
                                    else null,
                                    remoteFolderId = selectedFolder?.remoteId,
                                    remoteId = feedRemoteId
                                )
                            )
                        } catch (e: Exception) {
                            _updateFeedDialogState.update {
                                it.copy(
                                    exception = e,
                                    isLoading = false
                                )
                            }
                            return@launch
                        }
                    }

                    closeDialog(_feedState.value.dialog)
                }
            }
        }
    }

    //endregion

    //region Add/Update folder

    fun setFolderName(name: String) = _folderState.update {
        it.copy(
            value = name,
            textFieldError = null,
        )
    }

    fun folderValidate(updateFolder: Boolean = false) {
        _folderState.update { it.copy(isLoading = true) }
        val name = _folderState.value.value

        if (name.isEmpty()) {
            _folderState.update {
                it.copy(
                    textFieldError = TextFieldError.EmptyField,
                    isLoading = false
                )
            }
            return
        }

        screenModelScope.launch(dispatcher) {
            try {
                if (updateFolder) {
                    val folder = (_feedState.value.dialog as DialogState.UpdateFolder).folder
                    repository?.updateFolder(folder.copy(name = name))
                } else {
                    repository?.addFolder(Folder(name = name, accountId = currentAccount!!.id))
                }
            } catch (e: Exception) {
                _folderState.update { it.copy(exception = e, isLoading = false) }
                return@launch
            }

            closeDialog(_feedState.value.dialog)
        }
    }

    fun resetException() = _feedState.update { it.copy(exception = null) }

    //endregion
}