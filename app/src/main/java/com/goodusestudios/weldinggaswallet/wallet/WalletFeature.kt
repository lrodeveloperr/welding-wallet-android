@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.goodusestudios.weldinggaswallet.wallet

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.PropaneTank
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goodusestudios.weldinggaswallet.ui.FeatureCanvas
import com.goodusestudios.weldinggaswallet.ui.FeatureCanvasScope
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

fun weldingWalletFeature(store: WalletStore): FeatureCanvas = { scope -> WalletFeature(store, scope) }

private enum class WalletRoute { List, Detail }
private enum class StatusFilter { All, Ready, Low, Empty, Away }

@Composable
private fun WalletFeature(store: WalletStore, scope: FeatureCanvasScope) {
    when (scope.destinationId) {
        "activity" -> ActivityHome(store, scope.expanded)
        "suppliers" -> SupplierHome(store, scope.expanded)
        else -> CylinderHome(store, scope)
    }
}

@Composable
private fun CylinderHome(store: WalletStore, scope: FeatureCanvasScope) {
    val state by store.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(StatusFilter.All) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showForm by rememberSaveable { mutableStateOf(false) }
    val visible = state.activeCylinders.filter { cylinder ->
        val matchesStatus = filter == StatusFilter.All || cylinder.status.name == filter.name
        val haystack = listOf(cylinder.gas, cylinder.capacityLabel, store.supplierName(cylinder.supplierId), cylinder.relationship.label, cylinder.serial).joinToString(" ")
        matchesStatus && (query.isBlank() || haystack.contains(query, ignoreCase = true))
    }
    selectedId?.let { id ->
        state.cylinders.firstOrNull { it.id == id }?.let { CylinderDetail(store, it, scope, onBack = { selectedId = null }) } ?: run { selectedId = null }
        return
    }
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                if (state.activeCylinders.size >= 3 && !scope.isEntitled) scope.requestPaywall() else showForm = true
            }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add") })
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SummaryCard(state.activeCylinders.size, scope.isEntitled, scope.requestPaywall) }
            item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search cylinders") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true) }
            item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusFilter.entries.forEach { item -> FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.name) }) } } }
            if (visible.isEmpty()) item { EmptyState(if (state.activeCylinders.isEmpty()) "No cylinders yet" else "No matching cylinders", if (state.activeCylinders.isEmpty()) "Add a gas and capacity to start your wallet." else "Try another search or filter.", Icons.Outlined.PropaneTank) }
            items(visible, key = { it.id }) { cylinder -> CylinderCard(store, cylinder, expandedId == cylinder.id, { expandedId = if (expandedId == cylinder.id) null else cylinder.id }, { selectedId = cylinder.id }) }
            state.deleted?.takeIf { it.expiresAt >= System.currentTimeMillis() }?.let { deleted -> item { Card { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("${deleted.cylinder.gas} deleted", Modifier.weight(1f)); TextButton(store::undoDelete) { Text("Undo") } } } } }
        }
    }
    if (showForm) CylinderFormSheet(store, null, onDismiss = { showForm = false })
}

@Composable
private fun SummaryCard(count: Int, entitled: Boolean, onUpgrade: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PropaneTank, null, Modifier.size(44.dp).padding(6.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("$count active ${if (count == 1) "cylinder" else "cylinders"}", fontWeight = FontWeight.Bold); if (!entitled) Text("$count of 3 free", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (!entitled) TextButton(onUpgrade) { Text("Upgrade"); Icon(Icons.Outlined.ChevronRight, null) }
        }
    }
}

