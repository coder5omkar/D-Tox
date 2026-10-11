package com.dtox.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.core.graphics.createBitmap
import com.dtox.app.MainActivity
import com.dtox.app.data.AppInfo

/**
 * Pins a home-screen shortcut that opens D-Tox's breathing gate for [app]
 * instead of the app itself. This is what makes the "app icon" become a
 * 30-second breath — with zero special permissions.
 *
 * The shortcut icon is the app's own icon with a small green ring badge so
 * you can tell the gateway apart from the real app.
 */
fun pinGateShortcut(context: Context, app: AppInfo): Boolean {
    val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
    if (!manager.isRequestPinShortcutSupported) return false

    val shortcut = ShortcutInfo.Builder(context, "gate_" + app.pkg)
        .setShortLabel(app.label.take(12))
        .setIcon(Icon.createWithBitmap(shortcutBitmap(app)))
        .setIntent(
            Intent(context, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .putExtra(MainActivity.EXTRA_GATE_PACKAGE, app.pkg),
        )
        .build()

    return runCatching { manager.requestPinShortcut(shortcut, null) }.getOrDefault(false)
}

private fun shortcutBitmap(app: AppInfo): Bitmap {
    val size = 192
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    val inset = (size * 0.12f).toInt()
    app.icon.setBounds(inset, inset, size - inset, size - inset)
    app.icon.draw(canvas)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = size * 0.76f
    val cy = size * 0.76f
    paint.color = 0xFF7FD1AE.toInt()
    canvas.drawCircle(cx, cy, size * 0.155f, paint)
    paint.color = 0xFF0A1210.toInt()
    canvas.drawCircle(cx, cy, size * 0.085f, paint)

    return bitmap
}
