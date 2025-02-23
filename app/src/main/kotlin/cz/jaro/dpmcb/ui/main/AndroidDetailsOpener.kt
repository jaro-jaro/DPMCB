package cz.jaro.dpmcb.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import kotlin.system.exitProcess

class AndroidDetailsOpener(
    private val ctx: Context,
) : DetailsOpener {
    override fun openAppDetails() {
        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        })
        exitProcess(0)
    }
}