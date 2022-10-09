package com.orgzly.android.ui.views.richtext

import com.orgzly.android.ui.views.style.CheckboxSpan
import com.orgzly.android.ui.views.style.DrawerSpan

/**
 * Actions which user can perform in view mode.
 */
interface ActionableRichTextView {
    fun toggleDrawer(drawerSpan: DrawerSpan)
    fun toggleCheckbox(checkboxSpan: CheckboxSpan)
    fun followLinkToNoteWithProperty(name: String, value: String)
    fun followLinkToFile(path: String)
}