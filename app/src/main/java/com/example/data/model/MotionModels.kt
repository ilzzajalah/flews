package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

@IgnoreExtraProperties
data class ProjectEntity(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val aspectRatio: String = "16:9",
    val width: Long = 1920L,
    val height: Long = 1080L,
    val fps: Long = 30L,
    val durationMs: Long = 5000L,
    val backgroundColor: String = "#0A0F1E",
    val layersJson: String = "[]",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class LayerType {
    SHAPE,
    TEXT,
    MEDIA,
    NULL_OBJECT
}

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    LINE
}

enum class MotionBlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    ADD
}

enum class KeyframeProperty {
    POSITION_X,
    POSITION_Y,
    SCALE_X,
    SCALE_Y,
    ROTATION,
    OPACITY,
    EFFECT_VALUE
}

enum class EasingType {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT
}

enum class EffectType(
    val displayName: String,
    val minVal: Float,
    val maxVal: Float,
    val defaultVal: Float,
    val category: String = "Visual"
) {
    // 17 Visual Effects
    BLUR("Blur", 0f, 25f, 5f, "Visual"),
    BRIGHTNESS("Brightness", -1f, 1f, 0.2f, "Visual"),
    CONTRAST("Contrast", 0f, 2f, 1.2f, "Visual"),
    SATURATION("Saturation", 0f, 2f, 1.5f, "Visual"),
    HUE("Hue", -180f, 180f, 45f, "Visual"),
    VIGNETTE("Vignette", 0f, 1f, 0.5f, "Visual"),
    GLITCH("Glitch", 0f, 1f, 0.3f, "Visual"),
    SHAKE("Shake", 0f, 50f, 15f, "Visual"),
    TINT("Tint", 0f, 1f, 0.5f, "Visual"),
    CHROMATIC("Chromatic Aberration", 0f, 20f, 5f, "Visual"),
    POSTERIZE("Posterize", 2f, 32f, 8f, "Visual"),
    INVERT("Invert", 0f, 1f, 1f, "Visual"),
    NOISE("Noise", 0f, 1f, 0.15f, "Visual"),
    DUOTONE("Duotone", 0f, 1f, 0.8f, "Visual"),
    PIXELATE("Pixelate", 1f, 50f, 10f, "Visual"),
    GLOW("Glow", 0f, 30f, 10f, "Visual"),
    SHADOW("Drop Shadow", 0f, 40f, 12f, "Visual"),

    // 8 Transform Effects
    WARP("Warp", -50f, 50f, 15f, "Transform"),
    SKEW("Skew", -45f, 45f, 10f, "Transform"),
    SEPARATE_RGB("RGB Split", 0f, 30f, 8f, "Transform"),
    MIRROR("Mirror", 0f, 1f, 1f, "Transform"),
    RADIAL_BLUR("Radial Blur", 0f, 30f, 8f, "Transform"),
    FISHEYE("Fisheye", -1f, 1f, 0.4f, "Transform"),
    WAVE_WARP("Wave Warp", 0f, 50f, 12f, "Transform"),
    DISPLACEMENT("Displacement", 0f, 50f, 15f, "Transform"),

    // 10 Ayunan Motion Effects (Procedural Oscillations)
    OMBANG_AMBING("Ombang-ambing", 0f, 50f, 5f, "Ayunan"),
    AYUN("Ayun", 0f, 90f, 15f, "Ayunan"),
    GOYANG("Goyang", 0f, 50f, 10f, "Ayunan"),
    MENGAMBANG("Mengambang", 0f, 50f, 4f, "Ayunan"),
    OLENG("Oleng", 0f, 45f, 8f, "Ayunan"),
    JELLY("Jelly", 0f, 1f, 0.15f, "Ayunan"),
    PEGAS("Pegas", 0f, 50f, 8f, "Ayunan"),
    ORBIT("Orbit", 0f, 50f, 6f, "Ayunan"),
    PUTAR("Putar", 0f, 360f, 90f, "Ayunan"),
    MELAYANG("Melayang", 0f, 50f, 5f, "Ayunan")
}

data class MotionKeyframe(
    val id: String,
    val property: KeyframeProperty,
    val timeMs: Long,
    val value: Float,
    val easing: EasingType = EasingType.LINEAR
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("property", property.name)
        put("timeMs", timeMs)
        put("value", value.toDouble())
        put("easing", easing.name)
    }

    companion object {
        fun fromJson(json: JSONObject): MotionKeyframe = MotionKeyframe(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            property = KeyframeProperty.valueOf(json.optString("property", KeyframeProperty.POSITION_X.name)),
            timeMs = json.optLong("timeMs", 0L),
            value = json.optDouble("value", 0.0).toFloat(),
            easing = EasingType.valueOf(json.optString("easing", EasingType.LINEAR.name))
        )
    }
}

data class MotionEffect(
    val id: String,
    val type: EffectType,
    val value: Float,
    val isEnabled: Boolean = true,
    val colorHex: String = "#00E5FF"
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("value", value.toDouble())
        put("isEnabled", isEnabled)
        put("colorHex", colorHex)
    }

    companion object {
        fun fromJson(json: JSONObject): MotionEffect = MotionEffect(
            id = json.optString("id", java.util.UUID.randomUUID().toString()),
            type = EffectType.valueOf(json.optString("type", EffectType.BLUR.name)),
            value = json.optDouble("value", 0.0).toFloat(),
            isEnabled = json.optBoolean("isEnabled", true),
            colorHex = json.optString("colorHex", "#00E5FF")
        )
    }
}

