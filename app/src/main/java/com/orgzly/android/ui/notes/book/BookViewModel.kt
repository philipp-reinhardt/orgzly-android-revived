package com.orgzly.android.ui.notes.book

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.orgzly.android.App
import com.orgzly.android.data.DataRepository
import com.orgzly.android.db.entity.Book
import com.orgzly.android.db.entity.NoteView
import com.orgzly.android.ui.AppBar
import com.orgzly.android.ui.CommonViewModel
import com.orgzly.android.ui.SingleLiveEvent
import com.orgzly.android.usecase.BookCycleVisibility
import com.orgzly.android.usecase.UseCaseRunner

class BookViewModel(private val dataRepository: DataRepository, val bookId: Long) : CommonViewModel() {

    private data class Params(val noteId: Long? = null)

    private val params = MutableLiveData<Params>(Params())

    enum class FlipperDisplayedChild {
        LOADING,
        LOADED,
        EMPTY,
        DOES_NOT_EXIST
    }

    val flipperDisplayedChild = MutableLiveData(FlipperDisplayedChild.LOADING)

    fun setFlipperDisplayedChild(child: FlipperDisplayedChild) {
        flipperDisplayedChild.value = child
    }

    data class Data(val book: Book?, val notes: List<NoteView>?)

    val data = params.switchMap { _ ->
        MediatorLiveData<Data>().apply {
            addSource(dataRepository.getBookLiveData(bookId)) {
                value = Data(it, value?.notes)
            }
            addSource(dataRepository.getVisibleNotesLiveData(bookId)) {
                value = Data(value?.book, it)
            }
        }
    }

    companion object {
        const val APP_BAR_DEFAULT_MODE = 0
        const val APP_BAR_SELECTION_MODE = 1
        const val APP_BAR_SELECTION_MOVE_MODE = 2
    }

    val appBar = AppBar(mapOf(
        APP_BAR_DEFAULT_MODE to null,
        APP_BAR_SELECTION_MODE to APP_BAR_DEFAULT_MODE,
        APP_BAR_SELECTION_MOVE_MODE to APP_BAR_SELECTION_MODE))


    fun cycleVisibility() {
        data.value?.book?.let { book ->
            App.EXECUTORS.diskIO().execute {
                catchAndPostError {
                    UseCaseRunner.run(BookCycleVisibility(book))
                }
            }
        }
    }

    data class NotesToRefile(val selected: Set<Long>, val count: Int)

    val refileRequestEvent: SingleLiveEvent<NotesToRefile> = SingleLiveEvent()

    fun refile(ids: Set<Long>) {
        App.EXECUTORS.diskIO().execute {
            val count = dataRepository.getNotesAndSubtreesCount(ids)
            refileRequestEvent.postValue(NotesToRefile(ids, count))
        }
    }


    val notesDeleteRequest: SingleLiveEvent<Pair<Set<Long>, Int>> = SingleLiveEvent()

    fun requestNotesDelete(ids: Set<Long>) {
        App.EXECUTORS.diskIO().execute {
            val count = dataRepository.getNotesAndSubtreesCount(ids)
            notesDeleteRequest.postValue(Pair(ids, count))
        }
    }
}