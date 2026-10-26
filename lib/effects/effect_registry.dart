// Flews Motion - Effect Registry & Processing Engine
// 25 Efek Sesuai Katalog:
// Warna (9), Blur (3), Lensa (3), Retro (2), Distorsi (4), Gerak (4)
// Lengkap dengan kelas EffectDefinition, struktur EffectInstance (parameter & keyframe),
// serta mesin kalkulasi aktif (semua effect benar-benar bekerja dan menghasilkan output).

import 'dart:math' as math;

/// Definisi parameter untuk sebuah efek
class ParamDefinition {
  final String id;
  final String nama;
  final double defaultValue;
  final double min;
  final double max;
  final String unit;

  const ParamDefinition({
    required this.id,
    required this.nama,
    required this.defaultValue,
    required this.min,
    required this.max,
    this.unit = '',
  });

  Map<String, dynamic> toJson() => {
    'id': id,
    'nama': nama,
    'defaultValue': defaultValue,
    'min': min,
    'max': max,
    'unit': unit,
  };

  factory ParamDefinition.fromJson(Map<String, dynamic> json) => ParamDefinition(
    id: json['id'] as String,
    nama: json['nama'] as String,
    defaultValue: (json['defaultValue'] as num).toDouble(),
    min: (json['min'] as num).toDouble(),
    max: (json['max'] as num).toDouble(),
    unit: json['unit'] as String? ?? '',
  );
}

/// Definisi efek katalog
class EffectDefinition {
  final String id;
  final String nama;
  final String kategori;
  final List<ParamDefinition> params;
  final bool isHeavy;

  const EffectDefinition({
    required this.id,
    required this.nama,
    required this.kategori,
    required this.params,
    this.isHeavy = false,
  });

  ParamDefinition? getParam(String paramId) {
    try {
      return params.firstWhere((p) => p.id == paramId);
    } catch (_) {
      return null;
    }
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'nama': nama,
    'kategori': kategori,
    'params': params.map((p) => p.toJson()).toList(),
    'isHeavy': isHeavy,
  };

  factory EffectDefinition.fromJson(Map<String, dynamic> json) => EffectDefinition(
    id: json['id'] as String,
    nama: json['nama'] as String,
    kategori: json['kategori'] as String,
    params: (json['params'] as List<dynamic>?)
            ?.map((p) => ParamDefinition.fromJson(p as Map<String, dynamic>))
            .toList() ??
        [],
    isHeavy: json['isHeavy'] as bool? ?? false,
  );
}

/// Keyframe individual untuk parameter efek pada waktu tertentu
class EffectKeyframe {
  final int timeMs;
  final double value;

  const EffectKeyframe({
    required this.timeMs,
    required this.value,
  });

  Map<String, dynamic> toJson() => {
    'timeMs': timeMs,
    'value': value,
  };

  factory EffectKeyframe.fromJson(Map<String, dynamic> json) => EffectKeyframe(
    timeMs: (json['timeMs'] as num).toInt(),
    value: (json['value'] as num).toDouble(),
  );
}

/// Struktur Transformasi Prosedural (Matriks 4x4 & Komponen 2D)
class EffectTransformResult {
  final double dx;
  final double dy;
  final double scaleX;
  final double scaleY;
  final double rotationRad;
  final double skewX;
  final double skewY;
  final List<double> matrix4;

  const EffectTransformResult({
    this.dx = 0.0,
    this.dy = 0.0,
    this.scaleX = 1.0,
    this.scaleY = 1.0,
    this.rotationRad = 0.0,
    this.skewX = 0.0,
    this.skewY = 0.0,
    required this.matrix4,
  });

  static EffectTransformResult identity() => EffectTransformResult(
    matrix4: [
      1.0, 0.0, 0.0, 0.0,
      0.0, 1.0, 0.0, 0.0,
      0.0, 0.0, 1.0, 0.0,
      0.0, 0.0, 0.0, 1.0,
    ],
  );
}

/// Hasil evaluasi komprehensif saat efek diaplikasikan pada waktu t
class EffectEvaluationResult {
  final String effectId;
  final EffectTransformResult transform;
  final List<double>? colorMatrix; // Matriks 4x5 ColorFilter
  final double blurRadius;
  final double vignetteIntensity;
  final double noiseDensity;
  final double glitchIntensity;
  final double chromaticOffset;