@Composable
private fun CylinderCard(store: WalletStore, cylinder: Cylinder, expanded: Boolean, onExpand: () -> Unit, onOpen: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PropaneTank, null, Modifier.size(42.dp).padding(7.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(cylinder.gas, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(cylinder.capacityLabel); Text("${store.supplierName(cylinder.supplierId)} · ${cylinder.relationship.label}", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            StatusPill(cylinder.status); IconButton(onClick = onExpand) { Icon(if (expanded) Icons.Outlined.ArrowDropDown else Icons.Outlined.ChevronRight, if (expanded) "Collapse status" else "Update status") }
        }
        if (expanded) { HorizontalDivider(); Column(Modifier.padding(14.dp)) { Text("Update status", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CylinderStatus.entries.forEach { status -> StatusChoice(status, cylinder.status == status) { store.setStatus(cylinder.id, status) } } } } }
    }
}

@Composable private fun StatusPill(status: CylinderStatus) { val color = statusColor(status); OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = color.copy(alpha = .07f)), border = BorderStroke(1.dp, color.copy(alpha = .35f)), shape = RoundedCornerShape(50)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(statusIcon(status), null, Modifier.size(18.dp), tint = color); Spacer(Modifier.width(5.dp)); Text(status.name, color = color, fontWeight = FontWeight.SemiBold) } } }
@Composable private fun StatusChoice(status: CylinderStatus, selected: Boolean, onClick: () -> Unit) { val color = statusColor(status); OutlinedButton(onClick, border = BorderStroke(1.dp, color.copy(alpha = if (selected) 1f else .4f))) { Icon(statusIcon(status), null, tint = color); Spacer(Modifier.width(5.dp)); Text(status.name, color = color) } }
private fun statusColor(status: CylinderStatus) = when (status) { CylinderStatus.Ready -> Color(0xFF2E7D32); CylinderStatus.Low -> Color(0xFFB26A00); CylinderStatus.Empty -> Color(0xFFB3261E); CylinderStatus.Away -> Color(0xFF315A8A) }
private fun statusIcon(status: CylinderStatus): ImageVector = when (status) { CylinderStatus.Ready -> Icons.Outlined.CheckCircle; CylinderStatus.Low -> Icons.Outlined.WarningAmber; CylinderStatus.Empty -> Icons.Outlined.Cancel; CylinderStatus.Away -> Icons.Outlined.LocalShipping }

@Composable
private fun CylinderDetail(store: WalletStore, cylinder: Cylinder, scope: FeatureCanvasScope, onBack: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    val live = state.cylinders.firstOrNull { it.id == cylinder.id } ?: return
    var edit by remember { mutableStateOf(false) }; var service by remember { mutableStateOf<ActivityKind?>(null) }; var reminder by remember { mutableStateOf(false) }; var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }; Text(live.gas, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ edit = true }) { Icon(Icons.Outlined.Edit, "Edit cylinder") } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .35f)), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text(live.gas.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(live.capacityLabel, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); StatusPill(live.status) } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Fact("Supplier", store.supplierName(live.supplierId), Modifier.weight(1f)); Fact("Relationship", live.relationship.label, Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Fact("Serial", live.serial.ifBlank { "Not set" }, Modifier.weight(1f)); Fact("Acquired", DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(live.acquiredAt)), Modifier.weight(1f)) } }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Button({ service = ActivityKind.Refill }) { Text("Refill") }; OutlinedButton({ service = ActivityKind.Exchange }) { Text("Exchange") }; OutlinedButton({ service = ActivityKind.Cost }) { Text("Add cost") } } }
        item { OutlinedButton({ reminder = true }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(8.dp)); Text(live.reminderAt?.let { "Reminder · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))}" } ?: "Add reminder") } }
        item { Text("Recent activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(state.activity.filter { it.cylinderId == live.id }.take(5), key = { it.id }) { ActivityRow(store, it) }
        item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ store.archive(live.id, CylinderLifecycle.Returned); onBack() }) { Text("Return cylinder") }; OutlinedButton({ store.archive(live.id, CylinderLifecycle.Archived); onBack() }) { Text("Archive cylinder") }; TextButton({ confirmDelete = true }) { Text("Delete", color = MaterialTheme.colorScheme.error) } } }
    }
    if (edit) CylinderFormSheet(store, live, onDismiss = { edit = false }, onDuplicate = { if (state.activeCylinders.size >= 3 && !scope.isEntitled) scope.requestPaywall() else { store.duplicate(live); edit = false } })
    service?.let { ServiceSheet(store, live, it) { service = null } }
    if (reminder) ReminderSheet(store, live) { reminder = false }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete ${live.gas}?") }, text = { Text("Its linked activity will also be deleted. You can undo for 15 seconds.") }, confirmButton = { TextButton({ store.delete(live.id); confirmDelete = false; onBack() }) { Text("Delete cylinder", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton({ confirmDelete = false }) { Text("Cancel") } })
}

