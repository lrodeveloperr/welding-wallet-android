package com.goodusestudios.weldinggaswallet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class ShellIconCategory { Navigation, Actions, Content, Communication, Media, Commerce, Status, System }

data class ShellIconSpec(
    val id: String,
    val label: String,
    val category: ShellIconCategory,
    val vector: ImageVector,
)

/**
 * Semantic front door to the official Material icon set bundled with the shell.
 * Derived apps can keep stable IDs while replacing vectors with newer Material Symbols.
 */
object ShellIconCatalog {
    val all = listOf(
        icon("home", "Home", ShellIconCategory.Navigation, Icons.Outlined.Home),
        icon("menu", "Menu", ShellIconCategory.Navigation, Icons.Outlined.Menu),
        icon("apps", "Apps", ShellIconCategory.Navigation, Icons.Outlined.Apps),
        icon("inbox", "Inbox", ShellIconCategory.Navigation, Icons.Outlined.Inbox),
        icon("timeline", "Activity", ShellIconCategory.Navigation, Icons.Outlined.Timeline),
        icon("settings", "Settings", ShellIconCategory.Navigation, Icons.Outlined.Settings),
        icon("more", "More", ShellIconCategory.Navigation, Icons.Outlined.MoreVert),
        icon("open_external", "Open external", ShellIconCategory.Navigation, Icons.Outlined.OpenInNew),
        icon("add", "Add", ShellIconCategory.Actions, Icons.Outlined.Add),
        icon("edit", "Edit", ShellIconCategory.Actions, Icons.Outlined.Edit),
        icon("delete", "Delete", ShellIconCategory.Actions, Icons.Outlined.DeleteOutline),
        icon("save", "Save", ShellIconCategory.Actions, Icons.Outlined.Save),
        icon("search", "Search", ShellIconCategory.Actions, Icons.Outlined.Search),
        icon("filter", "Filter", ShellIconCategory.Actions, Icons.Outlined.FilterList),
        icon("tune", "Tune", ShellIconCategory.Actions, Icons.Outlined.Tune),
        icon("refresh", "Refresh", ShellIconCategory.Actions, Icons.Outlined.Refresh),
        icon("restart", "Restart", ShellIconCategory.Actions, Icons.Outlined.RestartAlt),
        icon("share", "Share", ShellIconCategory.Actions, Icons.Outlined.Share),
        icon("download", "Download", ShellIconCategory.Actions, Icons.Outlined.Download),
        icon("upload", "Upload", ShellIconCategory.Actions, Icons.Outlined.Upload),
        icon("visibility", "View", ShellIconCategory.Actions, Icons.Outlined.Visibility),
        icon("list", "List", ShellIconCategory.Content, Icons.Outlined.List),
        icon("folder", "Folder", ShellIconCategory.Content, Icons.Outlined.FolderOpen),
        icon("bookmark", "Bookmark", ShellIconCategory.Content, Icons.Outlined.BookmarkBorder),
        icon("favorite", "Favorite", ShellIconCategory.Content, Icons.Outlined.FavoriteBorder),
        icon("calendar", "Calendar", ShellIconCategory.Content, Icons.Outlined.CalendarMonth),
        icon("location", "Location", ShellIconCategory.Content, Icons.Outlined.LocationOn),
        icon("link", "Link", ShellIconCategory.Content, Icons.Outlined.Link),
        icon("language", "Language", ShellIconCategory.Content, Icons.Outlined.Language),
        icon("account", "Account", ShellIconCategory.Communication, Icons.Outlined.AccountCircle),
        icon("person", "Person", ShellIconCategory.Communication, Icons.Outlined.PersonOutline),
        icon("email", "Email", ShellIconCategory.Communication, Icons.Outlined.Email),
        icon("phone", "Phone", ShellIconCategory.Communication, Icons.Outlined.Phone),
        icon("chat", "Chat", ShellIconCategory.Communication, Icons.Outlined.ChatBubbleOutline),
        icon("send", "Send", ShellIconCategory.Communication, Icons.Outlined.Send),
        icon("support", "Support", ShellIconCategory.Communication, Icons.Outlined.SupportAgent),
        icon("notifications", "Notifications", ShellIconCategory.Communication, Icons.Outlined.NotificationsNone),
        icon("image", "Image", ShellIconCategory.Media, Icons.Outlined.Image),
        icon("camera", "Camera", ShellIconCategory.Media, Icons.Outlined.CameraAlt),
        icon("video", "Video", ShellIconCategory.Media, Icons.Outlined.VideoLibrary),
        icon("play", "Play", ShellIconCategory.Media, Icons.Outlined.PlayArrow),
        icon("shopping_cart", "Cart", ShellIconCategory.Commerce, Icons.Outlined.ShoppingCart),
        icon("work", "Work", ShellIconCategory.Commerce, Icons.Outlined.WorkOutline),
        icon("star", "Rating", ShellIconCategory.Commerce, Icons.Outlined.StarBorder),
        icon("success", "Success", ShellIconCategory.Status, Icons.Outlined.CheckCircle),
        icon("warning", "Warning", ShellIconCategory.Status, Icons.Outlined.WarningAmber),
        icon("error", "Error", ShellIconCategory.Status, Icons.Outlined.ErrorOutline),
        icon("info", "Information", ShellIconCategory.Status, Icons.Outlined.Info),
        icon("help", "Help", ShellIconCategory.Status, Icons.Outlined.HelpOutline),
        icon("cloud", "Cloud", ShellIconCategory.Status, Icons.Outlined.Cloud),
        icon("offline", "Offline", ShellIconCategory.Status, Icons.Outlined.CloudOff),
        icon("sync", "Sync", ShellIconCategory.Status, Icons.Outlined.Sync),
        icon("lock", "Locked", ShellIconCategory.System, Icons.Outlined.Lock),
        icon("unlock", "Unlocked", ShellIconCategory.System, Icons.Outlined.LockOpen),
        icon("policy", "Policy", ShellIconCategory.System, Icons.Outlined.Policy),
        icon("light_mode", "Light mode", ShellIconCategory.System, Icons.Outlined.LightMode),
        icon("dark_mode", "Dark mode", ShellIconCategory.System, Icons.Outlined.DarkMode),
    )

    fun byId(id: String): ShellIconSpec? = all.firstOrNull { it.id == id }

    fun search(query: String): List<ShellIconSpec> {
        val term = query.trim().lowercase()
        return if (term.isEmpty()) all else all.filter {
            it.id.contains(term) || it.label.lowercase().contains(term) || it.category.name.lowercase().contains(term)
        }
    }

    private fun icon(id: String, label: String, category: ShellIconCategory, vector: ImageVector) =
        ShellIconSpec(id, label, category, vector)
}

@Composable
fun IconLibraryScreen() {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<ShellIconSpec?>(null) }
    val results = remember(query) { ShellIconCatalog.search(query) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Official icon catalog", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Search the shell’s semantic Material icons. Tap one to see the stable ID used by app configuration.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search icons") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                )
                Text("${results.size} icons", style = MaterialTheme.typography.labelMedium)
            }
        }
        items(results, key = { it.id }) { item ->
            Card(onClick = { selected = item }, modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(item.vector, null)
                    Text(item.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            icon = { Icon(item.vector, null) },
            title = { Text(item.label) },
            text = { Text("ID: ${item.id}\nCategory: ${item.category.name}") },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } },
        )
    }
}