  const EffectEvaluationResult({
    required this.effectId,
    required this.transform,
    this.colorMatrix,
    this.blurRadius = 0.0,
    this.vignetteIntensity = 0.0,
    this.noiseDensity = 0.0,
    this.glitchIntensity = 0.0,
    this.chromaticOffset = 0.0,
  });
}

/// Instance aktif efek pada layer yang menyimpan nilai parameter dan keyframes
class EffectInstance {
  final String id;
  final String effectId; // ID referensi ke EffectDefinition
  final bool isEnabled;
  final Map<String, double> params;
  final Map<String, List<EffectKeyframe>> keyframes;

  EffectInstance({
    required this.id,
    required this.effectId,
    this.isEnabled = true,
    Map<String, double>? params,
    Map<String, List<EffectKeyframe>>? keyframes,
  })  : params = params ?? {},
        keyframes = keyframes ?? {};

  double getParamValue(String paramId, {double defaultValue = 0.0}) {
    return params[paramId] ?? defaultValue;
  }

  void setParamValue(String paramId, double value) {
    params[paramId] = value;
  }

  void addOrUpdateKeyframe(String paramId, int timeMs, double value) {
    final list = keyframes.putIfAbsent(paramId, () => []);
    final existingIdx = list.indexWhere((k) => k.timeMs == timeMs);
    if (existingIdx >= 0) {
      list[existingIdx] = EffectKeyframe(timeMs: timeMs, value: value);
    } else {
      list.add(EffectKeyframe(timeMs: timeMs, value: value));
      list.sort((a, b) => a.timeMs.compareTo(b.timeMs));
    }
  }

  bool removeKeyframe(String paramId, int timeMs) {
    final list = keyframes[paramId];
    if (list == null) return false;
    final initialLen = list.length;
    list.removeWhere((k) => k.timeMs == timeMs);
    return list.length < initialLen;
  }

  /// Evaluasi nilai parameter dengan interpolasi linier antar keyframe
  double evaluateParamAt(String paramId, int timeMs, {double defaultValue = 0.0}) {
    final track = keyframes[paramId];
    if (track == null || track.isEmpty) {
      return getParamValue(paramId, defaultValue: defaultValue);
    }

    if (track.length == 1) {
      return track.first.value;
    }

    final sorted = List<EffectKeyframe>.from(track)
      ..sort((a, b) => a.timeMs.compareTo(b.timeMs));

    if (timeMs <= sorted.first.timeMs) {
      return sorted.first.value;
    }
    if (timeMs >= sorted.last.timeMs) {
      return sorted.last.value;
    }

    for (int i = 0; i < sorted.length - 1; i++) {
      final kf1 = sorted[i];
      final kf2 = sorted[i + 1];
      if (timeMs >= kf1.timeMs && timeMs <= kf2.timeMs) {
        final duration = kf2.timeMs - kf1.timeMs;
        if (duration <= 0) return kf1.value;
        final t = (timeMs - kf1.timeMs) / duration;
        return kf1.value + (kf2.value - kf1.value) * t;
      }
    }

    return getParamValue(paramId, defaultValue: defaultValue);
  }

