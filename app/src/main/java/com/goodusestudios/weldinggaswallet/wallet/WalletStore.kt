package com.goodusestudios.weldinggaswallet.wallet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CylinderStatus { Ready, Low, Empty, Away }
enum class CylinderLifecycle { Active, Returned, Archived }
enum class Relationship(val label: String) { Owned("Owned"), Rental("Rental"), Leased("Leased"), Deposit("Deposit"), NotSet("Not set") }
enum class ActivityKind { Created, Status, Refill, Exchange, Cost, Returned, Archived }

data class Cylinder(
    val id: String = UUID.randomUUID().toString(), val gas: String, val capacityValue: Double,
    val capacityUnit: String, val supplierId: String? = null, val relationship: Relationship = Relationship.NotSet,
    val serial: String = "", val status: CylinderStatus = CylinderStatus.Ready,
    val lifecycle: CylinderLifecycle = CylinderLifecycle.Active, val acquiredAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null, val notes: String = "",
) {
    val capacityLabel: String get() = "${if (capacityValue % 1.0 == 0.0) capacityValue.toLong() else capacityValue} ${capacityUnit.replace("3", "³")}" 
}

data class Supplier(val id: String = UUID.randomUUID().toString(), val name: String, val phone: String = "", val notes: String = "")
data class Activity(val id: String = UUID.randomUUID().toString(), val cylinderId: String, val kind: ActivityKind, val occurredAt: Long = System.currentTimeMillis(), val title: String, val detail: String, val amountMinor: Long? = null, val currencyCode: String? = null)
data class CylinderDefaults(val supplierId: String? = null, val relationship: Relationship = Relationship.NotSet, val capacityUnit: String = defaultCapacityUnit())
data class DeletedCylinder(val cylinder: Cylinder, val activity: List<Activity>, val expiresAt: Long)
data class WalletState(
    val cylinders: List<Cylinder> = emptyList(), val suppliers: List<Supplier> = emptyList(),
    val activity: List<Activity> = emptyList(), val currencyOverride: String? = null,
    val defaults: CylinderDefaults = CylinderDefaults(), val deleted: DeletedCylinder? = null,
    val freeManagedCylinderIds: Set<String> = emptySet(), val message: String? = null,
) { val activeCylinders get() = cylinders.filter { it.lifecycle == CylinderLifecycle.Active } }

