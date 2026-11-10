package com.aashik.music.ui.mobile.glass

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Native AGSL (Android Graphics Shading Language) Liquid Glass Shader.
 * Recreates the exact shader architecture from Kyant0/AndroidLiquidGlass:
 * - Signed Distance Field (SDF) rounded rectangle boundary calculation
 * - Optical lens refraction (bending background light based on surface curvature)
 * - Chromatic aberration (RGB dispersion along lens bevel)
 * - Specular rim lighting and Fresnel highlight
 */
object MobileLiquidGlassShader {

    private const val AGSL_LIQUID_GLASS = """
        uniform shader composable;
        uniform float2 size;
        uniform float cornerRadius;
        uniform float refraction;
        uniform float chromaticAberration;
        uniform float4 tintColor;
        uniform float4 specularColor;

        float sdRoundedBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        half4 main(float2 fragCoord) {
            float2 halfSize = size * 0.5;
            float2 p = fragCoord - halfSize;
            float d = sdRoundedBox(p, halfSize, cornerRadius);

            if (d > 0.0) {
                return half4(0.0);
            }

            float edgeDist = -d;
            float bevel = smoothstep(0.0, 24.0, edgeDist);
            float refractFactor = (1.0 - bevel) * refraction;

            float2 dir = normalize(p + float2(0.0001, 0.0001));
            float2 sampleCoord = fragCoord - dir * refractFactor;

            // Chromatic aberration RGB splitting
            float ca = (1.0 - bevel) * chromaticAberration;
            half4 colorR = composable.eval(sampleCoord + dir * ca);
            half4 colorG = composable.eval(sampleCoord);
            half4 colorB = composable.eval(sampleCoord - dir * ca);
            half4 color = half4(colorR.r, colorG.g, colorB.b, colorG.a);

            // Specular rim light (Fresnel effect)
            float rimLight = pow(1.0 - bevel, 2.5) * 0.55;
            float topHighlight = smoothstep(-2.0, 0.0, d) * smoothstep(cornerRadius * 1.5, 0.0, fragCoord.y);

            color = mix(color, tintColor, tintColor.a);
            color.rgb += specularColor.rgb * (rimLight + topHighlight * 0.40);

            return color;
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var runtimeShader: RuntimeShader? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getShader(): RuntimeShader {
        var shader = runtimeShader
        if (shader == null) {
            shader = RuntimeShader(AGSL_LIQUID_GLASS)
            runtimeShader = shader
        }
        return shader
    }

    /**
     * Creates a hardware-accelerated RenderEffect combining blur, refraction, and specular highlight.
     */
    fun createLiquidGlassRenderEffect(
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        blurRadiusPx: Float = 40f,
        refractionPx: Float = 14f,
        chromaticAberrationPx: Float = 4f
    ): androidx.compose.ui.graphics.RenderEffect? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && widthPx > 0f && heightPx > 0f) {
            return try {
                val shader = getShader()
                shader.setFloatUniform("size", widthPx, heightPx)
                shader.setFloatUniform("cornerRadius", cornerRadiusPx)
                shader.setFloatUniform("refraction", refractionPx)
                shader.setFloatUniform("chromaticAberration", chromaticAberrationPx)
                // Translucent acrylic tint (subtle obsidian ice)
                shader.setFloatUniform("tintColor", 0.06f, 0.08f, 0.12f, 0.70f)
                // Specular rim highlight (pure white / cyan rim)
                shader.setFloatUniform("specularColor", 0.85f, 0.95f, 1.0f, 1.0f)

                val blur = RenderEffect.createBlurEffect(
                    blurRadiusPx,
                    blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
                val glassEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                RenderEffect.createChainEffect(glassEffect, blur).asComposeRenderEffect()
            } catch (e: Throwable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP).asComposeRenderEffect()
                } else null
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusPx > 0f) {
            return try {
                RenderEffect.createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP).asComposeRenderEffect()
            } catch (e: Throwable) {
                null
            }
        }
        return null
    }
}