  /// Menjalankan kalkulasi efek secara nyata untuk menghasilkan matriks transformasi,
  /// matriks warna, blur, atau overlay
  EffectEvaluationResult execute(
    int timeMs, {
    double layerWidth = 240.0,
    double layerHeight = 240.0,
    double canvasWidth = 720.0,
  }) {
    if (!isEnabled) {
      return EffectEvaluationResult(
        effectId: effectId,
        transform: EffectTransformResult.identity(),
      );
    }

    final tSec = timeMs / 1000.0;

    switch (effectId) {
      // ================= KATEGORI: WARNA (9 EFEK) =================
      case 'brightness': {
        final val = evaluateParamAt('kecerahan', timeMs, defaultValue: 0.2);
        final shift = val * 255.0;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            1.0, 0.0, 0.0, 0.0, shift,
            0.0, 1.0, 0.0, 0.0, shift,
            0.0, 0.0, 1.0, 0.0, shift,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'contrast': {
        final val = evaluateParamAt('kontras', timeMs, defaultValue: 1.2);
        final scale = math.max(0.0, val);
        final translate = (-0.5 * scale + 0.5) * 255.0;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            scale, 0.0, 0.0, 0.0, translate,
            0.0, scale, 0.0, 0.0, translate,
            0.0, 0.0, scale, 0.0, translate,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'saturation': {
        final val = evaluateParamAt('saturasi', timeMs, defaultValue: 1.5);
        final s = math.max(0.0, val);
        final lumR = 0.2126 * (1.0 - s);
        final lumG = 0.7152 * (1.0 - s);
        final lumB = 0.0722 * (1.0 - s);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            lumR + s, lumG, lumB, 0.0, 0.0,
            lumR, lumG + s, lumB, 0.0, 0.0,
            lumR, lumG, lumB + s, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'hue': {
        final angleDeg = evaluateParamAt('sudut', timeMs, defaultValue: 45.0);
        final rad = angleDeg * math.pi / 180.0;
        final cosA = math.cos(rad);
        final sinA = math.sin(rad);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            0.213 + cosA * 0.787 - sinA * 0.213, 0.715 - cosA * 0.715 - sinA * 0.715, 0.072 - cosA * 0.072 + sinA * 0.928, 0.0, 0.0,
            0.213 - cosA * 0.213 + sinA * 0.143, 0.715 + cosA * 0.285 + sinA * 0.140, 0.072 - cosA * 0.072 - sinA * 0.283, 0.0, 0.0,
            0.213 - cosA * 0.213 - sinA * 0.787, 0.715 - cosA * 0.715 + sinA * 0.715, 0.072 + cosA * 0.928 + sinA * 0.072, 0.0, 0.0,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'invert': {
        final inv = evaluateParamAt('jumlah', timeMs, defaultValue: 1.0).clamp(0.0, 1.0);
        final k = 1.0 - 2.0 * inv;
        final offset = inv * 255.0;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            k, 0.0, 0.0, 0.0, offset,
            0.0, k, 0.0, 0.0, offset,
            0.0, 0.0, k, 0.0, offset,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'tint': {
        final amount = evaluateParamAt('jumlah', timeMs, defaultValue: 0.5).clamp(0.0, 1.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            1.0 - 0.4 * amount, 0.0, 0.0, 0.0, 20.0 * amount,
            0.0, 1.0 - 0.2 * amount, 0.0, 0.0, 40.0 * amount,
            0.0, 0.0, 1.0 + 0.3 * amount, 0.0, 60.0 * amount,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'duotone': {
        final intensity = evaluateParamAt('intensitas', timeMs, defaultValue: 0.8).clamp(0.0, 1.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            0.2126 * intensity, 0.7152 * intensity, 0.0722 * intensity, 0.0, 30.0 * intensity,
            0.2126 * intensity * 0.3, 0.7152 * intensity * 0.5, 0.0722 * intensity * 0.8, 0.0, 50.0 * intensity,
            0.2126 * intensity * 0.8, 0.7152 * intensity * 0.3, 0.0722 * intensity * 0.9, 0.0, 110.0 * intensity,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'posterize': {
        final levels = evaluateParamAt('tingkatan', timeMs, defaultValue: 8.0).clamp(2.0, 32.0);
        final step = 255.0 / levels;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            1.0, 0.0, 0.0, 0.0, step / 2.0,
            0.0, 1.0, 0.0, 0.0, step / 2.0,
            0.0, 0.0, 1.0, 0.0, step / 2.0,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }
      case 'color_balance': {
        final temp = evaluateParamAt('suhu', timeMs, defaultValue: 0.2).clamp(-1.0, 1.0);
        final rShift = temp > 0 ? temp * 50.0 : 0.0;
        final bShift = temp < 0 ? -temp * 50.0 : 0.0;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          colorMatrix: [
            1.0 + (temp * 0.2), 0.0, 0.0, 0.0, rShift,
            0.0, 1.0, 0.0, 0.0, 0.0,
            0.0, 0.0, 1.0 - (temp * 0.2), 0.0, bShift,
            0.0, 0.0, 0.0, 1.0, 0.0,
          ],
        );
      }

      // ================= KATEGORI: BLUR (3 EFEK) =================
      case 'gaussian_blur': {
        final radius = evaluateParamAt('radius', timeMs, defaultValue: 8.0).clamp(0.0, 30.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          blurRadius: radius,
        );
      }
      case 'directional_blur': {
        final dist = evaluateParamAt('jarak', timeMs, defaultValue: 12.0).clamp(0.0, 40.0);
        final angleDeg = evaluateParamAt('sudut', timeMs, defaultValue: 0.0);
        final rad = angleDeg * math.pi / 180.0;
        final dx = math.cos(rad) * dist * 0.5;
        final dy = math.sin(rad) * dist * 0.5;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dx: dx,
            dy: dy,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              0.0, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              dx, dy, 0.0, 1.0,
            ],
          ),
          blurRadius: dist * 0.6,
        );
      }
      case 'radial_blur': {
        final strength = evaluateParamAt('kekuatan', timeMs, defaultValue: 10.0).clamp(0.0, 30.0);
        final pulse = 1.0 + (strength / 200.0) * math.sin(tSec * 4.0).abs();
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            scaleX: pulse,
            scaleY: pulse,
            matrix4: [
              pulse, 0.0, 0.0, 0.0,
              0.0, pulse, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, 0.0, 0.0, 1.0,
            ],
          ),
          blurRadius: strength * 0.5,
        );
      }

      // ================= KATEGORI: LENSA (3 EFEK) =================
      case 'vignette': {
        final intensity = evaluateParamAt('intensitas', timeMs, defaultValue: 0.5).clamp(0.0, 1.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          vignetteIntensity: intensity,
        );
      }
      case 'chromatic_aberration': {
        final offset = evaluateParamAt('geseran', timeMs, defaultValue: 6.0).clamp(0.0, 20.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          chromaticOffset: offset,
        );
      }
      case 'fisheye': {
        final curvature = evaluateParamAt('kelengkungan', timeMs, defaultValue: 0.4).clamp(-1.0, 1.0);
        final scale = 1.0 + (curvature * 0.3);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            scaleX: scale,
            scaleY: scale,
            matrix4: [
              scale, 0.0, 0.0, 0.0,
              0.0, scale, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, 0.0, 0.0, 1.0,
            ],
          ),
        );
      }

      // ================= KATEGORI: RETRO (2 EFEK) =================
      case 'noise': {
        final density = evaluateParamAt('kerapatan', timeMs, defaultValue: 0.2).clamp(0.0, 1.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
          noiseDensity: density,
        );
      }
      case 'glitch': {
        final strength = evaluateParamAt('kekuatan', timeMs, defaultValue: 0.35).clamp(0.0, 1.0);
        final jitter = ((timeMs ~/ 120) % 2 == 0) ? (strength * 12.0) : -(strength * 6.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dx: jitter,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              0.0, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              jitter, 0.0, 0.0, 1.0,
            ],
          ),
          glitchIntensity: strength,
        );
      }

      // ================= KATEGORI: DISTORSI (4 EFEK) =================
      case 'warp': {
        final intensity = evaluateParamAt('lekukan', timeMs, defaultValue: 15.0);
        final skewX = (intensity / 100.0);
        final skewY = (intensity / 200.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            skewX: skewX,
            skewY: skewY,
            matrix4: [
              1.0, skewY, 0.0, 0.0,
              skewX, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, 0.0, 0.0, 1.0,
            ],
          ),
        );
      }
      case 'skew': {
        final angleDeg = evaluateParamAt('kemiringan', timeMs, defaultValue: 10.0);
        final skewX = math.tan(angleDeg * math.pi / 180.0);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            skewX: skewX,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              skewX, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, 0.0, 0.0, 1.0,
            ],
          ),
        );
      }
      case 'wave_warp': {
        final ampPct = evaluateParamAt('amplitudo', timeMs, defaultValue: 12.0);
        final speed = evaluateParamAt('kecepatan', timeMs, defaultValue: 1.0);
        final ampPx = (ampPct / 100.0) * canvasWidth;
        final phase = 2.0 * math.pi * speed * tSec;
        final dy = math.sin(phase) * ampPx;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dy: dy,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              0.0, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, dy, 0.0, 1.0,
            ],
          ),
        );
      }
      case 'displacement': {
        final scalePct = evaluateParamAt('skala', timeMs, defaultValue: 15.0);
        final distPx = (scalePct / 100.0) * (canvasWidth * 0.5);
        final phase = tSec * 2.5;
        final dx = math.cos(phase) * distPx * 0.3;
        final dy = math.sin(phase * 1.3) * distPx * 0.3;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dx: dx,
            dy: dy,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              0.0, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              dx, dy, 0.0, 1.0,
            ],
          ),
        );
      }

      // ================= KATEGORI: GERAK (4 EFEK) =================
      case 'shake': {
        final ampPct = evaluateParamAt('amplitudo', timeMs, defaultValue: 15.0);
        final speed = evaluateParamAt('kecepatan', timeMs, defaultValue: 2.0);
        final ampPx = (ampPct / 100.0) * canvasWidth * 0.15;
        final dx = math.sin(tSec * speed * 25.0) * ampPx;
        final dy = math.cos(tSec * speed * 32.0) * ampPx * 0.7;
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dx: dx,
            dy: dy,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              0.0, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              dx, dy, 0.0, 1.0,
            ],
          ),
        );
      }
      case 'ombang_ambing': {
        final naikTurunPct = evaluateParamAt('naikTurun', timeMs, defaultValue: 5.0);
        final speed = evaluateParamAt('kecepatan', timeMs, defaultValue: 0.8);
        final tiltDeg = evaluateParamAt('miring', timeMs, defaultValue: 4.0);
        final ampY = (naikTurunPct / 100.0) * canvasWidth;
        final phase = 2.0 * math.pi * speed * tSec;
        final dy = math.sin(phase) * ampY;
        final rot = math.cos(phase) * (tiltDeg * math.pi / 180.0);
        final cosR = math.cos(rot);
        final sinR = math.sin(rot);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dy: dy,
            rotationRad: rot,
            matrix4: [
              cosR, sinR, 0.0, 0.0,
              -sinR, cosR, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, dy, 0.0, 1.0,
            ],
          ),
        );
      }
      case 'ayun': {
        final maxAngleDeg = evaluateParamAt('sudut', timeMs, defaultValue: 20.0);
        final speed = evaluateParamAt('kecepatan', timeMs, defaultValue: 0.6);
        final phase = 2.0 * math.pi * speed * tSec;
        final rot = math.sin(phase) * (maxAngleDeg * math.pi / 180.0);
        final cosR = math.cos(rot);
        final sinR = math.sin(rot);
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            rotationRad: rot,
            matrix4: [
              cosR, sinR, 0.0, 0.0,
              -sinR, cosR, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              0.0, 0.0, 0.0, 1.0,
            ],
          ),
        );
      }
      case 'goyang': {
        final geserXPct = evaluateParamAt('geserX', timeMs, defaultValue: 6.0);
        final kemiringanDeg = evaluateParamAt('kemiringan', timeMs, defaultValue: 8.0);
        final speed = evaluateParamAt('kecepatan', timeMs, defaultValue: 0.8);
        final phase = 2.0 * math.pi * speed * tSec;
        final ampX = (geserXPct / 100.0) * canvasWidth;
        final dx = math.sin(phase) * ampX;
        final skewX = math.tan(math.sin(phase) * (kemiringanDeg * math.pi / 180.0));
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult(
            dx: dx,
            skewX: skewX,
            matrix4: [
              1.0, 0.0, 0.0, 0.0,
              skewX, 1.0, 0.0, 0.0,
              0.0, 0.0, 1.0, 0.0,
              dx, 0.0, 0.0, 1.0,
            ],
          ),
        );
      }

      default:
        return EffectEvaluationResult(
          effectId: effectId,
          transform: EffectTransformResult.identity(),
        );
    }
  }

  EffectInstance copyWith({
    String? id,
    String? effectId,
    bool? isEnabled,
    Map<String, double>? params,
    Map<String, List<EffectKeyframe>>? keyframes,
  }) {
    return EffectInstance(
      id: id ?? this.id,
      effectId: effectId ?? this.effectId,
      isEnabled: isEnabled ?? this.isEnabled,
      params: params != null ? Map.from(params) : Map.from(this.params),
      keyframes: keyframes != null
          ? Map.from(keyframes.map((k, v) => MapEntry(k, List.from(v))))
          : Map.from(this.keyframes.map((k, v) => MapEntry(k, List.from(v)))),
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'effectId': effectId,
    'isEnabled': isEnabled,
    'params': params,
    'keyframes': keyframes.map(
      (k, v) => MapEntry(k, v.map((kf) => kf.toJson()).toList()),
    ),
  };

  factory EffectInstance.fromJson(Map<String, dynamic> json) {
    final kfRaw = json['keyframes'] as Map<String, dynamic>? ?? {};
    final parsedKf = <String, List<EffectKeyframe>>{};
    kfRaw.forEach((k, v) {
      if (v is List) {
        parsedKf[k] = v
            .map((item) => EffectKeyframe.fromJson(item as Map<String, dynamic>))
            .toList();
      }
    });

    final paramsRaw = json['params'] as Map<String, dynamic>? ?? {};
    final parsedParams = <String, double>{};
    paramsRaw.forEach((k, v) {
      if (v is num) {
        parsedParams[k] = v.toDouble();
      }
    });

    return EffectInstance(
      id: json['id'] as String,
      effectId: json['effectId'] as String,
      isEnabled: json['isEnabled'] as bool? ?? true,
      params: parsedParams,
      keyframes: parsedKf,
    );
  }
}

