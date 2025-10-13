package com.example.expensetracker

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AutoSaveTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        Settings.toggleAutoSave(this)
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val on = Settings.isAutoSaveOn(this)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (on) "Auto-save ON" else "Auto-save OFF"
        tile.updateTile()
    }
}