@Composable private fun Fact(title: String, value: String, modifier: Modifier = Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.padding(12.dp)) { Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CylinderFormSheet(store: WalletStore, existing: Cylinder?, onDismiss: () -> Unit, onDuplicate: (() -> Unit)? = null) {
    val state by store.state.collectAsStateWithLifecycle()
    var gas by remember { mutableStateOf(existing?.gas ?: "") }
    var capacity by remember { mutableStateOf(existing?.capacityValue?.toString() ?: "") }
    var unit by remember { mutableStateOf(existing?.capacityUnit ?: state.defaults.capacityUnit) }
    var supplierId by remember { mutableStateOf(existing?.supplierId ?: state.defaults.supplierId) }
    var relationship by remember { mutableStateOf(existing?.relationship ?: state.defaults.relationship) }
    var serial by remember { mutableStateOf(existing?.serial ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var optional by remember { mutableStateOf(false) }
    var addSupplier by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (existing == null) "Add cylinder" else "Edit cylinder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if (existing == null && state.activeCylinders.isNotEmpty()) {
                Text("Copy an existing cylinder", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.activeCylinders.forEach { cylinder ->
                        FilterChip(false, {
                            gas = cylinder.gas; capacity = cylinder.capacityValue.toString(); unit = cylinder.capacityUnit
                            supplierId = cylinder.supplierId; relationship = cylinder.relationship; serial = ""; notes = cylinder.notes
                        }, { Text("${cylinder.gas} · ${cylinder.capacityLabel}") }, leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) })
                    }
                }
            }
            Text("Gas")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Argon", "C25 Mix", "Oxygen", "Acetylene", "Nitrogen", "CO₂", "Helium").forEach { value -> FilterChip(gas == value, { gas = value }, { Text(value) }) }
            }
            OutlinedTextField(gas, { gas = it }, Modifier.fillMaxWidth(), label = { Text("Gas or custom gas") })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(capacity, { capacity = it }, Modifier.weight(1f), label = { Text("Capacity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                FlowRow(Modifier.weight(1f)) { listOf("ft3", "L", "m3", "kg", "lb").forEach { value -> FilterChip(unit == value, { unit = value }, { Text(value.replace("3", "³")) }) } }
            }
            OutlinedButton({ optional = !optional }, Modifier.fillMaxWidth()) { Text(if (optional) "Hide optional details" else "Add supplier, relationship or serial"); Icon(Icons.Outlined.ChevronRight, null) }
            if (optional) {
                Text("Supplier")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(supplierId == null, { supplierId = null }, { Text("Not set") })
                    state.suppliers.forEach { supplier -> FilterChip(supplierId == supplier.id, { supplierId = supplier.id }, { Text(supplier.name) }) }
                    FilterChip(false, { addSupplier = true }, { Text("Add supplier") }, leadingIcon = { Icon(Icons.Outlined.PersonAdd, null) })
                }
                Text("Relationship")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Relationship.entries.forEach { value -> FilterChip(relationship == value, { relationship = value }, { Text(value.label) }) } }
                OutlinedTextField(serial, { serial = it }, Modifier.fillMaxWidth(), label = { Text("Serial number") })
                OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes") })
            }
            Button({
                val number = capacity.toDoubleOrNull()
                val success = if (number == null || number <= 0) false else if (existing == null) store.addCylinder(gas, number, unit, supplierId, relationship, serial, notes) != null else store.updateCylinder(existing.copy(gas = gas.trim(), capacityValue = number, capacityUnit = unit, supplierId = supplierId, relationship = relationship, serial = serial, notes = notes))
                if (success) onDismiss() else error = "Enter a gas and positive capacity. Serial numbers must be unique."
            }, Modifier.fillMaxWidth(), enabled = gas.isNotBlank() && (capacity.toDoubleOrNull() ?: 0.0) > 0) { Text("Save") }
            onDuplicate?.let { OutlinedButton(it, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Duplicate cylinder") } }
        }
    }
    if (addSupplier) AddSupplierDialog(store, { supplierId = it.id; addSupplier = false }, { addSupplier = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceSheet(store: WalletStore, cylinder: Cylinder, kind: ActivityKind, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }; var serial by remember { mutableStateOf("") }; var sameCapacity by remember { mutableStateOf(true) }; var capacity by remember { mutableStateOf("") }; var unit by remember { mutableStateOf(cylinder.capacityUnit) }; var date by remember { mutableStateOf(System.currentTimeMillis()) }; var error by remember { mutableStateOf("") }; val sign = store.currencySign(store.defaultCurrency); val last = store.state.value.activity.firstOrNull { it.cylinderId == cylinder.id && it.amountMinor != null && it.currencyCode == store.defaultCurrency }?.amountMinor
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(kind.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Today · $sign", color = MaterialTheme.colorScheme.onSurfaceVariant); if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("Amount") }, prefix = { Text(sign) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); last?.let { minor -> TextButton({ amount = BigDecimal(minor).divide(BigDecimal(100)).toPlainString() }) { Text("Use last cost · $sign${BigDecimal(minor).divide(BigDecimal(100))}") } }; if (kind == ActivityKind.Exchange) { OutlinedTextField(serial, { serial = it }, Modifier.fillMaxWidth(), label = { Text("Replacement serial (optional)") }); Row(verticalAlignment = Alignment.CenterVertically) { Text("Same capacity", Modifier.weight(1f)); Switch(sameCapacity, { sameCapacity = it }) }; if (!sameCapacity) Row { OutlinedTextField(capacity, { capacity = it }, Modifier.weight(1f), label = { Text("Capacity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); TextButton({ unit = if (unit == "ft3") "L" else "ft3" }) { Text(unit.replace("3", "³")) } } }; Button({ val value = amount.toBigDecimalOrNull(); val ok = value != null && store.recordService(cylinder.id, kind, value, store.defaultCurrency, date, serial, if (sameCapacity) null else capacity.toDoubleOrNull(), if (sameCapacity) null else unit); if (ok) onDismiss() else error = "Enter a positive amount and a unique replacement serial." }, Modifier.fillMaxWidth(), enabled = (amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO) { Text("Save") } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSheet(store: WalletStore, cylinder: Cylinder, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var enabled by remember { mutableStateOf(cylinder.reminderAt != null) }
    var days by remember { mutableIntStateOf(7) }
    var date by remember { mutableStateOf(cylinder.reminderAt ?: System.currentTimeMillis() + 7 * 86_400_000L) }
    val chooseCustom = {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        DatePickerDialog(context, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); calendar.set(Calendar.SECOND, 0)
                date = calendar.timeInMillis; days = 0; enabled = true
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Notifications, null); Spacer(Modifier.width(10.dp)); Text("Refill reminder", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge); Switch(enabled, { enabled = it }) }
            if (enabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(3, 7, 14).forEach { value -> FilterChip(days == value, { days = value; date = System.currentTimeMillis() + value * 86_400_000L }, { Text("$value days") }) }; FilterChip(days == 0, chooseCustom, { Text("Custom") }) }
                Text("Scheduled for ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(date))}.")
            }
            Button({
                val reminderAt = if (enabled) date else null
                if (enabled && Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                store.setReminder(cylinder.id, reminderAt)
                ReminderReceiver.schedule(context, cylinder, reminderAt)
                onDismiss()
            }, Modifier.fillMaxWidth()) { Text("Save reminder") }
            Text("No account or cloud service is required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityHome(store: WalletStore, expanded: Boolean) {
    val state by store.state.collectAsStateWithLifecycle(); var filter by rememberSaveable { mutableStateOf("All") }; val filtered = state.activity.filter { filter == "All" || filter == "Refills" && it.kind == ActivityKind.Refill || filter == "Status" && it.kind == ActivityKind.Status }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { if (expanded) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Metric("Total spent", store.totals().entries.joinToString(" · ") { "${store.currencySign(it.key)}${it.value}" }.ifBlank { "—" }, "Currencies stay separate", Modifier.weight(1f)); Metric("Refill count", store.refillCount.toString(), "Recorded refills", Modifier.weight(1f)); Metric("Average refill interval", store.averageRefillDays?.let { "$it days" } ?: "—", "Across repeat refills", Modifier.weight(1f)) } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Total spent", store.totals().entries.joinToString(" · ") { "${store.currencySign(it.key)}${it.value}" }.ifBlank { "—" }, "Currencies stay separate"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Refill count", store.refillCount.toString(), "Recorded refills", Modifier.weight(1f)); Metric("Average refill interval", store.averageRefillDays?.let { "$it days" } ?: "—", "Across repeat refills", Modifier.weight(1f)) } } }; item { FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("All", "Refills", "Status").forEach { value -> FilterChip(filter == value, { filter = value }, { Text(value) }) } } }; if (filtered.isEmpty()) item { EmptyState("No activity yet", "Cylinder changes and costs will appear here.", Icons.Outlined.History) }; items(filtered, key = { it.id }) { ActivityRow(store, it) } }
}