class WalletStore(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("welding_wallet_v2", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<WalletState> = _state.asStateFlow()

    val automaticCurrency: String get() = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")
    val defaultCurrency: String get() = _state.value.currencyOverride ?: automaticCurrency
    fun supplierName(id: String?): String = _state.value.suppliers.firstOrNull { it.id == id }?.name ?: "Not set"
    fun canAddCylinder(isEntitled: Boolean) = isEntitled || _state.value.activeCylinders.size < FREE_LIMIT
    fun canManageCylinder(id: String, isEntitled: Boolean): Boolean {
        val cylinder = _state.value.cylinders.firstOrNull { it.id == id } ?: return false
        return cylinder.lifecycle == CylinderLifecycle.Active && (isEntitled || _state.value.activeCylinders.size <= FREE_LIMIT || id in _state.value.freeManagedCylinderIds)
    }
    fun requiresFreeCylinderSelection(isEntitled: Boolean): Boolean {
        val state = _state.value
        return !isEntitled && state.activeCylinders.size > FREE_LIMIT && state.freeManagedCylinderIds.intersect(state.activeCylinders.mapTo(mutableSetOf()) { it.id }).size < FREE_LIMIT
    }

    fun reconcileAccess(isEntitled: Boolean): Boolean {
        val current = _state.value; val activeIds = current.activeCylinders.mapTo(linkedSetOf()) { it.id }
        val managed = when { isEntitled -> emptySet(); activeIds.size <= FREE_LIMIT -> activeIds; else -> current.freeManagedCylinderIds.filterTo(linkedSetOf()) { it in activeIds }.take(FREE_LIMIT).toSet() }
        return managed == current.freeManagedCylinderIds || update(current.copy(freeManagedCylinderIds = managed))
    }
    fun selectFreeManagedCylinders(ids: Set<String>, isEntitled: Boolean): Boolean {
        val current = _state.value; val active = current.activeCylinders.mapTo(mutableSetOf()) { it.id }; val required = minOf(FREE_LIMIT, active.size)
        if (isEntitled || ids.size != required || !active.containsAll(ids) || !ids.containsAll(current.freeManagedCylinderIds)) return false
        return update(current.copy(freeManagedCylinderIds = ids))
    }

    fun addSupplier(name: String, phone: String = "", notes: String = ""): Supplier? {
        val clean = name.trim(); val current = _state.value
        if (clean.isEmpty() || current.suppliers.any { it.name.equals(clean, true) }) return null
        val supplier = Supplier(name = clean, phone = phone.trim(), notes = notes.trim())
        return if (update(current.copy(suppliers = current.suppliers + supplier))) supplier else null
    }

    fun addCylinder(gas: String, capacity: Double, unit: String, supplierId: String?, relationship: Relationship, serial: String, notes: String = "", isEntitled: Boolean = false): Cylinder? {
        val current = _state.value; val cleanGas = gas.trim(); val cleanSerial = serial.trim()
        if (!canAddCylinder(isEntitled) || cleanGas.isEmpty() || !capacity.isFinite() || capacity <= 0 || unit !in VALID_UNITS || (supplierId != null && current.suppliers.none { it.id == supplierId }) || (cleanSerial.isNotEmpty() && current.cylinders.any { it.serial.equals(cleanSerial, true) })) return null
        val cylinder = Cylinder(gas = cleanGas, capacityValue = capacity, capacityUnit = unit, supplierId = supplierId, relationship = relationship, serial = cleanSerial, notes = notes.trim())
        val event = Activity(cylinderId = cylinder.id, kind = ActivityKind.Created, title = "$cleanGas added", detail = "${supplierName(supplierId)} · ${cylinder.capacityLabel}")
        val managed = if (!isEntitled && current.activeCylinders.size < FREE_LIMIT) current.freeManagedCylinderIds + cylinder.id else current.freeManagedCylinderIds
        return if (update(current.copy(cylinders = current.cylinders + cylinder, activity = listOf(event) + current.activity, defaults = CylinderDefaults(supplierId, relationship, unit), freeManagedCylinderIds = managed))) cylinder else null
    }

    fun duplicate(source: Cylinder, isEntitled: Boolean = false): Cylinder? {
        if (!canManageCylinder(source.id, isEntitled)) return null
        return addCylinder(source.gas, source.capacityValue, source.capacityUnit, source.supplierId, source.relationship, "", source.notes, isEntitled)
    }

    fun updateCylinder(cylinder: Cylinder, isEntitled: Boolean = false): Boolean {
        val current = _state.value; val original = current.cylinders.firstOrNull { it.id == cylinder.id } ?: return false; val serial = cylinder.serial.trim()
        if (!canManageCylinder(cylinder.id, isEntitled) || cylinder.lifecycle != original.lifecycle || cylinder.gas.isBlank() || !cylinder.capacityValue.isFinite() || cylinder.capacityValue <= 0 || cylinder.capacityUnit !in VALID_UNITS || (cylinder.supplierId != null && current.suppliers.none { it.id == cylinder.supplierId }) || (serial.isNotEmpty() && current.cylinders.any { it.id != cylinder.id && it.serial.equals(serial, true) })) return false
        val changed = cylinder.copy(gas = cylinder.gas.trim(), serial = serial)
        val saved = update(current.copy(cylinders = current.cylinders.map { if (it.id == cylinder.id) changed else it }))
        if (saved && changed.reminderAt != null) ReminderReceiver.schedule(appContext, changed, changed.reminderAt)
        return saved
    }

    fun setStatus(id: String, status: CylinderStatus, isEntitled: Boolean = false): Boolean {
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return false
        if (!canManageCylinder(id, isEntitled) || cylinder.status == status) return false
        val event = Activity(cylinderId = id, kind = ActivityKind.Status, title = "${cylinder.gas} marked ${status.name}", detail = "${supplierName(cylinder.supplierId)} · ${cylinder.capacityLabel}")
        return update(current.copy(cylinders = current.cylinders.map { if (it.id == id) it.copy(status = status) else it }, activity = listOf(event) + current.activity))
    }

    fun recordService(id: String, kind: ActivityKind, amount: BigDecimal?, currency: String, date: Long, replacementSerial: String = "", replacementCapacity: Double? = null, replacementUnit: String? = null, isEntitled: Boolean = false): Boolean {
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return false
        if (!canManageCylinder(id, isEntitled) || amount == null || amount <= BigDecimal.ZERO || runCatching { Currency.getInstance(currency) }.isFailure || (replacementCapacity != null && (!replacementCapacity.isFinite() || replacementCapacity <= 0)) || (replacementCapacity == null && replacementUnit != null) || (replacementUnit != null && replacementUnit !in VALID_UNITS)) return false
        val serial = replacementSerial.trim()
        if (serial.isNotEmpty() && current.cylinders.any { it.id != id && it.serial.equals(serial, true) }) return false
        val minor = runCatching { amount.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact() }.getOrNull() ?: return false
        val changed = cylinder.copy(serial = serial.ifEmpty { cylinder.serial }, capacityValue = replacementCapacity ?: cylinder.capacityValue, capacityUnit = replacementUnit ?: cylinder.capacityUnit, status = if (kind == ActivityKind.Refill || kind == ActivityKind.Exchange) CylinderStatus.Ready else cylinder.status)
        val event = Activity(cylinderId = id, kind = kind, occurredAt = date, title = "${cylinder.gas} ${kind.name.lowercase()}", detail = if (serial.isNotEmpty()) "Replacement $serial" else supplierName(cylinder.supplierId), amountMinor = minor, currencyCode = currency)
        return update(current.copy(cylinders = current.cylinders.map { if (it.id == id) changed else it }, activity = listOf(event) + current.activity))
    }

    fun archive(id: String, lifecycle: CylinderLifecycle): Boolean {
        if (lifecycle == CylinderLifecycle.Active) return false
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return false
        if (cylinder.lifecycle != CylinderLifecycle.Active) return false
        val kind = if (lifecycle == CylinderLifecycle.Returned) ActivityKind.Returned else ActivityKind.Archived
        val event = Activity(cylinderId = id, kind = kind, title = "${cylinder.gas} ${lifecycle.name.lowercase()}", detail = "History retained")
        val saved = update(current.copy(cylinders = current.cylinders.map { if (it.id == id) it.copy(lifecycle = lifecycle, reminderAt = null) else it }, activity = listOf(event) + current.activity, freeManagedCylinderIds = current.freeManagedCylinderIds - id))
        if (saved) ReminderReceiver.schedule(appContext, cylinder, null)
        return saved
    }

    fun delete(id: String): Boolean {
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return false
        val saved = update(current.copy(cylinders = current.cylinders.filterNot { it.id == id }, activity = current.activity.filterNot { it.cylinderId == id }, deleted = DeletedCylinder(cylinder, current.activity.filter { it.cylinderId == id }, now() + 15_000), freeManagedCylinderIds = current.freeManagedCylinderIds - id))
        if (saved) ReminderReceiver.schedule(appContext, cylinder, null)
        return saved
    }
    fun undoDelete(): Boolean {
        val current = _state.value; val deleted = current.deleted ?: return false
        if (deleted.expiresAt < now()) return update(current.copy(deleted = null))
        val saved = update(current.copy(cylinders = current.cylinders + deleted.cylinder, activity = (current.activity + deleted.activity).sortedByDescending { it.occurredAt }, deleted = null))
        if (saved) ReminderReceiver.schedule(appContext, deleted.cylinder, deleted.cylinder.reminderAt)
        return saved
    }
    fun expireUndo() { _state.value.deleted?.takeIf { it.expiresAt < now() }?.let { update(_state.value.copy(deleted = null)) } }

    fun setReminder(id: String, date: Long?, isEntitled: Boolean = false): Boolean {
        val current = _state.value
        if (!canManageCylinder(id, isEntitled) || (date != null && date <= now())) return false
        return update(current.copy(cylinders = current.cylinders.map { if (it.id == id) it.copy(reminderAt = date) else it }))
    }
    fun setCurrency(code: String?): Boolean = if (code == null || runCatching { Currency.getInstance(code) }.isSuccess) update(_state.value.copy(currencyOverride = code)) else false
    fun deleteAllData(): Boolean { val old = _state.value.cylinders; val saved = update(WalletState()); if (saved) old.forEach { ReminderReceiver.schedule(appContext, it, null) }; return saved }

    fun totals(cylinderId: String? = null): Map<String, BigDecimal> = _state.value.activity.filter { cylinderId == null || it.cylinderId == cylinderId }.mapNotNull { event -> event.amountMinor?.let { minor -> event.currencyCode?.let { it to BigDecimal(minor).divide(BigDecimal(100)) } } }.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.fold(BigDecimal.ZERO, BigDecimal::add) }
    val refillCount get() = _state.value.activity.count { it.kind == ActivityKind.Refill }
    val averageRefillDays: Int? get() { val intervals = _state.value.activity.filter { it.kind == ActivityKind.Refill }.groupBy { it.cylinderId }.values.flatMap { events -> events.sortedBy { it.occurredAt }.zipWithNext { a, b -> (b.occurredAt - a.occurredAt).toDouble() / 86_400_000 } }; return intervals.takeIf { it.isNotEmpty() }?.average()?.toInt() }
    fun currencySign(code: String): String { val known = mapOf("USD" to "$", "CAD" to "$", "AUD" to "$", "NZD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "CNY" to "¥", "KRW" to "₩", "INR" to "₹", "NGN" to "₦", "RUB" to "₽", "TRY" to "₺", "UAH" to "₴", "THB" to "฿", "PHP" to "₱", "VND" to "₫", "ILS" to "₪", "BDT" to "৳", "IDR" to "Rp", "MYR" to "RM", "BRL" to "R$", "GHS" to "₵", "AED" to "د.إ", "SAR" to "﷼"); return known[code] ?: runCatching { NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(code) }.currency?.symbol ?: "¤" }.getOrDefault("¤") }

    fun exportJson(): String = encode(_state.value.copy(deleted = null, message = null)).toString(2)
    fun restoreJson(json: String, isEntitled: Boolean = false): Boolean {
        if (json.toByteArray().size > 5 * 1024 * 1024) return false
        val restored = runCatching { decodeStrict(json) }.getOrNull() ?: return false
        val activeIds = restored.activeCylinders.mapTo(linkedSetOf()) { it.id }
        val normalized = restored.copy(activity = restored.activity.sortedByDescending { it.occurredAt }, freeManagedCylinderIds = if (!isEntitled && activeIds.size <= FREE_LIMIT) activeIds else emptySet(), message = null)
        val old = _state.value.cylinders
        if (!update(normalized)) return false
        old.forEach { ReminderReceiver.schedule(appContext, it, null) }
        normalized.activeCylinders.forEach { ReminderReceiver.schedule(appContext, it, it.reminderAt) }
        return true
    }
    fun rescheduleReminders() { _state.value.cylinders.forEach { ReminderReceiver.schedule(appContext, it, if (it.lifecycle == CylinderLifecycle.Active) it.reminderAt else null) } }

    private fun update(candidate: WalletState): Boolean {
        val saved = prefs.edit().putString(KEY, encode(candidate.copy(deleted = null, message = null)).toString()).commit()
        _state.value = if (saved) candidate.copy(message = null) else _state.value.copy(message = "The wallet could not be saved. No changes were applied.")
        return saved
    }
    private fun load(): WalletState {
        val raw = prefs.getString(KEY, null) ?: return WalletState()
        return runCatching { decodeStrict(raw) }.getOrElse { prefs.edit().putString(CORRUPT_KEY_PREFIX + now(), raw).commit(); WalletState(message = "Saved wallet data was damaged. A recovery copy was preserved.") }
    }

    private fun encode(state: WalletState) = JSONObject().put("format", "welding-gas-wallet").put("version", 2).put("currencyOverride", state.currencyOverride).put("defaults", JSONObject().put("supplierId", state.defaults.supplierId).put("relationship", state.defaults.relationship.name).put("capacityUnit", state.defaults.capacityUnit)).put("freeManagedCylinderIds", JSONArray(state.freeManagedCylinderIds.toList())).put("suppliers", JSONArray(state.suppliers.map { JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone).put("notes", it.notes) })).put("cylinders", JSONArray(state.cylinders.map { JSONObject().put("id", it.id).put("gas", it.gas).put("capacityValue", it.capacityValue).put("capacityUnit", it.capacityUnit).put("supplierId", it.supplierId).put("relationship", it.relationship.name).put("serial", it.serial).put("status", it.status.name).put("lifecycle", it.lifecycle.name).put("acquiredAt", it.acquiredAt).put("reminderAt", it.reminderAt).put("notes", it.notes) })).put("activity", JSONArray(state.activity.map { JSONObject().put("id", it.id).put("cylinderId", it.cylinderId).put("kind", it.kind.name).put("occurredAt", it.occurredAt).put("title", it.title).put("detail", it.detail).put("amountMinor", it.amountMinor).put("currencyCode", it.currencyCode) }))
    private fun decodeStrict(raw: String): WalletState {
        val root = JSONObject(raw); require(root.optString("format") == "welding-gas-wallet" && root.optInt("version") == 2)
        val suppliers = root.getJSONArray("suppliers").objects().map { Supplier(it.getString("id"), it.getString("name"), it.optString("phone"), it.optString("notes")) }
        val cylinders = root.getJSONArray("cylinders").objects().map { Cylinder(it.getString("id"), it.getString("gas"), it.getDouble("capacityValue"), it.getString("capacityUnit"), it.optNullableString("supplierId"), Relationship.valueOf(it.getString("relationship")), it.optString("serial"), CylinderStatus.valueOf(it.getString("status")), CylinderLifecycle.valueOf(it.getString("lifecycle")), it.getLong("acquiredAt"), it.optNullableLong("reminderAt"), it.optString("notes")) }
        val activity = root.getJSONArray("activity").objects().map { Activity(it.getString("id"), it.getString("cylinderId"), ActivityKind.valueOf(it.getString("kind")), it.getLong("occurredAt"), it.getString("title"), it.optString("detail"), it.optNullableLong("amountMinor"), it.optNullableString("currencyCode")) }
        val defaultsJson = root.optJSONObject("defaults") ?: JSONObject(); val managed = root.optJSONArray("freeManagedCylinderIds")?.strings()?.toSet().orEmpty()
        val state = WalletState(cylinders, suppliers, activity, root.optNullableString("currencyOverride"), CylinderDefaults(defaultsJson.optNullableString("supplierId"), runCatching { Relationship.valueOf(defaultsJson.optString("relationship")) }.getOrDefault(Relationship.NotSet), defaultsJson.optString("capacityUnit", defaultCapacityUnit())), freeManagedCylinderIds = managed)
        requireValid(state); return state.copy(activity = state.activity.sortedByDescending { it.occurredAt })
    }
    private fun requireValid(state: WalletState) {
        val cylinderIds = state.cylinders.map { it.id }; val supplierIds = state.suppliers.map { it.id }; val activityIds = state.activity.map { it.id }
        require(cylinderIds.distinct().size == cylinderIds.size && supplierIds.distinct().size == supplierIds.size && activityIds.distinct().size == activityIds.size)
        val supplierNames = state.suppliers.map { it.name.trim().lowercase() }; require(supplierNames.all { it.isNotEmpty() } && supplierNames.distinct().size == supplierNames.size)
        val serials = state.cylinders.map { it.serial.trim().lowercase() }.filter { it.isNotEmpty() }; require(serials.distinct().size == serials.size)
        require(state.cylinders.all { it.gas.isNotBlank() && it.capacityValue.isFinite() && it.capacityValue > 0 && it.capacityUnit in VALID_UNITS && (it.supplierId == null || it.supplierId in supplierIds) })
        require(state.activity.all { event -> event.cylinderId in cylinderIds && (event.amountMinor == null || event.amountMinor >= 0) && if (event.currencyCode == null) event.amountMinor == null else event.amountMinor != null && runCatching { Currency.getInstance(event.currencyCode) }.isSuccess })
        require(state.defaults.capacityUnit in VALID_UNITS && (state.defaults.supplierId == null || state.defaults.supplierId in supplierIds))
        require(state.currencyOverride == null || runCatching { Currency.getInstance(state.currencyOverride) }.isSuccess)
        require(state.freeManagedCylinderIds.size <= FREE_LIMIT && state.freeManagedCylinderIds.all { id -> state.cylinders.any { it.id == id && it.lifecycle == CylinderLifecycle.Active } })
    }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun JSONObject.optNullableString(key: String) = if (isNull(key) || optString(key).isBlank()) null else optString(key)
    private fun JSONObject.optNullableLong(key: String) = if (isNull(key) || !has(key)) null else getLong(key)
    private companion object { const val KEY = "wallet_json"; const val CORRUPT_KEY_PREFIX = "wallet_corrupt_"; const val FREE_LIMIT = 3; val VALID_UNITS = setOf("ft3", "L", "m3", "kg", "lb") }
}

private fun defaultCapacityUnit() = if (Locale.getDefault().country == "US") "ft3" else "L"
