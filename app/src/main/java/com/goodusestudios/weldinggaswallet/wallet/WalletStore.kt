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
    val id: String = UUID.randomUUID().toString(),
    val gas: String,
    val capacityValue: Double,
    val capacityUnit: String,
    val supplierId: String? = null,
    val relationship: Relationship = Relationship.NotSet,
    val serial: String = "",
    val status: CylinderStatus = CylinderStatus.Ready,
    val lifecycle: CylinderLifecycle = CylinderLifecycle.Active,
    val acquiredAt: Long = System.currentTimeMillis(),
    val reminderAt: Long? = null,
    val notes: String = "",
) {
    val capacityLabel: String get() = "${if (capacityValue % 1.0 == 0.0) capacityValue.toLong() else capacityValue} ${capacityUnit.replace("3", "³")}" 
}

data class Supplier(val id: String = UUID.randomUUID().toString(), val name: String, val phone: String = "", val notes: String = "")
data class Activity(val id: String = UUID.randomUUID().toString(), val cylinderId: String, val kind: ActivityKind, val occurredAt: Long = System.currentTimeMillis(), val title: String, val detail: String, val amountMinor: Long? = null, val currencyCode: String? = null)
data class CylinderDefaults(val supplierId: String? = null, val relationship: Relationship = Relationship.NotSet, val capacityUnit: String = defaultCapacityUnit())
data class DeletedCylinder(val cylinder: Cylinder, val activity: List<Activity>, val expiresAt: Long)
data class WalletState(val cylinders: List<Cylinder> = emptyList(), val suppliers: List<Supplier> = emptyList(), val activity: List<Activity> = emptyList(), val currencyOverride: String? = null, val defaults: CylinderDefaults = CylinderDefaults(), val deleted: DeletedCylinder? = null) {
    val activeCylinders get() = cylinders.filter { it.lifecycle == CylinderLifecycle.Active }
}