@Composable private fun Metric(title: String, value: String, note: String, modifier: Modifier = Modifier) { OutlinedCard(modifier, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun ActivityRow(store: WalletStore, item: Activity) { OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { ListItem(headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(item.detail) }, leadingContent = { Icon(if (item.kind == ActivityKind.Refill) Icons.Outlined.Restore else Icons.Outlined.History, null) }, trailingContent = { Column(horizontalAlignment = Alignment.End) { item.amountMinor?.let { minor -> Text("${store.currencySign(item.currencyCode ?: store.defaultCurrency)}${BigDecimal(minor).divide(BigDecimal(100))}", fontWeight = FontWeight.Bold) }; Text(DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(item.occurredAt)), style = MaterialTheme.typography.labelSmall) } }) } }

@Composable
private fun SupplierHome(store: WalletStore, expanded: Boolean) {
    val state by store.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var add by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    selectedId?.let { id ->
        state.suppliers.firstOrNull { it.id == id }?.let { SupplierDetail(store, it) { selectedId = null } } ?: run { selectedId = null }
        return
    }
    val suppliers = state.suppliers.filter { query.isBlank() || it.name.contains(query, true) }
    Scaffold(floatingActionButton = { ExtendedFloatingActionButton(onClick = { add = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search suppliers") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
            Spacer(Modifier.height(12.dp))
            if (suppliers.isEmpty()) EmptyState("No suppliers yet", "Add a supplier here or directly from a cylinder.", Icons.Outlined.Groups)
            else LazyVerticalGrid(GridCells.Fixed(if (expanded) 2 else 1), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(suppliers, key = { it.id }) { supplier ->
                    OutlinedCard(Modifier.clickable { selectedId = supplier.id }) {
                        ListItem(
                            headlineContent = { Text(supplier.name, fontWeight = FontWeight.Bold) },
                            supportingContent = { val count = state.activeCylinders.count { it.supplierId == supplier.id }; Text("$count current ${if (count == 1) "cylinder" else "cylinders"}") },
                            leadingContent = { Icon(Icons.Outlined.Groups, null) },
                            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                        )
                    }
                }
            }
        }
    }
    if (add) AddSupplierDialog(store, { add = false }, { add = false })
}

@Composable
private fun SupplierDetail(store: WalletStore, supplier: Supplier, onBack: () -> Unit) {
    val state by store.state.collectAsStateWithLifecycle()
    val linked = state.cylinders.filter { it.supplierId == supplier.id }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }; Text(supplier.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
        item { OutlinedCard(Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(supplier.name, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${linked.count { it.lifecycle == CylinderLifecycle.Active }} current cylinders") }, leadingContent = { Icon(Icons.Outlined.Groups, null) }) } }
        if (supplier.phone.isNotBlank()) item { Fact("Phone", supplier.phone) }
        if (supplier.notes.isNotBlank()) item { Fact("Notes", supplier.notes) }
        item { Text("Cylinders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (linked.isEmpty()) item { EmptyState("No linked cylinders", "Add this supplier from a cylinder form.", Icons.Outlined.PropaneTank) }
        items(linked, key = { it.id }) { cylinder -> OutlinedCard(Modifier.fillMaxWidth()) { ListItem(headlineContent = { Text(cylinder.gas, fontWeight = FontWeight.Bold) }, supportingContent = { Text("${cylinder.capacityLabel} · ${cylinder.lifecycle.name}") }, leadingContent = { Icon(Icons.Outlined.PropaneTank, null) }, trailingContent = { StatusPill(cylinder.status) }) } }
    }
}

@Composable private fun AddSupplierDialog(store: WalletStore, onSaved: (Supplier) -> Unit, onDismiss: () -> Unit) { var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Add supplier") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error); OutlinedTextField(name, { name = it }, label = { Text("Supplier name") }); OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)); OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }) } }, confirmButton = { TextButton({ store.addSupplier(name, phone, notes)?.let(onSaved) ?: run { error = if (name.isBlank()) "Enter a supplier name." else "That supplier is already saved." } }, enabled = name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } }) }

@Composable private fun EmptyState(title: String, message: String, icon: ImageVector) { Column(Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
fun CurrencySettingsScreen(store: WalletStore) {
    val state by store.state.collectAsStateWithLifecycle(); var query by rememberSaveable { mutableStateOf("") }
    val codes = remember(query) { Currency.getAvailableCurrencies().map { it.currencyCode }.sorted().filter { code -> query.isBlank() || code.contains(query, true) || Currency.getInstance(code).displayName.contains(query, true) } }
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) { item { Text("This sets the currency for new costs. Existing history never changes.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { ListItem(headlineContent = { Text("Automatic") }, supportingContent = { Text("Device region") }, leadingContent = { Text(store.currencySign(store.automaticCurrency)) }, modifier = Modifier.clickable { store.setCurrency(null) }); HorizontalDivider() }; item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Search currencies") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }) }; items(codes) { code -> ListItem(headlineContent = { Text(Currency.getInstance(code).displayName) }, leadingContent = { Text(store.currencySign(code), fontWeight = FontWeight.Bold) }, trailingContent = { if (state.currencyOverride == code) Icon(Icons.Outlined.CheckCircle, null) }, modifier = Modifier.clickable { store.setCurrency(code) }); HorizontalDivider() } }
}

@Composable
fun WalletBackupScreen(store: WalletStore) {
    val context = LocalContext.current; var message by remember { mutableStateOf("") }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.exportJson()) } }.onSuccess { message = "Backup file created" }.onFailure { message = it.message ?: "Backup failed" } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Could not read backup") }.onSuccess { store.restoreJson(it); message = "Backup restored" }.onFailure { message = it.message ?: "Restore failed" } }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { item { Icon(Icons.Outlined.Backup, null, Modifier.size(46.dp)); Text("Keep a file copy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Use the system file picker to save to Google Drive, this device, or another document provider.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; item { Button({ create.launch("welding-wallet-backup.json") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.ArrowCircleUp, null); Spacer(Modifier.width(8.dp)); Text("Save backup file") } }; item { OutlinedButton({ open.launch(arrayOf("application/json")) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.ArrowCircleDown, null); Spacer(Modifier.width(8.dp)); Text("Restore backup file") } }; if (message.isNotBlank()) item { Text(message) }; item { Row { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(8.dp)); Text("Backup files never include purchase entitlement.") } } }
}

@Composable fun WalletHelpScreen() { val steps = listOf("Add a cylinder with only its gas and capacity.", "Copy an existing cylinder when the details are similar.", "Tap a status whenever it changes.", "Record a refill or exchange; today and your currency are already selected.", "Return or archive a cylinder when it leaves your active inventory.", "Create a backup file before moving to a new phone."); LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { itemsIndexed(steps) { index, step -> Row(verticalAlignment = Alignment.Top) { Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.size(36.dp).padding(8.dp)); Text(step, Modifier.padding(top = 8.dp)) } }; item { Text("If you make a mistake, edit the cylinder or use Delete cylinder and Undo.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