data class MotionLayer(
    val id: String,
    val name: String,
    val type: LayerType,
    val inTimeMs: Long = 0L,
    val outTimeMs: Long = 5000L,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1.0f,
    val blendMode: MotionBlendMode = MotionBlendMode.NORMAL,
    val parentId: String? = null,
    // Base transform (at time=0 or default without keyframes)
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotationDeg: Float = 0f,
    // Shape specifics
    val shapeType: ShapeType = ShapeType.RECTANGLE,
    val fillColor: String = "#2F6BFF",
    val strokeColor: String = "#5C8DFF",
    val strokeWidth: Float = 0f,
    val width: Float = 240f,
    val height: Float = 240f,
    // Text specifics
    val text: String = "Teks Baru",
    val fontSize: Float = 36f,
    val textColor: String = "#F2F5FF",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    // Media specifics
    val mediaUri: String? = null,
    val isVideo: Boolean = false,
    // Animation keyframes & effects
    val keyframes: List<MotionKeyframe> = emptyList(),
    val effects: List<MotionEffect> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type.name)
        put("inTimeMs", inTimeMs)
        put("outTimeMs", outTimeMs)
        put("isVisible", isVisible)
        put("isLocked", isLocked)
        put("opacity", opacity.toDouble())
        put("blendMode", blendMode.name)
        put("parentId", parentId ?: JSONObject.NULL)
        put("positionX", positionX.toDouble())
        put("positionY", positionY.toDouble())
        put("scaleX", scaleX.toDouble())
        put("scaleY", scaleY.toDouble())
        put("rotationDeg", rotationDeg.toDouble())
        put("shapeType", shapeType.name)
        put("fillColor", fillColor)
        put("strokeColor", strokeColor)
        put("strokeWidth", strokeWidth.toDouble())
        put("width", width.toDouble())
        put("height", height.toDouble())
        put("text", text)
        put("fontSize", fontSize.toDouble())
        put("textColor", textColor)
        put("isBold", isBold)
        put("isItalic", isItalic)
        put("mediaUri", mediaUri ?: JSONObject.NULL)
        put("isVideo", isVideo)
        val kfArray = JSONArray()
        keyframes.forEach { kfArray.put(it.toJson()) }
        put("keyframes", kfArray)
        val fxArray = JSONArray()
        effects.forEach { fxArray.put(it.toJson()) }
        put("effects", fxArray)
    }

    companion object {
        fun fromJson(json: JSONObject): MotionLayer {
            val kfList = mutableListOf<MotionKeyframe>()
            val kfArray = json.optJSONArray("keyframes")
            if (kfArray != null) {
                for (i in 0 until kfArray.length()) {
                    kfList.add(MotionKeyframe.fromJson(kfArray.getJSONObject(i)))
                }
            }

            val fxList = mutableListOf<MotionEffect>()
            val fxArray = json.optJSONArray("effects")
            if (fxArray != null) {
                for (i in 0 until fxArray.length()) {
                    fxList.add(MotionEffect.fromJson(fxArray.getJSONObject(i)))
                }
            }

            return MotionLayer(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Layer"),
                type = LayerType.valueOf(json.optString("type", LayerType.SHAPE.name)),
                inTimeMs = json.optLong("inTimeMs", 0L),
                outTimeMs = json.optLong("outTimeMs", 5000L),
                isVisible = json.optBoolean("isVisible", true),
                isLocked = json.optBoolean("isLocked", false),
                opacity = json.optDouble("opacity", 1.0).toFloat(),
                blendMode = MotionBlendMode.valueOf(json.optString("blendMode", MotionBlendMode.NORMAL.name)),
                parentId = if (json.isNull("parentId")) null else json.optString("parentId"),
                positionX = json.optDouble("positionX", 0.0).toFloat(),
                positionY = json.optDouble("positionY", 0.0).toFloat(),
                scaleX = json.optDouble("scaleX", 1.0).toFloat(),
                scaleY = json.optDouble("scaleY", 1.0).toFloat(),
                rotationDeg = json.optDouble("rotationDeg", 0.0).toFloat(),
                shapeType = ShapeType.valueOf(json.optString("shapeType", ShapeType.RECTANGLE.name)),
                fillColor = json.optString("fillColor", "#2F6BFF"),
                strokeColor = json.optString("strokeColor", "#5C8DFF"),
                strokeWidth = json.optDouble("strokeWidth", 0.0).toFloat(),
                width = json.optDouble("width", 240.0).toFloat(),
                height = json.optDouble("height", 240.0).toFloat(),
                text = json.optString("text", "Teks Baru"),
                fontSize = json.optDouble("fontSize", 36.0).toFloat(),
                textColor = json.optString("textColor", "#F2F5FF"),
                isBold = json.optBoolean("isBold", false),
                isItalic = json.optBoolean("isItalic", false),
                mediaUri = if (json.isNull("mediaUri")) null else json.optString("mediaUri"),
                isVideo = json.optBoolean("isVideo", false),
                keyframes = kfList,
                effects = fxList
            )
        }
    }
}

object LayerJsonSerializer {
    fun serialize(layers: List<MotionLayer>): String {
        val array = JSONArray()
        layers.forEach { array.put(it.toJson()) }
        return array.toString()
    }

    fun deserialize(jsonStr: String): List<MotionLayer> {
        if (jsonStr.isBlank()) return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MotionLayer>()
            for (i in 0 until array.length()) {
                list.add(MotionLayer.fromJson(array.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
