package app.common

import app.models.accounting.Transaction.Attachment

object AttachmentFormatting {
  def getUrl(attachment: Attachment): String = {
    val typeEncoded = attachment.fileType.replace('/', '>')
    f"/attachments/${attachment.contentHash}/$typeEncoded/${attachment.filename}"
  }

  def formatBytes(fileSizeBytes: Int): String = {
    if (fileSizeBytes < 1024){
      f"${fileSizeBytes}B"
    } else if (fileSizeBytes< 1024*1024){
      f"${fileSizeBytes/1024}kB"
    } else {
      f"${fileSizeBytes/1024/1024}MB"
    }
  }
}
