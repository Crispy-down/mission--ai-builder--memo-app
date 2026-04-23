package org.example.project.tools

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

/**
 * Good Vibe Memo 배포용 아이콘(.ico/.icns/.png) 생성기.
 *
 * MemoAppIcon과 동일한 디자인(어두운 둥근 배경 + 흰색 메모 페이지 + 주황 바인딩 링 + 파란/회색 라인)을
 * AWT Graphics2D로 재현해 여러 해상도의 BufferedImage를 만들고, 각 플랫폼 네이티브 아이콘 포맷으로 패킹한다.
 *
 * 사용:
 *   ./gradlew :composeApp:generateAppIcons
 * 또는 package* 태스크가 자동으로 호출.
 */
fun main(args: Array<String>) {
    val outDir = File(args.getOrNull(0) ?: "src/jvmMain/resources")
    if (!outDir.exists()) outDir.mkdirs()

    val icoSizes = listOf(16, 24, 32, 48, 64, 128, 256)
    val icnsSizes = listOf(16, 32, 64, 128, 256, 512)
    val allSizes = (icoSizes + icnsSizes).toSortedSet()

    val images = allSizes.associateWith { renderMemoIcon(it) }

    // Linux: 단일 PNG
    val linuxPng = File(outDir, "icon.png")
    ImageIO.write(images.getValue(256), "png", linuxPng)
    println("✔ ${linuxPng.absolutePath} (256×256)")

    // Windows: .ico (PNG-in-ICO, 여러 사이즈)
    val icoFile = File(outDir, "icon.ico")
    writeIco(icoFile, icoSizes.map { it to pngBytes(images.getValue(it)) })
    println("✔ ${icoFile.absolutePath} (${icoSizes.joinToString()})")

    // macOS: .icns (PNG 청크 여러 개)
    val icnsFile = File(outDir, "icon.icns")
    writeIcns(icnsFile, images.filterKeys { it in icnsSizes })
    println("✔ ${icnsFile.absolutePath} (${icnsSizes.joinToString()})")

    println("\n모든 아이콘 생성 완료 → $outDir")
}

// ──────── 렌더링 ────────

private fun renderMemoIcon(size: Int): BufferedImage {
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

        val w = size.toFloat()
        val h = size.toFloat()

        // 1. 어두운 둥근 배경
        g.color = Color(0x1E, 0x1E, 0x1E)
        g.fill(RoundRectangle2D.Float(0f, 0f, w, h, w * 0.36f, h * 0.36f))

        // 2. 상단 파란 액센트 라인
        g.color = Color(0x00, 0x7A, 0xCC)
        g.fill(RoundRectangle2D.Float(w * 0.18f, h * 0.07f, w * 0.64f, h * 0.018f, h * 0.018f, h * 0.018f))

        // 3. 흰색 메모 페이지
        val pageL = w * 0.18f
        val pageT = h * 0.20f
        val pageR = w * 0.82f
        val pageB = h * 0.86f
        g.color = Color(0xFA, 0xFA, 0xFA)
        g.fill(RoundRectangle2D.Float(pageL, pageT, pageR - pageL, pageB - pageT, w * 0.10f, h * 0.10f))

        // 4. 주황 바인딩 링 3개
        val bindingY = h * 0.185f
        val br = w * 0.028f
        g.color = Color(0xFF, 0x9F, 0x1C)
        for (i in 0..2) {
            val cx = w * (0.33f + i * 0.17f)
            g.fill(Ellipse2D.Float(cx - br, bindingY - br, br * 2, br * 2))
        }

        // 5/6. 텍스트 라인 (제목 파랑 + 본문 회색 3줄)
        val textL = pageL + w * 0.06f
        val textMaxW = (pageR - pageL) - w * 0.12f
        val lt = h * 0.032f
        val lr = lt

        g.color = Color(0x00, 0x7A, 0xCC)
        g.fill(RoundRectangle2D.Float(textL, h * 0.36f, textMaxW * 0.55f, lt, lr, lr))

        g.color = Color(0x9E, 0x9E, 0x9E)
        listOf(0.49f to 1.00f, 0.60f to 0.90f, 0.71f to 0.55f).forEach { (y, fw) ->
            g.fill(RoundRectangle2D.Float(textL, h * y, textMaxW * fw, lt, lr, lr))
        }
    } finally {
        g.dispose()
    }
    return img
}

private fun pngBytes(img: BufferedImage): ByteArray {
    val baos = ByteArrayOutputStream()
    ImageIO.write(img, "png", baos)
    return baos.toByteArray()
}

// ──────── .ico 포맷 writer (PNG-in-ICO) ────────
// https://en.wikipedia.org/wiki/ICO_(file_format)

private fun writeIco(outFile: File, entries: List<Pair<Int, ByteArray>>) {
    DataOutputStream(FileOutputStream(outFile)).use { out ->
        writeShortLE(out, 0)                // reserved
        writeShortLE(out, 1)                // type = 1 (ICO)
        writeShortLE(out, entries.size)     // count

        var offset = 6 + 16 * entries.size
        for ((size, data) in entries) {
            val wh = if (size >= 256) 0 else size    // 0 == 256
            out.writeByte(wh)                // bWidth
            out.writeByte(wh)                // bHeight
            out.writeByte(0)                 // bColorCount
            out.writeByte(0)                 // bReserved
            writeShortLE(out, 1)             // wPlanes
            writeShortLE(out, 32)            // wBitCount
            writeIntLE(out, data.size)       // dwBytesInRes
            writeIntLE(out, offset)          // dwImageOffset
            offset += data.size
        }
        for ((_, data) in entries) out.write(data)
    }
}

private fun writeShortLE(out: DataOutputStream, v: Int) {
    out.writeByte(v and 0xFF)
    out.writeByte((v shr 8) and 0xFF)
}

private fun writeIntLE(out: DataOutputStream, v: Int) {
    out.writeByte(v and 0xFF)
    out.writeByte((v shr 8) and 0xFF)
    out.writeByte((v shr 16) and 0xFF)
    out.writeByte((v shr 24) and 0xFF)
}

// ──────── .icns 포맷 writer (PNG 청크) ────────
// https://en.wikipedia.org/wiki/Apple_Icon_Image_format

private fun writeIcns(outFile: File, images: Map<Int, BufferedImage>) {
    val sizeToType = linkedMapOf(
        16 to "icp4",
        32 to "icp5",
        64 to "icp6",
        128 to "ic07",
        256 to "ic08",
        512 to "ic09",
    )
    val chunks = mutableListOf<Pair<String, ByteArray>>()
    for ((size, type) in sizeToType) {
        images[size]?.let { chunks += type to pngBytes(it) }
    }

    val chunksSize = chunks.sumOf { 8 + it.second.size }
    val totalSize = 8 + chunksSize

    DataOutputStream(FileOutputStream(outFile)).use { out ->
        out.writeBytes("icns")               // magic (4 bytes)
        out.writeInt(totalSize)              // total file size (big-endian)
        for ((type, data) in chunks) {
            out.writeBytes(type)             // type tag (4 bytes)
            out.writeInt(8 + data.size)      // chunk size incl. header (big-endian)
            out.write(data)
        }
    }
}
