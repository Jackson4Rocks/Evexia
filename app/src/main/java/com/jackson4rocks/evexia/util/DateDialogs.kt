package com.jackson4rocks.evexia.util

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate

fun showDatePicker(
    context: Context,
    initial: LocalDate,
    onPick: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, day -> onPick(LocalDate.of(year, month + 1, day)) },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth,
    ).show()
}

fun showTimePicker(
    context: Context,
    initialMinutes: Int,
    onPick: (Int) -> Unit,
) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPick(hour * 60 + minute) },
        initialMinutes / 60,
        initialMinutes % 60,
        DateFormat.is24HourFormat(context),
    ).show()
}
