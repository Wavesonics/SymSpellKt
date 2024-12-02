import okio.FileSystem
import okio.Path

internal val fs = FileSystem.SYSTEM

internal fun Path.validatePath(): Boolean {
	return fs.metadataOrNull(this)?.isDirectory == true || fs.metadataOrNull(this)?.isRegularFile == true
}