class WalletStore(context: Context, private val now: () -> Long = System::currentTimeMillis) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("welding_wallet_v2", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(decode(prefs.getString(KEY, null)))
    val state: StateFlow<WalletState> = _state.asStateFlow()

    val automaticCurrency: String get() = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("USD")
    val defaultCurrency: String get() = _state.value.currencyOverride ?: automaticCurrency

    fun supplierName(id: String?): String = _state.value.suppliers.firstOrNull { it.id == id }?.name ?: "Not set"

    fun addSupplier(name: String, phone: String = "", notes: String = ""): Supplier? {
        val clean = name.trim(); val current = _state.value
        if (clean.isEmpty() || current.suppliers.any { it.name.equals(clean, true) }) return null
        val supplier = Supplier(name = clean, phone = phone.trim(), notes = notes.trim())
        update(current.copy(suppliers = current.suppliers + supplier)); return supplier
    }

    fun addCylinder(gas: String, capacity: Double, unit: String, supplierId: String?, relationship: Relationship, serial: String, notes: String = ""): Cylinder? {
        val current = _state.value; val cleanGas = gas.trim(); val cleanSerial = serial.trim()
        if (cleanGas.isEmpty() || capacity <= 0 || (cleanSerial.isNotEmpty() && current.cylinders.any { it.serial.equals(cleanSerial, true) })) return null
        val cylinder = Cylinder(gas = cleanGas, capacityValue = capacity, capacityUnit = unit, supplierId = supplierId, relationship = relationship, serial = cleanSerial, notes = notes.trim())
        val event = Activity(cylinderId = cylinder.id, kind = ActivityKind.Created, title = "$cleanGas added", detail = "${supplierName(supplierId)} · ${cylinder.capacityLabel}")
        update(current.copy(cylinders = current.cylinders + cylinder, activity = listOf(event) + current.activity, defaults = CylinderDefaults(supplierId, relationship, unit)))
        return cylinder
    }

    fun duplicate(source: Cylinder): Cylinder? = addCylinder(source.gas, source.capacityValue, source.capacityUnit, source.supplierId, source.relationship, "", source.notes)

    fun updateCylinder(cylinder: Cylinder): Boolean {
        val current = _state.value; val cleanSerial = cylinder.serial.trim()
        if (cleanSerial.isNotEmpty() && current.cylinders.any { it.id != cylinder.id && it.serial.equals(cleanSerial, true) }) return false
        if (current.cylinders.none { it.id == cylinder.id }) return false
        update(current.copy(cylinders = current.cylinders.map { if (it.id == cylinder.id) cylinder.copy(serial = cleanSerial) else it })); return true
    }

    fun setStatus(id: String, status: CylinderStatus) {
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return
        if (cylinder.status == status) return
        val changed = cylinder.copy(status = status)
        val event = Activity(cylinderId = id, kind = ActivityKind.Status, title = "${cylinder.gas} marked ${status.name}", detail = "${supplierName(cylinder.supplierId)} · ${cylinder.capacityLabel}")
        update(current.copy(cylinders = current.cylinders.map { if (it.id == id) changed else it }, activity = listOf(event) + current.activity))
    }

    fun recordService(id: String, kind: ActivityKind, amount: BigDecimal?, currency: String, date: Long, replacementSerial: String = "", replacementCapacity: Double? = null, replacementUnit: String? = null): Boolean {
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return false
        if (amount != null && amount <= BigDecimal.ZERO) return false
        val serial = replacementSerial.trim()
        if (serial.isNotEmpty() && current.cylinders.any { it.id != id && it.serial.equals(serial, true) }) return false
        val changed = cylinder.copy(
            serial = serial.ifEmpty { cylinder.serial },
            capacityValue = replacementCapacity?.takeIf { it > 0 } ?: cylinder.capacityValue,
            capacityUnit = replacementUnit ?: cylinder.capacityUnit,
            status = if (kind == ActivityKind.Refill || kind == ActivityKind.Exchange) CylinderStatus.Ready else cylinder.status,
        )
        val minor = amount?.multiply(BigDecimal(100))?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
        val event = Activity(cylinderId = id, kind = kind, occurredAt = date, title = "${cylinder.gas} ${kind.name.lowercase()}", detail = if (serial.isNotEmpty()) "Replacement $serial" else supplierName(cylinder.supplierId), amountMinor = minor, currencyCode = if (minor == null) null else currency)
        update(current.copy(cylinders = current.cylinders.map { if (it.id == id) changed else it }, activity = listOf(event) + current.activity)); return true
    }

    fun archive(id: String, lifecycle: CylinderLifecycle) {
        if (lifecycle == CylinderLifecycle.Active) return
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return
        val kind = if (lifecycle == CylinderLifecycle.Returned) ActivityKind.Returned else ActivityKind.Archived
        val event = Activity(cylinderId = id, kind = kind, title = "${cylinder.gas} ${lifecycle.name.lowercase()}", detail = "History retained")
        ReminderReceiver.schedule(appContext, cylinder, null)
        update(current.copy(cylinders = current.cylinders.map { if (it.id == id) it.copy(lifecycle = lifecycle) else it }, activity = listOf(event) + current.activity))
    }

    fun delete(id: String) {
        val current = _state.value; val cylinder = current.cylinders.firstOrNull { it.id == id } ?: return
        val linked = current.activity.filter { it.cylinderId == id }
        ReminderReceiver.schedule(appContext, cylinder, null)
        update(current.copy(cylinders = current.cylinders.filterNot { it.id == id }, activity = current.activity.filterNot { it.cylinderId == id }, deleted = DeletedCylinder(cylinder, linked, now() + 15_000)))
    }

    fun undoDelete() {
        val current = _state.value; val deleted = current.deleted ?: return
        if (deleted.expiresAt < now()) { update(current.copy(deleted = null)); return }
        update(current.copy(cylinders = current.cylinders + deleted.cylinder, activity = (current.activity + deleted.activity).sortedByDescending { it.occurredAt }, deleted = null))
        ReminderReceiver.schedule(appContext, deleted.cylinder, deleted.cylinder.reminderAt)
    }

    fun setReminder(id: String, date: Long?) { val current = _state.value; update(current.copy(cylinders = current.cylinders.map { if (it.id == id) it.copy(reminderAt = date) else it })) }
    fun setCurrency(code: String?) { update(_state.value.copy(currencyOverride = code)) }
    fun deleteAllData() { _state.value.cylinders.forEach { ReminderReceiver.schedule(appContext, it, null) }; update(WalletState()) }

    fun totals(cylinderId: String? = null): Map<String, BigDecimal> = _state.value.activity.filter { cylinderId == null || it.cylinderId == cylinderId }.mapNotNull { event -> event.amountMinor?.let { minor -> event.currencyCode?.let { it to BigDecimal(minor).divide(BigDecimal(100)) } } }.groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.fold(BigDecimal.ZERO, BigDecimal::add) }
    val refillCount get() = _state.value.activity.count { it.kind == ActivityKind.Refill }
    val averageRefillDays: Int? get() { val intervals = _state.value.activity.filter { it.kind == ActivityKind.Refill }.groupBy { it.cylinderId }.values.flatMap { events -> events.sortedBy { it.occurredAt }.zipWithNext { a, b -> (b.occurredAt - a.occurredAt).toDouble() / 86_400_000 } }; return intervals.takeIf { it.isNotEmpty() }?.average()?.toInt() }

    fun currencySign(code: String): String {
        val known = mapOf("USD" to "$", "CAD" to "$", "AUD" to "$", "NZD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "CNY" to "¥", "KRW" to "₩", "INR" to "₹", "NGN" to "₦", "RUB" to "₽", "TRY" to "₺", "UAH" to "₴", "THB" to "฿", "PHP" to "₱", "VND" to "₫", "ILS" to "₪", "BDT" to "৳", "IDR" to "Rp", "MYR" to "RM", "BRL" to "R$", "GHS" to "₵", "AED" to "د.إ", "SAR" to "﷼")
        return known[code] ?: runCatching { NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(code) }.currency?.symbol ?: "¤" }.getOrDefault("¤")
    }

    fun exportJson(): String = encode(_state.value.copy(deleted = null)).toString(2)
    fun restoreJson(json: String) {
        require(json.toByteArray().size <= 5 * 1024 * 1024)
        val root = JSONObject(json)
        require(root.optString("format") == "welding-gas-wallet" && root.optInt("version") == 2)
        val restored = decode(json)
        update(restored)
        restored.activeCylinders.forEach { ReminderReceiver.schedule(appContext, it, it.reminderAt) }
    }

    private fun update(state: WalletState) { _state.value = state; prefs.edit().putString(KEY, encode(state.copy(deleted = null)).toString()).apply() }

    private fun encode(state: WalletState) = JSONObject().put("format", "welding-gas-wallet").put("version", 2).put("currencyOverride", state.currencyOverride).put("defaults", JSONObject().put("supplierId", state.defaults.supplierId).put("relationship", state.defaults.relationship.name).put("capacityUnit", state.defaults.capacityUnit)).put("suppliers", JSONArray(state.suppliers.map { JSONObject().put("id", it.id).put("name", it.name).put("phone", it.phone).put("notes", it.notes) })).put("cylinders", JSONArray(state.cylinders.map { JSONObject().put("id", it.id).put("gas", it.gas).put("capacityValue", it.capacityValue).put("capacityUnit", it.capacityUnit).put("supplierId", it.supplierId).put("relationship", it.relationship.name).put("serial", it.serial).put("status", it.status.name).put("lifecycle", it.lifecycle.name).put("acquiredAt", it.acquiredAt).put("reminderAt", it.reminderAt).put("notes", it.notes) })).put("activity", JSONArray(state.activity.map { JSONObject().put("id", it.id).put("cylinderId", it.cylinderId).put("kind", it.kind.name).put("occurredAt", it.occurredAt).put("title", it.title).put("detail", it.detail).put("amountMinor", it.amountMinor).put("currencyCode", it.currencyCode) }))

    private fun decode(raw: String?): WalletState {
        if (raw.isNullOrBlank()) return WalletState()
        return runCatching {
            val root = JSONObject(raw)
            require(root.optString("format") == "welding-gas-wallet" && root.optInt("version") == 2)
            val suppliers = root.getJSONArray("suppliers").objects().map { Supplier(it.getString("id"), it.getString("name"), it.optString("phone"), it.optString("notes")) }
            val cylinders = root.getJSONArray("cylinders").objects().map { Cylinder(it.getString("id"), it.getString("gas"), it.getDouble("capacityValue"), it.getString("capacityUnit"), it.optNullableString("supplierId"), Relationship.valueOf(it.getString("relationship")), it.optString("serial"), CylinderStatus.valueOf(it.getString("status")), CylinderLifecycle.valueOf(it.getString("lifecycle")), it.getLong("acquiredAt"), it.optNullableLong("reminderAt"), it.optString("notes")) }
            val activity = root.getJSONArray("activity").objects().map { Activity(it.getString("id"), it.getString("cylinderId"), ActivityKind.valueOf(it.getString("kind")), it.getLong("occurredAt"), it.getString("title"), it.optString("detail"), it.optNullableLong("amountMinor"), it.optNullableString("currencyCode")) }
            val defaultsJson = root.optJSONObject("defaults") ?: JSONObject()
            WalletState(cylinders, suppliers, activity, root.optNullableString("currencyOverride"), CylinderDefaults(defaultsJson.optNullableString("supplierId"), runCatching { Relationship.valueOf(defaultsJson.optString("relationship")) }.getOrDefault(Relationship.NotSet), defaultsJson.optString("capacityUnit", defaultCapacityUnit())))
        }.getOrDefault(WalletState())
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONObject.optNullableString(key: String) = if (isNull(key) || optString(key).isBlank()) null else optString(key)
    private fun JSONObject.optNullableLong(key: String) = if (isNull(key) || !has(key)) null else optLong(key)
    private companion object { const val KEY = "wallet_json" }
}

private fun defaultCapacityUnit() = if (Locale.getDefault().country == "US") "ft3" else "L"
