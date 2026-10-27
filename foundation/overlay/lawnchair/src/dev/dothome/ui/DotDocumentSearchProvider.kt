package dev.dothome.ui
import android.content.Context
import app.lawnchair.search.algorithms.data.FileInfo
import app.lawnchair.search.algorithms.engine.SearchProvider
import app.lawnchair.search.algorithms.engine.SearchResult
import kotlinx.coroutines.flow.flow
/** Standard local file result presentation, backed only by user-selected grants. */
object DotDocumentSearchProvider: SearchProvider {
    override val id="selected-documents"
    override fun search(context: Context,query: String)=flow {
        emit(DotDocuments.search(context,query).map {document->SearchResult.File(FileInfo(document.uri,document.uri,document.name,document.size,document.modified,document.mime))})
    }
}
