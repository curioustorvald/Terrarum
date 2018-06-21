#version 120
#ifdef GL_ES
    precision mediump float;
#endif


varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform vec2 resolution;

uniform vec3 phosphor_colour = vec3(1.3, 0.8567, 0.0);
vec3 scanline_darkening = vec3(0.45, 0.45, 0.45);

// 0: every odd line will get darkened; 1: every even line will get darkened
uniform float alternative_scanline = 0.0; // 1.0: true

uniform float blur_blend = 0.3;

void main(void) {
    vec3 color = texture2D(u_texture, v_texCoords).rgb;
    vec3 color_pre =  texture2D(u_texture, (gl_FragCoord + vec2(-1.0, 0.0)) / resolution).rgb;
    vec3 color_next = texture2D(u_texture, (gl_FragCoord + vec2( 1.0, 0.0)) / resolution).rgb;

    color = color * (1.0 - blur_blend) + color_pre * (blur_blend / 2.0) + color_next * (blur_blend / 2.0);

    bool is_scanline = mod(int(gl_FragCoord.y), 2) == int(alternative_scanline);

    float color_luminosity = (
        3.0 * color.r +
        4.0 * color.g +
        1.0 * color.b
    ) / 8.0;

    // out colour
    color = vec3(color_luminosity) * phosphor_colour;

    if (is_scanline) {
        color = color * scanline_darkening;
    }

    gl_FragColor = vec4(color, 1.0);
}