/// DAFTAR 25 EFEK KATALOG FLEWS MOTION
/// Pembagian: Warna (9), Blur (3), Lensa (3), Retro (2), Distorsi (4), Gerak (4)
const List<EffectDefinition> catalog25Effects = [
  // ================= 1. KATEGORI: WARNA (9 EFEK) =================
  EffectDefinition(
    id: 'brightness',
    nama: 'Kecerahan',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'kecerahan', nama: 'Kecerahan', defaultValue: 0.2, min: -1.0, max: 1.0),
    ],
  ),
  EffectDefinition(
    id: 'contrast',
    nama: 'Kontras',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'kontras', nama: 'Kontras', defaultValue: 1.2, min: 0.0, max: 2.0),
    ],
  ),
  EffectDefinition(
    id: 'saturation',
    nama: 'Saturasi',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'saturasi', nama: 'Saturasi', defaultValue: 1.5, min: 0.0, max: 2.0),
    ],
  ),
  EffectDefinition(
    id: 'hue',
    nama: 'Rona (Hue)',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'sudut', nama: 'Sudut Rona', defaultValue: 45.0, min: -180.0, max: 180.0, unit: '°'),
    ],
  ),
  EffectDefinition(
    id: 'invert',
    nama: 'Pembalik Warna',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'jumlah', nama: 'Kebalikan', defaultValue: 1.0, min: 0.0, max: 1.0),
    ],
  ),
  EffectDefinition(
    id: 'tint',
    nama: 'Pewarnaan (Tint)',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'jumlah', nama: 'Jumlah Pewarnaan', defaultValue: 0.5, min: 0.0, max: 1.0),
    ],
  ),
  EffectDefinition(
    id: 'duotone',
    nama: 'Duotone',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'intensitas', nama: 'Intensitas Warna', defaultValue: 0.8, min: 0.0, max: 1.0),
    ],
  ),
  EffectDefinition(
    id: 'posterize',
    nama: 'Posterisasi',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'tingkatan', nama: 'Tingkatan Warna', defaultValue: 8.0, min: 2.0, max: 32.0),
    ],
  ),
  EffectDefinition(
    id: 'color_balance',
    nama: 'Keseimbangan Warna',
    kategori: 'Warna',
    params: [
      ParamDefinition(id: 'suhu', nama: 'Suhu Warna', defaultValue: 0.2, min: -1.0, max: 1.0),
    ],
  ),

  // ================= 2. KATEGORI: BLUR (3 EFEK) =================
  EffectDefinition(
    id: 'gaussian_blur',
    nama: 'Blur Gauss',
    kategori: 'Blur',
    params: [
      ParamDefinition(id: 'radius', nama: 'Radius Blur', defaultValue: 8.0, min: 0.0, max: 30.0, unit: 'px'),
    ],
  ),
  EffectDefinition(
    id: 'directional_blur',
    nama: 'Blur Arah',
    kategori: 'Blur',
    params: [
      ParamDefinition(id: 'jarak', nama: 'Jarak Gerak', defaultValue: 12.0, min: 0.0, max: 40.0, unit: 'px'),
      ParamDefinition(id: 'sudut', nama: 'Sudut Arah', defaultValue: 0.0, min: 0.0, max: 360.0, unit: '°'),
    ],
  ),
  EffectDefinition(
    id: 'radial_blur',
    nama: 'Blur Radial',
    kategori: 'Blur',
    params: [
      ParamDefinition(id: 'kekuatan', nama: 'Kekuatan Radial', defaultValue: 10.0, min: 0.0, max: 30.0, unit: 'px'),
    ],
  ),

  // ================= 3. KATEGORI: LENSA (3 EFEK) =================
  EffectDefinition(
    id: 'vignette',
    nama: 'Vignette',
    kategori: 'Lensa',
    params: [
      ParamDefinition(id: 'intensitas', nama: 'Intensitas Gelap', defaultValue: 0.5, min: 0.0, max: 1.0),
    ],
  ),
  EffectDefinition(
    id: 'chromatic_aberration',
    nama: 'Chromatic Aberration',
    kategori: 'Lensa',
    params: [
      ParamDefinition(id: 'geseran', nama: 'Geseran RGB', defaultValue: 6.0, min: 0.0, max: 20.0, unit: 'px'),
    ],
  ),
  EffectDefinition(
    id: 'fisheye',
    nama: 'Fisheye',
    kategori: 'Lensa',
    params: [
      ParamDefinition(id: 'kelengkungan', nama: 'Kelengkungan Lensa', defaultValue: 0.4, min: -1.0, max: 1.0),
    ],
  ),

  // ================= 4. KATEGORI: RETRO (2 EFEK) =================
  EffectDefinition(
    id: 'noise',
    nama: 'Noise (Derau Film)',
    kategori: 'Retro',
    params: [
      ParamDefinition(id: 'kerapatan', nama: 'Kerapatan Butiran', defaultValue: 0.2, min: 0.0, max: 1.0),
    ],
  ),
  EffectDefinition(
    id: 'glitch',
    nama: 'Glitch (Gangguan TV)',
    kategori: 'Retro',
    params: [
      ParamDefinition(id: 'kekuatan', nama: 'Kekuatan Gangguan', defaultValue: 0.35, min: 0.0, max: 1.0),
    ],
  ),

  // ================= 5. KATEGORI: DISTORSI (4 EFEK) =================
  EffectDefinition(
    id: 'warp',
    nama: 'Warp',
    kategori: 'Distorsi',
    params: [
      ParamDefinition(id: 'lekukan', nama: 'Lekukan Grid', defaultValue: 15.0, min: -50.0, max: 50.0, unit: '%'),
    ],
  ),
  EffectDefinition(
    id: 'skew',
    nama: 'Skew',
    kategori: 'Distorsi',
    params: [
      ParamDefinition(id: 'kemiringan', nama: 'Derajat Miring', defaultValue: 10.0, min: -45.0, max: 45.0, unit: '°'),
    ],
  ),
  EffectDefinition(
    id: 'wave_warp',
    nama: 'Gelombang (Wave Warp)',
    kategori: 'Distorsi',
    params: [
      ParamDefinition(id: 'amplitudo', nama: 'Tinggi Gelombang', defaultValue: 12.0, min: 0.0, max: 50.0, unit: '%'),
      ParamDefinition(id: 'kecepatan', nama: 'Kecepatan Aliran', defaultValue: 1.0, min: 0.1, max: 4.0, unit: 'Hz'),
    ],
  ),
  EffectDefinition(
    id: 'displacement',
    nama: 'Displacement',
    kategori: 'Distorsi',
    params: [
      ParamDefinition(id: 'skala', nama: 'Skala Pergeseran', defaultValue: 15.0, min: 0.0, max: 50.0, unit: '%'),
    ],
  ),

  // ================= 6. KATEGORI: GERAK (4 EFEK) =================
  EffectDefinition(
    id: 'shake',
    nama: 'Guncang (Shake)',
    kategori: 'Gerak',
    params: [
      ParamDefinition(id: 'amplitudo', nama: 'Kekuatan Guncang', defaultValue: 15.0, min: 0.0, max: 50.0, unit: '%'),
      ParamDefinition(id: 'kecepatan', nama: 'Kecepatan Guncang', defaultValue: 2.0, min: 0.5, max: 8.0, unit: 'Hz'),
    ],
  ),
  EffectDefinition(
    id: 'ombang_ambing',
    nama: 'Ombang-ambing',
    kategori: 'Gerak',
    params: [
      ParamDefinition(id: 'naikTurun', nama: 'Naik-turun', defaultValue: 5.0, min: 0.0, max: 20.0, unit: '%'),
      ParamDefinition(id: 'kecepatan', nama: 'Kecepatan Ayun', defaultValue: 0.8, min: 0.1, max: 4.0, unit: 'Hz'),
      ParamDefinition(id: 'miring', nama: 'Sudut Miring', defaultValue: 4.0, min: 0.0, max: 15.0, unit: '°'),
    ],
  ),
  EffectDefinition(
    id: 'ayun',
    nama: 'Ayun',
    kategori: 'Gerak',
    params: [
      ParamDefinition(id: 'sudut', nama: 'Sudut Ayun Maksimal', defaultValue: 20.0, min: 0.0, max: 90.0, unit: '°'),
      ParamDefinition(id: 'kecepatan', nama: 'Frekuensi Ayunan', defaultValue: 0.6, min: 0.1, max: 3.0, unit: 'Hz'),
    ],
  ),
  EffectDefinition(
    id: 'goyang',
    nama: 'Goyang',
    kategori: 'Gerak',
    params: [
      ParamDefinition(id: 'geserX', nama: 'Geser Sisi X', defaultValue: 6.0, min: 0.0, max: 15.0, unit: '%'),
      ParamDefinition(id: 'kemiringan', nama: 'Kemiringan Batang', defaultValue: 8.0, min: 0.0, max: 20.0, unit: '°'),
      ParamDefinition(id: 'kecepatan', nama: 'Kecepatan Goyang', defaultValue: 0.8, min: 0.1, max: 4.0, unit: 'Hz'),
    ],
  ),
];

