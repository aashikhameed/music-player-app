package com.aashik.music.desktop.utils

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser

object DesktopFolderPicker {
    /**
     * Opens the native macOS Finder directory chooser dialog.
     * Falls back to Swing JFileChooser if AWT FileDialog is unsupported.
     */
    fun pickFolder(initialPath: String? = null): String? {
        return try {
            System.setProperty("apple.awt.fileDialogForDirectories", "true")
            val dialog = FileDialog(null as Frame?, "Select Music / Offline Storage Folder", FileDialog.LOAD)
            if (!initialPath.isNullOrBlank()) {
                val initFile = File(initialPath)
                if (initFile.exists()) {
                    dialog.directory = initFile.absolutePath
                }
            }
            dialog.isVisible = true
            val chosenDir = dialog.directory
            val chosenFile = dialog.file
            System.setProperty("apple.awt.fileDialogForDirectories", "false")

            if (chosenDir != null) {
                if (chosenFile != null) {
                    val candidate = File(chosenDir, chosenFile)
                    if (candidate.isDirectory) candidate.absolutePath else chosenDir
                } else {
                    chosenDir
                }
            } else null
        } catch (e: Exception) {
            println("[DesktopFolderPicker] Native FileDialog notice: ${e.message}")
            try {
                val chooser = JFileChooser()
                chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                chooser.dialogTitle = "Select Music / Offline Storage Folder"
                if (!initialPath.isNullOrBlank()) {
                    chooser.currentDirectory = File(initialPath)
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile?.absolutePath
                } else null
            } catch (ex: Exception) {
                println("[DesktopFolderPicker] JFileChooser fallback error: ${ex.message}")
                null
            }
        }
    }
}