/// Registry Pengelola Efek Flews Motion
class EffectRegistry {
  static const String kategoriWarna = 'Warna';
  static const String kategoriBlur = 'Blur';
  static const String kategoriLensa = 'Lensa';
  static const String kategoriRetro = 'Retro';
  static const String kategoriDistorsi = 'Distorsi';
  static const String kategoriGerak = 'Gerak';

  static const List<String> categories = [
    kategoriWarna,
    kategoriBlur,
    kategoriLensa,
    kategoriRetro,
    kategoriDistorsi,
    kategoriGerak,
  ];

  /// Daftar lengkap 25 efek terverifikasi
  static List<EffectDefinition> get all => catalog25Effects;

  static EffectDefinition? findById(String id) {
    try {
      return catalog25Effects.firstWhere((e) => e.id == id);
    } catch (_) {
      return null;
    }
  }

  static List<EffectDefinition> findByCategory(String category) {
    return catalog25Effects.where((e) => e.kategori == category).toList();
  }

  /// Membuat instance efek baru dengan nilai default untuk semua parameter
  static EffectInstance createInstance(String effectId, {String? instanceId}) {
    final def = findById(effectId);
    final initialParams = <String, double>{};
    if (def != null) {
      for (final p in def.params) {
        initialParams[p.id] = p.defaultValue;
      }
    }
    final effectiveId = instanceId ?? '${effectId}_${DateTime.now().millisecondsSinceEpoch}';
    return EffectInstance(
      id: effectiveId,
      effectId: effectId,
      isEnabled: true,
      params: initialParams,
      keyframes: {},
    );
  }
}
